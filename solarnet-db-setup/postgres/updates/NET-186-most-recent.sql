/**
 * TABLE solardatum.da_datum_range
 *
 * Table with one row per node+source combination, with associated timestamps that are meant to 
 * be the "most/least recent" timestamps for that node+source in the `solardatum.da_datum` table. This
 * is used for query performance only, as finding the most recent data directly from `solardatum.da_datum`
 * can be prohibitively expensive.
 *
 * @see solardatum.update_datum_most_recent(bigint, character varying(64), timestamp with time zone)
 * @see solardatum.update_datum_least_recent(bigint, character varying(64), timestamp with time zone)
 */
CREATE TABLE IF NOT EXISTS solardatum.da_datum_range (
  ts_min timestamp with time zone NOT NULL,
  ts_max timestamp with time zone NOT NULL,
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  CONSTRAINT da_datum_range_pkey PRIMARY KEY (node_id, source_id)
);

/**
 * FUNCTION solardatum.update_datum_range_dates(bigint, character varying(64), timestamp with time zone)
 *
 * Update the "most/least recent" dates of the `solardatum.da_datum_range` table to `rdate` if and only if
 * the current value is less/more than `rdate`.
 *
 * @param node the node ID
 * @param source the source ID
 * @param rdate the timestamp of the data, possibly the "most/least recent" date
 */
CREATE OR REPLACE FUNCTION solardatum.update_datum_range_dates(node bigint, source character varying(64), rdate timestamp with time zone)
RETURNS VOID LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	tsmin timestamp with time zone;
	tsmax timestamp with time zone;
BEGIN
	SELECT ts_min, ts_max
	FROM solardatum.da_datum_range
	WHERE node_id = node AND source_id = source
	INTO tsmin, tsmax;

	IF tsmin IS NULL THEN
		INSERT INTO solardatum.da_datum_range (ts_min, ts_max, node_id, source_id)
		VALUES (rdate, rdate, node, source)
		ON CONFLICT (node_id, source_id) DO NOTHING;
	ELSEIF rdate > tsmax THEN
		UPDATE solardatum.da_datum_range SET ts_max = rdate
		WHERE node_id = node AND source_id = source;
	ELSEIF rdate < tsmin THEN
		UPDATE solardatum.da_datum_range SET ts_min = rdate
		WHERE node_id = node AND source_id = source;
	END IF;

END;
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
		PERFORM solardatum.update_datum_range_dates(node, src, cdate);
	END IF;
END;
$BODY$;

/**
 * FUNCTION solardatum.find_least_recent_direct(bigint)
 * 
 * Find the smallest available dates for all source IDs for the given node ID. This does **not**
 * use the `solardatum.da_datum_range` table.
 *
 * @param node the node ID to find
 */
CREATE OR REPLACE FUNCTION solardatum.find_least_recent_direct(node bigint)
RETURNS SETOF solardatum.da_datum_data LANGUAGE SQL STABLE ROWS 20 AS
$$
	-- first look for least recent hours, because this more quickly narrows down the time range for each source
	WITH hours AS (
		SELECT min(d.ts_start) as ts_start, d.source_id, node AS node_id
		FROM solaragg.agg_datum_hourly d
		WHERE d. node_id = node
		GROUP BY d.source_id
	)
	-- next find the exact maximum time per source within each found hour, which is an index-only scan so quick
	, mins AS (
		SELECT min(d.ts) AS ts, d.source_id, node AS node_id
		FROM solardatum.da_datum d
		INNER JOIN hours ON d.node_id = hours.node_id AND d.source_id = hours.source_id AND d.ts >= hours.ts_start AND d.ts < hours.ts_start + interval '1 hour'
		GROUP BY d.source_id
	)
	-- finally query the raw data using the exact found timestamps, so loop over each (ts,source) tuple found in mins
	SELECT d.* 
	FROM solardatum.da_datum_data d
	INNER JOIN mins ON mins.node_id = d.node_id AND mins.source_id = d.source_id AND mins.ts = d.ts
	ORDER BY d.source_id ASC
$$;

/**
 * FUNCTION solardatum.find_most_recent_direct(bigint)
 * 
 * Find the highest available dates for all source IDs for the given node ID. This does **not**
 * use the `solardatum.da_datum_range` table.
 *
 * @param node the node ID to find
 */
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

/**
 * FUNCTION solardatum.find_most_recent(bigint)
 * 
 * Find the highest available dates for all source IDs for the given node ID. This query relies on
 * the `solardatum.da_datum_range` table.
 *
 * @param node the node ID to find
 */
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(node bigint)
RETURNS SETOF solardatum.da_datum_data LANGUAGE SQL STABLE ROWS 20 AS
$$
	SELECT d.*
	FROM  solardatum.da_datum_range mr
	INNER JOIN solardatum.da_datum_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts = mr.ts_max
	WHERE mr.node_id = node
	ORDER BY d.source_id
$$;

/**
 * FUNCTION solardatum.find_most_recent_direct(bigint, text[])
 * 
 * Find the highest available dates for the given source IDs for the given node ID. This query does **not** rely on
 * the `solardatum.da_datum_range` table.
 *
 * @param node the node ID to find
 * @param sources the source IDs to find
 */
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

/**
 * FUNCTION solardatum.find_most_recent(bigint, text[])
 * 
 * Find the highest available dates for the given source IDs for the given node ID. This query relies on
 * the `solardatum.da_datum_range` table.
 *
 * @param node the node ID to find
 * @param sources the source IDs to find
 */
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(node bigint, sources text[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE SQL STABLE ROWS 50 AS
$$
	SELECT d.*
	FROM  solardatum.da_datum_range mr
	INNER JOIN solardatum.da_datum_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts = mr.ts_max
	WHERE mr.node_id = node
		AND mr.source_id = ANY(sources)
	ORDER BY d.source_id
$$;

/**
 * FUNCTION solardatum.find_most_recent_direct(bigint[])
 * 
 * Find the highest available dates for all source IDs for the given node IDs. This query does **not** rely on
 * the `solardatum.da_datum_range` table.
 *
 * @param nodes the node IDs to find
 */
CREATE OR REPLACE FUNCTION solardatum.find_most_recent_direct(nodes bigint[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT r.*
	FROM (SELECT unnest(nodes) AS node_id) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent_direct(n.node_id)) AS r
	ORDER BY r.node_id, r.source_id;
$$;

/**
 * FUNCTION solardatum.find_most_recent(bigint[])
 * 
 * Find the highest available dates for all source IDs for the given node IDs. This query relies on
 * the `solardatum.da_datum_range` table.
 *
 * @param nodes the node IDs to find
 */
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(nodes bigint[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.*
	FROM  solardatum.da_datum_range mr
	INNER JOIN solardatum.da_datum_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts = mr.ts_max
	WHERE mr.node_id = ANY(nodes)
	ORDER BY d.node_id, d.source_id
$$;

/**
 * FUNCTION solaruser.find_most_recent_datum_for_user_direct(bigint[])
 * 
 * Find the highest available dates for all source IDs for all node IDs owned by the given user IDs.
 * This query does **not** rely on the `solardatum.da_datum_range` table.
 *
 * @param users the user IDs to find
 */
CREATE OR REPLACE FUNCTION solaruser.find_most_recent_datum_for_user_direct(users bigint[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT r.*
	FROM (SELECT node_id FROM solaruser.user_node WHERE user_id = ANY(users)) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent_direct(n.node_id)) AS r
	ORDER BY r.node_id, r.source_id;
$$;

/**
 * FUNCTION solaruser.find_most_recent_datum_for_user_direct(bigint[])
 * 
 * Find the highest available dates for all source IDs for all node IDs owned by the given user IDs.
 * This query relies on the `solardatum.da_datum_range` table.
 *
 * @param users the user IDs to find
 */
CREATE OR REPLACE FUNCTION solaruser.find_most_recent_datum_for_user(users bigint[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.*
	FROM solaruser.user_node un
	INNER JOIN solardatum.da_datum_range mr ON mr.node_id = un.node_id
	INNER JOIN solardatum.da_datum_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts = mr.ts_max
	WHERE un.user_id = ANY(users)
	ORDER BY d.node_id, d.source_id
$$;

CREATE OR REPLACE FUNCTION solardatum.find_reportable_interval(
	IN node bigint,
	IN src text DEFAULT NULL,
	OUT ts_start timestamp with time zone,
	OUT ts_end timestamp with time zone,
	OUT node_tz TEXT,
	OUT node_tz_offset INTEGER)
  RETURNS RECORD LANGUAGE SQL STABLE AS
$$
	WITH range AS (
		SELECT min(r.ts_min) AS ts_start, max(r.ts_max) AS ts_end
		FROM solardatum.da_datum_range r
		WHERE r.node_id = node
			AND (src IS NULL OR r.source_id = src)
	)
	SELECT r.ts_start
		, r.ts_end
		, COALESCE(l.time_zone, 'UTC') AS node_tz
		, COALESCE(CAST(EXTRACT(epoch FROM z.utc_offset) / 60 AS INTEGER), 0) AS node_tz_offset
	FROM range r
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = node
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	LEFT OUTER JOIN pg_timezone_names z ON z.name = l.time_zone
$$;

/**
 * FUNCTION solardatum.find_reportable_intervals(bigint[])
 *
 * Get the min/max date range for all source IDs of a given set of node IDs.
 *
 * @param nodes the node IDs to find
 */
CREATE OR REPLACE FUNCTION solardatum.find_reportable_intervals(nodes bigint[])
RETURNS TABLE (
	node_id bigint,
	source_id text,
	ts_start timestamp with time zone,
	ts_end timestamp with time zone,
	node_tz text,
	node_tz_offset integer
) LANGUAGE SQL STABLE ROWS 50 AS
$$
	WITH range AS (
		SELECT r.node_id, r.source_id, min(r.ts_min) AS ts_min, max(r.ts_max) AS ts_max
		FROM solardatum.da_datum_range r
		WHERE r.node_id = ANY(nodes)
		GROUP BY r.node_id, r.source_id
	)
	SELECT r.node_id
		, r.source_id
		, r.ts_min AS ts_start
		, r.ts_max AS ts_end
		, COALESCE(l.time_zone, 'UTC') AS node_tz
		, COALESCE(CAST(EXTRACT(epoch FROM z.utc_offset) / 60 AS INTEGER), 0) AS node_tz_offset
	FROM range r
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = r.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	LEFT OUTER JOIN pg_timezone_names z ON z.name = l.time_zone
$$;

/**
 * FUNCTION solardatum.find_reportable_intervals(bigint[])
 *
 * Get the min/max date range for a given set of source IDs and node IDs.
 *
 * @param nodes the node IDs to find
 * @param source the source IDs to find
 */
CREATE OR REPLACE FUNCTION solardatum.find_reportable_intervals(nodes bigint[], sources text[])
RETURNS TABLE (
	node_id bigint,
	source_id text,
	ts_start timestamp with time zone,
	ts_end timestamp with time zone,
	node_tz text,
	node_tz_offset integer
) LANGUAGE SQL STABLE ROWS 50 AS
$$
	WITH range AS (
		SELECT r.node_id, r.source_id, min(r.ts_min) AS ts_min, max(r.ts_max) AS ts_max
		FROM solardatum.da_datum_range r
		WHERE r.node_id = ANY(nodes)
			AND r.source_id = ANY(sources)
		GROUP BY r.node_id, r.source_id
	)
	SELECT r.node_id
		, r.source_id
		, r.ts_min AS ts_start
		, r.ts_max AS ts_end
		, COALESCE(l.time_zone, 'UTC') AS node_tz
		, COALESCE(CAST(EXTRACT(epoch FROM z.utc_offset) / 60 AS INTEGER), 0) AS node_tz_offset
	FROM range r
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = r.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	LEFT OUTER JOIN pg_timezone_names z ON z.name = l.time_zone
$$;

-- provide initial population of most-recent rows
INSERT INTO solardatum.da_datum_range (node_id, source_id, ts_min, ts_max)
WITH max AS (
	SELECT r.node_id, r.source_id, r.ts
	FROM (SELECT node_id FROM solarnet.sn_node) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent_direct(n.node_id)) AS r
)
, min AS (
	SELECT r.node_id, r.source_id, r.ts
	FROM (SELECT node_id FROM solarnet.sn_node) AS n,
	LATERAL (SELECT * FROM solardatum.find_least_recent_direct(n.node_id)) AS r
)
SELECT node_id, source_id, min(ts) AS ts_min, max(ts) AS ts_max
FROM (
	SELECT * FROM max
	UNION ALL
	SELECT * FROM min
) combined
GROUP BY node_id, source_id
ON CONFLICT (node_id, source_id) DO NOTHING;
