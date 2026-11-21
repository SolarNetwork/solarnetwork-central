/* ============================================================================
 * NET-545: store location datum property arrays starting at position 1, as node datum are, and
 * fix three slips in `solardatm.store_loc_datum()` that the node function does not have.
 *
 * `solardatm.store_loc_datum()` builds the `data_i`, `data_a` and `data_s` arrays by assigning
 * each property's value at the position of its name in the stream's `names_i`, `names_a` and
 * `names_s`. When a datum lacks the stream's first property of a kind, the first position
 * assigned is above 1, and Postgres makes an array whose lower bound is above 1, such as
 * `[2:3]={102700,13.8}`. Readers that ignore the lower bound then see the values under the wrong
 * properties: `solardatm.pad_vec()` and the `vec_agg_*` aggregates do, and so does JDBC, which
 * reads `[3:4]={3,4}` as `{3,4}`. The node function `solardatm.store_datum()` has filled the
 * leading places with NULL since February 2021; the location function now does the same, after
 * each of its three loops.
 *
 * The slips:
 *
 * - The accumulating loop's exception handler, for a value that cannot be cast to numeric, set
 *   the instantaneous value at that position to NULL instead of the accumulating one. A datum
 *   then lost an instantaneous value, or had its instantaneous array padded with NULLs, for
 *   example `{1,NULL,NULL}` or, with no instantaneous properties, `[2:2]={NULL}`.
 * - The status names update set the stream's `names_s` to its `names_i` when the name was already
 *   in `names_s` but not in the names the caller passed, which can happen when another datum adds
 *   the name in between. The store then failed with "array subscript in assignment must not be
 *   null", or, when the name was an instantaneous name as well, the stream's status names were
 *   replaced by its instantaneous names. NET-536 fixed the same slip in the node function.
 * - The 6-argument function passed the datum date `ddate` to `solardatm.calc_stale_datm()`
 *   instead of `ts_crea`, the datum date or the current time when it is NULL, so storing a datum
 *   with no date failed on a NULL stale hour. NET-284 fixed this in the node function, and dropped
 *   the unused `ts_recv_hour` variable, which is dropped here as well.
 *
 * Only datum stored from now on change. Location datum already stored keep their arrays.
 *
 * The function signatures are unchanged, so `CREATE OR REPLACE` keeps their existing owners and
 * ACLs.
 * ============================================================================
 */

/**
 * Migrate a JSON location datum into the `solardatm.da_datm` table.
 *
 * This function accepts the "current" property name arrays as an optimisation when migrating
 * multiple rows of data. If the given datum has properties that are not found in any of the
 * given name arrays, the name will be added and then the associated `solardatm.da_loc_datm_meta`
 * column updated to match. The name arrays are declared as `INOUT` so that the updated values
 * can be saved and used again, helping reduce the amount of updates needed to the
 * `solardatm.da_loc_datm_meta` table.
 *
 * @param sid 		the stream ID to use
 * @param ddate 	datum timestamp
 * @param src		datum source ID
 * @param rdate		datum received date
 * @param jdata_i	instantaneous JSON properties
 * @param jdata_a	accumulating JSON properties
 * @param jdata_s	status JSON properties
 * @param jdata_t	tag properties
 * @param p_i		the stream instantaneous property names; possibly appeneded to
 * @param p_a 		the stream accumulating property names; possible appended to
 * @param p_s		the stream status property names; possible appended to
 */
CREATE OR REPLACE FUNCTION solardatm.store_loc_datum(
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
			UPDATE solardatm.da_loc_datm_meta SET names_i = CASE
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
			UPDATE solardatm.da_loc_datm_meta SET names_a = CASE
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
			UPDATE solardatm.da_loc_datm_meta SET names_s = CASE
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


/**
 * Add or update a location datum record. The data is stored in the `solardatm.da_datm` table.
 *
 * @param ddate the datum timestamp
 * @param loc 	the location ID
 * @param src 	the source ID
 * @param rdate the date the datum was received by SolarNetwork
 * @param jdata the datum JSON object (with jdata_i, jdata_a, jdata_s, and jdata_t properties)
 * @param track if `TRUE` then also insert results of `solardatm.calc_stale_datm()` into the
 *              `solardatm.agg_stale_datm` table and call `solardatm.audit_increment_datum_count()`
 *              to keep the audit data updated
 */
CREATE OR REPLACE FUNCTION solardatm.store_loc_datum(
	ddate 			TIMESTAMP WITH TIME ZONE,
	loc 			BIGINT,
	src 			TEXT,
	rdate 			TIMESTAMP WITH TIME ZONE,
	jdata 			TEXT,
	track 			BOOLEAN DEFAULT TRUE)
  RETURNS UUID LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_crea 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(ddate, now());
	ts_recv 			TIMESTAMP WITH TIME ZONE	:= COALESCE(rdate, now());
	jdata_json 			JSONB 						:= jdata::jsonb;
	is_insert 			BOOLEAN 					:= false;

	sid 	UUID;

	-- property name arrays
	p_i		TEXT[];
	p_a		TEXT[];
	p_s		TEXT[];
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

	SELECT * FROM solardatm.store_loc_datum(sid, ts_crea, src, ts_recv,
					jdata_json->'i',
					jdata_json->'a',
					jdata_json->'s',
					solarcommon.json_array_to_text_array(jdata_json->'t'),
					p_i, p_a, p_s)
	INTO p_i, p_a, p_s, is_insert;

	IF track THEN
		-- add to audit datum in count
		PERFORM solardatm.audit_increment_datum_count(sid, ts_recv, 1,
			solardatm.json_datum_prop_count(jdata_json), is_insert);

		-- add stale aggregate hour(s)
		INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
		SELECT stream_id, ts_start, 'h' AS agg_kind
		FROM solardatm.calc_stale_datm(sid, ts_crea)
		ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
	END IF;

	RETURN sid;
END;
$$;
