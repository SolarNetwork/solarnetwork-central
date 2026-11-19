/* ============================================================================
 * NET-540 (step 1) rollback: undo NET-540-agg-datm-15m.sql.
 *
 * Restores `solardatm.process_one_agg_stale_datm()` and
 * `solardatm.delete_datm(UUID, TIMESTAMP, TIMESTAMP, TEXT)` as the setup DDL in
 * postgres-init-datm-agg.sql and postgres-init-datm-delete.sql defined them before NET-540, copied
 * unchanged, then drops the two functions and two tables NET-540 added. On production that drops
 * the hypertable `solardatm.agg_datm_15m` with all its chunks, and with them all its rows and the
 * grants of NET-540-production.sql.
 *
 * The function signatures are unchanged, so `CREATE OR REPLACE` keeps their existing owners and
 * ACLs. Hourly, daily and monthly aggregates are computed the same way with or without NET-540, so
 * nothing computed in between needs redoing.
 *
 * The migration tag is not changed. To set it back as well:
 *
 *     SELECT svalue FROM solarcommon.db_migration_set_tag('20261119');
 * ============================================================================
 */

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
 * @param kind 				the aggregate kind: 'h', 'd', or 'M' for daily, hourly, monthly
 * @see solardatm.rollup_datm_for_time_span()
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

DROP FUNCTION IF EXISTS solardatm.rollup_datm_15m_for_hour(UUID, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH TIME ZONE, INTERVAL, INTERVAL);
DROP FUNCTION IF EXISTS solardatm.agg_datm_15m_complete_from();
DROP TABLE IF EXISTS solardatm.agg_datm_15m_setting;
DROP TABLE IF EXISTS solardatm.agg_datm_15m;
