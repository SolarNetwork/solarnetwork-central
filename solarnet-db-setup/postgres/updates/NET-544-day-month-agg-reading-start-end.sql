/* ============================================================================
 * NET-544: take the start and end readings of a day or a month from the first and last row that
 * has them.
 *
 * `solardatm.rollup_agg_data_for_time_span()` rolls hourly rows up to a day and daily rows up to
 * a month. For each accumulating property it took the start reading (`read_a[n][2]`) from the
 * first row whose accumulation (`data_a[n]`) is not NULL, and the end reading (`read_a[n][3]`)
 * from the last. An hour in which the stream's datum did not carry a property has an accumulation
 * of 0 for it and no reading. So when the first or last hour of a day was such an hour, the day's
 * start or end reading came out NULL although its reading difference was right, and a month did
 * the same with such days. This happens on streams whose datum carry some of their accumulating
 * properties only part of the time.
 *
 * The two window functions that chose those readings are replaced by an `array_agg()` of each
 * property's readings that are not NULL, in the order the windows used: rows whose accumulation
 * is not NULL first, then by time, reversed for the end reading. The first element is the
 * reading. So the reading comes from the row chosen before when that row has one, and otherwise
 * from the first (or last) row that does, if any.
 *
 * Nothing else changes, and no start or end reading that was not NULL changes:
 *
 *  - the row chosen before is still chosen whenever it has the reading, because it was the first
 *    in the old order and the readings are still taken in that order;
 *  - the rows read, the rows of each property, the accumulation, the reading difference, the
 *    averages, counts, minimums, maximums, status and tags are computed exactly as before.
 *
 * The aggregates sort each property's rows on their own, where the windows sorted the rows of
 * every property together, twice, so the function takes less time than before, and less the more
 * accumulating properties the stream has.
 *
 * Daily and monthly aggregates computed from now on change only where their start or end reading
 * was NULL and a row has one. Existing aggregates are not recomputed: they keep their values until
 * something marks them stale. A month is rolled up from the daily rows as they are stored, so
 * until its first and last days are recomputed it can still have a NULL start or end reading from
 * them, or take the reading of a later first day or an earlier last day that has one.
 *
 * The function body is identical to the one to be compared on production as
 * `solardatm.rollup_agg_data_for_time_span_net544`, from net544-candidate-copy.sql in
 * solarnetwork-ops (md5 of `prosrc` 7ae308c50b55b4b7ac4dade8c995f610).
 *
 * The function is still a single SQL `SELECT` and not STRICT, and it reads the same rows as before.
 * Its signature is unchanged, so `CREATE OR REPLACE` keeps its existing owner and ACLs.
 * ============================================================================
 */

/**
 * Compute a higher-level rollup datm record of lower=level aggregate datum records over a time range.
 *
 * The start and end readings of each accumulating property in `read_a` are those of the first and
 * last row that has them, preferring rows whose accumulation in `data_a` is not NULL.
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
			, d.ts_start
			, d.data_a[p.idx] AS val
			, d.read_a[p.idx][1] AS rdiff
			, d.read_a[p.idx][2] AS rstart
			, d.read_a[p.idx][3] AS rend
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_a, 1)) AS p(idx) ON TRUE
	)
	-- calculate accumulating statistics; the start and end readings are those of the first and
	-- last row that has them, taking rows whose accumulation is not NULL first (see NET-544)
	, da AS (
		SELECT
			idx
			, sum(val) AS val
			, sum(rdiff) AS rdiff
			, (array_agg(rstart ORDER BY val IS NULL, ts_start) FILTER (WHERE rstart IS NOT NULL))[1] AS rstart
			, (array_agg(rend ORDER BY val IS NULL, ts_start DESC) FILTER (WHERE rend IS NOT NULL))[1] AS rend
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
