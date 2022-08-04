/**
 * Calculate week-of-year aggregate values for a stream.
 *
 * This relies on the `solardatm.agg_datm_daily` table for speedy results.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_datm_woy(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE ROWS 7 AS
$$
	WITH d AS (
		SELECT
			(DATE '2001-01-01' + CAST((EXTRACT(week FROM d.ts_start AT TIME ZONE COALESCE(m.time_zone, 'UTC')) - 1) || ' week' AS INTERVAL)) AT TIME ZONE 'UTC' AS woy
			, solardatm.avg_agg_data(
				(d.data_i, d.data_a, d.data_s, d.data_t, d.stat_i, d.read_a)::solardatm.agg_data
				ORDER BY d.ts_start
			) AS r
		FROM solardatm.agg_datm_daily d
		LEFT OUTER JOIN solardatm.find_metadata_for_stream(sid) m ON TRUE
		WHERE d.stream_id = sid
			AND d.ts_start >= start_ts
			AND d.ts_start < end_ts
		GROUP BY EXTRACT(week FROM d.ts_start AT TIME ZONE COALESCE(m.time_zone, 'UTC'))
	)
	SELECT
		  sid AS stream_id
		, woy AS ts_start
		, (r).data_i
		, (r).data_a
		, (r).data_s
		, (r).data_t
		, (r).stat_i
		, (r).read_a
	FROM d
$$;
