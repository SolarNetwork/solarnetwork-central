-- solardatum datum functions that rely on other namespaces like solaragg

/**
 * Return most recent datum records for a specific set of sources for a given node.
 *
 * @param node The node ID to return results for.
 * @param sources The source IDs to return results for, or <code>null</code> for all available sources.
 * @returns Set of solardatum.da_datum records.
 */
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(
	node bigint,
	sources text[] DEFAULT NULL)
  RETURNS SETOF solardatum.da_datum_data AS
$BODY$
	SELECT dd.* FROM solardatum.da_datum_data dd
	INNER JOIN (
		-- to speed up query for sources (which can be very slow when queried directly on da_datum),
		-- we find the most recent hour time slot in agg_datum_hourly, and then join to da_datum with that narrow time range
		SELECT max(d.ts) as ts, d.source_id FROM solardatum.da_datum d
		INNER JOIN (SELECT node_id, ts_start, source_id FROM solaragg.find_most_recent_hourly(node, sources)) AS days
			ON days.node_id = d.node_id
				AND days.ts_start <= d.ts
				AND days.ts_start + interval '1 hour' > d.ts
				AND days.source_id = d.source_id
		GROUP BY d.source_id
	) AS r ON r.ts = dd.ts AND r.source_id = dd.source_id AND dd.node_id = node
	ORDER BY dd.source_id ASC;
$BODY$
  LANGUAGE sql STABLE
  ROWS 20;

/**
 * Return most recent datum records for all available sources for a given set of node IDs.
 *
 * @param nodes An array of node IDs to return results for.
 * @returns Set of solardatum.da_datum records.
 */
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(nodes bigint[])
  RETURNS SETOF solardatum.da_datum_data AS
$BODY$
	SELECT r.*
	FROM (SELECT unnest(nodes) AS node_id) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent(n.node_id)) AS r
	ORDER BY r.node_id, r.source_id;
$BODY$
  LANGUAGE sql STABLE;

/**
 * Add or update a datum record. The data is stored in the <code>solardatum.da_datum</code> table.
 *
 * @param cdate The datum creation date.
 * @param node The node ID.
 * @param src The source ID.
 * @param pdate The date the datum was posted to SolarNet.
 * @param jdata The datum JSON document.
 */
CREATE OR REPLACE FUNCTION solardatum.store_datum(
	cdate timestamp with time zone,
	node bigint,
	src text,
	pdate timestamp with time zone,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	ts_crea timestamp with time zone := COALESCE(cdate, now());
	ts_post timestamp with time zone := COALESCE(pdate, now());
	jdata_json jsonb := jdata::jsonb;
	jdata_prop_count integer := solardatum.datum_prop_count(jdata_json);
	ts_post_hour timestamp with time zone := date_trunc('hour', ts_post);
	is_insert boolean := false;
BEGIN
	INSERT INTO solardatum.da_datum(ts, node_id, source_id, posted, jdata_i, jdata_a, jdata_s, jdata_t)
	VALUES (ts_crea, node, src, ts_post, jdata_json->'i', jdata_json->'a', jdata_json->'s', solarcommon.json_array_to_text_array(jdata_json->'t'))
	ON CONFLICT (node_id, ts, source_id) DO UPDATE
	SET jdata_i = EXCLUDED.jdata_i,
		jdata_a = EXCLUDED.jdata_a,
		jdata_s = EXCLUDED.jdata_s,
		jdata_t = EXCLUDED.jdata_t,
		posted = EXCLUDED.posted
	RETURNING (xmax = 0)
	INTO is_insert;

	INSERT INTO solaragg.aud_datum_hourly (
		ts_start, node_id, source_id, datum_count, prop_count)
	VALUES (ts_post_hour, node, src, 1, jdata_prop_count)
	ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
	SET datum_count = aud_datum_hourly.datum_count + (CASE is_insert WHEN TRUE THEN 1 ELSE 0 END),
		prop_count = aud_datum_hourly.prop_count + EXCLUDED.prop_count;
END;
$BODY$;

/**
 * Increment a datum query count for a single source.
 *
 * @param qdate The datum query date.
 * @param node The node ID.
 * @param source The source ID.
 * @param dcount The count of datum to increment by.
 */
CREATE OR REPLACE FUNCTION solaragg.aud_inc_datum_query_count(
	qdate timestamp with time zone,
	node bigint,
	source text,
	dcount integer)
	RETURNS void LANGUAGE sql VOLATILE AS
$BODY$
	INSERT INTO solaragg.aud_datum_hourly(ts_start, node_id, source_id, datum_q_count)
	VALUES (date_trunc('hour', qdate), node, source, dcount)
	ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
	SET datum_q_count = aud_datum_hourly.datum_q_count + EXCLUDED.datum_q_count;
$BODY$;
