/**
 * Calculate "stale datum" rows for a given datum primary key that has changed.
 *
 * This function will return 1-3 rows representing stale rows that must be re-calculated.
 * It is designed so that the results can be inserted into `solardatm.agg_stale_datm`, like:
 *
 * 	INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
 * 	SELECT stream_id, ts_start, 'h' AS agg_kind
 * 	FROM solardatm.calculate_stale_datm('11111'::uuid,'2020-10-07 08:00:40+13'::timestamptz)
 * 	ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
 *
 * @param sid 				the stream ID of the datum that has been changed (inserted, deleted)
 * @param ts_in				the date of the datum that has changed
 * @param tolerance 		the maximum time to look forward/backward for adjacent datm
 */
CREATE OR REPLACE FUNCTION solardatm.calculate_stale_datm(
		sid 		UUID,
		ts_in 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT interval '3 months'
	) RETURNS TABLE (
		stream_id	UUID,
		ts_start 	TIMESTAMP WITH TIME ZONE
	) LANGUAGE SQL STABLE ROWS 3 AS
$$
	WITH b AS (
		-- curr hour
		(
			SELECT date_trunc('hour', ts_in) AS ts
		)
		UNION ALL
		-- prev hour
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
		-- next hour
		(
			SELECT date_trunc('hour', d.ts) AS ts
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


/**
 * Store JSON datum values into the `solardatm.da_datm` table.
 *
 * This function accepts the "current" property name arrays as an optimisation when migrating
 * multiple rows of data. If the given datum has properties that are not found in any of the
 * given name arrays, the name will be added and then the associated `solardatm.da_datm_meta`
 * column updated to match. The name arrays are declared as `INOUT` so that the updated values
 * can be saved and used again, helping reduce the amount of updates needed to the
 * `solardatm.da_datm_meta` table.
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
			v_i[idx] := NULL;
		END;
	END LOOP;

	-- copy status props
	FOR p IN SELECT * FROM jsonb_each_text(jdata_s) LOOP
		idx := COALESCE(array_position(p_s, p.key), 0);
		IF idx < 1 THEN
			UPDATE solardatm.da_datm_meta SET names_s = CASE
					WHEN COALESCE(array_position(names_s, p.key), 0) < 1 THEN array_append(names_s, p.key)
					ELSE names_i
					END
			WHERE stream_id = sid
			RETURNING names_s INTO p_s;
			idx := array_position(p_s, p.key);
		END IF;
		v_s[idx] := p.value;
	END LOOP;

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
 * Add or update a datum record. The data is stored in the `solardatm.da_datm` table.
 *
 * @param ddate the datum timestamp
 * @param node 	the node ID
 * @param src 	the source ID
 * @param rdate the date the datum was received by SolarNetwork
 * @param jdata the datum JSON object (with jdata_i, jdata_a, jdata_s, and jdata_t properties)
 * @param track if `TRUE` then also insert results of `solardatum.calculate_stale_datum()`
 *                     into the `solaragg.agg_stale_datum` table and call
 *                     `solardatum.update_datum_range_dates()` to keep the
 *                     `solardatum.da_datum_range` table up-to-date
 */
CREATE OR REPLACE FUNCTION solardatm.store_datum(
	ddate 			TIMESTAMP WITH TIME ZONE,
	node 			BIGINT,
	src 			TEXT,
	rdate 			TIMESTAMP WITH TIME ZONE,
	jdata 			TEXT,
	track 			BOOLEAN DEFAULT TRUE)
  RETURNS TEXT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_crea 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(ddate, now());
	ts_recv 			TIMESTAMP WITH TIME ZONE	:= COALESCE(rdate, now());
	jdata_json 			JSONB 						:= jdata::jsonb;
	jdata_prop_count 	INTEGER 					:= solardatm.json_datum_prop_count(jdata_json);
	ts_recv_hour 		TIMESTAMP WITH TIME ZONE 	:= date_trunc('hour', ts_recv);
	is_insert 			BOOLEAN 					:= false;

	sid 	UUID;

	-- property name arrays
	p_i		TEXT[];
	p_a		TEXT[];
	p_s		TEXT[];
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

	SELECT * FROM solardatm.store_datum(sid, ts_crea, src, ts_recv,
					jdata_json->'i',
					jdata_json->'a',
					jdata_json->'s',
					solarcommon.json_array_to_text_array(jdata_json->'t'),
					p_i, p_a, p_s)
	INTO p_i, p_a, p_s, is_insert;

	INSERT INTO solardatm.aud_datm_hourly (stream_id, ts_start, datum_count, prop_count)
	VALUES (sid, ts_recv_hour, 1, jdata_prop_count)
	ON CONFLICT (stream_id, ts_start) DO UPDATE
	SET datum_count = aud_datm_hourly.datum_count + (CASE is_insert WHEN TRUE THEN 1 ELSE 0 END),
		prop_count = aud_datm_hourly.prop_count + EXCLUDED.prop_count;

	IF track THEN
		INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
		SELECT stream_id, ts_start, 'h' AS agg_kind
		FROM solardatm.calculate_stale_datm(sid, ddate)
		ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;

		--IF is_insert THEN
		--	PERFORM solardatm.update_datm_range_dates(sid, ddate);
		--END IF;
	END IF;

	RETURN sid::text;
END;
$$;


/**
 * Find hours with datum data over a time range in streams and mark them as "stale" for aggregate
 * processing.
 *
 * This function will insert into the `solardatm.agg_stale_datm` table records for all hours
 * of available data matching the given search criteria.
 *
 * @param sid 				the stream IDs to find hours for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 *
 * @see solardatm.find_datm_hours()
 */
CREATE OR REPLACE FUNCTION solardatm.mark_stale_datm_hours(
		sid 			UUID[],
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE
	) RETURNS VOID LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
	SELECT d.stream_id, d.ts_start, 'h'
	FROM unnest(sid) AS ids(stream_id)
	INNER JOIN solardatm.find_datm_hours(ids.stream_id, start_ts, end_ts) d ON d.stream_id = ids.stream_id
	ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING
$$;


/**
 * Add or replace datum auxiliary record data.
 *
 * @param ddate 		the datum timestamp
 * @param node 			the node ID
 * @param src 			the source ID
 * @param aux_type		the auxiliary type; must cast to solardatm.da_datm_aux_type, e.g. 'Reset'
 * @param aux_notes 	optional text notes
 * @param jdata_final 	the ending JSON datum object; only 'a' values are supported
 * @param jdata_start 	the starting JSON datum object; only 'a' values are supported
 * @param jmeta			optional JSON metadata
 */
CREATE OR REPLACE FUNCTION solardatm.store_datum_aux(
		ddate 			TIMESTAMP WITH TIME ZONE,
		node 			BIGINT,
		src 			CHARACTER VARYING(64),
		aux_type 		text,
		aux_notes 		text,
		jdata_final 	text,
		jdata_start 	text,
		jmeta 			text
	) RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	sid 	UUID;
BEGIN
	INSERT INTO solardatm.da_datm_aux(stream_id, ts, atype, updated, notes, jdata_af, jdata_as, jmeta)
	SELECT m.stream_id, ddate, aux_type::solardatm.da_datm_aux_type, CURRENT_TIMESTAMP, aux_notes,
		(jdata_final::jsonb)->'a', (jdata_start::jsonb)->'a', jmeta::jsonb
	FROM solardatm.da_datm_meta m
	WHERE m.node_id = node AND m.source_id = src
	ON CONFLICT (stream_id, ts, atype) DO UPDATE
		SET notes = EXCLUDED.notes,
			jdata_af = EXCLUDED.jdata_af,
			jdata_as = EXCLUDED.jdata_as,
			jmeta = EXCLUDED.jmeta,
			updated = EXCLUDED.updated
	RETURNING stream_id
	INTO sid;

	INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
	SELECT stream_id, ts_start, 'h' AS agg_kind
	FROM solardatm.calculate_stale_datm(sid, ddate)
	ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
END;
$$;


/**
 * Update and move datum auxiliary record data.
 *
 * @param ddate_from 	the datum timestamp of the "old" record
 * @param node_from 	the node ID of the "old" record
 * @param src_from 		the source ID of the "old" record
 * @param aux_type_from	the auxiliary type of the "old" record; must cast to solardatm.da_datm_aux_type
 * @param ddate 		the datum timestamp
 * @param node 			the node ID
 * @param src 			the source ID
 * @param aux_type		the auxiliary type; must cast to solardatm.da_datm_aux_type, e.g. 'Reset'
 * @param aux_notes 	optional text notes
 * @param jdata_final 	the ending JSON datum object; only 'a' values are supported
 * @param jdata_start 	the starting JSON datum object; only 'a' values are supported
 * @param jmeta			optional JSON metadata
 */
CREATE OR REPLACE FUNCTION solardatm.move_datum_aux(
		ddate_from		TIMESTAMP WITH TIME ZONE,
		node_from		BIGINT,
		src_from		CHARACTER VARYING(64),
		aux_type_from	text,

		ddate 			TIMESTAMP WITH TIME ZONE,
		node 			BIGINT,
		src 			CHARACTER VARYING(64),
		aux_type 		text,

		aux_notes 		text,
		jdata_final 	text,
		jdata_start 	text,
		jmeta 			text
	) RETURNS BOOLEAN LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	sid 		UUID;
	del_count 	INTEGER := 0;
BEGIN
	DELETE FROM solardatm.da_datm_aux
	USING solardatm.da_datm_meta
	WHERE da_datm_aux.stream_id = da_datm_meta.stream_id
		AND da_datm_aux.ts = ddate_from
		AND da_datm_meta.node_id = node_from
		AND da_datm_meta.source_id = src_from
		AND da_datm_aux.atype = aux_type_from::solardatm.da_datm_aux_type
	RETURNING da_datm_aux.stream_id
	INTO sid;

	GET DIAGNOSTICS del_count = ROW_COUNT;

	IF del_count > 0 THEN
		-- insert stale record for deleted row
		INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
		SELECT stream_id, ts_start, 'h' AS agg_kind
		FROM solardatm.calculate_stale_datm(sid, ddate_from)
		ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;

		-- insert new row
		PERFORM solardatm.store_datum_aux(ddate, node, src, aux_type, aux_notes, jdata_final, jdata_start, jmeta);
	END IF;

	RETURN (del_count > 0);
END;
$$;
