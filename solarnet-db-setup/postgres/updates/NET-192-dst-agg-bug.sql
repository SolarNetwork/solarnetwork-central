CREATE OR REPLACE FUNCTION solaragg.find_loc_datum_for_time_span(
    IN loc bigint,
    IN sources text[],
    IN start_ts timestamp with time zone,
    IN span interval,
    IN tolerance interval DEFAULT '01:00:00'::interval)
  RETURNS TABLE(ts timestamp with time zone, source_id text, jdata jsonb) AS
$BODY$
SELECT sub.ts, sub.source_id, sub.jdata FROM (
	-- subselect filters out "extra" leading/lagging rows from results
	SELECT
		d.ts,
		d.source_id,
		CASE
			WHEN lead(d.ts) over win < start_ts OR lag(d.ts) over win >= (start_ts + span)
				THEN TRUE
			ELSE FALSE
		END AS outside,
		solardatum.jdata_from_datum(d) as jdata
	FROM solardatum.da_loc_datum d
	WHERE d.loc_id = loc
		AND d.source_id = ANY(sources)
		AND d.ts >= start_ts - tolerance
		AND d.ts <= start_ts + span + tolerance
	WINDOW win AS (PARTITION BY d.source_id ORDER BY d.ts)
	ORDER BY d.ts, d.source_id
) AS sub
WHERE
	sub.outside = FALSE
$BODY$
  LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION solaragg.find_datum_for_time_span(
    IN node bigint,
    IN sources text[],
    IN start_ts timestamp with time zone,
    IN span interval,
    IN tolerance interval DEFAULT '01:00:00'::interval)
  RETURNS TABLE(ts timestamp with time zone, source_id text, jdata jsonb) AS
$BODY$
SELECT sub.ts, sub.source_id, sub.jdata FROM (
	-- subselect filters out "extra" leading/lagging rows from results
	SELECT
		d.ts,
		d.source_id,
		CASE
			WHEN lead(d.ts) over win < start_ts OR lag(d.ts) over win >= (start_ts + span)
				THEN TRUE
			ELSE FALSE
		END AS outside,
		solardatum.jdata_from_datum(d) as jdata
	FROM solardatum.da_datum d
	WHERE d.node_id = node
		AND d.source_id = ANY(sources)
		AND d.ts >= start_ts - tolerance
		AND d.ts <= start_ts + span + tolerance
	WINDOW win AS (PARTITION BY d.source_id ORDER BY d.ts)
	ORDER BY d.ts, d.source_id
) AS sub
WHERE
	sub.outside = FALSE
$BODY$
  LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION solaragg.process_one_agg_stale_datum(kind char)
  RETURNS integer LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					record;
	stale_t_start			timestamp;
	stale_t_end 			timestamp;
	stale_ts_prevstart		timestamptz;
	stale_ts_end 			timestamptz;
	agg_span 				interval;
	agg_json 				jsonb := NULL;
	agg_jmeta 				jsonb := NULL;
	agg_reading 			jsonb := NULL;
	agg_reading_ts_start 	timestamptz := NULL;
	agg_reading_ts_end 		timestamptz := NULL;
	node_tz 				text := 'UTC';
	proc_count 				integer := 0;
	curs CURSOR FOR SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = kind
		-- Too slow to order; not strictly fair but process much faster
		-- ORDER BY ts_start ASC, created ASC, node_id ASC, source_id ASC
		LIMIT 1
		FOR UPDATE SKIP LOCKED;
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
		-- get the node TZ for local date/time
		SELECT l.time_zone FROM solarnet.sn_node n
		INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE n.node_id = stale.node_id
		INTO node_tz;

		IF NOT FOUND THEN
			RAISE NOTICE 'Node % has no time zone, will use UTC.', stale.node_id;
			node_tz := 'UTC';
		END IF;
		
		-- stash local time start/end so date calculations for day+ correctly handles DST boundaries
		stale_t_start := stale.ts_start AT TIME ZONE node_tz;
		stale_t_end := stale_t_start + agg_span;
		stale_ts_prevstart := (stale_t_start - agg_span) AT TIME ZONE node_tz;
		stale_ts_end := stale_t_end AT TIME ZONE node_tz;

		CASE kind
			WHEN 'h' THEN
				SELECT jdata, jmeta
				FROM solaragg.calc_datum_time_slots(stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, agg_span, 0, interval '1 hour')
				INTO agg_json, agg_jmeta;
				
				SELECT jdata, ts_start, ts_end
				FROM solardatum.calculate_datum_diff_over(stale.node_id, stale.source_id::text, stale.ts_start, stale.ts_start + agg_span)
				INTO agg_reading, agg_reading_ts_start, agg_reading_ts_end;

			WHEN 'd' THEN
				SELECT jdata, jmeta
				FROM solaragg.calc_agg_datum_agg(stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, stale_ts_end, 'h')
				INTO agg_json, agg_jmeta;
				
				SELECT jsonb_strip_nulls(jsonb_build_object(
					 'as', solarcommon.first(jdata_as ORDER BY ts_start),
					 'af', solarcommon.first(jdata_af ORDER BY ts_start DESC),
					 'a', solarcommon.jsonb_sum_object(jdata_ad)
				))
				FROM solaragg.agg_datum_hourly
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start >= stale.ts_start
					AND ts_start < stale_ts_end
				GROUP BY node_id, source_id
				INTO agg_reading;

			ELSE
				SELECT jdata, jmeta
				FROM solaragg.calc_agg_datum_agg(stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, stale_ts_end, 'd')
				INTO agg_json, agg_jmeta;
				
				SELECT jsonb_strip_nulls(jsonb_build_object(
					 'as', solarcommon.first(jdata_as ORDER BY ts_start),
					 'af', solarcommon.first(jdata_af ORDER BY ts_start DESC),
					 'a', solarcommon.jsonb_sum_object(jdata_ad)
				))
				FROM solaragg.agg_datum_daily
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start >= stale.ts_start
					AND ts_start < stale_ts_end
				GROUP BY node_id, source_id
				INTO agg_reading;
		END CASE;

		IF agg_json IS NULL AND (agg_reading IS NULL 
				OR (agg_reading_ts_start IS NOT NULL AND agg_reading_ts_start = agg_reading_ts_end)
				) THEN
			-- no data in range, so delete agg row
			-- using date range in case time zone of node has changed
			CASE kind
				WHEN 'h' THEN
					-- note NOT using stale_ts_prevstart here because not needed for hourly
					DELETE FROM solaragg.agg_datum_hourly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
				WHEN 'd' THEN
					DELETE FROM solaragg.agg_datum_daily
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end;
				ELSE
					DELETE FROM solaragg.agg_datum_monthly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end;
			END CASE;
		ELSE
			CASE kind
				WHEN 'h' THEN
					INSERT INTO solaragg.agg_datum_hourly (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta,
						jdata_as, jdata_af, jdata_ad)
					VALUES (
						stale.ts_start,
						stale_t_start,
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta,
						agg_reading->'as',
						agg_reading->'af',
						agg_reading->'a'
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta,
						jdata_as = EXCLUDED.jdata_as,
						jdata_af = EXCLUDED.jdata_af,
						jdata_ad = EXCLUDED.jdata_ad;

					-- no delete from node tz change needed for hourly
				WHEN 'd' THEN
					INSERT INTO solaragg.agg_datum_daily (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta,
						jdata_as, jdata_af, jdata_ad)
					VALUES (
						stale.ts_start,
						CAST(stale_t_start AS DATE),
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta,
						agg_reading->'as',
						agg_reading->'af',
						agg_reading->'a'
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta,
						jdata_as = EXCLUDED.jdata_as,
						jdata_af = EXCLUDED.jdata_af,
						jdata_ad = EXCLUDED.jdata_ad;

					-- in case node tz changed, remove stale record(s)
					DELETE FROM solaragg.agg_datum_daily
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end
						AND ts_start <> stale.ts_start;
				ELSE
					INSERT INTO solaragg.agg_datum_monthly (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta,
						jdata_as, jdata_af, jdata_ad)
					VALUES (
						stale.ts_start,
						CAST(stale_t_start AS DATE),
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta,
						agg_reading->'as',
						agg_reading->'af',
						agg_reading->'a'
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta,
						jdata_as = EXCLUDED.jdata_as,
						jdata_af = EXCLUDED.jdata_af,
						jdata_ad = EXCLUDED.jdata_ad;

					-- in case node tz changed, remove stale record(s)
					DELETE FROM solaragg.agg_datum_monthly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end
						AND ts_start <> stale.ts_start;
			END CASE;
		END IF;
		DELETE FROM solaragg.agg_stale_datum WHERE CURRENT OF curs;
		proc_count := 1;

		-- now make sure we recalculate the next aggregate level by submitting a stale record for the next level
		-- and also update daily audit stats
		CASE kind
			WHEN 'h' THEN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'd')
				ON CONFLICT DO NOTHING;

			WHEN 'd' THEN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('month', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'm')
				ON CONFLICT DO NOTHING;

				-- handle update to raw audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'r')
				ON CONFLICT DO NOTHING;

				-- handle update to hourly audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'h')
				ON CONFLICT DO NOTHING;

				-- handle update to daily audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'd')
				ON CONFLICT DO NOTHING;
			ELSE
				-- handle update to monthly audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('month', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'm')
				ON CONFLICT DO NOTHING;
		END CASE;
	END IF;
	CLOSE curs;
	RETURN proc_count;
END;
$$;
