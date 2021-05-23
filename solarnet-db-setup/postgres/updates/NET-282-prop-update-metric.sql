ALTER TABLE solardatm.aud_datm_io
	ADD COLUMN prop_u_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE solardatm.aud_datm_daily
	ADD COLUMN prop_u_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE solardatm.aud_datm_monthly
	ADD COLUMN prop_u_count BIGINT NOT NULL DEFAULT 0;

DROP FUNCTION solardatm.add_aud_datm_io(uuid, timestamp with time zone, integer, integer);

CREATE OR REPLACE FUNCTION solardatm.audit_increment_datum_count(
	sid					UUID,
	ts_recv 			TIMESTAMP WITH TIME ZONE,
	dcount				INTEGER,
	pcount				INTEGER,
	is_insert			BOOL DEFAULT TRUE
	) RETURNS VOID LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solardatm.aud_datm_io (stream_id, ts_start, datum_count, prop_count, prop_u_count)
	VALUES (sid, date_trunc('hour', ts_recv), dcount, pcount
		, CASE is_insert WHEN TRUE THEN 0 ELSE pcount END)
	ON CONFLICT (stream_id, ts_start) DO UPDATE
	SET datum_count = aud_datm_io.datum_count + EXCLUDED.datum_count,
		prop_count = aud_datm_io.prop_count + EXCLUDED.prop_count,
		prop_u_count = aud_datm_io.prop_u_count + EXCLUDED.prop_u_count
$$;

CREATE OR REPLACE FUNCTION solardatm.store_datum(
	ddate 			TIMESTAMP WITH TIME ZONE,
	node 			BIGINT,
	src 			TEXT,
	rdate 			TIMESTAMP WITH TIME ZONE,
	jdata 			TEXT,
	track 			BOOLEAN DEFAULT TRUE)
  RETURNS UUID LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_crea 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(ddate, now());
	ts_recv 			TIMESTAMP WITH TIME ZONE	:= COALESCE(rdate, now());
	jdata_json 			JSONB 						:= jdata::jsonb;
	jdata_prop_count 	INTEGER 					:= solardatm.json_datum_prop_count(jdata_json);
	ts_recv_hour 		TIMESTAMP WITH TIME ZONE 	:= date_trunc('hour', ts_recv);
	is_insert 			BOOLEAN 					:= false;

	sid 	UUID;

	-- property name arrays
	p_i		TEXT[];
	p_a		TEXT[];
	p_s		TEXT[];
BEGIN
	-- get, or create, stream ID
	INSERT INTO solardatm.da_datm_meta (node_id, source_id)
	VALUES (node, src)
	ON CONFLICT (node_id, source_id) DO NOTHING
	RETURNING stream_id, names_i, names_a, names_s
	INTO sid, p_i, p_a, p_s;

	IF NOT FOUND THEN
		SELECT stream_id, names_i, names_a, names_s
		FROM solardatm.da_datm_meta
		WHERE node_id = node AND source_id = src
		INTO sid, p_i, p_a, p_s;
	END IF;

	SELECT * FROM solardatm.store_datum(sid, ts_crea, src, ts_recv,
					jdata_json->'i',
					jdata_json->'a',
					jdata_json->'s',
					solarcommon.json_array_to_text_array(jdata_json->'t'),
					p_i, p_a, p_s)
	INTO p_i, p_a, p_s, is_insert;

	IF track THEN
		-- add to audit datum in count
		PERFORM solardatm.audit_increment_datum_count(sid, ts_recv, 1, jdata_prop_count, is_insert);

		-- add stale aggregate hour(s)
		INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
		SELECT stream_id, ts_start, 'h' AS agg_kind
		FROM solardatm.calc_stale_datm(sid, ddate)
		ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
	END IF;

	RETURN sid;
END
$$;


CREATE OR REPLACE FUNCTION solardatm.store_loc_datum(
	ddate 			TIMESTAMP WITH TIME ZONE,
	loc 			BIGINT,
	src 			TEXT,
	rdate 			TIMESTAMP WITH TIME ZONE,
	jdata 			TEXT,
	track 			BOOLEAN DEFAULT TRUE)
  RETURNS UUID LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_crea 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(ddate, now());
	ts_recv 			TIMESTAMP WITH TIME ZONE	:= COALESCE(rdate, now());
	jdata_json 			JSONB 						:= jdata::jsonb;
	jdata_prop_count 	INTEGER 					:= solardatm.json_datum_prop_count(jdata_json);
	ts_recv_hour 		TIMESTAMP WITH TIME ZONE 	:= date_trunc('hour', ts_recv);
	is_insert 			BOOLEAN 					:= false;

	sid 	UUID;

	-- property name arrays
	p_i		TEXT[];
	p_a		TEXT[];
	p_s		TEXT[];
BEGIN
	-- get, or create, stream ID
	INSERT INTO solardatm.da_loc_datm_meta (loc_id, source_id)
	VALUES (loc, src)
	ON CONFLICT (loc_id, source_id) DO NOTHING
	RETURNING stream_id, names_i, names_a, names_s
	INTO sid, p_i, p_a, p_s;

	IF NOT FOUND THEN
		SELECT stream_id, names_i, names_a, names_s
		FROM solardatm.da_loc_datm_meta
		WHERE loc_id = loc AND source_id = src
		INTO sid, p_i, p_a, p_s;
	END IF;

	SELECT * FROM solardatm.store_loc_datum(sid, ts_crea, src, ts_recv,
					jdata_json->'i',
					jdata_json->'a',
					jdata_json->'s',
					solarcommon.json_array_to_text_array(jdata_json->'t'),
					p_i, p_a, p_s)
	INTO p_i, p_a, p_s, is_insert;

	PERFORM solardatm.audit_increment_datum_count(sid, ts_recv, 1, jdata_prop_count, is_insert);

	IF track THEN
		INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
		SELECT stream_id, ts_start, 'h' AS agg_kind
		FROM solardatm.calc_stale_datm(sid, ddate)
		ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
	END IF;

	RETURN sid;
END;
$$;

DROP FUNCTION IF EXISTS solardatm.calc_audit_datm_daily;
CREATE OR REPLACE FUNCTION solardatm.calc_audit_datm_daily(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		stream_id				UUID,
		ts_start 				TIMESTAMP WITH TIME ZONE,
		datum_daily_pres		BOOLEAN,
		prop_count				BIGINT,
		prop_u_count			BIGINT,
		datum_q_count	 		BIGINT
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	WITH datum AS (
		SELECT count(*)::integer::boolean AS datum_daily_pres
		FROM solardatm.agg_datm_daily d
		WHERE d.stream_id = sid
		AND d.ts_start = start_ts
	)
	SELECT
		sid,
		start_ts,
		bool_or(d.datum_daily_pres) AS datum_daily_pres,
		sum(aud.prop_count) AS prop_count,
		sum(aud.prop_u_count) AS prop_u_count,
		sum(aud.datum_q_count) AS datum_q_count
	FROM solardatm.aud_datm_io aud
	CROSS JOIN datum d
	WHERE aud.stream_id = sid
		AND aud.ts_start >= start_ts
		AND aud.ts_start < end_ts
	GROUP BY aud.stream_id
$$;

DROP FUNCTION IF EXISTS solardatm.calc_audit_datm_monthly;
CREATE OR REPLACE FUNCTION solardatm.calc_audit_datm_monthly(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		stream_id				UUID,
		ts_start 				TIMESTAMP WITH TIME ZONE,
		datum_count				INTEGER,
		datum_hourly_count		SMALLINT,
		datum_daily_count		SMALLINT,
		datum_monthly_pres		BOOLEAN,
		prop_count				BIGINT,
		prop_u_count			BIGINT,
		datum_q_count	 		BIGINT
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	WITH datum AS (
		SELECT count(*)::integer::boolean AS datum_monthly_pres
		FROM solardatm.agg_datm_monthly d
		WHERE d.stream_id = sid
		AND d.ts_start = start_ts
	)
	SELECT
		sid,
		start_ts,
		sum(aud.datum_count)::INTEGER AS datum_count,
		sum(aud.datum_hourly_count)::SMALLINT AS datum_hourly_count,
		sum(CASE aud.datum_daily_pres WHEN TRUE THEN 1 ELSE 0 END)::SMALLINT AS datum_daily_count,
		bool_or(d.datum_monthly_pres) AS datum_monthly_pres,
		sum(aud.prop_count)::BIGINT AS prop_count,
		sum(aud.prop_u_count)::BIGINT AS prop_u_count,
		sum(aud.datum_q_count)::BIGINT AS datum_q_count
	FROM solardatm.aud_datm_daily aud
	CROSS JOIN datum d
	WHERE aud.stream_id = sid
		AND aud.ts_start >= start_ts
		AND aud.ts_start < end_ts
	GROUP BY aud.stream_id
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
				-- day data counts, including sum of hourly audit prop_count, datum_q_count
				INSERT INTO solardatm.aud_datm_daily (stream_id, ts_start,
					datum_daily_pres, prop_count, prop_u_count, datum_q_count, processed_io_count)
				SELECT stream_id, ts_start, datum_daily_pres, prop_count, prop_u_count, datum_q_count,
					CURRENT_TIMESTAMP AS processed_io_count
				FROM solardatm.calc_audit_datm_daily(
					stale.stream_id, stale.ts_start, stale.ts_start + interval '1 day')
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_daily_pres = EXCLUDED.datum_daily_pres,
					prop_count = EXCLUDED.prop_count,
					prop_u_count = EXCLUDED.prop_u_count,
					datum_q_count = EXCLUDED.datum_q_count,
					processed_io_count = EXCLUDED.processed_io_count;

			ELSE
				-- month data counts
				INSERT INTO solardatm.aud_datm_monthly (stream_id, ts_start,
					datum_count, datum_hourly_count, datum_daily_count, datum_monthly_pres,
					prop_count, prop_u_count, datum_q_count, processed)
				SELECT stream_id, ts_start, datum_count, datum_hourly_count, datum_daily_count,
					datum_monthly_pres, prop_count, prop_u_count, datum_q_count, CURRENT_TIMESTAMP AS processed
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
