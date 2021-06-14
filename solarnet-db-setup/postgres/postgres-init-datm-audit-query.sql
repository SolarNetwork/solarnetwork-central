/**
 * Calculate raw audit datum values for a stream.
 *
 * This relies on the `da_datm` table.
 *
 * @param sid 				the stream ID to find audit datum for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.calc_audit_datm_raw(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		stream_id				UUID,
		ts_start 				TIMESTAMP WITH TIME ZONE,
		datum_count 			INTEGER
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT
		stream_id,
		start_ts,
		count(*)::INTEGER AS datum_count
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts >= start_ts
		AND ts < end_ts
	GROUP BY stream_id
$$;


/**
 * Calculate hourly audit datum values for a stream.
 *
 * For speed it relies on the pre-computed audit values in the `agg_datm_hourly` table.
 *
 * @param sid 				the stream ID to find audit datum for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.calc_audit_datm_hourly(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		stream_id				UUID,
		ts_start 				TIMESTAMP WITH TIME ZONE,
		datum_hourly_count 		SMALLINT
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT
		sid,
		start_ts,
		count(*)::SMALLINT AS datum_hourly_count
	FROM solardatm.agg_datm_hourly
	WHERE stream_id = sid
		AND ts_start >= start_ts
		AND ts_start < end_ts
	GROUP BY stream_id
$$;


/**
 * Calculate daily audit datum values for a stream.
 *
 * For speed it relies on the pre-computed audit values in the `agg_datm_daily` and
 * `aud_datm_io` tables.
 *
 * @param sid 				the stream ID to find audit datum for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 */
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
		datum_q_count	 		BIGINT,
		flux_byte_count			BIGINT
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
		SUM(aud.prop_count) AS prop_count,
		SUM(aud.prop_u_count) AS prop_u_count,
		SUM(aud.datum_q_count) AS datum_q_count,
		SUM(aud.flux_byte_count) AS flux_byte_count
	FROM solardatm.aud_datm_io aud
	CROSS JOIN datum d
	WHERE aud.stream_id = sid
		AND aud.ts_start >= start_ts
		AND aud.ts_start < end_ts
	GROUP BY aud.stream_id
$$;


/**
 * Calculate monthly audit datum values for a stream.
 *
 * For speed it relies on the pre-computed audit values in the `agg_datm_monthly` and
 * `aud_datm_daily` tables.
 *
 * @param sid 				the stream ID to find audit datum for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 */
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
		datum_q_count	 		BIGINT,
		flux_byte_count			BIGINT
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
		SUM(aud.datum_count)::INTEGER AS datum_count,
		SUM(aud.datum_hourly_count)::SMALLINT AS datum_hourly_count,
		SUM(CASE aud.datum_daily_pres WHEN TRUE THEN 1 ELSE 0 END)::SMALLINT AS datum_daily_count,
		bool_or(d.datum_monthly_pres) AS datum_monthly_pres,
		SUM(aud.prop_count)::BIGINT AS prop_count,
		SUM(aud.prop_u_count)::BIGINT AS prop_u_count,
		SUM(aud.datum_q_count)::BIGINT AS datum_q_count,
		SUM(aud.flux_byte_count)::BIGINT AS flux_byte_count
	FROM solardatm.aud_datm_daily aud
	CROSS JOIN datum d
	WHERE aud.stream_id = sid
		AND aud.ts_start >= start_ts
		AND aud.ts_start < end_ts
	GROUP BY aud.stream_id
$$;


/**
 * Calculate the current audit accumulative datum values for a stream.
 *
 * This produces an overall count of datum and aggregate datum within a stream.
 * For speed it relies on the pre-computed audit values in the `aud_datm_monthly` table.
 *
 * @param sid the stream ID to find audit counts for
 */
CREATE OR REPLACE FUNCTION solardatm.calc_audit_datm_acc(sid UUID)
	RETURNS TABLE (
		stream_id				UUID,
		ts_start 				TIMESTAMP WITH TIME ZONE,
		datum_count 			INTEGER,
		datum_hourly_count 		INTEGER,
		datum_daily_count 		INTEGER,
		datum_monthly_count 	INTEGER
	) LANGUAGE SQL VOLATILE ROWS 1 AS
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
		acc.datum_count::INTEGER,
		acc.datum_hourly_count::INTEGER,
		acc.datum_daily_count::INTEGER,
		acc.datum_monthly_count::INTEGER
	FROM acc
	LEFT OUTER JOIN solardatm.find_metadata_for_stream(sid) m ON TRUE
$$;


/**
 * Look for node sources that have no corresponding row in the `solardatm.aud_acc_datm_daily` table
 * on a particular date. The purpose of this is to support populating the accumulating storage
 * date for nodes even if they are offline and not posting data currently.
 *
 * @param ts the date to look for; defaults to the current date
 */
CREATE OR REPLACE FUNCTION solardatm.find_audit_datm_daily_missing(ts DATE DEFAULT CURRENT_DATE)
	RETURNS TABLE(
		stream_id 	UUID,
		ts_start 	TIMESTAMP WITH TIME ZONE,
		time_zone 	CHARACTER VARYING(64)
	) LANGUAGE SQL STABLE AS
$$
	WITH missing AS (
		-- all streams
		SELECT stream_id
		FROM solardatm.da_datm_meta

		-- except those with audit rows on given day
		EXCEPT
		SELECT a.stream_id
		FROM solardatm.aud_acc_datm_daily a
		INNER JOIN solardatm.da_datm_meta meta ON meta.stream_id = a.stream_id
		LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE a.ts_start = ts::timestamp AT TIME ZONE COALESCE(l.time_zone, 'UTC')
	)
	SELECT m.stream_id, ts::timestamp AT TIME ZONE COALESCE(l.time_zone, 'UTC'), COALESCE(l.time_zone, 'UTC')
	FROM missing m
	INNER JOIN solardatm.da_datm_meta meta ON meta.stream_id = m.stream_id
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
$$;
