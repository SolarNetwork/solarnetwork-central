CREATE OR REPLACE FUNCTION solardatm.migrate_datum(
	node 			BIGINT,
	src 			TEXT,
	start_date		TIMESTAMP WITH TIME ZONE,
	end_date		TIMESTAMP WITH TIME ZONE
	) RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	sid 	UUID;

	-- property name arrays
	p_i		TEXT[];
	p_a		TEXT[];
	p_s		TEXT[];

	d		solardatum.da_datum;
	rcount 	BIGINT := 0;
	is_ins	BOOLEAN;
	curs 	NO SCROLL CURSOR FOR
				SELECT * FROM solardatum.da_datum
				WHERE node_id = node
					AND source_id = src
					AND ts >= start_date
					AND ts < end_date;
BEGIN
	-- get, or create, stream ID
	INSERT INTO solardatm.da_datm_meta (node_id, source_id)
	VALUES (node, src)
	ON CONFLICT (node_id, source_id) DO NOTHING
	RETURNING stream_id, names_i, names_a, names_s
	INTO sid, p_i, p_a, p_s;

	IF NOT FOUND THEN
		SELECT stream_id, names_i, names_a, names_s
		FROM solardatm.da_datm_meta
		WHERE node_id = node AND source_id = src
		INTO sid, p_i, p_a, p_s;
	END IF;

	OPEN curs;
	LOOP
		FETCH curs INTO d;
		EXIT WHEN NOT FOUND;
		SELECT * FROM solardatm.store_datum(sid, d.ts, d.source_id, d.posted,
						d.jdata_i, d.jdata_a, d.jdata_s, d.jdata_t, p_i, p_a, p_s)
		INTO p_i, p_a, p_s, is_ins;
		IF is_ins THEN
			rcount := rcount + 1;
		END IF;
	END LOOP;
	CLOSE curs;

	RETURN rcount;
END;
$$;


/**
 * Convert a JSON aggregate record into stream form.
 *
 * @param sid 				the stream ID of the datum being converted
 * @param ddate				the datum date (e.g. start_ts value)
 * @param jdata_i 			the instantaneous JSON aggregate data
 * @param jdata_a 			the accumulating JSON aggregate data
 * @param jdata_s 			the status JSON aggregate data
 * @param jdata_t 			the tag JSON aggregate data
 * @param jmeta 			the instantaneous aggregate statistics (count, min, max)
 * @param jdata_as 			the accumulating reading start values
 * @param jdata_af 			the accumulating reading finish values
 * @param jdata_ad 			the accumulating reading difference values
 * @param p_i				the stream instantaneous property names
 * @param p_a 				the stream accumulating property names
 * @param p_s				the stream status property names
 */
CREATE OR REPLACE FUNCTION solardatm.agg_json_datum_to_datm(
		sid 			UUID,
		ddate			TIMESTAMP WITH TIME ZONE,
		jdata_i			JSONB,
		jdata_a			JSONB,
		jdata_s			JSONB,
		jdata_t			TEXT[],
		jmeta			JSONB,
		jdata_as		JSONB,
		jdata_af		JSONB,
		jdata_ad		JSONB,
		p_i 			TEXT[],
		p_a 			TEXT[],
		p_s 			TEXT[],
		OUT stream_id 	UUID,
		OUT ts_start	TIMESTAMP WITH TIME ZONE,
		OUT data_i		NUMERIC[],					-- array of instantaneous property average values
		OUT data_a		NUMERIC[],					-- array of accumulating property clock difference values
		OUT data_s		TEXT[],						-- array of status property values
		OUT data_t		TEXT[],						-- array of all tags seen over period
		OUT stat_i		NUMERIC[][],				-- array of instantaneous property [count,min,max] statistic tuples
		OUT read_a		NUMERIC[][] 				-- array of accumulating property reading [start,finish,diff] tuples
	) LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	idx		INTEGER;
BEGIN
	stream_id := sid;
	ts_start := ddate;

	-- copy instantaneous props
	FOR idx IN 1..COALESCE(array_length(p_i, 1),0) LOOP
		-- catch cast exceptions: saw example of 'Infinity' string
		BEGIN
			data_i[idx] := (jdata_i->>p_i[idx])::numeric;
		EXCEPTION WHEN others THEN
			RAISE WARNING 'JSON value not numeric: stream %, ts %, i.key %, i.value %',
				sid, ddate, p_i[idx], jdata_i->>p_i[idx];
			data_i[idx] := NULL::numeric;
		END;
		BEGIN
			stat_i := stat_i || ARRAY[ARRAY[
				  COALESCE((jmeta->'i'->p_i[idx]->>'count')::numeric, 1::numeric)
				, COALESCE((jmeta->'i'->p_i[idx]->>'min')::numeric, data_i[idx])
				, COALESCE((jmeta->'i'->p_i[idx]->>'max')::numeric, data_i[idx])
				]::numeric[]];
		EXCEPTION WHEN others THEN
			RAISE WARNING 'JSON value not numeric: stream %, ts %, m.key %, m.value %',
				sid, ddate, p_i[idx], jmeta->'i'->p_i[idx];
			stat_i := stat_i || ARRAY[ARRAY[NULL,NULL,NULL]::numeric[]];
		END;
	END LOOP;

	-- copy accumulating props
	FOR idx IN 1..COALESCE(array_length(p_a, 1),0) LOOP
		-- catch cast exceptions: saw example of 'Infinity' string
		BEGIN
			data_a[idx] := (jdata_a->>p_a[idx])::numeric;
		EXCEPTION WHEN others THEN
			RAISE WARNING 'JSON value not numeric: stream %, ts %, i.key %, i.value %',
				sid, ddate, p_a[idx], jdata_a->>p_a[idx];
			data_a[idx] := NULL::numeric;
		END;
		BEGIN
			read_a := read_a || ARRAY[ARRAY[
				  (jdata_as->>p_a[idx])::numeric
				, (jdata_af->>p_a[idx])::numeric
				, (jdata_ad->>p_a[idx])::numeric
				]::numeric[]];
		EXCEPTION WHEN others THEN
			RAISE WARNING 'JSON value not numeric: stream %, ts %, ra.key %, as.value %, af.value %, ad.value %',
				sid, ddate, p_i[idx], jdata_as->>p_a[idx], jdata_af->>p_a[idx], jdata_ad->>p_a[idx];
			read_a := read_a || ARRAY[ARRAY[NULL,NULL,NULL]::numeric[]];
		END;
	END LOOP;

	-- copy accumulating props
	FOR idx IN 1..COALESCE(array_length(p_s, 1),0) LOOP
		data_s[idx] := (jdata_s->>p_s[idx]);
	END LOOP;

END;
$$;
