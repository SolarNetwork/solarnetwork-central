/* ============================================================================
 * NET-527: compute daily audit rollup windows in the entity time zone.
 *
 * The daily rollup windows were computed as `stale.ts_start + interval '1 day'`. For a
 * `timestamptz` Postgres evaluates that in the *session* time zone, not the stream, node, or
 * user time zone that `stale.ts_start` is a local midnight in, so the window was 24 hours long
 * whenever the session zone had no daylight saving transition on that day. On a transition day
 * in the entity zone that meant the final local hour of a 25-hour day was left out of the
 * rollup, and the first local hour of the day following a 23-hour day was counted in both days.
 *
 * The "time zone changed" cleanup windows either side of the day had the same problem, and
 * there it could delete a legitimate adjacent daily row: 24 hours before a local midnight that
 * follows a 23-hour day reaches back past the previous local midnight.
 *
 * These now use the same local-zone form the monthly branches already used, so each window
 * spans one day in the entity time zone whatever the session time zone is.
 *
 * The function signatures are unchanged, so `CREATE OR REPLACE` keeps their existing owner and
 * ACLs and no companion production DDL is needed.
 * ============================================================================
 */

/**
 * Compute a single stale audit datum rollup and store the results in the
 * `solardatm.aud_datm_daily`, `solardatm.aud_datm_monthly`, and/or `aud_acc_datm_daily` tables.
 *
 * After saving the rollup value for any kind except `M`, a new stale audit datum record will be
 * inserted into the `aud_stale_datm` table for the `M` kind.
 *
 * @param kind 				the aggregate kind: '0', 'h', 'd', or 'M' for raw, daily, hourly, monthly
 *
 * @see solardatm.calc_audit_datm_raw()
 * @see solardatm.calc_audit_datm_hourly()
 * @see solardatm.calc_audit_datm_daily()
 * @see solardatm.calc_audit_datm_monthly()
 * @see solardatm.calc_audit_datm_acc()
 */
CREATE OR REPLACE FUNCTION solardatm.process_one_aud_stale_datm(kind CHARACTER)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					solardatm.aud_stale_datm;
	meta					record;
	tz						TEXT;
	result_cnt 				INTEGER := 0;
BEGIN
	-- use a limited delete here to immediately lock the row and block future concurrent
	-- updates that insert same row back into solardatm.aud_stale_datm
	WITH del AS (
		SELECT stream_id, ts_start, aud_kind
		FROM solardatm.aud_stale_datm
		WHERE aud_kind = kind
		FOR UPDATE SKIP LOCKED
		LIMIT 1
	)
	DELETE FROM solardatm.aud_stale_datm d
	USING del
	WHERE d.stream_id = del.stream_id
		AND d.ts_start = del.ts_start
		AND d.aud_kind = del.aud_kind
	RETURNING d.stream_id, d.ts_start, d.aud_kind
	INTO stale.stream_id, stale.ts_start, stale.aud_kind;

	IF FOUND THEN
		-- get stream time zone; will determine if node or location stream
		SELECT * FROM solardatm.find_metadata_for_stream(stale.stream_id) INTO meta;
		tz := COALESCE(meta.time_zone, 'UTC');

		CASE kind
			WHEN '0' THEN
				-- raw data counts
				INSERT INTO solardatm.aud_datm_daily (stream_id, ts_start, datum_count, processed_count)
				SELECT stream_id, ts_start, datum_count, CURRENT_TIMESTAMP AS processed_count
				FROM solardatm.calc_audit_datm_raw(
					stale.stream_id, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz)
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					processed_count = EXCLUDED.processed_count;

			WHEN 'h' THEN
				-- hour data counts
				INSERT INTO solardatm.aud_datm_daily (stream_id, ts_start, datum_hourly_count, processed_hourly_count)
				SELECT stream_id, ts_start, datum_hourly_count, CURRENT_TIMESTAMP AS processed_hourly_count
				FROM solardatm.calc_audit_datm_hourly(
					stale.stream_id, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz)
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_hourly_count = EXCLUDED.datum_hourly_count,
					processed_hourly_count = EXCLUDED.processed_hourly_count;

			WHEN 'd' THEN
				-- day data counts, including sum of hourly audit prop_count, datum_q_count, flux_byte_count
				INSERT INTO solardatm.aud_datm_daily (stream_id, ts_start,
					datum_daily_pres, prop_count, prop_u_count, datum_q_count, flux_byte_count, processed_io_count)
				SELECT stream_id, ts_start, datum_daily_pres, prop_count, prop_u_count, datum_q_count,
					flux_byte_count, CURRENT_TIMESTAMP AS processed_io_count
				FROM solardatm.calc_audit_datm_daily(
					stale.stream_id, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz)
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_daily_pres = EXCLUDED.datum_daily_pres,
					prop_count = EXCLUDED.prop_count,
					prop_u_count = EXCLUDED.prop_u_count,
					datum_q_count = EXCLUDED.datum_q_count,
					flux_byte_count = EXCLUDED.flux_byte_count,
					processed_io_count = EXCLUDED.processed_io_count;

			ELSE
				-- month data counts
				INSERT INTO solardatm.aud_datm_monthly (stream_id, ts_start,
					datum_count, datum_hourly_count, datum_daily_count, datum_monthly_pres,
					prop_count, prop_u_count, datum_q_count, flux_byte_count, processed)
				SELECT stream_id, ts_start, datum_count, datum_hourly_count, datum_daily_count,
					datum_monthly_pres, prop_count, prop_u_count, datum_q_count,
					flux_byte_count, CURRENT_TIMESTAMP AS processed
				FROM solardatm.calc_audit_datm_monthly(
					stale.stream_id, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz)
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					datum_hourly_count = EXCLUDED.datum_hourly_count,
					datum_daily_count = EXCLUDED.datum_daily_count,
					datum_monthly_pres = EXCLUDED.datum_monthly_pres,
					prop_count = EXCLUDED.prop_count,
					prop_u_count = EXCLUDED.prop_u_count,
					datum_q_count = EXCLUDED.datum_q_count,
					flux_byte_count = EXCLUDED.flux_byte_count,
					processed = EXCLUDED.processed;
		END CASE;

		CASE kind
			WHEN 'M' THEN
				-- in case node tz changed, remove record(s) from other zone
				-- monthly records clean 1 month on either side
				DELETE FROM solardatm.aud_datm_monthly a
				WHERE a.stream_id = stale.stream_id
					AND a.ts_start > (stale.ts_start AT TIME ZONE tz - interval '1 month') AT TIME ZONE tz
					AND a.ts_start < (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz
					AND a.ts_start <> stale.ts_start;

				-- recalculate full accumulated audit counts for today
				INSERT INTO solardatm.aud_acc_datm_daily (stream_id, ts_start,
					datum_count, datum_hourly_count, datum_daily_count, datum_monthly_count,
					processed)
				SELECT stream_id, ts_start,
					COALESCE(datum_count, 0) AS datum_count,
					COALESCE(datum_hourly_count, 0) AS datum_hourly_count,
					COALESCE(datum_daily_count, 0) AS datum_daily_count,
					COALESCE(datum_monthly_count, 0) AS datum_monthly_count,
					CURRENT_TIMESTAMP
				FROM solardatm.calc_audit_datm_acc(stale.stream_id)
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					datum_hourly_count = EXCLUDED.datum_hourly_count,
					datum_daily_count = EXCLUDED.datum_daily_count,
					datum_monthly_count = EXCLUDED.datum_monthly_count,
					processed = EXCLUDED.processed;
			ELSE
				-- in case node tz changed, remove record(s) from other zone
				-- daily records clean 1 day on either side
				DELETE FROM solardatm.aud_datm_daily
				WHERE stream_id = stale.stream_id
					AND ts_start > (stale.ts_start AT TIME ZONE tz - interval '1 day') AT TIME ZONE tz
					AND ts_start < (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz
					AND ts_start <> stale.ts_start;

				-- recalculate monthly audit based on updated daily values
				INSERT INTO solardatm.aud_stale_datm (stream_id, ts_start, aud_kind)
				VALUES (
					stale.stream_id,
					date_trunc('month', stale.ts_start AT TIME ZONE tz) AT TIME ZONE tz,
					'M')
				ON CONFLICT DO NOTHING;
		END CASE;

		-- remove processed stale record
		result_cnt := 1;
	END IF;
	RETURN result_cnt;
END;
$$;


/**
 * Compute a single stale audit node rollup and store the results in the
 * `solardatm.aud_node_daily` or `solardatm.aud_node_monthly` tables.
 *
 * After saving the rollup value for any kind except `M`, a new stale audit datum record will be
 * inserted into the `aud_stale_node` table for the `M` kind.
 *
 * @param kind the aggregate kind: 'd' or 'M' for  daily or monthly
 *
 * @see solardatm.calc_audit_node_daily()
 * @see solardatm.calc_audit_node_monthly()
 */
CREATE OR REPLACE FUNCTION solardatm.process_one_aud_stale_node(kind CHARACTER)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					solardatm.aud_stale_node;
	tz						TEXT;
	result_cnt 				INTEGER := 0;
BEGIN
	-- use a limited delete here to immediately lock the row and block future concurrent
	-- updates that insert same row back into solardatm.aud_stale_node
	WITH del AS (
		SELECT node_id, service, ts_start, aud_kind
		FROM solardatm.aud_stale_node
		WHERE aud_kind = kind
		FOR UPDATE SKIP LOCKED
		LIMIT 1
	)
	DELETE FROM solardatm.aud_stale_node d
	USING del
	WHERE d.node_id = del.node_id
		AND d.service = del.service
		AND d.ts_start = del.ts_start
		AND d.aud_kind = del.aud_kind
	RETURNING d.node_id, d.service, d.ts_start, d.aud_kind
	INTO stale.node_id, stale.service, stale.ts_start, stale.aud_kind;

	IF FOUND THEN
		-- get node time zone; will determine if node or location stream
		SELECT COALESCE(solarnet.get_node_timezone(stale.node_id), 'UTC') INTO tz;

		CASE kind
			WHEN 'd' THEN
				-- day data counts summerized from hourly data
				INSERT INTO solardatm.aud_node_daily (node_id, service, ts_start, cnt, processed)
				SELECT node_id, service, ts_start, cnt, CURRENT_TIMESTAMP AS processed
				FROM solardatm.calc_audit_node_daily(
					stale.node_id, stale.service, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz)
				ON CONFLICT (node_id, service, ts_start) DO UPDATE
				SET cnt = EXCLUDED.cnt, processed = EXCLUDED.processed;

			ELSE
				-- month data counts summerized from daily data
				INSERT INTO solardatm.aud_node_monthly (node_id, service, ts_start, cnt, processed)
				SELECT node_id, service, ts_start, cnt, CURRENT_TIMESTAMP AS processed
				FROM solardatm.calc_audit_node_monthly(
					stale.node_id, stale.service, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz)
				ON CONFLICT (node_id, service, ts_start) DO UPDATE
				SET cnt = EXCLUDED.cnt, processed = EXCLUDED.processed;
		END CASE;

		CASE kind
			WHEN 'M' THEN
				-- in case node tz changed, remove record(s) from other zone
				-- monthly records clean 1 month on either side
				DELETE FROM solardatm.aud_node_monthly
				WHERE node_id = stale.node_id
					AND service = stale.service
					AND ts_start > (stale.ts_start AT TIME ZONE tz - interval '1 month') AT TIME ZONE tz
					AND ts_start < (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz
					AND ts_start <> stale.ts_start;
			ELSE
				-- in case node tz changed, remove record(s) from other zone
				-- daily records clean 1 day on either side
				DELETE FROM solardatm.aud_node_daily
				WHERE node_id = stale.node_id
					AND service = stale.service
					AND ts_start > (stale.ts_start AT TIME ZONE tz - interval '1 day') AT TIME ZONE tz
					AND ts_start < (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz
					AND ts_start <> stale.ts_start;

				-- recalculate monthly audit based on updated daily values
				INSERT INTO solardatm.aud_stale_node (node_id, service, ts_start, aud_kind)
				VALUES (
					stale.node_id,
					stale.service,
					date_trunc('month', stale.ts_start AT TIME ZONE tz) AT TIME ZONE tz,
					'M')
				ON CONFLICT DO NOTHING;
		END CASE;

		result_cnt := 1;
	END IF;
	RETURN result_cnt;
END
$$;


/**
 * Compute a single stale audit user rollup and store the results in the
 * `solardatm.aud_user_daily` or `solardatm.aud_user_monthly` tables.
 *
 * After saving the rollup value for any kind except `M`, a new stale audit datum record will be
 * inserted into the `aud_stale_user` table for the `M` kind.
 *
 * @param kind the aggregate kind: 'd' or 'M' for  daily or monthly
 *
 * @see solardatm.calc_audit_user_daily()
 * @see solardatm.calc_audit_user_monthly()
 */
CREATE OR REPLACE FUNCTION solardatm.process_one_aud_stale_user(kind CHARACTER)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					solardatm.aud_stale_user;
	tz						TEXT;
	result_cnt 				INTEGER := 0;
BEGIN
	-- use a limited delete here to immediately lock the row and block future concurrent
	-- updates that insert same row back into solardatm.aud_stale_user
	WITH del AS (
		SELECT user_id, service, ts_start, aud_kind
		FROM solardatm.aud_stale_user
		WHERE aud_kind = kind
		FOR UPDATE SKIP LOCKED
		LIMIT 1
	)
	DELETE FROM solardatm.aud_stale_user d
	USING del
	WHERE d.user_id = del.user_id
		AND d.service = del.service
		AND d.ts_start = del.ts_start
		AND d.aud_kind = del.aud_kind
	RETURNING d.user_id, d.service, d.ts_start, d.aud_kind
	INTO stale.user_id, stale.service, stale.ts_start, stale.aud_kind;

	IF FOUND THEN
		-- get user time zone; will determine if node or location stream
		SELECT COALESCE(solaruser.get_user_timezone(stale.user_id), 'UTC') INTO tz;

		CASE kind
			WHEN 'd' THEN
				-- day data counts summerized from hourly data
				INSERT INTO solardatm.aud_user_daily (user_id, service, ts_start, cnt, processed)
				SELECT user_id, service, ts_start, cnt, CURRENT_TIMESTAMP AS processed
				FROM solardatm.calc_audit_user_daily(
					stale.user_id, stale.service, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz)
				ON CONFLICT (user_id, service, ts_start) DO UPDATE
				SET cnt = EXCLUDED.cnt, processed = EXCLUDED.processed;

			ELSE
				-- month data counts summerized from daily data
				INSERT INTO solardatm.aud_user_monthly (user_id, service, ts_start, cnt, processed)
				SELECT user_id, service, ts_start, cnt, CURRENT_TIMESTAMP AS processed
				FROM solardatm.calc_audit_user_monthly(
					stale.user_id, stale.service, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz)
				ON CONFLICT (user_id, service, ts_start) DO UPDATE
				SET cnt = EXCLUDED.cnt, processed = EXCLUDED.processed;
		END CASE;

		CASE kind
			WHEN 'M' THEN
				-- in case user tz changed, remove record(s) from other zone
				-- monthly records clean 1 month on either side
				DELETE FROM solardatm.aud_user_monthly
				WHERE user_id = stale.user_id
					AND service = stale.service
					AND ts_start > (stale.ts_start AT TIME ZONE tz - interval '1 month') AT TIME ZONE tz
					AND ts_start < (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz
					AND ts_start <> stale.ts_start;
			ELSE
				-- in case user tz changed, remove record(s) from other zone
				-- daily records clean 1 day on either side
				DELETE FROM solardatm.aud_user_daily
				WHERE user_id = stale.user_id
					AND service = stale.service
					AND ts_start > (stale.ts_start AT TIME ZONE tz - interval '1 day') AT TIME ZONE tz
					AND ts_start < (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz
					AND ts_start <> stale.ts_start;

				-- recalculate monthly audit based on updated daily values
				INSERT INTO solardatm.aud_stale_user (user_id, service, ts_start, aud_kind)
				VALUES (
					stale.user_id,
					stale.service,
					date_trunc('month', stale.ts_start AT TIME ZONE tz) AT TIME ZONE tz,
					'M')
				ON CONFLICT DO NOTHING;
		END CASE;

		result_cnt := 1;
	END IF;
	RETURN result_cnt;
END
$$;
