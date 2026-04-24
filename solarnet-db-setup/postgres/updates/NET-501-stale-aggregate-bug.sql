CREATE OR REPLACE FUNCTION solardatm.calc_stale_datm(
		sid 		UUID,
		ts_in 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT interval '3 months'
	) RETURNS TABLE (
		stream_id	UUID,
		ts_start 	TIMESTAMP WITH TIME ZONE
	) LANGUAGE SQL STABLE ROWS 4 AS
$$
	WITH b AS (
		-- curr hour
		(
			SELECT date_trunc('hour', ts_in) AS ts
		)
		UNION ALL
		-- prev datum hour
		(
			SELECT date_trunc('hour', d.ts) AS ts
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts < ts_in
				AND d.ts > ts_in - tolerance
			ORDER BY d.stream_id, d.ts DESC
			LIMIT 1
		)
		UNION ALL
		-- prev hour, if datum exactly on hour and prev datum exists
		(
			SELECT date_trunc('hour', ts_in) - INTERVAL 'PT1H' AS ts
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts < ts_in
				AND d.ts > ts_in - tolerance
				AND date_trunc('hour', ts_in) = ts_in
			ORDER BY d.stream_id, d.ts DESC
			LIMIT 1
		)
		UNION ALL
		-- next datum hour, or next datum hour - 1 if next datum exactly on hour
		(
			SELECT CASE WHEN date_trunc('hour', d.ts) = d.ts
				THEN d.ts - INTERVAL 'PT1H'
				ELSE date_trunc('hour', d.ts)
				END AS ts
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts > ts_in
				AND d.ts < ts_in + tolerance
			ORDER BY d.stream_id, d.ts
			LIMIT 1
		)
	)
	SELECT DISTINCT sid, ts FROM b
$$;
