CREATE OR REPLACE FUNCTION solardatm.find_datm_aux_for_time_span(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		aux_type 	solardatm.da_datm_aux_type DEFAULT 'Reset'::solardatm.da_datm_aux_type
	) RETURNS TABLE (
		stream_id 	UUID,
		ts 			TIMESTAMP WITH TIME ZONE,
		data_a		NUMERIC[],
		rtype		SMALLINT
	) LANGUAGE SQL STABLE ROWS 50 AS
$$
	-- find reset records for same time range, split into two rows for each record: final
	-- and starting accumulating values
	WITH aux AS (
		SELECT
			  m.stream_id
			, aux.ts
			, m.names_a
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
			, unnest(ARRAY[1::SMALLINT, 2::SMALLINT]) AS rr
		FROM solardatm.da_datm_aux aux
		INNER JOIN solardatm.da_datm_meta m ON m.stream_id = aux.stream_id
		WHERE aux.atype = aux_type
			AND aux.stream_id = sid
			AND aux.ts >= start_ts
			AND aux.ts <= end_ts
	)
	-- convert reset record rows into datm rows by turning jdata_a JSON into data_a value array,
	-- respecting the array order defined by solardatm.da_datm_meta.names_a and excluding values
	-- not defined there
	SELECT
		  aux.stream_id
		, aux.ts
		, array_agg(val::TEXT::NUMERIC ORDER BY a_idx) AS data_a
		, min(aux.rr) AS rtype
	FROM aux
	INNER JOIN unnest(aux.names_a) WITH ORDINALITY AS a(a_name, a_idx) ON TRUE
	LEFT OUTER JOIN jsonb_each(aux.jdata_a) AS p(key,val) ON p.key = a.a_name
	GROUP BY aux.stream_id, aux.ts, aux.rr
$$;
