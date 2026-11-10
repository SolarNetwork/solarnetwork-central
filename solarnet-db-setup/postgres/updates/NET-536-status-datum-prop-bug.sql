
-- ==========================================================================================
-- Updates
-- ==========================================================================================

CREATE OR REPLACE FUNCTION solardatm.store_datum(
		sid 			UUID,
		ddate			TIMESTAMP WITH TIME ZONE,
		src				TEXT,
		rdate			TIMESTAMP WITH TIME ZONE,
		jdata_i			JSONB,
		jdata_a			JSONB,
		jdata_s			JSONB,
		jdata_t			TEXT[],
		INOUT p_i 		TEXT[],
		INOUT p_a 		TEXT[],
		INOUT p_s 		TEXT[],
		OUT   is_ins 	BOOLEAN
	) LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	-- property name arrays
	p		RECORD;

	-- property value arrays
	v_i 	NUMERIC[];
	v_a		NUMERIC[];
	v_s		TEXT[];

	idx		INTEGER;
BEGIN
	-- copy instantaneous props
	FOR p IN SELECT * FROM jsonb_each_text(jdata_i) LOOP
		idx := COALESCE(array_position(p_i, p.key), 0);
		IF idx < 1 THEN
			UPDATE solardatm.da_datm_meta SET names_i = CASE
					WHEN COALESCE(array_position(names_i, p.key), 0) < 1 THEN array_append(names_i, p.key)
					ELSE names_i
					END
			WHERE stream_id = sid
			RETURNING names_i INTO p_i;
			idx := array_position(p_i, p.key);
		END IF;
		-- catch cast exceptions: saw example of 'Infinity' string
		BEGIN
			v_i[idx] := p.value::numeric;
		EXCEPTION WHEN others THEN
			RAISE WARNING 'JSON value not numeric: stream %, source %, ts %, i.key %, i.value %',
				sid, src, ddate, p.key, p.value;
			v_i[idx] := NULL;
		END;
	END LOOP;
	IF array_lower(v_i, 1) > 1 THEN
		-- ensure array starts at position 1, because JDBC doesn't handle offset slice easily
		v_i := array_fill(NULL::NUMERIC, ARRAY[array_lower(v_i, 1) - 1]) || v_i;
	END IF;

	-- copy accumulating props
	FOR p IN SELECT * FROM jsonb_each_text(jdata_a) LOOP
		idx := COALESCE(array_position(p_a, p.key), 0);
		IF idx < 1 THEN
			UPDATE solardatm.da_datm_meta SET names_a = CASE
					WHEN COALESCE(array_position(names_a, p.key), 0) < 1 THEN array_append(names_a, p.key)
					ELSE names_a
					END
			WHERE stream_id = sid
			RETURNING names_a INTO p_a;
			idx := array_position(p_a, p.key);
		END IF;
		BEGIN
			v_a[idx] := p.value::numeric;
		EXCEPTION WHEN others THEN
			RAISE WARNING 'JSON value not numeric: stream %, source %, ts %, i.key %, i.value %',
				sid, src, ddate, p.key, p.value;
			v_a[idx] := NULL;
		END;
	END LOOP;
	IF array_lower(v_a, 1) > 1 THEN
		-- ensure array starts at position 1, because JDBC doesn't handle offset slice easily
		v_a := array_fill(NULL::NUMERIC, ARRAY[array_lower(v_a, 1) - 1]) || v_a;
	END IF;

	-- copy status props
	FOR p IN SELECT * FROM jsonb_each_text(jdata_s) LOOP
		idx := COALESCE(array_position(p_s, p.key), 0);
		IF idx < 1 THEN
			UPDATE solardatm.da_datm_meta SET names_s = CASE
					WHEN COALESCE(array_position(names_s, p.key), 0) < 1 THEN array_append(names_s, p.key)
					ELSE names_s
					END
			WHERE stream_id = sid
			RETURNING names_s INTO p_s;
			idx := array_position(p_s, p.key);
		END IF;
		v_s[idx] := p.value;
	END LOOP;
	IF array_lower(v_s, 1) > 1 THEN
		-- ensure array starts at position 1, because JDBC doesn't handle offset slice easily
		v_s := array_fill(NULL::TEXT, ARRAY[array_lower(v_s, 1) - 1]) || v_s;
	END IF;

	INSERT INTO solardatm.da_datm (stream_id, ts, received, data_i, data_a, data_s, data_t)
	VALUES (
		sid
		, ddate
		, rdate
		, CASE WHEN COALESCE(array_length(v_i, 1), 0) < 1 THEN NULL ELSE v_i END
		, CASE WHEN COALESCE(array_length(v_a, 1), 0) < 1 THEN NULL ELSE v_a END
		, CASE WHEN COALESCE(array_length(v_s, 1), 0) < 1 THEN NULL ELSE v_s END
		, jdata_t
	)
	ON CONFLICT (stream_id, ts) DO UPDATE
	SET received = EXCLUDED.received,
		data_i = EXCLUDED.data_i,
		data_a = EXCLUDED.data_a,
		data_s = EXCLUDED.data_s,
		data_t = EXCLUDED.data_t
	RETURNING (xmax = 0)
	INTO is_ins;
END;
$$;
