/**
 * TABLE solardatum.da_datum_most_recent
 *
 * Table with one row per node+source combination, with an associated timestamp that is meant to 
 * be the "most recent" timestamp for that node+source in the `solardatum.da_datum` table. This
 * is used for query performance only, as finding the most recent data directly from `solardatum.da_datum`
 * can be prohibitively expensive.
 *
 * @see solardatum.update_datum_most_recent(bigint, character varying(64), timestamp with time zone)
 */
CREATE TABLE IF NOT EXISTS solardatum.da_datum_most_recent (
  ts timestamp with time zone NOT NULL,
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  CONSTRAINT da_datum_most_recent_pkey PRIMARY KEY (node_id, source_id)
);

-- provide initial population of most-recent rows
WITH d AS (
	SELECT r.ts, r.node_id, r.source_id
	FROM (SELECT node_id FROM solarnet.sn_node) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent(n.node_id)) AS r
)
INSERT INTO solardatum.da_datum_most_recent (ts, node_id, source_id)
SELECT ts, node_id, source_id FROM d
ON CONFLICT (node_id, source_id) DO NOTHING;

/**
 * FUNCTION solardatum.update_datum_most_recent(bigint, character varying(64), timestamp with time zone)
 *
 * Update the "most recent" date of the `solardatum.da_datum_most_recent` table to `mrts` if and only if
 * the current value is less than `mrts`.
 *
 * @param node the node ID
 * @param source the source ID
 * @param mrts the timestamp of the data, possibly the "most recent" date
 */
CREATE OR REPLACE FUNCTION solardatum.update_datum_most_recent(node bigint, source character varying(64), mrts timestamp with time zone)
RETURNS VOID LANGUAGE SQL VOLATILE AS
$$
	-- strive to avoid inserting if the date is actually not the most recent, and support
	-- inserting if no row exists yet
	INSERT INTO solardatum.da_datum_most_recent (node_id, source_id, ts)
	WITH mr AS (
		SELECT node_id, source_id, ts, 0 as pos
		FROM solardatum.da_datum_most_recent
		WHERE node_id = node AND source_id = source
		UNION ALL
		SELECT node AS node_id, source AS source_id, mrts AS ts, 1 as pos
	)
	, ranked AS (
		SELECT *, max(ts) OVER () AS ts_max
		FROM mr
		ORDER BY pos
	)
	, combined AS (
		SELECT node_id, source_id, ts
		FROM ranked WHERE (pos = 0 AND ts < mrts) OR (pos = 1 AND ts = ts_max)
		ORDER BY pos LIMIT 1
	)
	SELECT node_id, source_id, mrts FROM combined
	ON CONFLICT (node_id, source_id) DO UPDATE
	SET ts = EXCLUDED.ts
$$;


-- add call to solardatum.update_datum_most_recent when inserting datum
DROP FUNCTION IF EXISTS solardatum.store_datum(
	 timestamp with time zone,
	 bigint,
	 text,
	 timestamp with time zone,
	 text);
CREATE OR REPLACE FUNCTION solardatum.store_datum(
	cdate timestamp with time zone,
	node bigint,
	src text,
	pdate timestamp with time zone,
	jdata text,
	track_recent boolean DEFAULT TRUE)
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
		
	IF track_recent AND is_insert THEN
		PERFORM solardatum.update_datum_most_recent(node, src, cdate);
	END IF;
END;
$BODY$;

CREATE OR REPLACE FUNCTION solardatum.find_most_recent_direct(node bigint)
RETURNS SETOF solardatum.da_datum_data LANGUAGE SQL STABLE ROWS 20 AS
$$
	-- first look for most recent hours, because this more quickly narrows down the time range for each source
	WITH hours AS (
		SELECT max(d.ts_start) as ts_start, d.source_id, node AS node_id
		FROM solaragg.agg_datum_hourly d
		WHERE d. node_id = node
		GROUP BY d.source_id
	)
	-- next find the exact maximum time per source within each found hour, which is an index-only scan so quick
	, maxes AS (
		SELECT max(d.ts) AS ts, d.source_id, node AS node_id
		FROM solardatum.da_datum d
		INNER JOIN hours ON d.node_id = hours.node_id AND d.source_id = hours.source_id AND d.ts >= hours.ts_start AND d.ts < hours.ts_start + interval '1 hour'
		GROUP BY d.source_id
	)
	-- finally query the raw data using the exact found timestamps, so loop over each (ts,source) tuple found in maxes
	SELECT d.* 
	FROM solardatum.da_datum_data d
	INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts = d.ts
	ORDER BY d.source_id ASC
$$;

CREATE OR REPLACE FUNCTION solardatum.find_most_recent(node bigint)
RETURNS SETOF solardatum.da_datum_data LANGUAGE SQL STABLE ROWS 20 AS
$$
	SELECT d.*
	FROM  solardatum.da_datum_most_recent mr
	INNER JOIN solardatum.da_datum_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts = mr.ts
	WHERE mr.node_id = node
	ORDER BY d.source_id
$$;

CREATE OR REPLACE FUNCTION solardatum.find_most_recent_direct(node bigint, sources text[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE SQL STABLE ROWS 50 AS
$$
	-- first look for most recent hours, because this more quickly narrows down the time range for each source
	WITH hours AS (
		SELECT max(d.ts_start) as ts_start, d.source_id, node AS node_id
		FROM solaragg.agg_datum_hourly d
		INNER JOIN (SELECT unnest(sources) AS source_id) AS s ON s.source_id = d.source_id
		WHERE d. node_id = node
		GROUP BY d.source_id
	)
	-- next find the exact maximum time per source within each found hour, which is an index-only scan so quick
	, maxes AS (
		SELECT max(d.ts) AS ts, d.source_id, node AS node_id
		FROM solardatum.da_datum d
		INNER JOIN hours ON d.node_id = hours.node_id AND d.source_id = hours.source_id AND d.ts >= hours.ts_start AND d.ts < hours.ts_start + interval '1 hour'
		GROUP BY d.source_id
	)
	-- finally query the raw data using the exact found timestamps, so loop over each (ts,source) tuple found in maxes
	SELECT d.* 
	FROM solardatum.da_datum_data d
	INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts = d.ts
	ORDER BY d.source_id ASC
$$;

DROP FUNCTION IF EXISTS solardatum.find_most_recent(bigint, text[]);
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(node bigint, sources text[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE SQL STABLE ROWS 50 AS
$$
	SELECT d.*
	FROM  solardatum.da_datum_most_recent mr
	INNER JOIN solardatum.da_datum_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts = mr.ts
	WHERE mr.node_id = node
		AND mr.source_id = ANY(sources)
	ORDER BY d.source_id
$$;

CREATE OR REPLACE FUNCTION solardatum.find_most_recent_direct(nodes bigint[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT r.*
	FROM (SELECT unnest(nodes) AS node_id) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent(n.node_id)) AS r
	ORDER BY r.node_id, r.source_id;
$$;

CREATE OR REPLACE FUNCTION solardatum.find_most_recent(nodes bigint[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.*
	FROM  solardatum.da_datum_most_recent mr
	INNER JOIN solardatum.da_datum_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts = mr.ts
	WHERE mr.node_id = ANY(nodes)
	ORDER BY d.node_id, d.source_id
$$;

CREATE OR REPLACE FUNCTION solaruser.find_most_recent_datum_for_user(users bigint[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT r.*
	FROM (SELECT node_id FROM solaruser.user_node WHERE user_id = ANY(users)) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent(n.node_id)) AS r
	ORDER BY r.node_id, r.source_id;
$$;

CREATE OR REPLACE FUNCTION solaruser.find_most_recent_datum_for_user(users bigint[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.*
	FROM solaruser.user_node un
	INNER JOIN solardatum.da_datum_most_recent mr ON mr.node_id = un.node_id
	INNER JOIN solardatum.da_datum_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts = mr.ts
	WHERE un.user_id = ANY(users)
	ORDER BY d.node_id, d.source_id
$$;
