-- Must run the following as aa superuser:
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

CREATE SEQUENCE IF NOT EXISTS solaruser.user_node_event_hook_seq;

CREATE TABLE IF NOT EXISTS solaruser.user_node_event_hook (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_node_event_hook_seq'),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	node_ids		BIGINT[],
	source_ids		CHARACTER VARYING(64)[],
	topic 			TEXT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	sprops 			jsonb,
	CONSTRAINT user_node_event_hook_pkey PRIMARY KEY (id),
	CONSTRAINT user_node_event_hook_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS user_node_event_hook_user_topic_idx ON solaruser.user_node_event_hook
	(user_id, topic);

CREATE TABLE IF NOT EXISTS solaruser.user_node_event_task (
	id				uuid NOT NULL,
	hook_id			BIGINT NOT NULL,
	node_id			BIGINT NOT NULL,
	source_id		CHARACTER VARYING(64),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	jdata			jsonb,
	CONSTRAINT user_node_event_task_pkey PRIMARY KEY (id),
	CONSTRAINT user_node_event_task_hook_fk FOREIGN KEY (hook_id)
		REFERENCES solaruser.user_node_event_hook (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS solaruser.user_node_event_task_result (
	id				uuid NOT NULL,
	hook_id			BIGINT NOT NULL,
	node_id			BIGINT NOT NULL,
	source_id		CHARACTER VARYING(64),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	status			CHARACTER(1) NOT NULL,
	jdata			jsonb,
	success 		BOOLEAN,
	completed 		TIMESTAMP WITH TIME ZONE,
	message			TEXT,
	CONSTRAINT user_node_event_task_result_pkey PRIMARY KEY (id),
	CONSTRAINT user_node_event_task_result_hook_fk FOREIGN KEY (hook_id)
		REFERENCES solaruser.user_node_event_hook (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS user_node_event_task_result_hook_idx ON solaruser.user_node_event_task_result
	(hook_id);

CREATE OR REPLACE FUNCTION solaruser.find_user_node_event_tasks(
	node BIGINT, source CHARACTER VARYING(64), etopic TEXT)
  RETURNS SETOF BIGINT LANGUAGE SQL STABLE AS
$$
	SELECT h.id
	FROM solaruser.user_node_event_hook h, solaruser.user_node un
	WHERE
		un.node_id = node
		AND h.user_id = un.user_id
		AND h.topic = etopic
		AND (
			COALESCE(cardinality(h.node_ids), 0) = 0
			OR h.node_ids @> ARRAY[node]
		)
		AND (
			COALESCE(cardinality(source_ids), 0) = 0
			OR source ~ ANY(
				ARRAY(SELECT array_agg(solarcommon.ant_pattern_to_regexp(s)) AS source_pats FROM unnest(h.source_ids) s)
			)
		)
$$;

CREATE OR REPLACE FUNCTION solaruser.add_user_node_event_tasks(
	node BIGINT, source CHARACTER VARYING(64), etopic TEXT, edata jsonb)
  RETURNS void LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaruser.user_node_event_task (id, hook_id, node_id, source_id, jdata)
	SELECT uuid_generate_v4() AS id
		, hook_id
		, node
		, source
		, edata AS jdata
	FROM solaruser.find_user_node_event_tasks(node, source, etopic) AS hook_id
$$;

/**
 * FUNCTION solaruser.claim_user_node_event_task
 *
 * "Claim" a user node event, so it may be processed by some external job. This function must be
 * called within a transaction. The returned row will be locked, so that the external job can
 * delete it once complete.
 */
CREATE OR REPLACE FUNCTION solaruser.claim_user_node_event_task()
  RETURNS solaruser.user_node_event_task LANGUAGE SQL VOLATILE AS
$$
	SELECT * FROM solaruser.user_node_event_task
	LIMIT 1
	FOR UPDATE SKIP LOCKED
$$;

CREATE OR REPLACE FUNCTION solaruser.add_user_node_event_task_result(
	task_id uuid
	, is_success BOOLEAN
	, task_status char DEFAULT 'c'
	, msg TEXT DEFAULT NULL
	, completed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
  RETURNS SETOF solaruser.user_node_event_task_result LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaruser.user_node_event_task_result (id, hook_id, node_id, source_id, jdata
			, status, success, message, completed)
	SELECT id
		, hook_id
		, node_id
		, source_id
		, jdata
		, task_status
		, is_success
		, msg
		, completed_at
	FROM solaruser.user_node_event_task
	WHERE id = task_id
	ON CONFLICT (id) DO UPDATE SET
		status = EXCLUDED.status
		, success = EXCLUDED.success
		, message = EXCLUDED.message
		, completed = EXCLUDED.completed
	RETURNING *
$$;

--
--
--

DROP FUNCTION IF EXISTS solaragg.process_one_agg_stale_datum(char);

CREATE OR REPLACE FUNCTION solaragg.process_one_agg_stale_datum(kind char)
  RETURNS SETOF solaragg.agg_stale_datum LANGUAGE plpgsql VOLATILE AS
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
				-- Dramatically faster execution via EXECUTE than embedded SQL here; better query plan
				
				EXECUTE 'SELECT jdata, jmeta FROM solaragg.calc_datum_time_slots($1, $2, $3, $4, $5, $6)'
				INTO agg_json, agg_jmeta
				USING stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, agg_span, 0, interval '1 hour';
				
				EXECUTE 'SELECT jdata, ts_start, ts_end FROM solardatum.calculate_datum_diff_over($1, $2, $3, $4)'
				INTO agg_reading, agg_reading_ts_start, agg_reading_ts_end
				USING stale.node_id, stale.source_id::text, stale.ts_start, stale.ts_start + agg_span;

			WHEN 'd' THEN
				EXECUTE 'SELECT jdata, jmeta FROM solaragg.calc_agg_datum_agg($1, $2, $3, $4, $5)'
				INTO agg_json, agg_jmeta
				USING stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, stale_ts_end, 'h';
				
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
				EXECUTE 'SELECT jdata, jmeta FROM solaragg.calc_agg_datum_agg($1, $2, $3, $4, $5)'
				INTO agg_json, agg_jmeta
				USING stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, stale_ts_end, 'd';
				
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
		RETURN NEXT stale;
	END IF;
	CLOSE curs;
END;
$$;

CREATE OR REPLACE FUNCTION solaragg.process_agg_stale_datum(kind char, max integer)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	one_result INTEGER := 1;
	total_result INTEGER := 0;
BEGIN
	LOOP
		IF one_result < 1 OR (max > -1 AND total_result >= max) THEN
			EXIT;
		END IF;
		PERFORM solaragg.process_one_agg_stale_datum(kind);
		IF FOUND THEN
			one_result := 1;
			total_result := total_result + 1;
		ELSE
			one_result := 0;
		END IF;
	END LOOP;
	RETURN total_result;
END;
$$;

--
--
--

DROP FUNCTION IF EXISTS solaragg.process_one_agg_stale_loc_datum(char);

CREATE OR REPLACE FUNCTION solaragg.process_one_agg_stale_loc_datum(kind char)
  RETURNS SETOF solaragg.agg_stale_loc_datum LANGUAGE plpgsql VOLATILE AS
$$
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
		RETURN NEXT stale;
	END IF;
	CLOSE curs;
END;
$$;

CREATE OR REPLACE FUNCTION solaragg.process_agg_stale_loc_datum(kind char, max integer)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	one_result INTEGER := 1;
	total_result INTEGER := 0;
BEGIN
	LOOP
		IF one_result < 1 OR (max > -1 AND total_result >= max) THEN
			EXIT;
		END IF;
		PERFORM solaragg.process_one_agg_stale_loc_datum(kind);
		IF FOUND THEN
			one_result := 1;
			total_result := total_result + 1;
		ELSE
			one_result := 0;
		END IF;
	END LOOP;
	RETURN total_result;
END;
$$;
