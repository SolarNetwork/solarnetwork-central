/**
 * Calculate hour-of-day aggregate values for a node and set of source IDs.
 *
 * @param node		node ID
 * @param source	array of source IDs
 * @param start_ts	the start timestamp (defaults to SN epoch)
 * @param end_ts	the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_hod(
		node bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		node_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	node AS node_id
	, d.source_id
	, (CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AT TIME ZONE 'UTC' AS ts_start
	, (CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
FROM solaragg.agg_datum_hourly d
WHERE
	d.node_id = node
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	EXTRACT(hour FROM d.local_date),
	d.source_id
$$;


/**
 * Calculate seasonal hour-of-day aggregate values for a node and set of source IDs.
 *
 * @param node		node ID
 * @param source	array of source IDs
 * @param start_ts	the start timestamp (defaults to SN epoch)
 * @param end_ts	the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_seasonal_hod(
		node bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		node_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	node AS node_id
	, d.source_id
	, (solarnet.get_season_monday_start(CAST(d.local_date AS DATE))
		+ CAST(EXTRACT(hour FROM d.local_date) || ' hour' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start
	, solarnet.get_season_monday_start(CAST(d.local_date AS DATE))
		+ CAST(EXTRACT(hour FROM d.local_date) || ' hour' AS INTERVAL) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
FROM solaragg.agg_datum_hourly d
WHERE
	d.node_id = node
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	solarnet.get_season_monday_start(CAST(d.local_date AS date)),
	EXTRACT(hour FROM d.local_date),
	d.source_id
$$;


/**
 * Calculate day-of-week aggregate values for a node and set of source IDs.
 *
 * @param node				node ID
 * @param source			array of source IDs
 * @param path				the JSON path to the value to extract, e.g. ['i','watts']
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_dow(
		node bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		node_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	node AS node_id
	, d.source_id
	, (DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start
	, (DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
FROM solaragg.agg_datum_daily d
WHERE
	d.node_id = node
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	EXTRACT(isodow FROM d.local_date),
	d.source_id
$$;


/**
 * Calculate seasonal day-of-week aggregate values for a node and set of source IDs.
 *
 * @param node				node ID
 * @param source			array of source IDs
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_seasonal_dow(
		node bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		node_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	node AS node_id
	, d.source_id
	, (solarnet.get_season_monday_start(d.local_date)
		+ CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start
	, (solarnet.get_season_monday_start(d.local_date)
		+ CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
FROM solaragg.agg_datum_daily d
WHERE
	d.node_id = node
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	solarnet.get_season_monday_start(CAST(d.local_date AS date)),
	EXTRACT(isodow FROM d.local_date),
	d.source_id
$$;

--
--
--

/**
 * Calculate hour-of-day aggregate values for a loc and set of source IDs.
 *
 * @param loc				loc ID
 * @param source			array of source IDs
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_loc_datum_hod(
		loc bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		loc_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	loc AS loc_id
	, d.source_id
	, (CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AT TIME ZONE 'UTC' AS ts_start
	, (CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
FROM solaragg.agg_loc_datum_hourly d
WHERE
	d.loc_id = loc
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	EXTRACT(hour FROM d.local_date),
	d.source_id
$$;


/**
 * Calculate seasonal hour-of-day aggregate values for a loc and set of source IDs.
 *
 * @param loc				loc ID
 * @param source			array of source IDs
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_loc_datum_seasonal_hod(
		loc bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		loc_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	loc AS loc_id
	, d.source_id
	, (solarnet.get_season_monday_start(CAST(d.local_date AS DATE))
		+ CAST(EXTRACT(hour FROM d.local_date) || ' hour' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start
	, solarnet.get_season_monday_start(CAST(d.local_date AS DATE))
		+ CAST(EXTRACT(hour FROM d.local_date) || ' hour' AS INTERVAL) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
FROM solaragg.agg_loc_datum_hourly d
WHERE
	d.loc_id = loc
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	solarnet.get_season_monday_start(CAST(d.local_date AS date)),
	EXTRACT(hour FROM d.local_date),
	d.source_id
$$;


/**
 * Calculate day-of-week aggregate values for a loc and set of source IDs
 * and one specific general data value. Note that the `path` parameter currently only
 * supports an array with exactly two elements.
 *
 * @param loc				loc ID
 * @param source			array of source IDs
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_loc_datum_dow(
		loc bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		loc_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	loc AS loc_id
	, d.source_id
	, (DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start
	, (DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
FROM solaragg.agg_loc_datum_daily d
WHERE
	d.loc_id = loc
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	EXTRACT(isodow FROM d.local_date),
	d.source_id
$$;


/**
 * Calculate seasonal day-of-week aggregate values for a loc and set of source IDs
 * and one specific general data value. Note that the `path` parameter currently only
 * supports an array with exactly two elements.
 *
 * @param loc				loc ID
 * @param source			array of source IDs
 * @param path				the JSON path to the value to extract, e.g. ['i','watts']
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_loc_datum_seasonal_dow(
		loc bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		loc_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	loc AS loc_id
	, d.source_id
	, (solarnet.get_season_monday_start(d.local_date)
		+ CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start
	, (solarnet.get_season_monday_start(d.local_date)
		+ CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
FROM solaragg.agg_loc_datum_daily d
WHERE
	d.loc_id = loc
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	solarnet.get_season_monday_start(CAST(d.local_date AS date)),
	EXTRACT(isodow FROM d.local_date),
	d.source_id
$$;
