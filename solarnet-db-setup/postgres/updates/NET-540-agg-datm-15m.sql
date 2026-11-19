/* ============================================================================
 * NET-540 (step 1): a 15 minute aggregate table, `solardatm.agg_datm_15m`, whose rows are computed
 * by the hourly stale processing, together with each hour.
 *
 * Adds:
 *
 *  - the table `solardatm.agg_datm_15m`, with the columns of `solardatm.agg_datm_hourly`, and its
 *    unique index `agg_datm_15m_pkey`;
 *  - the table `solardatm.agg_datm_15m_setting`, of at most one row, whose `complete_from` is the
 *    "complete from" boundary: hours that start at or after it get their 15 minute rows when they
 *    are processed, and earlier hours get none. This script inserts no row;
 *  - `solardatm.agg_datm_15m_complete_from()`, which reads the boundary;
 *  - `solardatm.rollup_datm_15m_for_hour()`, which returns an hour's row exactly as
 *    `solardatm.rollup_datm_for_time_span()` computes it, together with the hour's 15 minute rows.
 *
 * Replaces, with unchanged signatures, so `CREATE OR REPLACE` keeps their owners and ACLs:
 *
 *  - `solardatm.process_one_agg_stale_datm()`: for an hour that starts at or after the boundary,
 *    it stores the hour and its 15 minute rows in one statement. Unchanged 15 minute rows are not
 *    rewritten, and those the hour no longer has are deleted, all of them when the hour has no
 *    row. It reads the boundary only after it has claimed the stale record. Hours before the
 *    boundary, and days and months, are processed exactly as before;
 *  - `solardatm.delete_datm(UUID, TIMESTAMP, TIMESTAMP, TEXT)`: it also deletes the 15 minute
 *    rows of the whole hours whose hourly rows it deletes.
 *
 * With no boundary row, this script changes no stored value and no result: every hour is processed
 * by the same statement as before, and no 15 minute row can be written. On production the table
 * must first become a hypertable with its chunks seeded and be granted to the app roles, which
 * `solarnetwork-ops/dba/postgres/updates/timescaledb/NET-540-production.sql` does after this
 * migration, setting the boundary last. A database built from the setup DDL has the boundary at
 * `-infinity` instead, so it computes the 15 minute rows of every hour it processes.
 *
 * Rollback: NET-540-agg-datm-15m-rollback.sql.
 * ============================================================================
 */

/**
 * Aggregate 15 minute datum table: one row for each 15 minute slot of an hour that has datum or
 * reset records, with the same columns as `agg_datm_hourly`.
 *
 * The rows of an hour are computed together with the hour itself, when the hour is processed by
 * `solardatm.process_one_agg_stale_datm()`, so they add up to the hour. Hours that start before
 * the `complete_from` time of `solardatm.agg_datm_15m_setting` get no rows when processed.
 *
 * The default fill factor is used, as for `agg_datm_hourly`: a row is rewritten only while its
 * slot is current, and unchanged rows are not rewritten at all.
 *
 * @see solardatm.rollup_datm_15m_for_hour()
 */
CREATE TABLE solardatm.agg_datm_15m (
	stream_id	UUID NOT NULL,
	ts_start	TIMESTAMP WITH TIME ZONE NOT NULL,
	data_i		NUMERIC[],
	data_a		NUMERIC[],
	data_s		TEXT[],
	data_t		TEXT[],
	stat_i		NUMERIC[][],
	read_a		NUMERIC[][]
);

CREATE UNIQUE INDEX IF NOT EXISTS agg_datm_15m_pkey ON solardatm.agg_datm_15m (stream_id, ts_start DESC);

/**
 * Settings of the 15 minute aggregate table, `solardatm.agg_datm_15m`, in at most one row.
 *
 * `complete_from`: hours that start at or after this time get their 15 minute rows when they are
 * processed, so the table is complete from here on; hours that start before it get none. With no
 * row, no hour gets 15 minute rows. A backfill that writes the rows of earlier hours moves it back.
 *
 * Read it with `solardatm.agg_datm_15m_complete_from()`.
 */
CREATE TABLE solardatm.agg_datm_15m_setting (
	id				BOOLEAN NOT NULL DEFAULT TRUE,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	complete_from	TIMESTAMP WITH TIME ZONE NOT NULL,
	CONSTRAINT agg_datm_15m_setting_pkey PRIMARY KEY (id),
	CONSTRAINT agg_datm_15m_setting_one_row CHECK (id)
);

/**
 * Get the time from which the 15 minute aggregate table `solardatm.agg_datm_15m` is complete.
 *
 * Hours that start at or after this time get their 15 minute rows when they are processed; hours
 * that start before it get none.
 *
 * @return the time, `-infinity` if the table is complete for all time, or `NULL` if no hour gets
 *         15 minute rows
 * @see solardatm.agg_datm_15m_setting
 */
CREATE OR REPLACE FUNCTION solardatm.agg_datm_15m_complete_from()
	RETURNS TIMESTAMP WITH TIME ZONE LANGUAGE SQL STABLE AS
$$
	SELECT complete_from FROM solardatm.agg_datm_15m_setting
$$;


/**
 * Compute the rollup datm record of one hour, together with the rollup datm records of the 15
 * minute slots of that hour.
 *
 * The first eight output columns are the hour, exactly as `solardatm.rollup_datm_for_time_span()`
 * computes it for the same arguments, and no row is returned when that function returns none. The
 * `slots` column holds the hour's 15 minute rows in time order, or `NULL` when there are none.
 *
 * The slots are computed from exactly the rows, and the per-row differences and portions, that
 * the hour uses, and each contribution to the hour is given to one or two of its slots, so the
 * slots add up to the hour:
 *
 *  - clock accumulation between two rows goes to the slot of the earlier row, up to the start of
 *    the slot of the later row; the later row's slot gets the remainder of the hour's portion
 *  - a contribution that crosses the start of the hour goes to the slot of the later row, and one
 *    that crosses the end of the hour to the slot of the earlier row
 *  - a reading difference goes to the slot of its row; a row before the hour counts in the first
 *    slot holding a row, and a row exactly at the end of the hour in the last one (or the last
 *    slot of the hour, if no slot holds a row)
 *  - averages, counts, minimums, maximums, status and tags are grouped by slot
 *  - a slot whose rows have no value for an accumulating property gets NULL for it when another
 *    slot of the hour has one, and zero when none has, as the hour reports
 *
 * Slots are 15 minute buckets of UTC time, so the hour must start on a 15 minute boundary, which
 * every hour of a whole, half or quarter hour time zone does.
 *
 * NOTE the CTEs that compute the hour are a copy of the query of
 * `solardatm.rollup_datm_for_time_span()`, with only `p_ts` added to `wa`: a change there must be
 * made here too. Sharing them would make every caller of that function pay for the slots.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the start of the hour (inclusive)
 * @param end_ts 			the end of the hour (exclusive)
 * @param tolerance_clock 	as for `solardatm.rollup_datm_for_time_span()`
 * @param tolerance_read 	as for `solardatm.rollup_datm_for_time_span()`
 * @see solardatm.rollup_datm_for_time_span()
 * @see solardatm.process_one_agg_stale_datm()
 */
CREATE OR REPLACE FUNCTION solardatm.rollup_datm_15m_for_hour(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance_clock INTERVAL DEFAULT INTERVAL 'PT1H',
		tolerance_read 	INTERVAL DEFAULT INTERVAL 'P1Y'
	) RETURNS TABLE (
		stream_id	UUID,
		ts_start	TIMESTAMP WITH TIME ZONE,
		data_i		NUMERIC[],
		data_a		NUMERIC[],
		data_s		TEXT[],
		data_t		TEXT[],
		stat_i		NUMERIC[][],
		read_a		NUMERIC[][],
		slots		solardatm.agg_datm[]
	) LANGUAGE SQL STABLE ROWS 1 AS
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
			-- the time of the row before, for the 15 minute slots
			, lag(d.ts) OVER slot AS p_ts
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

	-- the 15 minute slots of the hour, from here on

	-- slots of the hour holding a datum or reset record
	, occ AS (
		SELECT DISTINCT time_bucket(INTERVAL 'PT15M', d.ts) AS slot
		FROM d
		WHERE d.inc
	)
	-- the first and last of those slots, or the last slot of the hour if there are none
	, edge AS (
		SELECT COALESCE(min(slot), end_ts - INTERVAL 'PT15M') AS first_slot
			 , COALESCE(max(slot), end_ts - INTERVAL 'PT15M') AS last_slot
		FROM occ
	)
	-- instantaneous statistics per slot
	, slot_di_ary AS (
		SELECT
			  time_bucket(INTERVAL 'PT15M', d.ts) AS slot
			, vec_trim_scale(vec_agg_mean(d.data_i)) AS data_i
			, solarcommon.array_transpose2d(ARRAY[
				  vec_agg_count(d.data_i)::numeric[]
				, vec_agg_min(d.data_i)
				, vec_agg_max(d.data_i)
			  ]) AS stat_i
		FROM d
		WHERE d.inc
		GROUP BY 1
	)
	-- the slots of each row and of the row before it, when they are within the hour
	, wb AS (
		SELECT wa.*
			, wa.portion::numeric AS hp
			, CASE WHEN wa.ts >= start_ts AND wa.ts < end_ts THEN time_bucket(INTERVAL 'PT15M', wa.ts) END AS c_slot
			, CASE WHEN wa.p_ts >= start_ts AND wa.p_ts < end_ts THEN time_bucket(INTERVAL 'PT15M', wa.p_ts) END AS p_slot
		FROM wa
	)
	-- the earlier row's share of the hour's portion, when the two rows are in different slots of the hour;
	-- rows without a value for the property have no difference to share out
	, wc AS (
		SELECT wb.*
			, CASE
				WHEN p_slot IS NOT NULL AND c_slot IS NOT NULL AND c_slot > p_slot
					THEN (EXTRACT('epoch' FROM c_slot - p_ts) / EXTRACT('epoch' FROM ts - p_ts))::numeric
				END AS p_share
		FROM wb
		WHERE hp <> 0 AND val IS NOT NULL
	)
	, alloc AS (
		-- to the slot of the earlier row
		SELECT idx, p_slot AS slot, diff * COALESCE(p_share, hp) AS amt
		FROM wc
		WHERE p_slot IS NOT NULL AND (c_slot IS NULL OR c_slot > p_slot)

		UNION ALL

		-- to the slot of the later row: everything, or the remainder after the earlier row's share
		SELECT idx, c_slot AS slot, diff * (hp - COALESCE(p_share, 0)) AS amt
		FROM wc
		WHERE c_slot IS NOT NULL

		UNION ALL

		-- neither row is within the hour
		SELECT idx, edge.last_slot AS slot, diff * hp AS amt
		FROM wc, edge
		WHERE p_slot IS NULL AND c_slot IS NULL
	)
	-- clock accumulation per slot
	, ca AS (
		SELECT idx, slot, sum(amt) AS cdiff
		FROM alloc
		GROUP BY idx, slot
	)
	-- the rows the hour's readings count, each in its slot
	, rd AS (
		SELECT wa.idx
			, CASE
				WHEN wa.ts < start_ts THEN edge.first_slot
				WHEN wa.ts < end_ts THEN time_bucket(INTERVAL 'PT15M', wa.ts)
				ELSE edge.last_slot
				END AS slot
			, wa.val
			, wa.diff
			, wa.ts
			, wa.rtype
		FROM wa, edge
		WHERE wa.rinc
	)
	-- readings per slot
	, ra0 AS (
		SELECT idx, slot
			, sum(diff) AS rdiff
			, solarcommon.first(val ORDER BY ts, rtype) AS rstart_first
			, solarcommon.first(val - diff ORDER BY ts, rtype) AS rstart_later
			, solarcommon.first(val ORDER BY ts DESC, rtype DESC) AS rend
		FROM rd
		GROUP BY idx, slot
	)
	-- for each property: whether any slot of the hour has a value for it, and the first slot that does
	, ra AS (
		SELECT ra0.*
			, bool_or(ra0.rend IS NOT NULL) OVER (PARTITION BY ra0.idx) AS hour_has
			, ra0.slot = min(ra0.slot) FILTER (WHERE ra0.rend IS NOT NULL) OVER (PARTITION BY ra0.idx) AS is_first
		FROM ra0
	)
	, slot_ts AS (
		SELECT slot FROM occ
		UNION
		SELECT slot FROM ca
		UNION
		SELECT slot FROM ra
	)
	-- A slot whose rows have no value for a property gets NULL for it when another slot of the hour
	-- has one, so that rolling the slots up takes the hour's first and last readings from slots that
	-- have them. When no slot has a value the hourly rollup reports zero, and so does each slot.
	, slot_da_ary AS (
		SELECT s.slot
			, CASE WHEN count(ca.idx) + count(ra.idx) > 0 THEN
				vec_trim_scale(array_agg(
					CASE
						WHEN ra.rend IS NOT NULL OR ca.cdiff <> 0 THEN COALESCE(ca.cdiff, 0)
						WHEN ra.hour_has THEN NULL
						WHEN ra.idx IS NOT NULL THEN 0
						END
					ORDER BY p.idx))
				END AS data_a
			, CASE WHEN count(ra.idx) > 0 THEN
				array_agg(ARRAY[
					  ra.rdiff
					, CASE WHEN ra.is_first THEN ra.rstart_first ELSE ra.rstart_later END
					, ra.rend
				] ORDER BY p.idx)
				END AS read_a
		FROM slot_ts s
		CROSS JOIN m
		CROSS JOIN generate_series(1, m.len_a) AS p(idx)
		LEFT OUTER JOIN ca ON ca.slot = s.slot AND ca.idx = p.idx
		LEFT OUTER JOIN ra ON ra.slot = s.slot AND ra.idx = p.idx
		GROUP BY s.slot
	)
	-- status per slot, as most-frequent status values
	, slot_ws AS (
		SELECT
			  time_bucket(INTERVAL 'PT15M', d.ts) AS slot
			, p.idx AS idx
			, d.data_s[p.idx] AS val
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_s, 1)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL AND d.inc
	)
	, slot_ds AS (
		SELECT slot_ws.slot, slot_ws.idx, mode() WITHIN GROUP (ORDER BY slot_ws.val) AS val
		FROM slot_ws
		GROUP BY slot_ws.slot, slot_ws.idx
	)
	, slot_ds_ary AS (
		SELECT slot, array_agg(val ORDER BY idx) AS data_s
		FROM slot_ds
		GROUP BY slot
	)
	-- tags per slot
	, slot_dt_ary AS (
		SELECT time_bucket(INTERVAL 'PT15M', d.ts) AS slot
			, array_agg(DISTINCT p.val) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL AND d.inc
		GROUP BY 1
	)
	-- the slot rows, in time order
	, slot_ary AS (
		SELECT array_agg(ROW(
				  sid
				, slot_ts.slot
				, slot_di_ary.data_i
				, slot_da_ary.data_a
				, slot_ds_ary.data_s
				, slot_dt_ary.data_t
				, slot_di_ary.stat_i
				, slot_da_ary.read_a
			)::solardatm.agg_datm ORDER BY slot_ts.slot) AS slots
		FROM slot_ts
		LEFT OUTER JOIN slot_di_ary ON slot_di_ary.slot = slot_ts.slot
		LEFT OUTER JOIN slot_da_ary ON slot_da_ary.slot = slot_ts.slot
		LEFT OUTER JOIN slot_ds_ary ON slot_ds_ary.slot = slot_ts.slot
		LEFT OUTER JOIN slot_dt_ary ON slot_dt_ary.slot = slot_ts.slot
		WHERE slot_di_ary.data_i IS NOT NULL OR slot_da_ary.data_a IS NOT NULL
			OR slot_ds_ary.data_s IS NOT NULL OR slot_dt_ary.data_t IS NOT NULL
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
		, slot_ary.slots
	FROM di_ary, da_ary, ds_ary, dt_ary, slot_ary
	WHERE data_i IS NOT NULL OR data_a IS NOT NULL OR data_s IS NOT NULL OR data_t IS NOT NULL
$$;

/**
 * Compute a single stale aggregate datum rollup and store the results in the appropriate table.
 *
 * After saving the rollup value, if there is a higher-level aggregate above the given `kind` then
 * a new stale aggregate datum record will be inserted into the `stale_agg_datum` table for that
 * higher aggregate level. For example if `kind` is `h` then a `d` stale record will be inserted.
 *
 * When processing a `d` aggregate, 3 `aud_stale_datm` records will be inserted for the
 * `0`, `h`, and `d` aggregate levels, so the associated audit values for the stale aggregate
 * period can be computed.
 *
 * When processing an `h` aggregate for an hour that starts at or after the 15 minute boundary of
 * `solardatm.agg_datm_15m_complete_from()`, the hour and its 15 minute rows are computed together
 * by `solardatm.rollup_datm_15m_for_hour()`, and both are stored in one statement: the hour as
 * before, then each 15 minute row that is new or has changed, while any 15 minute row of the hour
 * that was not computed is deleted, as are all of them when the hour has no row. The boundary is
 * read only after the stale record is claimed, so a change to it applies to every hour claimed
 * after it commits. Hours that start before the boundary are processed exactly as before, and their
 * 15 minute rows are neither written nor deleted.
 *
 * @param kind 				the aggregate kind: 'h', 'd', or 'M' for daily, hourly, monthly
 * @see solardatm.rollup_datm_for_time_span()
 * @see solardatm.rollup_datm_15m_for_hour()
 * @see solardatm.rollup_agg_data_for_time_span()
 */
CREATE OR REPLACE FUNCTION solardatm.process_one_agg_stale_datm(kind CHARACTER)
	RETURNS SETOF solardatm.obj_datm_id LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	agg_span 				INTERVAL;
	dest_name				TEXT;

	stale 					solardatm.agg_stale_datm;
	meta					record;
	tz						TEXT;

	local_ts_start			TIMESTAMP;
	local_ts_end			TIMESTAMP;
	ts_end					TIMESTAMP WITH TIME ZONE;
	ts_prevstart			TIMESTAMP WITH TIME ZONE;

	num_rows				BIGINT;
	complete_15m			TIMESTAMP WITH TIME ZONE;

	result_row				solardatm.obj_datm_id;
	flux_pub 				solardatm.flux_pub_settings;
BEGIN
	CASE kind
		WHEN 'd' THEN
			agg_span := interval '1 day';
			dest_name := 'agg_datm_daily';
		WHEN 'M' THEN
			agg_span := interval '1 month';
			dest_name := 'agg_datm_monthly';
		ELSE
			agg_span := interval '1 hour';
			dest_name := 'agg_datm_hourly';
	END CASE;

	-- use a limited delete here to immediately lock the row and block future concurrent
	-- datum solardatm.store_datum() that inserts same row back into solardatm.agg_stale_datm
	WITH del AS (
		SELECT stream_id, ts_start, agg_kind
		FROM solardatm.agg_stale_datm
		WHERE agg_kind = kind
		FOR UPDATE SKIP LOCKED
		LIMIT 1
	)
	DELETE FROM solardatm.agg_stale_datm d
	USING del
	WHERE d.stream_id = del.stream_id
		AND d.ts_start = del.ts_start
		AND d.agg_kind = del.agg_kind
	RETURNING d.stream_id, d.ts_start, d.agg_kind
	INTO stale.stream_id, stale.ts_start, stale.agg_kind;

	IF FOUND THEN
		-- get stream metadata & time zone; will determine if node or location stream
		SELECT * FROM solardatm.find_metadata_for_stream(stale.stream_id) INTO meta;
		tz := COALESCE(meta.time_zone, 'UTC');
		result_row := (stale.stream_id, stale.ts_start, stale.agg_kind, meta.obj_id, meta.source_id, meta.kind);

		-- stash local start/end dates to work with calendar intervals
		-- the ts_prevstart is used to deal with tz changes with streams
		local_ts_start := stale.ts_start AT TIME ZONE tz;
		local_ts_end   := local_ts_start + agg_span;
		ts_end         := CASE kind WHEN 'h' THEN stale.ts_start + agg_span ELSE local_ts_end AT TIME ZONE tz END;
		ts_prevstart   := CASE kind WHEN 'h' THEN stale.ts_start - agg_span ELSE (local_ts_start - agg_span) AT TIME ZONE tz END;

		IF kind = 'h' THEN
			-- NOTE read only after the stale record is claimed above: the NET-540 production
			-- script that first sets the boundary relies on this
			complete_15m := solardatm.agg_datm_15m_complete_from();
		END IF;

		BEGIN
			IF kind = 'h' AND stale.ts_start >= complete_15m THEN
				-- store the hour and its 15 minute rows, all computed together; a 15 minute row is
				-- rewritten only when it has changed, and those of the hour not computed are deleted
				-- (all of them, when the hour has no row)
				EXECUTE
						'WITH r AS ('
						'    SELECT * FROM solardatm.rollup_datm_15m_for_hour($1, $2, $3)'
						')'
						', h AS ('
						'    INSERT INTO solardatm.agg_datm_hourly (stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a) '
						'    SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
						'    FROM r '
						'    ON CONFLICT (stream_id, ts_start) DO UPDATE SET '
						'        data_i = EXCLUDED.data_i, '
						'        data_a = EXCLUDED.data_a, '
						'        data_s = EXCLUDED.data_s, '
						'        data_t = EXCLUDED.data_t, '
						'        stat_i = EXCLUDED.stat_i, '
						'        read_a = EXCLUDED.read_a '
						'    RETURNING 1'
						')'
						', s AS ('
						'    SELECT x.* FROM r, unnest(r.slots) x'
						')'
						', su AS ('
						'    INSERT INTO solardatm.agg_datm_15m (stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a) '
						'    SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
						'    FROM s '
						'    ON CONFLICT (stream_id, ts_start) DO UPDATE SET '
						'        data_i = EXCLUDED.data_i, '
						'        data_a = EXCLUDED.data_a, '
						'        data_s = EXCLUDED.data_s, '
						'        data_t = EXCLUDED.data_t, '
						'        stat_i = EXCLUDED.stat_i, '
						'        read_a = EXCLUDED.read_a '
						'    WHERE (agg_datm_15m.data_i, agg_datm_15m.data_a, agg_datm_15m.data_s, '
						'            agg_datm_15m.data_t, agg_datm_15m.stat_i, agg_datm_15m.read_a) '
						'        IS DISTINCT FROM (EXCLUDED.data_i, EXCLUDED.data_a, EXCLUDED.data_s, '
						'            EXCLUDED.data_t, EXCLUDED.stat_i, EXCLUDED.read_a)'
						')'
						', sd AS ('
						'    DELETE FROM solardatm.agg_datm_15m d '
						'    WHERE d.stream_id = $1 AND d.ts_start >= $2 AND d.ts_start < $3 '
						'        AND NOT EXISTS (SELECT 1 FROM s WHERE s.ts_start = d.ts_start)'
						')'
						'SELECT count(*) FROM h'
				INTO num_rows
				USING stale.stream_id, stale.ts_start, ts_end;
			ELSEIF kind = 'h' THEN
				EXECUTE format(
						'INSERT INTO solardatm.%I (stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a) '
						'SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
						'FROM solardatm.rollup_datm_for_time_span($1, $2, $3) '
						'ON CONFLICT (stream_id, ts_start) DO UPDATE SET '
						'    data_i = EXCLUDED.data_i, '
						'    data_a = EXCLUDED.data_a, '
						'    data_s = EXCLUDED.data_s, '
						'    data_t = EXCLUDED.data_t, '
						'    stat_i = EXCLUDED.stat_i, '
						'    read_a = EXCLUDED.read_a'
						, dest_name)
				USING stale.stream_id, stale.ts_start, ts_end;
				GET DIAGNOSTICS num_rows = ROW_COUNT;
			ELSE
				EXECUTE format(
						'INSERT INTO solardatm.%I (stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a) '
						'SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
						'FROM solardatm.rollup_agg_data_for_time_span($1, $2, $3, $4) '
						'ON CONFLICT (stream_id, ts_start) DO UPDATE SET '
						'    data_i = EXCLUDED.data_i,'
						'    data_a = EXCLUDED.data_a,'
						'    data_s = EXCLUDED.data_s,'
						'    data_t = EXCLUDED.data_t,'
						'    stat_i = EXCLUDED.stat_i,'
						'    read_a = EXCLUDED.read_a'
						, dest_name)
				USING stale.stream_id, stale.ts_start, ts_end, CASE kind WHEN 'M' THEN 'd' ELSE 'h' END;
				GET DIAGNOSTICS num_rows = ROW_COUNT;
			END IF;
		EXCEPTION WHEN invalid_text_representation THEN
			RAISE EXCEPTION 'Invalid text representation processing stream % aggregate % range % - %',
				stale.stream_id, kind, stale.ts_start, ts_end
			USING ERRCODE = 'invalid_text_representation',
				SCHEMA = 'solardatm',
				TABLE = dest_name,
				HINT = 'Check the solardatm.rollup_datm_for_time_span()/da_datum or solardatm.rollup_agg_data_for_time_span()/solardatm.find_agg_datm_for_time_span() with matching stream/date range parameters.';
		END;

		IF num_rows < 1 THEN
			-- delete everything within time span, using >ts_prevstart to handle tz changes
			EXECUTE format(
					'DELETE FROM solardatm.%I '
					'WHERE stream_id = $1 AND ts_start > $2 AND ts_start < $3'
					, dest_name)
			USING stale.stream_id, ts_prevstart, ts_end;
		ELSEIF kind <> 'h' THEN
			-- delete everything but inserted row, using >ts_prevstart to handle tz changes
			EXECUTE format(
					'DELETE FROM solardatm.%I '
					'WHERE stream_id = $1 AND ts_start > $2 AND ts_start < $3 '
					'    AND ts_start <> $4'
					, dest_name)
			USING stale.stream_id, ts_prevstart, ts_end, stale.ts_start;
		END IF;

		-- now make sure we recalculate the next aggregate level by submitting a stale record
		-- for the next level; also update daily audit stats
		CASE kind
			WHEN 'h' THEN
				INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
				VALUES (stale.stream_id, date_trunc('day', local_ts_start) AT TIME ZONE tz, 'd')
				ON CONFLICT DO NOTHING;

			WHEN 'd' THEN
				INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
				VALUES (stale.stream_id, date_trunc('month', local_ts_start) AT TIME ZONE tz, 'M')
				ON CONFLICT DO NOTHING;

				-- handle update to raw audit data
				INSERT INTO solardatm.aud_stale_datm (stream_id, ts_start, aud_kind)
				VALUES (stale.stream_id, date_trunc('day', local_ts_start) AT TIME ZONE tz, '0')
				ON CONFLICT DO NOTHING;

				-- handle update to hourly audit data
				INSERT INTO solardatm.aud_stale_datm (stream_id, ts_start, aud_kind)
				VALUES (stale.stream_id, date_trunc('day', local_ts_start) AT TIME ZONE tz, 'h')
				ON CONFLICT DO NOTHING;

				-- handle update to daily audit data
				INSERT INTO solardatm.aud_stale_datm (stream_id, ts_start, aud_kind)
				VALUES (stale.stream_id, date_trunc('day', local_ts_start) AT TIME ZONE tz, 'd')
				ON CONFLICT DO NOTHING;
			ELSE
				-- handle update to monthly audit data
				INSERT INTO solardatm.aud_stale_datm (stream_id, ts_start, aud_kind)
				VALUES (stale.stream_id, date_trunc('month', local_ts_start) AT TIME ZONE tz, 'M')
				ON CONFLICT DO NOTHING;
		END CASE;

		-- mark flux stale if node datum and processed record is for the "current" time
		-- TODO: consider publishing location datum as well; would require support in SolarJobs
		IF meta.kind = 'n' AND local_ts_start = date_trunc(
							CASE kind WHEN 'h' THEN 'hour' WHEN 'd' THEN 'day' ELSE 'month' END
							, CURRENT_TIMESTAMP AT TIME ZONE tz) THEN
			SELECT * FROM solardatm.flux_agg_pub_settings(result_row.obj_id, result_row.source_id) INTO flux_pub;
			IF FOUND AND flux_pub.publish THEN
				INSERT INTO solardatm.agg_stale_flux (stream_id, agg_kind)
				VALUES (stale.stream_id, kind)
				ON CONFLICT (stream_id, agg_kind) DO NOTHING;
			END IF;
		END IF;

		RETURN NEXT result_row;
	END IF;
END;
$$;

/**
 * Delete datum rows matching a stream ID and a local date range.
 *
 * The time zones of each node are used to calculate absolute date ranges for each node.
 *
 * @param sid the datum stream ID
 * @param ts_min the starting local date
 * @param ts_max the ending local date
 * @param tz the time zone of the stream
 */
CREATE OR REPLACE FUNCTION solardatm.delete_datm(
	sid			UUID,
	ts_min 		TIMESTAMP,
	ts_max 		TIMESTAMP,
	tz			TEXT
) RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS $$
DECLARE
	start_date timestamp := COALESCE(ts_min, CURRENT_TIMESTAMP);
	end_date timestamp := COALESCE(ts_max, CURRENT_TIMESTAMP);
	total_count bigint := 0;
BEGIN
	WITH audit AS (
		UPDATE solardatm.aud_datm_daily d
		SET datum_count = 0, datum_daily_pres = FALSE
		WHERE d.stream_id = sid
			-- whole days only; partial to be handled by stale processing
			AND d.ts_start >= CASE
				WHEN date_trunc('day', ts_min) = ts_min THEN ts_min AT TIME ZONE tz
				ELSE date_trunc('day', ts_min) AT TIME ZONE tz + interval '1 day'
				END
			AND d.ts_start < date_trunc('day', ts_max) AT TIME ZONE tz
	)
	, hourly AS (
		DELETE FROM solardatm.agg_datm_hourly d
		WHERE d.stream_id = sid
			-- whole hours (ceil) only; partial to be handled by stale processing
			AND d.ts_start >= date_trunc('hour', ts_min) AT TIME ZONE tz + interval '1 hour'
			AND d.ts_start < date_trunc('hour', ts_max) AT TIME ZONE tz
		RETURNING d.ts_start
	)
	, fifteen AS (
		-- the 15 minute rows of the hours deleted above, which are taken from the deleted rows
		-- because hourly rows start on the hours of the session time zone, not always the
		-- stream's; those of partial hours are handled by stale processing, with the hours
		DELETE FROM solardatm.agg_datm_15m d
		WHERE d.stream_id = sid
			AND d.ts_start >= (SELECT min(h.ts_start) FROM hourly h)
			AND d.ts_start < (SELECT max(h.ts_start) FROM hourly h) + interval '1 hour'
	)
	, daily AS (
		DELETE FROM solardatm.agg_datm_daily d
		WHERE d.stream_id = sid
			-- whole days only; partial to be handled by stale processing
			AND d.ts_start >= CASE
				WHEN date_trunc('day', ts_min) = ts_min THEN ts_min AT TIME ZONE tz
				ELSE date_trunc('day', ts_min) AT TIME ZONE tz + interval '1 day'
				END
			AND d.ts_start < date_trunc('day', ts_max) AT TIME ZONE tz
	)
	, monthly AS (
		DELETE FROM solardatm.agg_datm_monthly d
		WHERE d.stream_id = sid
			-- whole months only; partial to be handled by stale processing
			AND d.ts_start >= CASE
				WHEN date_trunc('month', ts_min) = ts_min THEN ts_min AT TIME ZONE tz
				ELSE date_trunc('month', ts_min) AT TIME ZONE tz + interval '1 month'
				END
			AND d.ts_start < date_trunc('month', ts_max)
	)
	DELETE FROM solardatm.da_datm d
	WHERE d.stream_id = sid
		AND d.ts >= ts_min AT TIME ZONE tz
		AND d.ts < ts_max AT TIME ZONE tz;
	GET DIAGNOSTICS total_count = ROW_COUNT;

	IF total_count > 0 THEN
		-- mark remaining hourly aggregates as stale, so partial hours/days/months recalculated
		WITH dates AS (
			SELECT sid AS stream_id, ts_min AT TIME ZONE tz AS ts_start
			UNION ALL
			SELECT sid AS stream_id, ts_max AT TIME ZONE tz AS ts_start
		)
		INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
		SELECT s.stream_id, s.ts_start, 'h' AS agg_kind
		FROM dates, solardatm.calc_stale_datm(dates.stream_id, dates.ts_start) s
		ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
	END IF;

	--GET DIAGNOSTICS stale_count = ROW_COUNT;
	--RAISE NOTICE 'INSERTED % solardatm.agg_stale_datm rows after delete.', stale_count;

	RETURN total_count;
END
$$;
