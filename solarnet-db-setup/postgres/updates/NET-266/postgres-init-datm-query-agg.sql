/**
 * Calculate day-of-week aggregate values for a stream.
 *
 * This relies on the `solardatm.agg_datm_daily` table for speedy results.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_datm_dow(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		stream_id 	UUID,
		ts_start	TIMESTAMP WITH TIME ZONE,
		data_i		NUMERIC[],
		data_a		NUMERIC[],
		data_s		TEXT[],
		data_t		TEXT[],
		stat_i		NUMERIC[][]
	) LANGUAGE SQL STABLE ROWS 7 AS
$$
	WITH d AS (
		SELECT
			(DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.ts_start AT TIME ZONE COALESCE(m.time_zone, 'UTC')) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS dow
			, (solardatm.rollup_agg_datm(
				(d.stream_id, d.ts_start, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a)::solardatm.agg_datm
				, start_ts
			)).*
		FROM solardatm.agg_datm_daily d
		LEFT OUTER JOIN solardatm.find_metadata_for_stream(sid) m ON TRUE
		WHERE d.stream_id = sid
			AND d.ts_start >= start_ts
			AND d.ts_start < end_ts
		GROUP BY EXTRACT(isodow FROM d.ts_start AT TIME ZONE COALESCE(m.time_zone, 'UTC'))
	)
	SELECT
		  stream_id
		, dow AS ts_start
		, data_i
		, data_a
		, data_s
		, data_t
		, stat_i
	FROM d
$$;


/**
 * Calculate seasonal day-of-week aggregate values for a stream.
 *
 * The year is split into four 3-month seasons and the results grouped by both season and DOW.
 * This relies on the `solardatm.agg_datm_daily` table for speedy results.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @see solarnet.get_season_monday_start()
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_datm_dow_seasonal(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		stream_id 	UUID,
		ts_start	TIMESTAMP WITH TIME ZONE,
		data_i		NUMERIC[],
		data_a		NUMERIC[],
		data_s		TEXT[],
		data_t		TEXT[],
		stat_i		NUMERIC[][]
	) LANGUAGE SQL STABLE ROWS 28 AS
$$
	WITH d AS (
		SELECT
			(solarnet.get_season_monday_start(CAST(d.ts_start AT TIME ZONE COALESCE(m.time_zone, 'UTC') AS date))
			    + CAST((EXTRACT(isodow FROM d.ts_start AT TIME ZONE COALESCE(m.time_zone, 'UTC')) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS dow
			, (solardatm.rollup_agg_datm(
				(d.stream_id, d.ts_start, d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a)::solardatm.agg_datm
				, start_ts
			)).*
		FROM solardatm.agg_datm_daily d
		LEFT OUTER JOIN solardatm.find_metadata_for_stream(sid) m ON TRUE
		WHERE d.stream_id = sid
			AND d.ts_start >= start_ts
			AND d.ts_start < end_ts
		GROUP BY
			solarnet.get_season_monday_start(CAST(d.ts_start AT TIME ZONE COALESCE(m.time_zone, 'UTC') AS date)),
			EXTRACT(isodow FROM d.ts_start AT TIME ZONE COALESCE(m.time_zone, 'UTC'))
	)
	SELECT
		  stream_id
		, dow AS ts_start
		, data_i
		, data_a
		, data_s
		, data_t
		, stat_i
	FROM d
$$;
