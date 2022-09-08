CREATE OR REPLACE FUNCTION solardatm.find_datm_diff_at_rows(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance 		INTERVAL DEFAULT INTERVAL '3 months'
	) RETURNS SETOF solardatm.datm_rec LANGUAGE SQL STABLE ROWS 10 AS
$$
	WITH d AS (
		SELECT (solardatm.calc_datm_at(d, start_ts)).*
		FROM solardatm.find_datm_around(sid, start_ts, tolerance) d
		HAVING count(*) > 0
		UNION
		SELECT (solardatm.calc_datm_at(d, end_ts)).*
		FROM solardatm.find_datm_around(sid, end_ts, tolerance) d
		HAVING count(*) > 0
	)
	-- combine raw datm with reset datm
	SELECT d.stream_id
		, d.ts
		, d.data_i
		, d.data_a
		, d.data_s
		, d.data_t
		, 0::SMALLINT AS rtype
	FROM d
$$;
