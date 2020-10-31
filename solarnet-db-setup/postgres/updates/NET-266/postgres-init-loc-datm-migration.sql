CREATE OR REPLACE FUNCTION solardatm.migrate_loc_datum(
	loc 			BIGINT,
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

	d		solardatum.da_loc_datum;
	rcount 	BIGINT := 0;
	is_ins  BOOLEAN;
	curs 	NO SCROLL CURSOR FOR
				SELECT * FROM solardatum.da_loc_datum
				WHERE loc_id = loc
					AND source_id = src
					AND ts >= start_date
					AND ts < end_date;
BEGIN
	-- get, or create, stream ID
	INSERT INTO solardatm.da_loc_datm_meta (loc_id, source_id)
	VALUES (loc, src)
	ON CONFLICT (loc_id, source_id) DO NOTHING
	RETURNING stream_id, names_i, names_a, names_s
	INTO sid, p_i, p_a, p_s;

	IF NOT FOUND THEN
		SELECT stream_id, names_i, names_a, names_s
		FROM solardatm.da_loc_datm_meta
		WHERE loc_id = loc AND source_id = src
		INTO sid, p_i, p_a, p_s;
	END IF;

	OPEN curs;
	LOOP
		FETCH curs INTO d;
		EXIT WHEN NOT FOUND;
		SELECT * FROM solardatm.store_loc_datum(sid, d.ts, d.source_id, d.posted,
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
