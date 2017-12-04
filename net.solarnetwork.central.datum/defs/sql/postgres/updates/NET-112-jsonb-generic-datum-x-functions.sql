DROP FUNCTION solardatum.find_most_recent(bigint, text[]);
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(
	node bigint,
	sources text[] DEFAULT NULL)
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

DROP FUNCTION solardatum.find_most_recent(bigint[]);
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(nodes bigint[])
  RETURNS SETOF solardatum.da_datum AS
$BODY$
	SELECT r.*
	FROM (SELECT unnest(nodes) AS node_id) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent(n.node_id)) AS r
	ORDER BY r.node_id, r.source_id;
$BODY$
  LANGUAGE sql STABLE;

DROP FUNCTION solardatum.store_datum(timestamp with time zone, bigint, text, timestamp with time zone, text);
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
BEGIN
	INSERT INTO solardatum.da_datum(ts, node_id, source_id, posted, jdata_i, jdata_a, jdata_s, jdata_t)
	VALUES (ts_crea, node, src, ts_post, jdata_json->'i', jdata_json->'a', jdata_json->'s', jdata_json->'t')
	ON CONFLICT (node_id, ts, source_id) DO UPDATE
	SET jdata_i = EXCLUDED.jdata_i,
		jdata_a = EXCLUDED.jdata_a,
		jdata_s = EXCLUDED.jdata_s,
		jdata_t = EXCLUDED.jdata_t,
		posted = EXCLUDED.posted;

	INSERT INTO solaragg.aud_datum_hourly (
		ts_start, node_id, source_id, prop_count)
	VALUES (ts_post_hour, node, src, jdata_prop_count)
	ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
	SET prop_count = aud_datum_hourly.prop_count + EXCLUDED.prop_count;
END;
$BODY$;
