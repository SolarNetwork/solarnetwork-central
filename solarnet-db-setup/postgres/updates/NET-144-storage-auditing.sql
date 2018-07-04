-- keep track of requests to calculate counts, so parallel jobs can compute
CREATE TABLE solaragg.aud_datum_daily_stale (
	ts_start timestamp with time zone NOT NULL,
	node_id bigint NOT NULL,
	source_id text NOT NULL,
	aud_kind char(1) NOT NULL,
	created timestamp NOT NULL DEFAULT now(),
	CONSTRAINT aud_datum_daily_stale_pkey PRIMARY KEY (aud_kind, ts_start, node_id, source_id)
);

-- hold the daily level datum audit data
CREATE TABLE solaragg.aud_datum_daily (
	ts_start timestamp with time zone NOT NULL,
	node_id bigint NOT NULL,
	source_id character varying(64) NOT NULL,
    prop_count bigint NOT NULL DEFAULT 0,
    datum_q_count bigint NOT NULL DEFAULT 0,
	datum_count integer NOT NULL DEFAULT 0,
	datum_hourly_count smallint NOT NULL DEFAULT 0,
	datum_daily_pres BOOLEAN NOT NULL DEFAULT FALSE,
	CONSTRAINT aud_datum_daily_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

-- hold the monthly level datum audit data
CREATE TABLE solaragg.aud_datum_monthly (
	ts_start timestamp with time zone NOT NULL,
	node_id bigint NOT NULL,
	source_id character varying(64) NOT NULL,
    prop_count bigint NOT NULL DEFAULT 0,
    datum_q_count bigint NOT NULL DEFAULT 0,
	datum_count integer NOT NULL DEFAULT 0,
	datum_hourly_count smallint NOT NULL DEFAULT 0,
	datum_daily_count smallint NOT NULL DEFAULT 0,
	datum_monthly_pres boolean NOT NULL DEFAULT FALSE,
	CONSTRAINT aud_datum_monthly_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

/**
 * View of node time zones and local time information.
 */
CREATE OR REPLACE VIEW solarnet.node_local_time AS
	SELECT n.node_id,
		COALESCE(l.time_zone, 'UTC'::character varying(64)) AS time_zone,
		CURRENT_TIMESTAMP AT TIME ZONE COALESCE(l.time_zone, 'UTC') AS local_ts,
		EXTRACT(HOUR FROM CURRENT_TIMESTAMP AT TIME ZONE COALESCE(l.time_zone, 'UTC'))::integer AS local_hour_of_day
	FROM solarnet.sn_node n
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id;

ALTER TABLE solaragg.aud_datum_hourly
	ADD COLUMN datum_count integer NOT NULL DEFAULT 0;

CREATE OR REPLACE FUNCTION solardatum.store_datum(
	cdate timestamp with time zone,
	node bigint,
	src text,
	pdate timestamp with time zone,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	ts_crea timestamp with time zone := COALESCE(cdate, now());
	ts_post timestamp with time zone := COALESCE(pdate, now());
	jdata_json jsonb := jdata::jsonb;
	jdata_prop_count integer := solardatum.datum_prop_count(jdata_json);
	ts_post_hour timestamp with time zone := date_trunc('hour', ts_post);
	is_insert boolean := false;
BEGIN
	INSERT INTO solardatum.da_datum(ts, node_id, source_id, posted, jdata_i, jdata_a, jdata_s, jdata_t)
	VALUES (ts_crea, node, src, ts_post, jdata_json->'i', jdata_json->'a', jdata_json->'s', solarcommon.json_array_to_text_array(jdata_json->'t'))
	ON CONFLICT (node_id, ts, source_id) DO UPDATE
	SET jdata_i = EXCLUDED.jdata_i,
		jdata_a = EXCLUDED.jdata_a,
		jdata_s = EXCLUDED.jdata_s,
		jdata_t = EXCLUDED.jdata_t,
		posted = EXCLUDED.posted
	RETURNING (xmax = 0)
	INTO is_insert;

	INSERT INTO solaragg.aud_datum_hourly (
		ts_start, node_id, source_id, datum_count, prop_count)
	VALUES (ts_post_hour, node, src, 1, jdata_prop_count)
	ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
	SET datum_count = aud_datum_hourly.datum_count + (CASE is_insert WHEN TRUE THEN 1 ELSE 0 END),
		prop_count = aud_datum_hourly.prop_count + EXCLUDED.prop_count;
END;
$BODY$;

CREATE OR REPLACE FUNCTION solaragg.process_one_agg_stale_datum(kind char)
  RETURNS integer LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	stale record;
	curs CURSOR FOR SELECT * FROM solaragg.agg_stale_datum
			WHERE agg_kind = kind
			ORDER BY ts_start ASC, created ASC, node_id ASC, source_id ASC
			LIMIT 1
			FOR UPDATE SKIP LOCKED;
	agg_span interval;
	agg_json jsonb := NULL;
	node_tz text := 'UTC';
	result integer := 0;
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
		SELECT l.time_zone  FROM solarnet.sn_node n
		INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE n.node_id = stale.node_id
		INTO node_tz;

		IF NOT FOUND THEN
			RAISE NOTICE 'Node % has no time zone, will use UTC.', stale.node_id;
			node_tz := 'UTC';
		END IF;

		SELECT jdata FROM solaragg.calc_datum_time_slots(stale.node_id, ARRAY[stale.source_id::text],
			stale.ts_start, agg_span, 0, interval '1 hour')
		INTO agg_json;
		IF agg_json IS NULL THEN
			-- delete agg, using date range in case time zone of node has changed
			CASE kind
				WHEN 'h' THEN
					DELETE FROM solaragg.agg_datum_hourly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span;
				WHEN 'd' THEN
					DELETE FROM solaragg.agg_datum_daily
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span;
				ELSE
					DELETE FROM solaragg.agg_datum_monthly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span;
			END CASE;
		ELSE
			CASE kind
				WHEN 'h' THEN
					INSERT INTO solaragg.agg_datum_hourly (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t)
					VALUES (
						stale.ts_start,
						stale.ts_start at time zone node_tz,
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t')
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t;

					-- in case node tz changed, remove stale record(s)
					DELETE FROM solaragg.agg_datum_hourly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span
						AND ts_start <> stale.ts_start;
				WHEN 'd' THEN
					INSERT INTO solaragg.agg_datum_daily (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t)
					VALUES (
						stale.ts_start,
						CAST(stale.ts_start at time zone node_tz AS DATE),
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t')
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t;

					-- in case node tz changed, remove stale record(s)
					DELETE FROM solaragg.agg_datum_daily
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span
						AND ts_start <> stale.ts_start;
				ELSE
					INSERT INTO solaragg.agg_datum_monthly (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t)
					VALUES (
						stale.ts_start,
						CAST(stale.ts_start at time zone node_tz AS DATE),
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t')
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t;

					-- in case node tz changed, remove stale record(s)
					DELETE FROM solaragg.agg_datum_monthly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span
						AND ts_start <> stale.ts_start;
			END CASE;
		END IF;
		DELETE FROM solaragg.agg_stale_datum WHERE CURRENT OF curs;
		result := 1;

		-- now make sure we recalculate the next aggregate level by submitting a stale record for the next level
		-- and also update daily audit stats
		CASE kind
			WHEN 'h' THEN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('day', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'd')
				ON CONFLICT DO NOTHING;
			WHEN 'd' THEN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('month', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'm')
				ON CONFLICT DO NOTHING;

				-- handle update to raw audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'r')
				ON CONFLICT DO NOTHING;

				-- handle update to hourly audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'h')
				ON CONFLICT DO NOTHING;

				-- handle update to daily audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'd')
				ON CONFLICT DO NOTHING;
			ELSE
				-- handle update to monthly audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('month', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'm')
				ON CONFLICT DO NOTHING;
		END CASE;
	END IF;
	CLOSE curs;
	RETURN result;
END;
$BODY$;


/**
 * Process a single row from the `solaragg.aud_datum_daily_stale` table by performing
 * the appropriate calculations and updating the appropriate audit table with the results.
 *
 * Supported `kind` values are:
 *
 *  * `r` for solardatum.da_datum data rolled up to day values into the `solaragg.aud_datum_daily` table
 *  * `h` for solaragg.agg_datum_hourly data rolled up to day values into the `solaragg.aud_datum_daily` table
 *  * `d` for solaragg.agg_datum_daily data rolled up to day values into the `solaragg.aud_datum_daily` table
 *  * `m` for solaragg.agg_datum_monthly data rolled up to month values into the `solaragg.aud_datum_monthly` table
 *
 * @param kind the rollup kind to process
 * @returns the number of rows processed (always 1 or 0)
 */
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
				SET datum_count = EXCLUDED.datum_count;

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
				SET datum_hourly_count = EXCLUDED.datum_hourly_count;

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
					datum_q_count = EXCLUDED.datum_q_count;

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
				CROSS JOIN datum d
				WHERE aud.node_id = stale.node_id
					AND aud.source_id = stale.source_id
					AND aud.ts_start >= stale.ts_start
					AND aud.ts_start < stale.ts_start + interval '1 month'
				GROUP BY aud.node_id, aud.source_id
				ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					datum_hourly_count = EXCLUDED.datum_hourly_count,
					datum_daily_count = EXCLUDED.datum_daily_count,
					datum_monthly_pres = EXCLUDED.datum_monthly_pres,
					prop_count = EXCLUDED.prop_count,
					datum_q_count = EXCLUDED.datum_q_count;
		END CASE;

		-- in case node tz changed, remove record(s) from other zone
		CASE kind
			WHEN 'm' THEN
				-- monthly records clean 1 month on either side
				DELETE FROM solaragg.aud_datum_monthly
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start > stale.ts_start - interval '1 month'
					AND ts_start < stale.ts_start + interval '1 month'
					AND ts_start <> stale.ts_start;
			ELSE
				-- daily records clean 1 day on either side
				DELETE FROM solaragg.aud_datum_daily
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start > stale.ts_start - interval '1 day'
					AND ts_start < stale.ts_start + interval '1 day'
					AND ts_start <> stale.ts_start;
		END CASE;

		-- remove processed stale record
		DELETE FROM solaragg.aud_datum_daily_stale WHERE CURRENT OF curs;
		result := 1;
	END IF;
	CLOSE curs;
	RETURN result;
END;
$BODY$;
