-- solardatum datum functions that rely on other namespaces like solaragg

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
 * FUNCTION solardatum.find_most_recent(bigint[], text[])
 * 
 * Find the highest available data for all source IDs for the given node IDs. This query relies on
 * the `solardatum.da_datum_range` table.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find, or NULL/empty array for all available sources
 */
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(nodes bigint[], sources text[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.*
	FROM  solardatum.da_datum_range mr
	INNER JOIN solardatum.da_datum_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts = mr.ts_max
	WHERE mr.node_id = ANY(nodes) AND (COALESCE(array_length(sources, 1), 0) < 1 OR mr.source_id = ANY(sources))
	ORDER BY d.node_id, d.source_id
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
 * Add or update a datum record. The data is stored in the `solardatum.da_datum` table.
 *
 * @param cdate The datum creation date.
 * @param node The node ID.
 * @param src The source ID.
 * @param pdate The date the datum was posted to SolarNet.
 * @param jdata The datum JSON document.
 * @param track_recent if `TRUE` then also insert results of `solardatum.calculate_stale_datum()`
 *                     into the `solaragg.agg_stale_datum` table and call
 *                     `solardatum.update_datum_range_dates()` to keep the
 *                     `solardatum.da_datum_range` table up-to-date
 */
CREATE OR REPLACE FUNCTION solardatum.store_datum(
	cdate 			TIMESTAMP WITH TIME ZONE,
	node 			BIGINT,
	src 			TEXT,
	pdate 			TIMESTAMP WITH TIME ZONE,
	jdata 			TEXT,
	track_recent 	BOOLEAN DEFAULT TRUE)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_crea 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(cdate, now());
	ts_post 			TIMESTAMP WITH TIME ZONE	:= COALESCE(pdate, now());
	jdata_json 			JSONB 						:= jdata::jsonb;
	jdata_prop_count 	INTEGER 					:= solardatum.datum_prop_count(jdata_json);
	ts_post_hour 		TIMESTAMP WITH TIME ZONE 	:= date_trunc('hour', ts_post);
	is_insert 			BOOLEAN 					:= false;
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

	IF track_recent THEN
		INSERT INTO solaragg.agg_stale_datum (agg_kind, node_id, ts_start, source_id)
		SELECT 'h' AS agg_kind, node_id, ts_start, source_id
		FROM solardatum.calculate_stale_datum(node, src, cdate)
		ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING;

		IF is_insert THEN
			PERFORM solardatum.update_datum_range_dates(node, src, cdate);
		END IF;
	END IF;
END;
$$;


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
