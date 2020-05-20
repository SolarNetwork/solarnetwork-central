CREATE OR REPLACE FUNCTION solaragg.process_one_aud_datum_daily_stale(kind char)
  RETURNS integer LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	stale record;
	curs CURSOR FOR SELECT * FROM solaragg.aud_datum_daily_stale
			WHERE aud_kind = kind
			ORDER BY ts_start ASC, created ASC, node_id ASC, source_id ASC
			LIMIT 1
			FOR UPDATE SKIP LOCKED;
	result integer := 0;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO stale;

	IF FOUND THEN
		CASE kind
			WHEN 'r' THEN
				-- raw data counts
				INSERT INTO solaragg.aud_datum_daily (node_id, source_id, ts_start, datum_count)
				SELECT
					node_id,
					source_id,
					stale.ts_start,
					count(*) AS datum_count
				FROM solardatum.da_datum
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts >= stale.ts_start
					AND ts < stale.ts_start + interval '1 day'
				GROUP BY node_id, source_id
				ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					processed_count = CURRENT_TIMESTAMP;

			WHEN 'h' THEN
				-- hour data counts
				INSERT INTO solaragg.aud_datum_daily (node_id, source_id, ts_start, datum_hourly_count)
				SELECT
					node_id,
					source_id,
					stale.ts_start,
					count(*) AS datum_hourly_count
				FROM solaragg.agg_datum_hourly
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start >= stale.ts_start
					AND ts_start < stale.ts_start + interval '1 day'
				GROUP BY node_id, source_id
				ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
				SET datum_hourly_count = EXCLUDED.datum_hourly_count,
					processed_hourly_count = CURRENT_TIMESTAMP;

			WHEN 'd' THEN
				-- day data counts, including sum of hourly audit prop_count, datum_q_count
				INSERT INTO solaragg.aud_datum_daily (node_id, source_id, ts_start, datum_daily_pres, prop_count, datum_q_count)
				WITH datum AS (
					SELECT count(*)::integer::boolean AS datum_daily_pres
					FROM solaragg.agg_datum_daily d
					WHERE d.node_id = stale.node_id
					AND d.source_id = stale.source_id
					AND d.ts_start = stale.ts_start
				)
				SELECT
					aud.node_id,
					aud.source_id,
					stale.ts_start,
					bool_or(d.datum_daily_pres) AS datum_daily_pres,
					sum(aud.prop_count) AS prop_count,
					sum(aud.datum_q_count) AS datum_q_count
				FROM solaragg.aud_datum_hourly aud
				CROSS JOIN datum d
				WHERE aud.node_id = stale.node_id
					AND aud.source_id = stale.source_id
					AND aud.ts_start >= stale.ts_start
					AND aud.ts_start < stale.ts_start + interval '1 day'
				GROUP BY aud.node_id, aud.source_id
				ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
				SET datum_daily_pres = EXCLUDED.datum_daily_pres,
					prop_count = EXCLUDED.prop_count,
					datum_q_count = EXCLUDED.datum_q_count,
					processed_io_count = CURRENT_TIMESTAMP;

			ELSE
				-- month data counts
				INSERT INTO solaragg.aud_datum_monthly (node_id, source_id, ts_start,
					datum_count, datum_hourly_count, datum_daily_count, datum_monthly_pres,
					prop_count, datum_q_count)
				WITH datum AS (
					SELECT count(*)::integer::boolean AS datum_monthly_pres
					FROM solaragg.agg_datum_monthly d
					WHERE d.node_id = stale.node_id
					AND d.source_id = stale.source_id
					AND d.ts_start = stale.ts_start
				)
				SELECT
					aud.node_id,
					aud.source_id,
					stale.ts_start,
					sum(aud.datum_count) AS datum_count,
					sum(aud.datum_hourly_count) AS datum_hourly_count,
					sum(CASE aud.datum_daily_pres WHEN TRUE THEN 1 ELSE 0 END) AS datum_daily_count,
					bool_or(d.datum_monthly_pres) AS datum_monthly_pres,
					sum(aud.prop_count) AS prop_count,
					sum(aud.datum_q_count) AS datum_q_count
				FROM solaragg.aud_datum_daily aud
				INNER JOIN solarnet.node_local_time node ON node.node_id = aud.node_id
				CROSS JOIN datum d
				WHERE aud.node_id = stale.node_id
					AND aud.source_id = stale.source_id
					AND aud.ts_start >= stale.ts_start
					AND aud.ts_start < (stale.ts_start AT TIME ZONE node.time_zone + interval '1 month') AT TIME ZONE node.time_zone
				GROUP BY aud.node_id, aud.source_id
				ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					datum_hourly_count = EXCLUDED.datum_hourly_count,
					datum_daily_count = EXCLUDED.datum_daily_count,
					datum_monthly_pres = EXCLUDED.datum_monthly_pres,
					prop_count = EXCLUDED.prop_count,
					datum_q_count = EXCLUDED.datum_q_count,
					processed = CURRENT_TIMESTAMP;
		END CASE;

		CASE kind
			WHEN 'm' THEN
				-- in case node tz changed, remove record(s) from other zone
				-- monthly records clean 1 month on either side
				DELETE FROM solaragg.aud_datum_monthly a
				USING solarnet.node_local_time node
				WHERE node.node_id = stale.node_id
					AND a.node_id = stale.node_id
					AND a.source_id = stale.source_id
					AND a.ts_start > (stale.ts_start AT TIME ZONE node.time_zone - interval '1 month') AT TIME ZONE node.time_zone
					AND a.ts_start < (stale.ts_start AT TIME ZONE node.time_zone + interval '1 month') AT TIME ZONE node.time_zone
					AND a.ts_start <> stale.ts_start;

				-- recalculate full accumulated audit counts for today
				PERFORM solaragg.populate_audit_acc_datum_daily(stale.node_id, stale.source_id);
			ELSE
				-- in case node tz changed, remove record(s) from other zone
				-- daily records clean 1 day on either side
				DELETE FROM solaragg.aud_datum_daily
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start > stale.ts_start - interval '1 day'
					AND ts_start < stale.ts_start + interval '1 day'
					AND ts_start <> stale.ts_start;

				-- recalculate monthly audit based on updated daily values
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				SELECT
					date_trunc('month', stale.ts_start AT TIME ZONE node.time_zone) AT TIME ZONE node.time_zone,
					stale.node_id,
					stale.source_id,
					'm'
				FROM solarnet.node_local_time node
				WHERE node.node_id = stale.node_id
				ON CONFLICT DO NOTHING;
		END CASE;

		-- remove processed stale record
		DELETE FROM solaragg.aud_datum_daily_stale WHERE CURRENT OF curs;
		result := 1;
	END IF;
	CLOSE curs;
	RETURN result;
END;
$BODY$;

CREATE OR REPLACE FUNCTION solaragg.process_one_agg_stale_loc_datum(kind char)
  RETURNS integer LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	stale record;
	curs CURSOR FOR SELECT * FROM solaragg.agg_stale_loc_datum
			WHERE agg_kind = kind
			-- Too slow to order; not strictly fair but process much faster
			-- ORDER BY ts_start ASC, created ASC, loc_id ASC, source_id ASC
			LIMIT 1
			FOR UPDATE SKIP LOCKED;
	agg_span interval;
	agg_json jsonb := NULL;
	agg_jmeta jsonb := NULL;
	loc_tz text := 'UTC';
	proc_count integer := 0;
BEGIN
	CASE kind
		WHEN 'h' THEN
			agg_span := interval '1 hour';
		WHEN 'd' THEN
			agg_span := interval '1 day';
		ELSE
			agg_span := interval '1 month';
	END CASE;

	OPEN curs;
	FETCH NEXT FROM curs INTO stale;

	IF FOUND THEN
		-- get the loc TZ for local date/time
		SELECT l.time_zone FROM solarnet.sn_loc l
		WHERE l.id = stale.loc_id
		INTO loc_tz;

		IF NOT FOUND THEN
			RAISE NOTICE 'Location % has no time zone, will use UTC.', stale.loc_id;
			loc_tz := 'UTC';
		END IF;

		CASE kind
			WHEN 'h' THEN
				SELECT jdata, jmeta
				FROM solaragg.calc_loc_datum_time_slots(stale.loc_id, ARRAY[stale.source_id::text], stale.ts_start, agg_span, 0, interval '1 hour')
				INTO agg_json, agg_jmeta;

			WHEN 'd' THEN
				SELECT jdata, jmeta
				FROM solaragg.calc_agg_loc_datum_agg(stale.loc_id, ARRAY[stale.source_id::text], stale.ts_start, stale.ts_start + agg_span, 'h')
				INTO agg_json, agg_jmeta;

			ELSE
				SELECT jdata, jmeta
				FROM solaragg.calc_agg_loc_datum_agg(stale.loc_id, ARRAY[stale.source_id::text], stale.ts_start
					, (stale.ts_start AT TIME ZONE loc_tz + agg_span) AT TIME ZONE loc_tz, 'd')
				INTO agg_json, agg_jmeta;
		END CASE;

		IF agg_json IS NULL THEN
			CASE kind
				WHEN 'h' THEN
					DELETE FROM solaragg.agg_loc_datum_hourly
					WHERE loc_id = stale.loc_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
				WHEN 'd' THEN
					DELETE FROM solaragg.agg_loc_datum_daily
					WHERE loc_id = stale.loc_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
				ELSE
					DELETE FROM solaragg.agg_loc_datum_monthly
					WHERE loc_id = stale.loc_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
			END CASE;
		ELSE
			CASE kind
				WHEN 'h' THEN
					INSERT INTO solaragg.agg_loc_datum_hourly (
						ts_start, local_date, loc_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta)
					VALUES (
						stale.ts_start,
						stale.ts_start AT TIME ZONE loc_tz,
						stale.loc_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta
					)
					ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta;

				WHEN 'd' THEN
					INSERT INTO solaragg.agg_loc_datum_daily (
						ts_start, local_date, loc_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta)
					VALUES (
						stale.ts_start,
						CAST(stale.ts_start AT TIME ZONE loc_tz AS DATE),
						stale.loc_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta
					)
					ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta;
				ELSE
					INSERT INTO solaragg.agg_loc_datum_monthly (
						ts_start, local_date, loc_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta)
					VALUES (
						stale.ts_start,
						CAST(stale.ts_start AT TIME ZONE loc_tz AS DATE),
						stale.loc_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta
					)
					ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta;
			END CASE;
		END IF;
		DELETE FROM solaragg.agg_stale_loc_datum WHERE CURRENT OF curs;
		proc_count := 1;

		-- now make sure we recalculate the next aggregate level by submitting a stale record for the next level
		CASE kind
			WHEN 'h' THEN
				INSERT INTO solaragg.agg_stale_loc_datum (ts_start, loc_id, source_id, agg_kind)
				VALUES (date_trunc('day', stale.ts_start AT TIME ZONE loc_tz) AT TIME ZONE loc_tz, stale.loc_id, stale.source_id, 'd')
				ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;
			WHEN 'd' THEN
				INSERT INTO solaragg.agg_stale_loc_datum (ts_start, loc_id, source_id, agg_kind)
				VALUES (date_trunc('month', stale.ts_start AT TIME ZONE loc_tz) AT TIME ZONE loc_tz, stale.loc_id, stale.source_id, 'm')
				ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;
			ELSE
				-- nothing
		END CASE;
	END IF;
	CLOSE curs;
	RETURN proc_count;
END;
$BODY$;

 CREATE OR REPLACE FUNCTION solaragg.find_running_loc_datum(
	IN loc bigint,
	IN sources text[],
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
RETURNS TABLE(
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	loc_id bigint,
	source_id text,
	jdata jsonb,
	weight integer)
LANGUAGE sql
STABLE AS
$BODY$
	WITH loctz AS (
		SELECT lids.loc_id, COALESCE(l.time_zone, 'UTC') AS tz
		FROM (SELECT loc AS loc_id) lids
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = lids.loc_id
	)
	SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d), CAST(extract(epoch from (local_date + interval '1 month') - local_date) / 3600 AS integer) AS weight
	FROM solaragg.agg_loc_datum_monthly d
	INNER JOIN loctz ON loctz.loc_id = d.loc_id
	WHERE d.ts_start < date_trunc('month', end_ts AT TIME ZONE loctz.tz) AT TIME ZONE loctz.tz
		AND d.source_id = ANY(sources)
	UNION ALL
	SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d), 24::integer as weight
	FROM solaragg.agg_loc_datum_daily d
	INNER JOIN loctz ON loctz.loc_id = d.loc_id
	WHERE ts_start < date_trunc('day', end_ts AT TIME ZONE loctz.tz) AT TIME ZONE loctz.tz
		AND d.ts_start >= date_trunc('month', end_ts AT TIME ZONE loctz.tz) AT TIME ZONE loctz.tz
		AND d.source_id = ANY(sources)
	UNION ALL
	SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d), 1::INTEGER as weight
	FROM solaragg.agg_loc_datum_hourly d
	INNER JOIN loctz ON loctz.loc_id = d.loc_id
	WHERE d.ts_start < date_trunc('hour', end_ts AT TIME ZONE loctz.tz) AT TIME ZONE loctz.tz
		AND d.ts_start >= date_trunc('day', end_ts AT TIME ZONE loctz.tz) AT TIME ZONE loctz.tz
		AND d.source_id = ANY(sources)
	UNION ALL
	SELECT ts_start, ts_start AT TIME ZONE loctz.tz AS local_date, loctz.loc_id, source_id, jdata, 1::integer as weight
	FROM solaragg.calc_loc_datum_time_slots(
		loc,
		sources,
		date_trunc('hour', end_ts),
		interval '1 hour',
		0,
		interval '1 hour')
	INNER JOIN loctz ON loctz.loc_id = loc_id
	ORDER BY ts_start, source_id
$BODY$;
