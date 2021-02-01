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
		ts_end         := local_ts_end AT TIME ZONE tz;
		ts_prevstart   := (local_ts_start - agg_span) AT TIME ZONE tz;

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
			INSERT INTO solardatm.agg_stale_flux (stream_id, agg_kind)
			VALUES (stale.stream_id, kind)
			ON CONFLICT (stream_id, agg_kind) DO NOTHING;
		END IF;

		DELETE FROM solardatm.agg_stale_datm WHERE CURRENT OF curs;

		RETURN NEXT result_row;
	END IF;

	CLOSE curs;
END;
$$;
