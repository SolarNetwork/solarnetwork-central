-- solardatum datum functions that rely on other namespaces like solaragg

/**
 * Return most recent datum records for a specific set of sources for a given node.
 *
 * @param node The node ID to return results for.
 * @param sources The source IDs to return results for, or <code>null</code> for all available sources.
 * @returns Set of solardatum.da_datum records.
 */
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(
	node solarcommon.node_id,
	sources solarcommon.source_ids DEFAULT NULL)
  RETURNS SETOF solardatum.da_datum AS
$BODY$
	SELECT dd.* FROM solardatum.da_datum dd
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
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(nodes solarcommon.node_ids)
  RETURNS SETOF solardatum.da_datum AS
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
	cdate solarcommon.ts,
	node solarcommon.node_id,
	src solarcommon.source_id,
	pdate solarcommon.ts,
	jdata text)
  RETURNS void AS
$BODY$
DECLARE
	ts_crea solarcommon.ts := COALESCE(cdate, now());
	ts_post solarcommon.ts := COALESCE(pdate, now());
	jdata_json json := jdata::json;
	jdata_prop_count integer := solardatum.datum_prop_count(jdata_json);
	ts_post_hour timestamp with time zone := date_trunc('hour', ts_post);
BEGIN
	BEGIN
		INSERT INTO solardatum.da_datum(ts, node_id, source_id, posted, jdata)
		VALUES (ts_crea, node, src, ts_post, jdata_json);
	EXCEPTION WHEN unique_violation THEN
		-- We mostly expect inserts, but we allow updates
		UPDATE solardatum.da_datum SET
			jdata = jdata_json,
			posted = ts_post
		WHERE
			node_id = node
			AND ts = ts_crea
			AND source_id = src;
	END;

	-- for auditing we mostly expect updates
	<<update_audit>>
	LOOP
		UPDATE solaragg.aud_datum_hourly
		SET prop_count = prop_count + jdata_prop_count
		WHERE
			node_id = node
			AND source_id = src
			AND ts_start = ts_post_hour;

		EXIT update_audit WHEN FOUND;

		INSERT INTO solaragg.aud_datum_hourly (
			ts_start, node_id, source_id, prop_count)
		VALUES (
			ts_post_hour,
			node,
			src,
			jdata_prop_count
		);
		EXIT update_audit;
	END LOOP update_audit;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE;
