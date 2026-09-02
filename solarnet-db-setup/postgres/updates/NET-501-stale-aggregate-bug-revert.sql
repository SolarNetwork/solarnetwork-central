-- a revert to pre-NET-501 process_one_* functions, in case something goes wrong

CREATE OR REPLACE FUNCTION solardatm.process_one_agg_stale_datm(kind CHARACTER)
	RETURNS SETOF solardatm.obj_datm_id LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	agg_span 				INTERVAL;
	dest_name				TEXT;

	curs 					CURSOR FOR
							SELECT * FROM solardatm.agg_stale_datm
							WHERE agg_kind = kind LIMIT 1 FOR UPDATE SKIP LOCKED;
	stale 					solardatm.agg_stale_datm;
	meta					record;
	tz						TEXT;

	local_ts_start			TIMESTAMP;
	local_ts_end			TIMESTAMP;
	ts_end					TIMESTAMP WITH TIME ZONE;
	ts_prevstart			TIMESTAMP WITH TIME ZONE;

	num_rows				BIGINT;

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

	OPEN curs;
	FETCH NEXT FROM curs INTO stale;

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

		BEGIN
			IF kind = 'h' THEN
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
			END IF;
			GET DIAGNOSTICS num_rows = ROW_COUNT;
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

		DELETE FROM solardatm.agg_stale_datm WHERE CURRENT OF curs;

		RETURN NEXT result_row;
	END IF;

	CLOSE curs;
END;
$$;

CREATE OR REPLACE FUNCTION solardatm.process_one_aud_stale_datm(kind CHARACTER)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					solardatm.aud_stale_datm;
	meta					record;
	tz						TEXT;
	curs 					CURSOR FOR
							SELECT * FROM solardatm.aud_stale_datm
							WHERE aud_kind = kind LIMIT 1 FOR UPDATE SKIP LOCKED;
	result_cnt 				INTEGER := 0;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO stale;

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
					stale.stream_id, stale.ts_start, stale.ts_start + interval '1 day')
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					processed_count = EXCLUDED.processed_count;

			WHEN 'h' THEN
				-- hour data counts
				INSERT INTO solardatm.aud_datm_daily (stream_id, ts_start, datum_hourly_count, processed_hourly_count)
				SELECT stream_id, ts_start, datum_hourly_count, CURRENT_TIMESTAMP AS processed_hourly_count
				FROM solardatm.calc_audit_datm_hourly(
					stale.stream_id, stale.ts_start, stale.ts_start + interval '1 day')
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
					stale.stream_id, stale.ts_start, stale.ts_start + interval '1 day')
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
					AND ts_start > stale.ts_start - interval '1 day'
					AND ts_start < stale.ts_start + interval '1 day'
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
		DELETE FROM solardatm.aud_stale_datm WHERE CURRENT OF curs;
		result_cnt := 1;
	END IF;
	CLOSE curs;
	RETURN result_cnt;
END;
$$;

CREATE OR REPLACE FUNCTION solardatm.process_one_aud_stale_node(kind CHARACTER)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					solardatm.aud_stale_node;
	tz						TEXT;
	curs 					CURSOR FOR
							SELECT * FROM solardatm.aud_stale_node
							WHERE aud_kind = kind
							LIMIT 1 FOR UPDATE SKIP LOCKED;
	result_cnt 				INTEGER := 0;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO stale;

	IF FOUND THEN
		-- get node time zone; will determine if node or location stream
		SELECT COALESCE(solarnet.get_node_timezone(stale.node_id), 'UTC') INTO tz;

		CASE kind
			WHEN 'd' THEN
				-- day data counts summerized from hourly data
				INSERT INTO solardatm.aud_node_daily (node_id, service, ts_start, cnt, processed)
				SELECT node_id, service, ts_start, cnt, CURRENT_TIMESTAMP AS processed
				FROM solardatm.calc_audit_node_daily(
					stale.node_id, stale.service, stale.ts_start, stale.ts_start + interval '1 day')
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
					AND ts_start > stale.ts_start - interval '1 day'
					AND ts_start < stale.ts_start + interval '1 day'
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

		-- remove processed stale record
		DELETE FROM solardatm.aud_stale_node WHERE CURRENT OF curs;
		result_cnt := 1;
	END IF;
	CLOSE curs;
	RETURN result_cnt;
END
$$;

CREATE OR REPLACE FUNCTION solardatm.process_one_aud_stale_user(kind CHARACTER)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					solardatm.aud_stale_user;
	tz						TEXT;
	curs 					CURSOR FOR
							SELECT * FROM solardatm.aud_stale_user
							WHERE aud_kind = kind
							LIMIT 1 FOR UPDATE SKIP LOCKED;
	result_cnt 				INTEGER := 0;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO stale;

	IF FOUND THEN
		-- get user time zone; will determine if node or location stream
		SELECT COALESCE(solaruser.get_user_timezone(stale.user_id), 'UTC') INTO tz;

		CASE kind
			WHEN 'd' THEN
				-- day data counts summerized from hourly data
				INSERT INTO solardatm.aud_user_daily (user_id, service, ts_start, cnt, processed)
				SELECT user_id, service, ts_start, cnt, CURRENT_TIMESTAMP AS processed
				FROM solardatm.calc_audit_user_daily(
					stale.user_id, stale.service, stale.ts_start, stale.ts_start + interval '1 day')
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
					AND ts_start > stale.ts_start - interval '1 day'
					AND ts_start < stale.ts_start + interval '1 day'
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

		-- remove processed stale record
		DELETE FROM solardatm.aud_stale_user WHERE CURRENT OF curs;
		result_cnt := 1;
	END IF;
	CLOSE curs;
	RETURN result_cnt;
END
$$;
