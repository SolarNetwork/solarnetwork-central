CREATE OR REPLACE FUNCTION solardatm.find_audit_acc_datm_daily(sid UUID)
	RETURNS TABLE (
		stream_id				UUID,
		ts_start 				TIMESTAMP WITH TIME ZONE,
		datum_count 			INTEGER,
		datum_hourly_count 		INTEGER,
		datum_daily_count 		INTEGER,
		datum_monthly_count 	INTEGER
	) LANGUAGE SQL VOLATILE AS
$$
	WITH acc AS (
		SELECT
			sum(d.datum_count) AS datum_count,
			sum(d.datum_hourly_count) AS datum_hourly_count,
			sum(d.datum_daily_count) AS datum_daily_count,
			sum(CASE d.datum_monthly_pres WHEN TRUE THEN 1 ELSE 0 END) AS datum_monthly_count
		FROM solardatm.aud_datm_monthly d
		WHERE d.stream_id = sid
	)
	SELECT
		sid,
		date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE COALESCE(m.time_zone, 'UTC')) AT TIME ZONE COALESCE(m.time_zone, 'UTC'),
		acc.datum_count::integer,
		acc.datum_hourly_count::integer,
		acc.datum_daily_count::integer,
		acc.datum_monthly_count::integer
	FROM acc
	LEFT OUTER JOIN solardatm.find_metadata_for_stream(sid) m ON TRUE
$$;


CREATE OR REPLACE FUNCTION solardatm.populate_audit_acc_datm_daily(sid UUID)
	RETURNS void LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solardatm.aud_acc_datm_daily (stream_id, ts_start,
		datum_count, datum_hourly_count, datum_daily_count, datum_monthly_count)
	SELECT
		sid,
		ts_start,
		COALESCE(datum_count, 0) AS datum_count,
		COALESCE(datum_hourly_count, 0) AS datum_hourly_count,
		COALESCE(datum_daily_count, 0) AS datum_daily_count,
		COALESCE(datum_monthly_count, 0) AS datum_monthly_count
	FROM solardatm.find_audit_acc_datm_daily(sid)
	ON CONFLICT (stream_id, ts_start) DO UPDATE
	SET datum_count = EXCLUDED.datum_count,
		datum_hourly_count = EXCLUDED.datum_hourly_count,
		datum_daily_count = EXCLUDED.datum_daily_count,
		datum_monthly_count = EXCLUDED.datum_monthly_count,
		processed = CURRENT_TIMESTAMP;
$$;


CREATE OR REPLACE FUNCTION solaragg.process_one_stale_aud_datm_daily(kind CHARACTER)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					solardatm.aud_stale_datm_daily;
	meta					record;
	tz						TEXT;
	curs 					CURSOR FOR
							SELECT * FROM solardatm.aud_stale_datm_daily
							WHERE aud_kind = kind LIMIT 1 FOR UPDATE SKIP LOCKED;
	result_cnt 				INTEGER := 0;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO stale;

	IF FOUND THEN
		IF kind = 'M' THEN
			-- get stream time zone; will determine if node or location stream
			SELECT * FROM solardatm.find_metadata_for_stream(stale.stream_id) INTO meta;
			tz := COALESCE(meta.time_zone, 'UTC');
		END IF;

		CASE kind
			WHEN '0' THEN
				-- raw data counts
				INSERT INTO solardatm.aud_stale_datm_daily (stream_id, ts_start, datum_count)
				SELECT
					stream_id,
					stale.ts_start,
					count(*) AS datum_count
				FROM solardatm.da_datm
				WHERE stream_id = stale.stream_id
					AND ts >= stale.ts_start
					AND ts < stale.ts_start + interval '1 day'
				GROUP BY stream_id
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					processed_count = CURRENT_TIMESTAMP;

			WHEN 'h' THEN
				-- hour data counts
				INSERT INTO solardatm.aud_stale_datm_daily (stream_id, ts_start, datum_hourly_count)
				SELECT
					stream_id,
					stale.ts_start,
					count(*) AS datum_hourly_count
				FROM solardatm.agg_datm_hourly
				WHERE stream_id = stale.stream_id
					AND ts_start >= stale.ts_start
					AND ts_start < stale.ts_start + interval '1 day'
				GROUP BY stream_id
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_hourly_count = EXCLUDED.datum_hourly_count,
					processed_hourly_count = CURRENT_TIMESTAMP;

			WHEN 'd' THEN
				-- day data counts, including sum of hourly audit prop_count, datum_q_count
				INSERT INTO solardatm.aud_stale_datm_daily (stream_id, ts_start, datum_daily_pres, prop_count, datum_q_count)
				WITH datum AS (
					SELECT count(*)::integer::boolean AS datum_daily_pres
					FROM solardatm.agg_datm_daily d
					WHERE d.stream_id = stale.stream_id
					AND d.ts_start = stale.ts_start
				)
				SELECT
					aud.stream_id,
					stale.ts_start,
					bool_or(d.datum_daily_pres) AS datum_daily_pres,
					sum(aud.prop_count) AS prop_count,
					sum(aud.datum_q_count) AS datum_q_count
				FROM solardatm.aud_datm_hourly aud
				CROSS JOIN datum d
				WHERE aud.stream_id = stale.stream_id
					AND aud.ts_start >= stale.ts_start
					AND aud.ts_start < stale.ts_start + interval '1 day'
				GROUP BY aud.stream_id
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_daily_pres = EXCLUDED.datum_daily_pres,
					prop_count = EXCLUDED.prop_count,
					datum_q_count = EXCLUDED.datum_q_count,
					processed_io_count = CURRENT_TIMESTAMP;

			ELSE
				-- month data counts
				INSERT INTO solardatm.aud_datm_monthly (stream_id, ts_start,
					datum_count, datum_hourly_count, datum_daily_count, datum_monthly_pres,
					prop_count, datum_q_count)
				WITH datum AS (
					SELECT count(*)::integer::boolean AS datum_monthly_pres
					FROM solardatm.agg_datm_monthly d
					WHERE d.stream_id_id = stale.stream_id
					AND d.ts_start = stale.ts_start
				)
				SELECT
					aud.stream_id,
					stale.ts_start,
					sum(aud.datum_count) AS datum_count,
					sum(aud.datum_hourly_count) AS datum_hourly_count,
					sum(CASE aud.datum_daily_pres WHEN TRUE THEN 1 ELSE 0 END) AS datum_daily_count,
					bool_or(d.datum_monthly_pres) AS datum_monthly_pres,
					sum(aud.prop_count) AS prop_count,
					sum(aud.datum_q_count) AS datum_q_count
				FROM solardatm.aud_datm_daily aud
				CROSS JOIN datum d
				WHERE aud.stream_id = stale.stream_id
					AND aud.ts_start >= stale.ts_start
					AND aud.ts_start < (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz
				GROUP BY aud.stream_id
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					datum_hourly_count = EXCLUDED.datum_hourly_count,
					datum_daily_count = EXCLUDED.datum_daily_count,
					datum_monthly_pres = EXCLUDED.datum_monthly_pres,
					prop_count = EXCLUDED.prop_count,
					datum_q_count = EXCLUDED.datum_q_count,
					processed = CURRENT_TIMESTAMP;
		END CASE;

		CASE kind
			WHEN 'M' THEN
				-- in case node tz changed, remove record(s) from other zone
				-- monthly records clean 1 month on either side
				DELETE FROM solaragg.aud_datum_monthly a
				WHERE a.stream_id = stale.stream_id
					AND a.ts_start > (stale.ts_start AT TIME ZONE tz - interval '1 month') AT TIME ZONE tz
					AND a.ts_start < (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz
					AND a.ts_start <> stale.ts_start;

				-- recalculate full accumulated audit counts for today
				PERFORM solardatm.populate_audit_acc_datum_daily(stale.stream_id);
			ELSE
				-- in case node tz changed, remove record(s) from other zone
				-- daily records clean 1 day on either side
				DELETE FROM solaragg.aud_datum_daily
				WHERE stream_id = stale.stream_id
					AND ts_start > stale.ts_start - interval '1 day'
					AND ts_start < stale.ts_start + interval '1 day'
					AND ts_start <> stale.ts_start;

				-- recalculate monthly audit based on updated daily values
				INSERT INTO solardatm.stale_aud_datum_daily (stream_id, ts_start, aud_kind)
				VALUES (
					stale.stream_id,
					date_trunc('month', stale.ts_start AT TIME ZONE tz) AT TIME ZONE tz,
					'M')
				ON CONFLICT DO NOTHING;
		END CASE;

		-- remove processed stale record
		DELETE FROM solardatm.stale_aud_datm_daily WHERE CURRENT OF curs;
		result_cnt := 1;
	END IF;
	CLOSE curs;
	RETURN result_cnt;
END;
$$;
