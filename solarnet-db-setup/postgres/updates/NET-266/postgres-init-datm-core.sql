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
CREATE TYPE solardatm.da_datm_aux_type AS ENUM ('Reset');

CREATE TABLE solardatm.da_datm_aux (
	stream_id	UUID NOT NULL,
	ts			TIMESTAMP WITH TIME ZONE NOT NULL,
	atype 		solardatm.da_datm_aux_type NOT NULL DEFAULT 'Reset'::solardatm.da_datm_aux_type,
	updated 	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	notes 		TEXT,
	jdata_af 	JSONB,
	jdata_as 	JSONB,
	jmeta 		JSONB,
	CONSTRAINT da_datm_aux_pkey PRIMARY KEY (stream_id, ts, atype)
);

/*
	================================================================================================
	Aggregate datm tables
	================================================================================================
*/

-- type to use in aggregate functions regardless of physical table
CREATE TYPE solardatm.agg_datm AS (
	stream_id	UUID,
	ts_start	TIMESTAMP WITH TIME ZONE,
	data_i		NUMERIC[],
	data_a		NUMERIC[],
	data_s		TEXT[],
	data_t		TEXT[],
	stat_i		NUMERIC[][],
	read_a		NUMERIC[][]
);

-- type to use in aggregate functions to help interpolate datum at point in time
CREATE TYPE solardatm.agg_datm_at AS (
	-- array of datum; normally we expect 2 values to interpolate between
	datms 		solardatm.da_datm[],
	ts_at 		TIMESTAMP WITH TIME ZONE
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

-- "stale" aggregate queue table
CREATE TABLE solardatm.agg_stale_datm (
	stream_id	UUID NOT NULL,
	ts_start	TIMESTAMP WITH TIME ZONE NOT NULL,
	agg_kind 	CHARACTER NOT NULL,
	created 	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT agg_stale_datm_pkey PRIMARY KEY (agg_kind, ts_start, stream_id)
);

/**
 * Holds records for stale aggregate SolarFlux publishing support. There is no time component to
 * this table because SolarFlux cares only about the "most recent" value. Thus a record in this
 * table means the "most recent" data for the associated stream + agg_kind needs to be published
 * to SolarFlux.
 *
 * This table serves as a queue for updates. Any number of workers are expected to read from this
 * table and publish the updated data to SolarFlux.
 */
CREATE TABLE solardatm.agg_stale_flux (
	stream_id	UUID NOT NULL,
	agg_kind  	CHARACTER NOT NULL,
	created   	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT agg_stale_flux_pkey PRIMARY KEY (agg_kind, stream_id)
);

/*
	================================================================================================
	Audit statistics tables (to support fast queries for counts of data)
	================================================================================================
*/

-- audit hourly data
CREATE TABLE solardatm.aud_datm_hourly (
	stream_id				UUID NOT NULL,
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
	prop_count 				INTEGER NOT NULL DEFAULT 0,
	datum_q_count 			INTEGER NOT NULL DEFAULT 0,
	datum_count 			INTEGER NOT NULL DEFAULT 0,
	CONSTRAINT aud_datm_hourly_pkey PRIMARY KEY (stream_id, ts_start)
);

-- audit daily data
CREATE TABLE solardatm.aud_datm_daily (
	stream_id				UUID NOT NULL,
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
    prop_count 				BIGINT NOT NULL DEFAULT 0,
    datum_q_count 			BIGINT NOT NULL DEFAULT 0,
	datum_count 			INTEGER NOT NULL DEFAULT 0,
	datum_hourly_count 		SMALLINT NOT NULL DEFAULT 0,
	datum_daily_pres 		BOOLEAN NOT NULL DEFAULT FALSE,
	processed_count 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	processed_hourly_count 	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	processed_io_count 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT aud_datm_daily_pkey PRIMARY KEY (stream_id, ts_start)
);

-- audit monthly data
CREATE TABLE solardatm.aud_datm_monthly (
	stream_id				UUID NOT NULL,
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
    prop_count 				BIGINT NOT NULL DEFAULT 0,
    datum_q_count 			BIGINT NOT NULL DEFAULT 0,
	datum_count 			INTEGER NOT NULL DEFAULT 0,
	datum_hourly_count 		SMALLINT NOT NULL DEFAULT 0,
	datum_daily_count 		SMALLINT NOT NULL DEFAULT 0,
	datum_monthly_pres 		BOOLEAN NOT NULL DEFAULT FALSE,
	processed 				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT aud_datm_monthly_pkey PRIMARY KEY (stream_id, ts_start)
);

-- track total accumulated counts per day
CREATE TABLE solardatm.aud_acc_datm_daily (
	stream_id				UUID NOT NULL,
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
	datum_count 			INTEGER NOT NULL DEFAULT 0,
	datum_hourly_count 		INTEGER NOT NULL DEFAULT 0,
	datum_daily_count 		INTEGER NOT NULL DEFAULT 0,
	datum_monthly_count 	INTEGER NOT NULL DEFAULT 0,
	processed 				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT aud_acc_datm_daily_pkey PRIMARY KEY (stream_id, ts_start)
);

-- "stale" audit queue table
CREATE TABLE solardatm.aud_stale_datm (
	stream_id				UUID NOT NULL,
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
	aud_kind 				CHARACTER NOT NULL,
	created 				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT aud_stale_datm_pkey PRIMARY KEY (aud_kind, ts_start, stream_id)
);

/*
	================================================================================================
	General datm supporting functions
	================================================================================================
*/

/**
 * Count the properties in a datum JSON object.
 *
 * @param jdata the datum JSON, e.g. `{"i":{"a":1},"a":{"b":2},"s":{"c":3},"t":["d"]}`
 *
 * @returns the sum of the count of `i`, `a`, `s` object keys and the number of elements in the
 *          `t` array
 */
CREATE OR REPLACE FUNCTION solardatm.json_datum_prop_count(jdata jsonb)
  RETURNS INTEGER LANGUAGE SQL IMMUTABLE AS
$$
	SELECT count(*)::INTEGER FROM (
		SELECT jsonb_object_keys(jdata->'i')
		UNION ALL
		SELECT jsonb_object_keys(jdata->'a')
		UNION ALL
		SELECT jsonb_object_keys(jdata->'s')
		UNION ALL
		SELECT unnest(solarcommon.json_array_to_text_array(jdata->'t'))
	) p
$$;


/**
 * Get the metadata associated with a node datum or location datum stream.
 *
 * The `kind` output column will be `n` if the found metadata is for a node datum stream,
 * or `l` for a location datum stream, by looking in the `da_datum_meta` and `da_loc_datum_meta`
 * tables for a matching stream ID. If the stream ID is found in both tables, the node metadata
 * will be returned.
 *
 * The `time_zone` output column will be the time zone associated with the location of the stream,
 * either via the location of the node for a node stream or the location itself for a location
 * stream. If no time zone is available, it will be returned as `UTC`.
 *
 * @param sid the stream ID to find metadata for
 */
CREATE OR REPLACE FUNCTION solardatm.find_metadata_for_stream(
		sid 		UUID
	) RETURNS TABLE(
		stream_id 	UUID,
		obj_id		BIGINT,
		source_id	CHARACTER VARYING(64),
		created		TIMESTAMP WITH TIME ZONE,
		updated		TIMESTAMP WITH TIME ZONE,
		names_i		TEXT[],
		names_a		TEXT[],
		names_s		TEXT[],
		jdata		JSONB,
		kind		CHARACTER,
		time_zone	CHARACTER VARYING(64)
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT * FROM (
		SELECT m.stream_id, m.node_id AS obj_id, m.source_id, m.created, m.updated
			, m.names_i, m.names_a, m.names_s, m.jdata, 'n' AS kind
			, COALESCE(l.time_zone, 'UTC') AS time_zone
		FROM solardatm.da_datm_meta m
		LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = m.node_id
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE stream_id = sid
		UNION ALL
		SELECT m.stream_id, m.loc_id AS obj_id, m.source_id, m.created, m.updated
			, m.names_i, m.names_a, m.names_s, m.jdata, 'l' AS kind
			, COALESCE(l.time_zone, 'UTC') AS time_zone
		FROM solardatm.da_loc_datm_meta m
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = m.loc_id
		WHERE stream_id = sid
	) m
	LIMIT 1
$$;


/**
 * Return a valid minute-level time slot seconds value for an arbitrary input value.
 *
 * This function is meant to validate and correct inappropriate input for minute level
 * time slot second values, which must be between 60 and 1800 and evenly divide into 1800.
 *
 * @param secs the seconds to validate
 * @returns integer seconds value, possibly different than the input seconds value
 */
CREATE OR REPLACE FUNCTION solardatm.slot_seconds(secs INTEGER DEFAULT 600)
	RETURNS INTEGER LANGUAGE SQL IMMUTABLE AS
$$
	SELECT CASE
		WHEN secs < 60 OR secs > 1800 OR 1800 % secs <> 0 THEN 600
	ELSE
		secs
	END
$$;


/**
 * Return a normalized minute time-slot timestamp for a given timestamp and slot interval.
 *
 * This function returns the appropriate minute-level time aggregate `ts_start` value for a given
 * timestamp. For example passing <b>600</b> for `secs` will return a timestamp who is truncated to
 * `:00`,`:10`, `:20`, `:30`, `:40`, or `:50`.
 *
 * @param ts the timestamp to normalize
 * @param secs the slot seconds
 * @returns normalized timestamp
 */
CREATE OR REPLACE FUNCTION solardatm.minute_time_slot(ts TIMESTAMP WITH TIME ZONE, secs INTEGER DEFAULT 600)
  RETURNS TIMESTAMP WITH TIME ZONE LANGUAGE SQL IMMUTABLE AS
$$
	SELECT date_trunc('hour', ts) + (
		ceil(extract('epoch' from ts) - extract('epoch' from date_trunc('hour', ts)))
		- ceil(extract('epoch' from ts))::bigint % secs
	) * interval '1 second'
$$;


/**
 * Find hours with datum data in a stream.
 *
 * This function can be used to find hour time slots where aggregate data can be computed from.
 * It returns only whole-hour rows where datm data actually exists.
 *
 * @param sid 				the stream ID to find hours for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_hours(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		stream_id 		UUID,
		ts_start		TIMESTAMP WITH TIME ZONE
	) LANGUAGE SQL STABLE ROWS 2000 AS
$$
	SELECT sid, date_trunc('hour', ts) AS ts_start
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts >= start_ts
		AND ts < end_ts
	GROUP BY date_trunc('hour', ts)
$$;


/**
 * Find the datum that exist immediately before and after a point in time for a stream.
 *
 * If a datum exists exactly at the given timestamp, that datum alone will be returned.
 * Otherwise up to two datum will be returned, one immediately before and one immediately after
 * the given timestamp.
 *
 * @param sid 				the stream ID of the datum that has been changed (inserted, deleted)
 * @param ts_at				the date of the datum to find adjacent datm for
 * @param tolerance 		the maximum time to look forward/backward for adjacent datm
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_around(
		sid 		UUID,
		ts_at 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT interval '1 months'
	) RETURNS SETOF solardatm.da_datm LANGUAGE SQL STABLE ROWS 2 AS
$$
	WITH b AS (
		-- exact
		(
			SELECT d.*, 0 AS rtype
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts = ts_at
		)
		UNION ALL
		-- prev
		(
			SELECT d.*, 1 AS rtype
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts < ts_at
				AND d.ts > ts_at - tolerance
			ORDER BY d.stream_id, d.ts DESC
			LIMIT 1
		)
		UNION ALL
		-- next
		(
			SELECT d.*, 1 AS rtype
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts > ts_at
				AND d.ts < ts_at + tolerance
			ORDER BY d.stream_id, d.ts
			LIMIT 1
		)
	)
	, d AS (
		-- choose exact if available, fall back to before/after otherwise
		SELECT b.*
			, CASE
				WHEN rtype = 0 THEN TRUE
				WHEN rtype = 1 AND rank() OVER (ORDER BY rtype) = 1 THEN TRUE
				ELSE FALSE
				END AS inc
		FROM b
	)
	SELECT stream_id, ts, received, data_i, data_a, data_s, data_t
	FROM d
	WHERE inc
$$;
