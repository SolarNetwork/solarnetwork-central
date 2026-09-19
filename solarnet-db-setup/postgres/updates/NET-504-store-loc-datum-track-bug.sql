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
	ts_recv_hour 		TIMESTAMP WITH TIME ZONE 	:= date_trunc('hour', ts_recv);
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
		FROM solardatm.calc_stale_datm(sid, ddate)
		ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
	END IF;

	RETURN sid;
END;
$$;

CREATE OR REPLACE FUNCTION solardatm.store_datum(
	ddate 			TIMESTAMP WITH TIME ZONE,
	node 			BIGINT,
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
END
$$;

CREATE OR REPLACE FUNCTION solardatm.store_stream_datum(
	sid				UUID,
	ddate 			TIMESTAMP WITH TIME ZONE,
	rdate 			TIMESTAMP WITH TIME ZONE,
	idata 			NUMERIC[],
	adata			NUMERIC[],
	sdata			TEXT[],
	tdata			TEXT[],
	track 			BOOLEAN DEFAULT TRUE)
  RETURNS VOID LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_crea 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(ddate, now());
	ts_recv 			TIMESTAMP WITH TIME ZONE	:= COALESCE(rdate, now());
	is_insert 			BOOLEAN 					:= false;
	prop_count			INTEGER						:= 0;
BEGIN
	INSERT INTO solardatm.da_datm (stream_id, ts, received, data_i, data_a, data_s, data_t)
	VALUES (
		sid
		, ts_crea
		, ts_recv
		, CASE WHEN COALESCE(array_length(idata, 1), 0) < 1 THEN NULL ELSE idata END
		, CASE WHEN COALESCE(array_length(adata, 1), 0) < 1 THEN NULL ELSE adata END
		, CASE WHEN COALESCE(array_length(sdata, 1), 0) < 1 THEN NULL ELSE sdata END
		, CASE WHEN COALESCE(array_length(tdata, 1), 0) < 1 THEN NULL ELSE tdata END
	)
	ON CONFLICT (stream_id, ts) DO UPDATE
	SET received = EXCLUDED.received,
		data_i = EXCLUDED.data_i,
		data_a = EXCLUDED.data_a,
		data_s = EXCLUDED.data_s,
		data_t = EXCLUDED.data_t
	RETURNING (xmax = 0)
	INTO is_insert;

	IF track THEN
		-- get count of non-null properties
		SELECT n.cnt + t.cnt FROM
		(
			SELECT count(*) AS cnt FROM (
				SELECT v FROM unnest(idata) v
				UNION ALL
				SELECT v FROM unnest(adata) v
			) n
			WHERE v IS NOT NULL
		) n,
		(
			SELECT count(*) AS cnt FROM (
				SELECT v from unnest(sdata) v
				UNION ALL
				SELECT v from unnest(tdata) v
			) t
			WHERE v IS NOT NULL
		) t
		INTO prop_count;

		-- add to audit datum in count
		PERFORM solardatm.audit_increment_datum_count(sid, ts_recv, 1, prop_count, is_insert);

		-- add stale aggregate hour(s)
		INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
		SELECT stream_id, ts_start, 'h' AS agg_kind
		FROM solardatm.calc_stale_datm(sid, ts_crea)
		ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
	END IF;
END
$$;
