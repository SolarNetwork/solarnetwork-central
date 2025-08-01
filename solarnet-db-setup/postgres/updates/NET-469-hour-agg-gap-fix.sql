CREATE OR REPLACE FUNCTION solardatm.find_datm_hours(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		stream_id 		UUID,
		ts_start		TIMESTAMP WITH TIME ZONE
	) LANGUAGE SQL STABLE ROWS 2000 AS
$$
	-- include previous adjacent hour if first datum in hour falls exactly on hour, to
	-- account for possible accumulation after an hour gap (NET-469)
	SELECT DISTINCT ON (ts_start)
		  sid
		, UNNEST(CASE 
			  	WHEN date_trunc('hour', ts) = min(ts) THEN ARRAY[date_trunc('hour', ts) - INTERVAL 'PT1H', date_trunc('hour', ts)]
			  	ELSE ARRAY[date_trunc('hour', ts)]
			  END) AS ts_start
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts >= start_ts
		AND ts < end_ts
	GROUP BY date_trunc('hour', ts)
$$;
