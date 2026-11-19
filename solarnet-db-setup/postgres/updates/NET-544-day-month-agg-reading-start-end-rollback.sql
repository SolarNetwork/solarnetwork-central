/* ============================================================================
 * NET-544 rollback: restore `solardatm.rollup_agg_data_for_time_span()` as it was before
 * NET-544-day-month-agg-reading-start-end.sql, which takes a day's or month's start and end
 * readings from the first and last row whose accumulation is not NULL, whether or not that row has
 * a reading.
 *
 * This is the function as the setup DDL in postgres-init-datm-agg-query.sql defined it before
 * NET-544, copied unchanged.
 *
 * The function signature is unchanged, so `CREATE OR REPLACE` keeps its existing owner and ACLs.
 * Where NET-544 was never applied, this changes nothing.
 *
 * Daily and monthly aggregates computed while NET-544 was in place keep their results until
 * something marks them stale.
 * ============================================================================
 */

/**
 * Compute a higher-level rollup datm record of lower=level aggregate datum records over a time range.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param kind 				the aggregate kind: 'h', 'd', or 'M' for daily, hourly, monthly
 * @see solardatm.find_agg_datm_for_time_span()
 */
CREATE OR REPLACE FUNCTION solardatm.rollup_agg_data_for_time_span(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		kind 			CHARACTER
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE ROWS 500 AS
$$
	WITH d AS (
		SELECT * FROM solardatm.find_agg_datm_for_time_span(
			sid,
			start_ts,
			end_ts,
			kind
		)
	)
	-- calculate instantaneous values per property
	, wi AS (
		SELECT
			  p.idx
			, d.data_i[p.idx] AS val
			, d.stat_i[p.idx][1] AS cnt
			, d.stat_i[p.idx][2] AS min
			, d.stat_i[p.idx][3] AS max
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_i, 1)) AS p(idx) ON TRUE
	)
	-- calculate instantaneous statistics
	, di AS (
		SELECT
			idx
			, sum(val * cnt) AS val
			, sum(cnt) AS cnt
			, min(min) AS val_min
			, max(max) AS val_max
		FROM wi
		GROUP BY idx
	)
	-- join data_i and stat_i property values back into arrays
	, di_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val / cnt ORDER BY idx)) AS data_i
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
			) AS stat_i
		FROM di
	)
	-- calculate accumulating values per property
	, wa AS (
		SELECT
			  p.idx
			, d.data_a[p.idx] AS val
			, d.read_a[p.idx][1] AS rdiff
			, first_value(d.read_a[p.idx][2]) OVER slot_start AS rstart
			, first_value(d.read_a[p.idx][3]) OVER slot_end AS rend
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_a, 1)) AS p(idx) ON TRUE
		WINDOW slot_start AS (PARTITION BY p.idx ORDER BY CASE WHEN d.data_a[p.idx] IS NULL THEN 1 ELSE 0 END, d.ts_start
				RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING),
			slot_end AS (PARTITION BY p.idx ORDER BY CASE WHEN d.data_a[p.idx] IS NULL THEN 1 ELSE 0 END, d.ts_start DESC
				RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			idx
			, sum(val) AS val
			, sum(rdiff) AS rdiff
			, min(rstart) AS rstart
			, min(rend) AS rend
		FROM wa
		GROUP BY idx
	)
	-- join data_a and read_a property values back into arrays
	, da_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val ORDER BY idx)) AS data_a
			, array_agg(
				ARRAY[rdiff, rstart, rend] ORDER BY idx
			) AS read_a
		FROM da
	)
	-- calculate status for data_s values per property
	, ws AS (
		SELECT
			  p.idx AS idx,
			  d.data_s[p.idx] AS val
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_s, 1)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL
	)
	-- calculate status statistics, as most-frequent status values
	, ds AS (
		SELECT
			  ws.idx
			, mode() WITHIN GROUP (ORDER BY ws.val) AS val
		FROM ws
		GROUP BY ws.idx
	)
	-- join data_s property values back into arrays
	, ds_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_s
		FROM ds
	)
	-- join data_t property values into mega array
	, dt_ary AS (
		SELECT
			  array_agg(DISTINCT p.val) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL
	)
	SELECT
		sid AS stream_id,
		start_ts AS ts_start
		, di_ary.data_i
		, da_ary.data_a
		, ds_ary.data_s
		, dt_ary.data_t
		, di_ary.stat_i
		, da_ary.read_a
	FROM di_ary, da_ary, ds_ary, dt_ary
	WHERE data_i IS NOT NULL OR data_a IS NOT NULL OR data_s IS NOT NULL OR data_t IS NOT NULL
$$;
