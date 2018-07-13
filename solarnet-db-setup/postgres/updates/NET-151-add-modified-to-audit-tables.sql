ALTER TABLE solaragg.aud_datum_daily ADD COLUMN processed_count TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE solaragg.aud_datum_daily ADD COLUMN processed_hourly_count TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE solaragg.aud_datum_daily ADD COLUMN processed_io_count TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE solaragg.aud_datum_monthly ADD COLUMN processed TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE solaragg.aud_acc_datum_daily ADD COLUMN processed TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- update to populate the new processed columns
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
					datum_q_count = EXCLUDED.datum_q_count,
					processed = CURRENT_TIMESTAMP;
		END CASE;

		CASE kind
			WHEN 'm' THEN
				-- in case node tz changed, remove record(s) from other zone
				-- monthly records clean 1 month on either side
				DELETE FROM solaragg.aud_datum_monthly
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start > stale.ts_start - interval '1 month'
					AND ts_start < stale.ts_start + interval '1 month'
					AND ts_start <> stale.ts_start;

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

-- update to populate new processed column
CREATE OR REPLACE FUNCTION solaragg.populate_audit_acc_datum_daily(node bigint, source text)
	RETURNS void LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaragg.aud_acc_datum_daily (ts_start, node_id, source_id,
		datum_count, datum_hourly_count, datum_daily_count, datum_monthly_count)
	SELECT
		ts_start,
		node_id,
		source_id,
		datum_count,
		datum_hourly_count,
		datum_daily_count,
		datum_monthly_count
	FROM solaragg.find_audit_acc_datum_daily(node, source)
	ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
	SET datum_count = EXCLUDED.datum_count,
		datum_hourly_count = EXCLUDED.datum_hourly_count,
		datum_daily_count = EXCLUDED.datum_daily_count,
		datum_monthly_count = EXCLUDED.datum_monthly_count,
		processed = CURRENT_TIMESTAMP;
$$;
