CREATE OR REPLACE FUNCTION solaragg.populate_audit_acc_datum_daily(node bigint, source text)
	RETURNS void LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaragg.aud_acc_datum_daily (ts_start, node_id, source_id,
		datum_count, datum_hourly_count, datum_daily_count, datum_monthly_count)
	SELECT
		ts_start,
		node_id,
		source_id,
		COALESCE(datum_count, 0) AS datum_count,
		COALESCE(datum_hourly_count, 0) AS datum_hourly_count,
		COALESCE(datum_daily_count, 0) AS datum_daily_count,
		COALESCE(datum_monthly_count, 0) AS datum_monthly_count
	FROM solaragg.find_audit_acc_datum_daily(node, source)
	ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
	SET datum_count = EXCLUDED.datum_count,
		datum_hourly_count = EXCLUDED.datum_hourly_count,
		datum_daily_count = EXCLUDED.datum_daily_count,
		datum_monthly_count = EXCLUDED.datum_monthly_count,
		processed = CURRENT_TIMESTAMP;
$$;
