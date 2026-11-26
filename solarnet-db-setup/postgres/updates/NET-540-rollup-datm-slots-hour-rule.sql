/* ============================================================================
 * NET-540: allocate the clock accumulation of sub-hour slots as the 15 minute aggregates do, so
 * the slots of an hour add up to the hourly aggregate.
 *
 * `solardatm.rollup_datm_for_time_span_slots()` computes the 5, 10, 15 and 30 minute aggregates
 * that SolarQuery returns. Its clock accumulation did not add up to the hourly aggregate of the
 * same hour:
 *
 *  - It counted a difference across any gap. The hourly rollup counts nothing across a gap longer
 *    than the clock tolerance, so datum at 12:05 and 13:55 gave the two slots 105 and 5 of 110,
 *    where each hour has 0.
 *  - It never split a difference at the hour. The earlier datum's slot got the part up to the
 *    start of the later datum's slot, even when that was in the next hour, so datum at 12:50 and
 *    13:10 gave 10 minute slots 20 and 0 of 20, where each hour has 10.
 *  - It lost the part of a difference that crosses the end of a slot holding a datum without
 *    accumulating properties: it took the last row of the slot to carry that part, and sorted
 *    such a datum last, so no row carried it.
 *  - It lost the part of the difference from a reset record's start reading to the next datum
 *    that falls in the reset record's slot, by giving the start reading no part after it.
 *  - It counted the difference between a datum and a reset record's final reading at the same
 *    time, which the hourly rollup does not.
 *  - It returned only slots holding records in the range, so an hour with no records but a datum
 *    exactly at its end got no slot, though it has an hourly aggregate with no accumulation.
 *
 * The function now takes the accumulation of each UTC hour holding the range as
 * `solardatm.rollup_datm_for_time_span()` computes it, and allocates it to the slots of the hour
 * as `solardatm.rollup_datm_15m_for_hour()` allocates it, for any slot length. With 900 second
 * slots it returns the same slots as that function, except for `read_a`, which stays NULL. The
 * function's doc comment gives the rule, and the one case where the slots of an hour still do not
 * add up: a datum that lacks some of the stream's accumulating properties.
 *
 * Other changes to the results:
 *
 *  - A range that starts part way through a slot no longer returns that slot, as the other
 *    aggregate levels do not return the period holding an unaligned start.
 *  - A slot is computed from all its records, including those after the end of the range, so its
 *    result does not depend on the range. Before, the last slot averaged in the next datum with
 *    accumulating properties after the range when that datum fell in the slot, but not its status
 *    or tags, nor any other datum after the range.
 *  - A `secs` that `solardatm.slot_seconds()` does not accept is replaced by 600, as the doc
 *    comment always said. SolarQuery only passes 300, 600, 900 and 1800.
 *
 * `solardatm.count_datm_time_span_slots()`, which SolarQuery runs for the total result count of a
 * paged minute-level query, now counts the slots the rollup returns: the slots starting in the
 * range, with the records after the end of the range in the last slot, slots holding only a reset
 * record, and the slot of an hour with no records described above. It still reads only the time
 * of each record. It is now written in plpgsql, so a session reuses the plan of its query: as a
 * SQL function, its body was planned at every call, over every chunk of `solardatm.da_datm`.
 *
 * Measured on a local database built by the ops setup-db.sh, with 74 30-day chunks and synthetic
 * streams: the rollup takes 62 to 94 percent of the time of the function it replaces, in 17 cases
 * from an hour to a week of 1 minute to sparse data with 2 to 22 accumulating properties. The
 * highest figures, 86 to 94 percent, are for ranges of one to three hours, which take 2 to 3 ms,
 * and for a stream with partial datum. The count takes 0.4 to 2.1 ms, against 1.0 to 3.0 ms for
 * the function as NET-289 installed it and 1.0 to 16 ms for the one the setup DDL had. On a
 * sample of 100 production streams over a day, the rollup took 65 to 72 percent of the time of
 * the function it replaces, and the count 30 to 38 percent of the time of NET-289's.
 *
 * Minute-level aggregates are computed when queried, not stored, so the new results apply from
 * the moment this runs and nothing needs recomputing. Both function signatures are unchanged, so
 * `CREATE OR REPLACE` keeps their existing owners and ACLs. Both functions now call
 * `solardatm.slot_seconds()`, which the rollup they replace and NET-289's count do not, so each
 * role that runs minute-level queries needs EXECUTE on it: check that with
 * `has_function_privilege()` before running this where grants are not the setup defaults.
 * ============================================================================
 */

/**
 * Calculate the number of rollup datum `solardatm.rollup_datm_for_time_span_slots()` returns for a
 * time range and slot period.
 *
 * This is meant to compliment the `solardatm.rollup_datm_for_time_span_slots()` function to
 * more quickly find the count of datum slots that are available. This function can be much
 * faster to execute when only a count is needed.
 *
 * It counts the slots that start within the range and hold a datum or reset record, counting all
 * the records of the last slot even past the end of the range, and the last slot of each hour
 * without records that ends with a record exactly on the next hour, when the stream has
 * accumulating properties. It reads only the time of each record, not its values, so it also
 * counts the rare slots that `solardatm.rollup_datm_for_time_span_slots()` omits: one whose datum
 * have no values at all, and the last slot of an hour before a datum on the hour that has no
 * accumulating values.
 *
 * It is written in plpgsql so that a session reuses the plan of its query: PostgreSQL cannot
 * inline a SQL function that returns a single value from a query like this one, and plans its
 * body at every call, without the argument values, over every chunk of `solardatm.da_datm`.
 *
 * @param sid 				the stream ID to find the count of datm slots for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param secs				the slot period, in seconds; must be between 60 and 1800 and evenly
 *                          divide into 1800, or else 600 is used
 * @see solardatm.rollup_datm_for_time_span_slots()
 * @see solardatm.slot_seconds()
 */
CREATE OR REPLACE FUNCTION solardatm.count_datm_time_span_slots(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		secs			INTEGER DEFAULT 600
	) RETURNS BIGINT LANGUAGE plpgsql STABLE AS
$$
DECLARE
	n BIGINT;
BEGIN
	-- the slot length, the first slot starting in the range, and the end of the last slot starting
	-- in it
	WITH b AS (
		SELECT q.slot_len
			, time_bucket(q.slot_len, start_ts - INTERVAL 'PT0.000001S') + q.slot_len AS lo
			, time_bucket(q.slot_len, end_ts - INTERVAL 'PT0.000001S') + q.slot_len AS hi
		FROM (SELECT make_interval(secs => solardatm.slot_seconds(secs)) AS slot_len) q
	)
	-- the slots with datum or reset records, each with the time of its first record; the records
	-- are read from an hour before the range to an hour after it, which holds the hour before the
	-- first slot and the slot after the last, with bounds on the arguments alone so the chunks of
	-- the datum table are excluded whether the plan is generic or not
	, s AS (
		SELECT time_bucket(r.slot_len, r.ts) AS slot, min(r.ts) AS ts
		FROM (
			SELECT b.slot_len, d.ts
			FROM b
			INNER JOIN solardatm.da_datm d ON d.stream_id = sid
				AND d.ts >= start_ts - INTERVAL 'PT1H'
				AND d.ts < end_ts + INTERVAL 'PT1H'
			UNION ALL
			SELECT b.slot_len, a.ts
			FROM b
			INNER JOIN solardatm.da_datm_aux a ON a.stream_id = sid
				AND a.atype = 'Reset'::solardatm.da_datm_aux_type
				AND a.ts >= start_ts - INTERVAL 'PT1H'
				AND a.ts < end_ts + INTERVAL 'PT1H'
		) r
		GROUP BY 1
	)
	SELECT count(*) FILTER (WHERE w.slot >= b.lo AND w.slot < b.hi)
		-- the last slot of an hour without records, when a record is exactly at the end of the hour
		+ count(*) FILTER (WHERE w.ts = w.slot
			AND w.slot = time_bucket(INTERVAL 'PT1H', w.slot)
			AND w.slot - b.slot_len >= b.lo
			AND w.slot - b.slot_len < b.hi
			AND (w.p_slot IS NULL OR w.p_slot < w.slot - INTERVAL 'PT1H')
			AND m.has_a)
	INTO n
	FROM b
	, (SELECT COALESCE(CARDINALITY(md.names_a) > 0, FALSE) AS has_a
		FROM solardatm.find_metadata_for_stream(sid) md) m
	, (SELECT s.slot, s.ts, lag(s.slot) OVER (ORDER BY s.slot) AS p_slot FROM s) w;
	RETURN n;
END
$$;


/**
 * Compute rollup datm records for a time range into sub-hour "slots", including "reset" auxiliary
 * records.
 *
 * The slots are `secs` long and line up with UTC hours, as `time_bucket()` gives them. A slot is
 * returned when it starts within the range, and is computed from all its datum, so a range that
 * ends part way through a slot gets all of that slot, and one that starts part way through a slot
 * does not get that slot. A slot's result therefore does not depend on the range it is part of.
 *
 * The `data_i` output column contains the average values for the raw `data_i` properties within
 * the slot.
 *
 * The `stat_i` output column contains a tuple of [count, min, max] values for the raw `data_i`
 * properties within the slot.
 *
 * The `data_a` output column contains the clock accumulation of the raw `data_a` properties
 * allocated to the slot. The accumulation of each UTC hour is what
 * `solardatm.rollup_datm_for_time_span()` computes for the hour, and it is allocated to the slots
 * of the hour as `solardatm.rollup_datm_15m_for_hour()` allocates it to 15 minute slots, for any
 * slot length, so the slots of an hour add up to the hour:
 *
 *  - the difference between two datum or reset readings in the same slot goes to that slot;
 *  - the difference between two readings of the hour in different slots is split at the start of
 *    the later reading's slot, so that slot gets the part after its start and the earlier
 *    reading's slot the rest; slots between them get nothing;
 *  - the hour's part of a difference that crosses the start of the hour goes to the slot of the
 *    first reading in the hour, and the hour's part of one that crosses the end of the hour goes
 *    to the slot of the last reading in the hour;
 *  - no accumulation is counted across a gap longer than `tolerance_clock`, nor between readings
 *    at the same time, nor from a reset record's final reading to its start reading;
 *  - the last slot of an hour without datum or reset records, but with a datum with accumulating
 *    properties or a reset record exactly at its end, is returned with no accumulation, as the
 *    hour has an hourly rollup.
 *
 * A datum that lacks some of the stream's accumulating properties is differenced against the
 * nearest readings with any accumulating property, and a property one of the two lacks adds
 * nothing. The hourly rollup instead differences each property against the nearest readings that
 * have it, so for such a property the slots of an hour can add up to less than the hour.
 *
 * The `read_a` output column is always NULL.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param secs				the slot period, in seconds; must be between 60 and 1800 and evenly
 *                          divide into 1800, or else 600 is used
 * @param tolerance_clock 	the maximum time to look forward/backward for adjacent datm within
 *                          the "clock" period
 *
 * @see solardatm.count_datm_time_span_slots()
 * @see solardatm.rollup_datm_for_time_span()
 * @see solardatm.rollup_datm_15m_for_hour()
 * @see solardatm.slot_seconds()
 */
CREATE OR REPLACE FUNCTION solardatm.rollup_datm_for_time_span_slots(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		secs			INTEGER DEFAULT 600,
		tolerance_clock INTERVAL DEFAULT interval '1 hour'
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE ROWS 500 AS
$$
	-- the stream's array lengths, the slot length, and the whole UTC hours holding the range
	WITH m AS (
		SELECT COALESCE(array_length(names_i, 1), 0) AS len_i
			, COALESCE(array_length(names_a, 1), 0) AS len_a
			, COALESCE(CARDINALITY(names_a) = 0, TRUE) AS has_no_a
			, make_interval(secs => solardatm.slot_seconds(secs)) AS slot_len
			, time_bucket(INTERVAL 'PT1H', start_ts) AS hs
			, time_bucket(INTERVAL 'PT1H', end_ts - INTERVAL 'PT0.000001S') + INTERVAL 'PT1H' AS he
		FROM solardatm.find_metadata_for_stream(sid)
	)
	-- the datum with accumulating properties just before and after those hours, within the
	-- tolerance
	, b AS (
		SELECT m.*
			, COALESCE((SELECT t FROM solardatm.find_time_before_ts(sid, m.hs, m.hs - tolerance_clock
				, TRUE, m.has_no_a) AS t), m.hs) AS lo
			, COALESCE((SELECT t FROM solardatm.find_time_after_ts(sid, m.he, m.he + tolerance_clock
				, TRUE, m.has_no_a) AS t), m.he) AS hi
		FROM m
	)
	-- datum and reset records
	, d AS (
		SELECT d.ts
			, pad_vec(d.data_i, b.len_i) AS data_i
			, pad_vec(d.data_a, b.len_a) AS data_a
			, d.data_s
			, d.data_t
			, 0::SMALLINT AS rtype
		FROM b, solardatm.find_datm_between(sid, b.lo, b.hi) d
		UNION ALL
		SELECT aux.ts
			, NULL::NUMERIC[]
			, pad_vec(aux.data_a, b.len_a)
			, NULL::TEXT[]
			, NULL::TEXT[]
			, aux.rtype
		FROM b, solardatm.find_datm_aux_for_time_span(sid, b.hs - tolerance_clock, b.he + tolerance_clock) aux
	)
	-- the times on the hour of datum without accumulating properties, and the hours holding such
	-- datum: a datum without accumulating properties on the hour ends the rows of the hour before
	-- it, so no accumulation crosses the end of that hour
	, nb AS (
		SELECT array_agg(d.ts) FILTER (WHERE d.ts = time_bucket(INTERVAL 'PT1H', d.ts)) AS ts
			, array_agg(DISTINCT time_bucket(INTERVAL 'PT1H', d.ts)) AS hrs
		FROM d
		WHERE d.data_a IS NULL
	)
	-- each row with the rows before and after it that have accumulating properties
	, w AS (
		SELECT d.*
			, time_bucket(b.slot_len, d.ts) AS slot
			, time_bucket(INTERVAL 'PT1H', d.ts) AS hr
			, lag(d.ts) OVER win AS p_ts
			, lag(d.data_a) OVER win AS p_a
			, lead(d.ts) OVER win AS n_ts
			, lead(d.data_a) OVER win AS n_a
			, lead(d.rtype) OVER win AS n_rtype
			-- the rows after a reset record that follows this row, and the latest datum at or
			-- before this row
			, lead(d.ts, 3) OVER win AS n3_ts
			, lead(d.rtype, 3) OVER win AS n3_rtype
			, lead(d.ts, 5) OVER win AS n5_ts
			, lead(d.rtype, 5) OVER win AS n5_rtype
			, max(d.ts) FILTER (WHERE d.rtype = 0) OVER win AS d_ts
			-- whether the row and the rows before and after it have every accumulating property
			, array_position(d.data_a, NULL) IS NULL AS full_a
			, lag(array_position(d.data_a, NULL) IS NULL) OVER win AS p_full_a
			, lead(array_position(d.data_a, NULL) IS NULL) OVER win AS n_full_a
		FROM b, d
		WINDOW win AS (ORDER BY d.data_a IS NULL, d.ts, d.rtype)
	)
	-- the portion of the difference from the row before that falls in this row's slot, and the
	-- portion of the difference to the row after that falls in this row's slot
	, p0 AS (
		SELECT w.*
			, CASE
				WHEN w.data_a IS NULL OR w.p_ts IS NULL OR w.rtype = 2
					OR w.ts - w.p_ts > tolerance_clock OR w.ts = w.p_ts
					THEN 0
				-- crossing the start of the hour
				WHEN w.p_ts < w.hr
					THEN CASE
						WHEN w.ts = w.hr THEN 0
						ELSE EXTRACT('epoch' FROM w.ts - w.hr) / EXTRACT('epoch' FROM w.ts - w.p_ts)
						END
				WHEN w.p_ts >= w.slot
					THEN 1
				ELSE 1 - EXTRACT('epoch' FROM w.slot - w.p_ts) / EXTRACT('epoch' FROM w.ts - w.p_ts)
				END AS pb
			, CASE
				WHEN w.data_a IS NULL OR w.n_a IS NULL OR w.n_rtype = 2
					OR w.n_ts - w.ts > tolerance_clock OR w.n_ts = w.ts
					THEN 0
				-- at or crossing the end of the hour
				WHEN w.n_ts >= w.hr + INTERVAL 'PT1H'
					THEN CASE
						WHEN w.n_ts = w.hr + INTERVAL 'PT1H' THEN 1
						-- a datum without accumulating properties at the end of the hour ends its rows
						WHEN w.hr + INTERVAL 'PT1H' = ANY(nb.ts) THEN 0
						-- an hour without datum ends at its last reset record
						WHEN w.rtype = 2 AND NOT COALESCE(w.d_ts >= w.hr OR w.hr = ANY(nb.hrs), FALSE)
							THEN 0
						-- a reset record after the end of the hour counts only when a datum with
						-- accumulating properties follows it within the hour after the end
						WHEN w.n_rtype = 1 AND NOT COALESCE(
								w.n3_rtype = 0 AND w.n3_ts <= w.hr + INTERVAL 'PT2H'
								OR w.n3_rtype = 1 AND w.n5_rtype = 0 AND w.n5_ts <= w.hr + INTERVAL 'PT2H'
							, FALSE) THEN 0
						ELSE EXTRACT('epoch' FROM w.hr + INTERVAL 'PT1H' - w.ts) / EXTRACT('epoch' FROM w.n_ts - w.ts)
						END
				WHEN w.n_ts < w.slot + b.slot_len
					THEN 0
				ELSE EXTRACT('epoch' FROM time_bucket(b.slot_len, w.n_ts) - w.ts) / EXTRACT('epoch' FROM w.n_ts - w.ts)
				END AS pa
		FROM b, nb, w
		-- computed once per row, not once per reference below
		OFFSET 0
	)
	-- per slot: instantaneous statistics, and the clock accumulation of its rows, NULL for a
	-- property none of its rows has
	, sa AS (
		SELECT p.slot
			, time_bucket(INTERVAL 'PT1H', p.slot) AS hr
			, bool_or(p.a_at_slot) AS a_at_slot
			, vec_to_sum(p.c) AS acc
			, vec_trim_scale(vec_agg_mean(p.data_i)) AS data_i
			, vec_agg_count(p.data_i)::numeric[] AS cnt_i
			, vec_agg_min(p.data_i) AS min_i
			, vec_agg_max(p.data_i) AS max_i
		FROM (
			-- each row's accumulation within its slot, NULL for a property the row lacks
			SELECT p.slot
				, p.data_i
				, (p.data_a IS NOT NULL AND p.rtype < 2 AND p.ts = p.slot) AS a_at_slot
				, CASE
					WHEN p.data_a IS NULL THEN NULL
					WHEN p.pb = 0 AND p.pa = 0 THEN vec_mul(p.data_a, 0::numeric)
					-- every property present: the differences have no NULL to mask
					WHEN p.full_a AND (p.pb = 0 OR p.p_full_a) AND (p.pa = 0 OR p.n_full_a) THEN CASE
						WHEN p.pa = 0 AND p.pb = 1 THEN vec_sub(p.data_a, p.p_a)
						WHEN p.pa = 0 THEN vec_mul(vec_sub(p.data_a, p.p_a), p.pb::numeric)
						WHEN p.pb = 0 THEN vec_mul(vec_sub(p.n_a, p.data_a), p.pa::numeric)
						WHEN p.pb = 1 THEN vec_add(vec_sub(p.data_a, p.p_a)
							, vec_mul(vec_sub(p.n_a, p.data_a), p.pa::numeric))
						ELSE vec_add(vec_mul(vec_sub(p.data_a, p.p_a), p.pb::numeric)
							, vec_mul(vec_sub(p.n_a, p.data_a), p.pa::numeric))
						END
					-- otherwise a property the row has but a row it is differenced against lacks adds
					-- zero, and one the row lacks stays NULL
					ELSE vec_add(vec_mul(p.data_a, 0::numeric), vec_add(
						  CASE WHEN p.pb <> 0
							THEN vec_coalesce(vec_mul(vec_sub(p.data_a, p.p_a), p.pb::numeric), 0::numeric)
							ELSE vec_coalesce(vec_mul(p.data_a, 0::numeric), 0::numeric)
							END
						, CASE WHEN p.pa <> 0
							THEN vec_coalesce(vec_mul(vec_sub(p.n_a, p.data_a), p.pa::numeric), 0::numeric)
							ELSE vec_coalesce(vec_mul(p.data_a, 0::numeric), 0::numeric)
							END
					))
					END AS c
			FROM p0 p
			-- so the aggregate reads (and spills) only these columns
			OFFSET 0
		) p
		GROUP BY p.slot
	)
	-- per hour: its first and last slots, and which properties any of its rows has
	, sh AS (
		SELECT sa.hr
			, min(sa.slot) AS fs
			, max(sa.slot) AS ls
			, bool_or(sa.acc IS NOT NULL) AS has_a
			, bool_or(sa.a_at_slot) FILTER (WHERE sa.slot = sa.hr) AS a_at_hr
			, vec_to_max(vec_mul(sa.acc, 0::numeric)) AS a_mask
		FROM sa
		GROUP BY sa.hr
	)
	-- with the hours before and after
	, sh2 AS (
		SELECT sh.*
			, COALESCE(lag(sh.hr) OVER o = sh.hr - INTERVAL 'PT1H', FALSE) AS prev_rows
			, COALESCE(lag(sh.hr) OVER o = sh.hr - INTERVAL 'PT1H' AND lag(sh.has_a) OVER o, FALSE) AS prev_a
			, COALESCE(lead(sh.hr) OVER o = sh.hr + INTERVAL 'PT1H' AND lead(sh.has_a) OVER o, FALSE) AS next_a
			, COALESCE(lead(sh.hr) OVER o = sh.hr + INTERVAL 'PT1H' AND lead(sh.a_at_hr) OVER o, FALSE) AS end_a
		FROM sh
		WINDOW o AS (ORDER BY sh.hr)
	)
	-- status per slot, as most-frequent status values
	-- NOTE "unnest() WITH ORDINALITY" not used because of possible sparse array slice
	, ws AS (
		SELECT time_bucket(b.slot_len, d.ts) AS slot
			, p.idx AS idx
			, d.data_s[p.idx] AS val
		FROM b, d
		INNER JOIN generate_series(1, array_upper(d.data_s, 1)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL AND d.ts >= start_ts AND d.ts < b.he
	)
	, ds AS (
		SELECT ws.slot, ws.idx, mode() WITHIN GROUP (ORDER BY ws.val) AS val
		FROM ws
		GROUP BY ws.slot, ws.idx
	)
	, ds_ary AS (
		SELECT ds.slot, array_agg(ds.val ORDER BY ds.idx) AS data_s
		FROM ds
		GROUP BY ds.slot
	)
	-- tags per slot
	, dt_ary AS (
		SELECT time_bucket(b.slot_len, d.ts) AS slot
			, array_agg(DISTINCT p.val) AS data_t
		FROM b, d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL AND d.ts >= start_ts AND d.ts < b.he
		GROUP BY 1
	)
	SELECT
		  sid AS stream_id
		, r.slot AS ts_start
		, r.data_i
		, r.data_a
		, ds_ary.data_s
		, dt_ary.data_t
		, r.stat_i
		, NULL::NUMERIC[][] AS read_a
	FROM (
		SELECT sa.slot
			, sa.data_i
			, CASE
				-- a property none of the slot's rows has is NULL when another row of the hour has
				-- it, and zero when none has
				WHEN sa.acc IS NOT NULL THEN vec_trim_scale(CASE
					WHEN array_position(sa.acc, NULL) IS NULL THEN sa.acc
					ELSE vec_add(vec_coalesce(sa.acc, 0::numeric), vec_without_outliers(
						vec_sub(vec_coalesce(vec_mul(sa.acc, 0::numeric), 1::numeric)
							, vec_coalesce(sh2.a_mask, 1::numeric))
						, array_fill(0::numeric, ARRAY[b.len_a]), array_fill(0::numeric, ARRAY[b.len_a])))
					END)
				-- the first slot of an hour gets the reading of the datum before the hour, and the
				-- last slot the reading of a datum at the end of the hour
				WHEN (sa.slot = sh2.fs AND sh2.prev_a AND (sh2.has_a OR sh2.next_a))
						OR (sa.slot = sh2.ls AND sh2.end_a)
					THEN array_fill(0::numeric, ARRAY[b.len_a])
				END AS data_a
			-- [count, min, max] per property, as solarcommon.array_transpose2d() would give but
			-- without the cost of a function call per slot
			, (SELECT array_agg(ARRAY[x.c, x.mn, x.mx] ORDER BY x.idx)
				FROM unnest(sa.cnt_i, sa.min_i, sa.max_i) WITH ORDINALITY AS x(c, mn, mx, idx)) AS stat_i
		FROM b, sa
		INNER JOIN sh2 ON sh2.hr = sa.hr
		WHERE sa.slot >= start_ts AND sa.slot < end_ts

		UNION ALL

		-- an hour with no rows, but a datum or reset record with accumulating properties exactly
		-- at its end, gets a row in its last slot
		SELECT sh2.hr - b.slot_len AS slot
			, NULL::NUMERIC[] AS data_i
			, array_fill(0::numeric, ARRAY[b.len_a]) AS data_a
			, NULL::NUMERIC[][] AS stat_i
		FROM b, sh2
		WHERE sh2.a_at_hr AND NOT sh2.prev_rows
			AND sh2.hr - b.slot_len >= start_ts AND sh2.hr - b.slot_len < end_ts
	) r
	LEFT OUTER JOIN ds_ary ON ds_ary.slot = r.slot
	LEFT OUTER JOIN dt_ary ON dt_ary.slot = r.slot
	WHERE r.data_i IS NOT NULL OR r.data_a IS NOT NULL OR ds_ary.data_s IS NOT NULL OR dt_ary.data_t IS NOT NULL
$$;
