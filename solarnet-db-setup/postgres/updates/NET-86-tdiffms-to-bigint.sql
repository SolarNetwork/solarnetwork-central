DROP FUNCTION solaragg.find_datum_for_time_slot(bigint, text[], timestamp with time zone, interval, interval);

CREATE OR REPLACE FUNCTION solaragg.find_datum_for_time_slot(
	IN node bigint, 
	IN sources text[], 
	IN start_ts timestamp with time zone, 
	IN span interval, 
	IN tolerance interval DEFAULT interval '1 hour')
  RETURNS TABLE(ts timestamp with time zone, source_id text, tsms bigint, percent real, tdiffms bigint, jdata json) AS
$BODY$
SELECT * FROM (
	SELECT 
		d.ts,
		d.source_id,
		CAST(EXTRACT(EPOCH FROM d.ts) * 1000 AS BIGINT) as tsms,
		CASE 
			WHEN lead(d.ts) over win < start_ts OR lag(d.ts) over win > (start_ts + span)
				THEN -1::real
			WHEN d.ts < start_ts
				THEN 0::real
			WHEN d.ts > (start_ts + span) AND lag(d.ts) over win IS NULL
				THEN 0::real
			WHEN d.ts > (start_ts + span)
				THEN (1.0::real - EXTRACT('epoch' FROM (d.ts - (start_ts + span))) / EXTRACT('epoch' FROM (d.ts - lag(d.ts) over win)))::real
			WHEN lag(d.ts) over win < start_ts
				THEN (EXTRACT('epoch' FROM (d.ts - start_ts)) / EXTRACT('epoch' FROM (d.ts - lag(d.ts) over win)))::real
			ELSE 1::real
		END AS percent,
		COALESCE(CAST(EXTRACT(EPOCH FROM d.ts - lag(d.ts) over win) * 1000 AS BIGINT), 0) as tdiff,
		d.jdata as jdata
	FROM solardatum.da_datum d
	WHERE d.node_id = node
		AND d.source_id = ANY(sources)
		AND d.ts >= start_ts - tolerance
		AND d.ts <= start_ts + span + tolerance
	WINDOW win AS (PARTITION BY d.source_id ORDER BY d.ts)
	ORDER BY d.ts, d.source_id
) AS sub
WHERE 
	sub.percent > -1
$BODY$
  LANGUAGE sql STABLE;
