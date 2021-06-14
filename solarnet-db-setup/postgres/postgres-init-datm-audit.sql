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


/**
 * Call the `solardatm.find_audit_datum_daily_missing(date)` function and insert the results
 * into the `solardatm.aud_stale_datm` table with an `aud_kind = 'M'` so a record of the found
 * node sources gets generated.
 *
 * The `aud_kind = M` value is used because the processor that handles that record also populates
 * the `solardatm.aud_acc_datum_daily` table.
 *
 * @param ts the date to look for; defaults to the current date
 * @return the number of rows inserted
 */
CREATE OR REPLACE FUNCTION solardatm.populate_audit_datm_daily_missing(ts DATE DEFAULT CURRENT_DATE)
	RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ins_count bigint := 0;
BEGIN
	INSERT INTO solardatm.aud_stale_datm (ts_start, stream_id, aud_kind)
	SELECT date_trunc('month', m.ts_start AT TIME ZONE m.time_zone) AT TIME ZONE m.time_zone
		, m.stream_id
		, 'M' AS aud_kind
	FROM solardatm.find_audit_datm_daily_missing(ts) m
	ON CONFLICT DO NOTHING;

	GET DIAGNOSTICS ins_count = ROW_COUNT;
	RETURN ins_count;
END;
$$;


/**
 * Update the `solardatm.aud_datm_io` table by adding count values.
 *
 * @param sid 		the stream ID
 * @param ts_recv 	the datum receive date; this will be truncated to the hour
 * @param dcount	the datum count to add
 * @param pcount	the datum property count to add
 * @param is_insert `TRUE` if the datum was inserted, `FALSE` for updated
 */
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


/**
 * Increment the `solardatm.aud_datm_io` table `datum_q_count` for a stream.
 *
 * @param sid 				the stream ID to update audit datum for
 * @param ts				ts the query date
 * @param dcount			the datum count to insert, or add to an existing record
 */
CREATE OR REPLACE FUNCTION solardatm.audit_increment_datum_q_count(
		node	BIGINT,
		source	TEXT,
		ts 		TIMESTAMP WITH TIME ZONE,
		dcount 	INTEGER
	) RETURNS void LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solardatm.aud_datm_io(stream_id, ts_start, datum_q_count)
	SELECT s.stream_id, date_trunc('hour', ts), dcount
	FROM solardatm.da_datm_meta s
	WHERE s.node_id = node AND s.source_id = source
	ON CONFLICT (stream_id, ts_start) DO UPDATE
	SET datum_q_count = aud_datm_io.datum_q_count + EXCLUDED.datum_q_count;
$$;


/**
 * Update the `solardatm.aud_datm_io` table by adding MQTT publish byte counts.
 *
 * @param service 	the MQTT service name; currently ignored and "solarflux" is assumed
 * @param node 		the node ID
 * @param src		the source ID
 * @param ts_recv	the timestamp; will be truncated to 'hour' level
 * @param bcount	the byte count to add
 */
CREATE OR REPLACE FUNCTION solardatm.audit_increment_mqtt_publish_byte_count(
	service			TEXT,
	node 			BIGINT,
	src				TEXT,
	ts_recv 		TIMESTAMP WITH TIME ZONE,
	bcount			INTEGER
	) RETURNS VOID LANGUAGE SQL VOLATILE AS
$$
	WITH s AS (
		SELECT stream_id
		FROM solardatm.da_datm_meta
		WHERE node_id = node
			AND source_id = ANY(ARRAY[src, TRIM(leading '/' FROM src)])
		LIMIT 1
	)
	INSERT INTO solardatm.aud_datm_io (stream_id, ts_start, flux_byte_count)
	SELECT s.stream_id, date_trunc('hour', ts_recv) AS ta_start, bcount AS flux_byte_count
	FROM s
	ON CONFLICT (stream_id, ts_start) DO UPDATE
	SET flux_byte_count = aud_datm_io.flux_byte_count + EXCLUDED.flux_byte_count
$$;
