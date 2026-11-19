/* ============================================================================
 * NET-543: do not count the difference between two reset records after the end of an hour in
 * that hour.
 *
 * `solardatm.rollup_datm_for_time_span()` differences each row against the row before it, for
 * each accumulating property, and counts in the slot the `portion` of each difference that falls
 * in it: all of a difference between two rows in the slot, a share of one that crosses the start
 * or the end of the slot, and none of one before it. A reset record is two rows at the same time:
 * its final reading, differenced against the row before it, and its start reading, which the row
 * after it is differenced against. The rows read end at the next datum with accumulating
 * properties after the slot, within the hour after it, or, when reset records lie between the
 * end of the slot and that datum, at the last of them.
 *
 * A difference between two rows that are both at or after the end of the slot fell through to
 * `ELSE 1` and was counted in full. One reset record after the end of the slot gives only its
 * own two rows there, and they are at the same time, so nothing was counted. Two or more give a
 * difference from the start reading of one to the final reading of the next, which was counted in
 * the slot as well as in the hour it belongs to. For example, with datum at 10:50 and 11:10 and
 * reset records at 11:02 and 11:04, the hour from 10:00 was given the accumulation from 11:02 to
 * 11:04 on top of its own share up to 11:00.
 *
 * Such a difference now has no portion in the slot. A new `WHEN` branch, placed just before the
 * `ELSE`, is the only change, so every case an earlier branch decides is unchanged. The reading
 * values (`read_a`) never included rows after the end of the slot, so only the clock accumulation
 * (`data_a`) of the affected hours changes.
 *
 * Hourly aggregates computed from now on change only for an hour followed by two or more reset
 * records before the next datum with accumulating properties, within the hour after it. Existing
 * aggregates are not recomputed: they keep their values until something marks them stale. On
 * production, the hours around all 87 pairs of reset records with no datum between them were
 * compared, the live function against this one, in November 2026, and none differed.
 *
 * The function body is identical to the one compared on production as
 * `solardatm.rollup_datm_for_time_span_net540`, from net540-candidate-copy.sql in
 * solarnetwork-ops (md5 of `prosrc` 6291c0c4841851ea80a61f6cc750c39f), so its comment on the new
 * branch names NET-540, where the defect was found.
 *
 * The function's doc comment now gives the order of each `read_a` tuple as the function builds
 * it, [difference, start, end]; it said [start, finish, difference].
 *
 * The function signature is unchanged, so `CREATE OR REPLACE` keeps its existing owner and ACLs.
 * ============================================================================
 */

/**
 * Compute a rollup datm record for a time range, including "reset" auxiliary records.
 *
 * The `data_i` output column contains the average values for the raw `data_i` properties within
 * the "clock" period.
 *
 * The `stat_i` output column contains a tuple of [count, min, max] values for the raw `data_i`
 * properties within the "clock" period.
 *
 * The `data_a` output column contains the accumulated difference of the raw `data_a` properties
 * within the "clock" period.
 *
 * The `read_a` output column contains a tuple of [difference, start, end] values for the raw
 * `data_a` properties within the "reading" period.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param tolerance_clock 	the maximum time to look forward/backward for adjacent datm within
 *                          the "clock" period
 * @param tolerance_read 	the maximum time to look forward/backward for adjacent datm within
 *                          the "reading" period
 * @see solardatm.find_datm_for_time_slot()
 */
CREATE OR REPLACE FUNCTION solardatm.rollup_datm_for_time_span(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance_clock INTERVAL DEFAULT INTERVAL 'PT1H',
		tolerance_read 	INTERVAL DEFAULT INTERVAL 'P1Y'
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE ROWS 500 AS
$$
	WITH m AS (
		SELECT COALESCE(array_length(names_i, 1), 0) AS len_i
			 , COALESCE(array_length(names_a, 1), 0) AS len_a
		FROM solardatm.find_metadata_for_stream(sid)
	)
	-- grab raw data + reset records, constrained by stream/date range
	, d AS (
		SELECT
			  stream_id
			, ts
			, pad_vec(data_i, len_i) AS data_i
			, pad_vec(data_a, len_a) AS data_a
			, data_s
			, data_t
			, rtype
			, (ts >= start_ts AND ts < end_ts) AS inc
		FROM m, solardatm.find_datm_for_time_slot(
			sid,
			start_ts,
			end_ts,
			tolerance_read
		)
	)
	-- calculate instantaneous statistis per property
	, di_ary AS (
		SELECT
			  vec_trim_scale(vec_agg_mean(d.data_i)) AS data_i
			, solarcommon.array_transpose2d(ARRAY[
				  vec_agg_count(d.data_i)::numeric[]
				, vec_agg_min(d.data_i)
				, vec_agg_max(d.data_i)
			  ]) AS stat_i
		FROM d
		WHERE d.inc
	)
	-- calculate clock accumulation for data_a values per property
	-- NOTE "unnest() WITH ORDINALITY" not used because of possible sparse array slice
	, wa AS (
		SELECT
			  p.idx
			, d.data_a[p.idx] AS val
			, d.ts
			, d.rtype
			, COALESCE(CASE
				WHEN rtype <> 2 THEN d.data_a[p.idx] - lag(d.data_a[p.idx]) OVER slot
				ELSE 0::numeric
				END, 0)::numeric AS diff
			, CASE
				-- too much time between rows for clock diff, or no time passed
				WHEN d.ts - lag(d.ts) OVER slot > tolerance_clock OR d.ts = lag(d.ts) OVER slot
					THEN 0

				-- before bounds
				WHEN d.ts < start_ts
					THEN 0

				-- crossing start of slot; allocate only end portion of accumulation within this slot
				WHEN lag(d.ts) OVER slot < start_ts
					THEN EXTRACT('epoch' FROM d.ts - start_ts) / EXTRACT('epoch' FROM d.ts - lag(d.ts) OVER slot)

				-- crossing end of slot; allocate only start portion of accumulation within this slot
				WHEN d.ts > end_ts AND lag(d.ts) OVER slot < end_ts
					THEN EXTRACT('epoch' FROM end_ts - lag(d.ts) OVER slot) / EXTRACT('epoch' FROM d.ts - lag(d.ts) OVER slot)

				-- both rows at or after the end of the slot: the difference belongs to a later slot
				-- (two reset records after the end, before the next datum; see NET-540)
				WHEN lag(d.ts) OVER slot >= end_ts
					THEN 0

				ELSE 1
				END as portion
			, (ts < end_ts AND NOT (ts <= start_ts AND rtype = 1)
				OR (ts = end_ts AND rtype < 2)) AS rinc
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_a, 1)) AS p(idx) ON TRUE
		WINDOW slot AS (PARTITION BY p.idx ORDER BY CASE WHEN d.data_a[p.idx] IS NULL THEN 1 ELSE 0 END, d.ts, d.rtype
			RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			  idx
			, sum(diff * portion::numeric) AS cdiff
			, sum(diff) FILTER (WHERE rinc) AS rdiff
			, solarcommon.first(val ORDER BY ts, rtype) FILTER (WHERE rinc) AS rstart
			, solarcommon.first(val ORDER BY ts DESC, rtype DESC) FILTER (WHERE rinc) AS rend
		FROM wa
		GROUP BY idx
	)
	-- join accumulating property values back into arrays
	, da_ary AS (
		SELECT
			  vec_trim_scale(array_agg(cdiff ORDER BY idx)) AS data_a
			, array_agg(
				ARRAY[rdiff, rstart, rend] ORDER BY idx
			) AS read_a
		FROM da
	)
	-- calculate status for data_s values per property
	-- NOTE "unnest() WITH ORDINALITY" not used because of possible sparse array slice
	, ws AS (
		SELECT
			  p.idx AS idx
			, d.data_s[p.idx] AS val
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_s, 1)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL AND d.inc
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
			  array_agg(d.val ORDER BY d.idx) AS data_s
		FROM ds d
	)
	-- join data_t property values into mega array
	, dt_ary AS (
		SELECT
			  array_agg(DISTINCT p.val) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL AND d.inc
	)
	SELECT
		sid AS stream_id
		, start_ts AS ts_start
		, di_ary.data_i
		, da_ary.data_a
		, ds_ary.data_s
		, dt_ary.data_t
		, di_ary.stat_i
		, da_ary.read_a
	FROM di_ary, da_ary, ds_ary, dt_ary
	WHERE data_i IS NOT NULL OR data_a IS NOT NULL OR data_s IS NOT NULL OR data_t IS NOT NULL
$$;
