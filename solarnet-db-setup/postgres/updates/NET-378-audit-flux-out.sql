/**
 * General user audit ingest data table.
 *
 * This data represents the lowest level audit data for users. The `service` column defines the
 * metric being counted, and is application defined.
 */
CREATE TABLE solardatm.aud_user_io (
	user_id					BIGINT NOT NULL,
	service					CHARACTER(4) NOT NULL,
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
	cnt 					INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS aud_user_io_pkey ON solardatm.aud_user_io (user_id, service, ts_start DESC);

/**
 * User audit daily summary data table.
 *
 * This data represents a calculated summary of counts of user-related rows in other tables.
 * It must be maintained as the source data tables are updated.
 */
CREATE TABLE solardatm.aud_user_daily (
	user_id					BIGINT NOT NULL,
	service					CHARACTER(4) NOT NULL,
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
    cnt 					BIGINT NOT NULL DEFAULT 0,
	processed				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS aud_user_daily_pkey
ON solardatm.aud_user_daily (user_id, service, ts_start DESC);

/**
 * User audit monthly summary data table.
 *
 * This data represents a calculated summary of counts of user-related rows in the
 * solardatm.aud_user_daily table. It must be maintained as the source data table is updated.
 */
CREATE TABLE solardatm.aud_user_monthly (
	user_id					BIGINT NOT NULL,
	service					CHARACTER(4) NOT NULL,
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
    cnt 					BIGINT NOT NULL DEFAULT 0,
	processed 				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS aud_user_monthly_pkey
ON solardatm.aud_user_monthly (user_id, service, ts_start DESC);

/**
 * Queue table for stale user audit aggregate records.
 *
 * The `aud_kind` is an aggregate key, like `d` or `M` for daily or monthly. Each record represents
 * an aggregate period that is "stale" and needs to be (re)computed.
 */
CREATE TABLE solardatm.aud_stale_user (
	user_id					BIGINT NOT NULL,
	service					CHARACTER(4),
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
	aud_kind 				CHARACTER NOT NULL,
	created 				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT aud_stale_user_pkey PRIMARY KEY (aud_kind, service, ts_start, user_id)
);

/******************************************************************************
 * FUNCTION solaruser.get_user_timezone(bigint)
 *
 * Return a user's time zone, or NULL if not set.
 *
 * @param bigint the user ID
 * @return time zone name, e.g. 'Pacific/Auckland'
 */
CREATE OR REPLACE FUNCTION solaruser.get_user_timezone(bigint)
  RETURNS text LANGUAGE 'sql' STABLE AS
$$
	SELECT l.time_zone
	FROM solaruser.user_user u
	INNER JOIN solarnet.sn_loc l ON l.id = u.loc_id
	WHERE u.id = $1
$$;

/**
 * Update the `solardatm.aud_user_io` table by adding counts.
 *
 * @param usr 		the user ID
 * @param srvc 		the srvc name
 * @param ts_recv	the timestamp; will be truncated to 'hour' level
 * @param icount	the count to add
 */
CREATE OR REPLACE FUNCTION solardatm.audit_increment_user_count(
	usr 			BIGINT,
	srvc			CHARACTER(4),
	ts_recv 		TIMESTAMP WITH TIME ZONE,
	icount			INTEGER
	) RETURNS VOID LANGUAGE PLPGSQL VOLATILE AS
$$
DECLARE
	tz				TEXT;
BEGIN
	-- get user time zone
	SELECT COALESCE(solarnet.get_user_timezone(usr), 'UTC') INTO tz;

	INSERT INTO solardatm.aud_user_io (user_id, service, ts_start, cnt)
	VALUES (usr, srvc, date_trunc('hour', ts_recv), icount)
	ON CONFLICT (user_id, service, ts_start) DO UPDATE
	SET cnt = aud_user_io.cnt + EXCLUDED.cnt;

	INSERT INTO solardatm.aud_stale_user (user_id, service, ts_start, aud_kind)
	VALUES (usr, srvc, date_trunc('day', ts_recv AT TIME ZONE tz) AT TIME ZONE tz, 'd')
	ON CONFLICT DO NOTHING;
END
$$;

/**
 * Calculate user daily audit values for a specific user service over a time range.
 *
 * @param usr the 	user ID
 * @param srvc the 	audit service name
 * @param start_ts 	the starting time (inclusive)
 * @param end_ts 	the ending time (exclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.calc_audit_user_daily(
		usr 		BIGINT,
		srvc		CHARACTER(4),
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		user_id		BIGINT,
		service		CHARACTER(4),
		ts_start 	TIMESTAMP WITH TIME ZONE,
		cnt			BIGINT
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT
		usr,
		srvc,
		start_ts,
		SUM(aud.cnt) AS cnt
	FROM solardatm.aud_user_io aud
	WHERE aud.user_id = usr
		AND aud.service = srvc
		AND aud.ts_start >= start_ts
		AND aud.ts_start < end_ts
	GROUP BY aud.user_id, aud.service
$$;

/**
 * Calculate user monthly audit values for a specific user service over a time range.
 *
 * @param usr the 	user ID
 * @param srvc the 	audit service name
 * @param start_ts 	the starting time (inclusive)
 * @param end_ts 	the ending time (exclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.calc_audit_user_monthly(
		usr 		BIGINT,
		srvc		CHARACTER(4),
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		node_id		BIGINT,
		service		CHARACTER(4),
		ts_start 	TIMESTAMP WITH TIME ZONE,
		cnt			BIGINT
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT
		usr,
		srvc,
		start_ts,
		SUM(aud.cnt)::BIGINT AS cnt
	FROM solardatm.aud_user_daily aud
	WHERE aud.user_id = usr
		AND aud.service = srvc
		AND aud.ts_start >= start_ts
		AND aud.ts_start < end_ts
	GROUP BY aud.user_id, aud.service
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


/**
 * Update MQTT byte counts in either the `solardatm.aud_datm_io` table or the
 * `solardatm.aud_user_io` table, by adding MQTT byte counts.
 *
 * @param service 	the MQTT service name
 * @param obj_id 	the node ID (if src is not NULL) or user ID (if src is NULL)
 * @param src		the source ID, or NULL for user data
 * @param ts_recv	the timestamp; will be truncated to 'hour' level
 * @param bcount	the byte count to add
 */
CREATE OR REPLACE FUNCTION solardatm.audit_increment_mqtt_byte_count(
	service			TEXT,
	obj_id 			BIGINT,
	src				TEXT,
	ts_recv 		TIMESTAMP WITH TIME ZONE,
	bcount			INTEGER
	) RETURNS VOID LANGUAGE plpgsql VOLATILE AS
$$
BEGIN
	IF src IS NULL THEN
		PERFORM solardatm.audit_increment_user_count(obj_id, service, ts_recv, bcount);
	ELSE
		PERFORM solardatm.audit_increment_mqtt_publish_byte_count(service, obj_id, src, ts_recv, bcount);
	END IF;
END
$$;
