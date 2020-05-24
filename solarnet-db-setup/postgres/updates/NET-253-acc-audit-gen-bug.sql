/**
 * Look for node sources that have no corresponding row in the `solaragg.aud_acc_datum_daily` table
 * on a particular date. The purpose of this is to support populating the accumulating storage
 * date for nodes even if they are offline and not posting data currently.
 *
 * @param ts the date to look for; defaults to the current date
 */
CREATE OR REPLACE FUNCTION solaragg.find_audit_datum_daily_missing(ts date DEFAULT CURRENT_DATE)
	RETURNS TABLE(
		node_id bigint,
		source_id character varying(64),
		ts_start timestamp with time zone,
		time_zone character varying(64)
	)
	LANGUAGE sql STABLE AS
$$
	WITH missing AS (
		SELECT r.node_id, r.source_id
		FROM solardatum.da_datum_range r
		EXCEPT
		SELECT a.node_id, a.source_id
		FROM solaragg.aud_acc_datum_daily a
		INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = a.node_id
		WHERE 
			ts_start >= ts::timestamptz - interval '24 hours'
			AND ts_start < ts::timestamptz + interval '24 hours'
			AND ts_start AT TIME ZONE nlt.time_zone = ts
	)
	SELECT m.node_id, m.source_id, ts::timestamp AT TIME ZONE nlt.time_zone, nlt.time_zone
	FROM missing m
	INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = m.node_id
$$;

/**
 * Call the `solaragg.find_audit_datum_daily_missing(date)` function and insert the results
 * into the `solaragg.aud_datum_daily_stale` table with an `aud_kind = 'm'` so a record of the found
 * node sources gets generated.
 *
 * The `aud_kind = m` value is used because the processor that handles that record also populates
 * the `solaragg.aud_acc_datum_daily` table.
 *
 * @param ts the date to look for; defaults to the current date
 * @return the number of rows inserted
 */
CREATE OR REPLACE FUNCTION solaragg.populate_audit_datum_daily_missing(ts date DEFAULT CURRENT_DATE)
	RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ins_count bigint := 0;
BEGIN
	INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
	SELECT 
		date_trunc('month', ts_start at time zone time_zone) at time zone time_zone
		, node_id
		, source_id
		, 'm' AS aud_kind
	FROM solaragg.find_audit_datum_daily_missing(ts)
	ON CONFLICT DO NOTHING;

	GET DIAGNOSTICS ins_count = ROW_COUNT;
	RETURN ins_count;
END;
$$;
