--DROP SCHEMA IF EXISTS solardatm CASCADE;
CREATE SCHEMA IF NOT EXISTS solardatm;

-- datum stream indirection table
CREATE TABLE solardatm.da_datm_meta (
	stream_id	UUID NOT NULL DEFAULT uuid_generate_v4(),
	node_id		BIGINT NOT NULL,
	source_id	CHARACTER VARYING(64) NOT NULL,
	created		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	names_i		TEXT[],
	names_a		TEXT[],
	names_s		TEXT[],
	jdata		JSONB,
	CONSTRAINT da_datm_meta_pkey PRIMARY KEY (stream_id),
	CONSTRAINT da_datm_meta_unq UNIQUE (node_id, source_id)
);

-- location datum stream indirection table
CREATE TABLE solardatm.da_loc_datm_meta (
	stream_id	UUID NOT NULL DEFAULT uuid_generate_v4(),
	loc_id		BIGINT NOT NULL,
	source_id	CHARACTER VARYING(64) NOT NULL,
	created		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	names_i		TEXT[],
	names_a		TEXT[],
	names_s		TEXT[],
	jdata		JSONB,
	CONSTRAINT da_loc_datm_meta_pkey PRIMARY KEY (stream_id),
	CONSTRAINT da_loc_datm_meta_unq UNIQUE (loc_id, source_id)
);

-- datum table
CREATE TABLE solardatm.da_datm (
	stream_id	UUID NOT NULL,
	ts			TIMESTAMP WITH TIME ZONE NOT NULL,
	received	TIMESTAMP WITH TIME ZONE NOT NULL,
	data_i		NUMERIC[],
	data_a		NUMERIC[],
	data_s		TEXT[],
	data_t		TEXT[],
	CONSTRAINT da_datm_pkey PRIMARY KEY (stream_id, ts)
);

CREATE UNIQUE INDEX IF NOT EXISTS da_datm_unq_reverse ON solardatm.da_datm (stream_id, ts DESC);

-- datum aux table
CREATE TABLE solardatm.da_datm_aux (
	stream_id	UUID NOT NULL,
	ts			TIMESTAMP WITH TIME ZONE NOT NULL,
	atype 		solardatum.da_datum_aux_type NOT NULL DEFAULT 'Reset'::solardatum.da_datum_aux_type,
	updated 	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	notes 		TEXT,
	jdata_af 	JSONB,
	jdata_as 	JSONB,
	jmeta 		JSONB,
	CONSTRAINT da_datm_aux_pkey PRIMARY KEY (stream_id, ts, atype)
);

-- agg hourly datum table
CREATE TABLE solardatm.agg_datm_hourly (
	stream_id	UUID NOT NULL,
	ts_start	TIMESTAMP WITH TIME ZONE NOT NULL,
	data_i		NUMERIC[],
	data_a		NUMERIC[],
	data_s		TEXT[],
	data_t		TEXT[],
	stat_i		NUMERIC[][],
	read_a		NUMERIC[][],
	CONSTRAINT agg_datm_hourly_pkey PRIMARY KEY (stream_id, ts_start)
);

-- agg daily datum table
CREATE TABLE solardatm.agg_datm_daily (
	stream_id	UUID NOT NULL,
	ts_start	TIMESTAMP WITH TIME ZONE NOT NULL,
	data_i		NUMERIC[],
	data_a		NUMERIC[],
	data_s		TEXT[],
	data_t		TEXT[],
	stat_i		NUMERIC[][],
	read_a		NUMERIC[][],
	CONSTRAINT agg_datm_daily_pkey PRIMARY KEY (stream_id, ts_start)
);

-- agg monthly datum table
CREATE TABLE solardatm.agg_datm_monthly (
	stream_id	UUID NOT NULL,
	ts_start	TIMESTAMP WITH TIME ZONE NOT NULL,
	data_i		NUMERIC[],
	data_a		NUMERIC[],
	data_s		TEXT[],
	data_t		TEXT[],
	stat_i		NUMERIC[][],
	read_a		NUMERIC[][],
	CONSTRAINT agg_datm_monthly_pkey PRIMARY KEY (stream_id, ts_start)
);

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
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_crea 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(ddate, now());
	ts_recv 			TIMESTAMP WITH TIME ZONE	:= COALESCE(rdate, now());
	jdata_json 			JSONB 						:= jdata::jsonb;
	jdata_prop_count 	INTEGER 					:= solardatum.datum_prop_count(jdata_json);
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

	SELECT * FROM solardatm.migrate_datum_json(sid, ts_crea, src, ts_recv,
					jdata_json->'i',
					jdata_json->'a',
					jdata_json->'s',
					solarcommon.json_array_to_text_array(jdata_json->'t'),
					p_i, p_a, p_s)
	INTO p_i, p_a, p_s, is_insert;

	/*
	INSERT INTO solaragg.aud_datum_hourly (
		ts_start, node_id, source_id, datum_count, prop_count)
	VALUES (ts_post_hour, node, src, 1, jdata_prop_count)
	ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
	SET datum_count = aud_datum_hourly.datum_count + (CASE is_insert WHEN TRUE THEN 1 ELSE 0 END),
		prop_count = aud_datum_hourly.prop_count + EXCLUDED.prop_count;

	IF track THEN
		INSERT INTO solaragg.agg_stale_datum (agg_kind, node_id, ts_start, source_id)
		SELECT 'h' AS agg_kind, node_id, ts_start, source_id
		FROM solardatum.calculate_stale_datum(node, src, cdate)
		ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING;

		IF is_insert THEN
			PERFORM solardatum.update_datum_range_dates(node, src, cdate);
		END IF;
	END IF;
	*/
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
 * @param track if `TRUE` then also insert results of `solardatum.calculate_stale_datum()`
 *                     into the `solaragg.agg_stale_datum` table and call
 *                     `solardatum.update_datum_range_dates()` to keep the
 *                     `solardatum.da_datum_range` table up-to-date
 */
CREATE OR REPLACE FUNCTION solardatm.store_loc_datum(
	ddate 			TIMESTAMP WITH TIME ZONE,
	loc 			BIGINT,
	src 			TEXT,
	rdate 			TIMESTAMP WITH TIME ZONE,
	jdata 			TEXT,
	track 			BOOLEAN DEFAULT TRUE)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_crea 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(ddate, now());
	ts_recv 			TIMESTAMP WITH TIME ZONE	:= COALESCE(rdate, now());
	jdata_json 			JSONB 						:= jdata::jsonb;
	jdata_prop_count 	INTEGER 					:= solardatum.datum_prop_count(jdata_json);
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

	SELECT * FROM solardatm.migrate_loc_datum_json(sid, ts_crea, src, ts_recv,
					jdata_json->'i',
					jdata_json->'a',
					jdata_json->'s',
					solarcommon.json_array_to_text_array(jdata_json->'t'),
					p_i, p_a, p_s)
	INTO p_i, p_a, p_s, is_insert;

	/*
	INSERT INTO solaragg.aud_datum_hourly (
		ts_start, loc_id, source_id, datum_count, prop_count)
	VALUES (ts_post_hour, loc, src, 1, jdata_prop_count)
	ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
	SET datum_count = aud_datum_hourly.datum_count + (CASE is_insert WHEN TRUE THEN 1 ELSE 0 END),
		prop_count = aud_datum_hourly.prop_count + EXCLUDED.prop_count;

	IF track THEN
		INSERT INTO solaragg.agg_stale_datum (agg_kind, loc_id, ts_start, source_id)
		SELECT 'h' AS agg_kind, loc_id, ts_start, source_id
		FROM solardatum.calculate_stale_datum(loc, src, cdate)
		ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;

		IF is_insert THEN
			PERFORM solardatum.update_datum_range_dates(loc, src, cdate);
		END IF;
	END IF;
	*/
END;
$$;
