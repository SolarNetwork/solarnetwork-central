CREATE SCHEMA IF NOT EXISTS solardatum;

CREATE SCHEMA IF NOT EXISTS solaragg;

CREATE TABLE solardatum.da_datum (
  ts timestamp with time zone NOT NULL,
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  posted timestamp with time zone NOT NULL,
  jdata_i jsonb,
  jdata_a jsonb,
  jdata_s jsonb,
  jdata_t text[],
  CONSTRAINT da_datum_pkey PRIMARY KEY (node_id, ts, source_id)
);

CREATE OR REPLACE FUNCTION solardatum.jdata_from_datum(datum solardatum.da_datum)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarcommon.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE VIEW solardatum.da_datum_data AS
	SELECT
		d.ts,
		d.node_id,
		d.source_id,
		d.posted,
		solardatum.jdata_from_datum(d.*) AS jdata,
		d.ts AT TIME ZONE COALESCE(l.time_zone, 'UTC') AS local_date
	FROM solardatum.da_datum d
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = d.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id;

CREATE TABLE solardatum.da_meta (
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  created timestamp with time zone NOT NULL,
  updated timestamp with time zone NOT NULL,
  jdata jsonb NOT NULL,
  CONSTRAINT da_meta_pkey PRIMARY KEY (node_id, source_id)
);


CREATE TYPE solardatum.da_datum_aux_type AS ENUM ('Reset');

/**************************************************************************************************
 * TABLE solardatum.da_datum_aux
 *
 * Holds auxiliary records for datum records, where final/start data is inserted into the data stream.
 * Thus each row contains essentially two datum records. These auxiliary records serve to 
 * help transition the data stream in some way.
 *
 * atype - the auxiliary record type
 * jdata_af - the accumulating data "final" value
 * jdata_as - the accumulating data "start" value
 */
CREATE TABLE solardatum.da_datum_aux (
  ts timestamp with time zone NOT NULL,
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  atype solardatum.da_datum_aux_type NOT NULL DEFAULT 'Reset'::solardatum.da_datum_aux_type,
  updated timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  notes text,
  jdata_af jsonb,
  jdata_as jsonb,
  CONSTRAINT da_datum_aux_pkey PRIMARY KEY (node_id, ts, source_id, atype)
);

/**
 * FUNCTION solardatum.store_aux(timestamp with time zone,bigint,character varying,solardatum.da_datum_aux_type,text,text,text)
 *
 * Add or replace datum auxiliary record data.
 */
CREATE OR REPLACE FUNCTION solardatum.store_datum_aux(
	cdate timestamp with time zone,
	node bigint,
	src character varying(64),
	aux_type solardatum.da_datum_aux_type,
	aux_notes text,
	jdata_final text,
	jdata_start text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	udate timestamp with time zone := now();
	jdata_final_json json := jdata_final::jsonb;
	jdata_start_json json := jdata_start::jsonb;
BEGIN
	INSERT INTO solardatum.da_datum_aux(ts, node_id, source_id, atype, updated, notes, jdata_af, jdata_as)
	VALUES (cdate, node, src, aux_type, udate, aux_notes, jdata_final_json->'a', jdata_start_json->'a')
	ON CONFLICT (ts, node_id, source_id, atype) DO UPDATE
	SET notes = EXCLUDED.notes,
		jdata_af = EXCLUDED.jdata_af,
		jdata_as = EXCLUDED.jdata_as, 
		updated = EXCLUDED.updated;
END;
$$;

/**
 * FUNCTION solardatum.jdata_from_datum_aux_final(datum solardatum.da_datum_aux)
 *
 * Get a full jdata object from the "final" data elements of a datum auxiliary record.
 */
CREATE OR REPLACE FUNCTION solardatum.jdata_from_datum_aux_final(datum solardatum.da_datum_aux)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarcommon.jdata_from_components(NULL, datum.jdata_af, NULL, ARRAY[datum.atype::text, 'final']);
$$;


/**
 * FUNCTION solardatum.jdata_from_datum_aux_start(datum solardatum.da_datum_aux)
 *
 * Get a full jdata object from the "starting" data elements of a datum auxiliary record.
 */
CREATE OR REPLACE FUNCTION solardatum.jdata_from_datum_aux_start(datum solardatum.da_datum_aux)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarcommon.jdata_from_components(NULL, datum.jdata_as, NULL, ARRAY[datum.atype::text, 'start']);
$$;


CREATE TABLE solaragg.agg_stale_datum (
  ts_start timestamp with time zone NOT NULL,
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  agg_kind char(1) NOT NULL,
  created timestamp NOT NULL DEFAULT now(),
  CONSTRAINT agg_stale_datum_pkey PRIMARY KEY (agg_kind, ts_start, node_id, source_id)
);

CREATE TABLE solaragg.agg_messages (
  created timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  ts timestamp with time zone NOT NULL,
  msg text NOT NULL
);

CREATE INDEX agg_messages_ts_node_idx ON solaragg.agg_messages (ts, node_id);

/**
 * TABLE solaragg.agg_datum_hourly
 *
 * @param ts_start		the starting date of the hourly time slot
 * @param local_date		the `ts_start` in the node's local time zone
 * @param node_id		the node ID
 * @param source_id		the source ID
 * @param jdata_i		the instantaneous sample data, averaged for the time slot
 * @param jdata_a		the accumulating sample data, summed for the time slot
 * @param jdata_s		the status sample data; FILO values for the time slot
 * @param jdata_t		the tags seen over the time slot
 * @param jmeta			aggregate metadata used for aggregating higher levels
 * @param jdata_as		start reading values of the accumulating sample data in the time slot
 * @param jdata_af		final reading values of the accumulating sample data in the time slot
 * @param jdata_ad		difference reading values of the accumulating sample data in the time slot
 */
CREATE TABLE solaragg.agg_datum_hourly (
  ts_start timestamp with time zone NOT NULL,
  local_date timestamp without time zone NOT NULL,
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  jdata_i jsonb,
  jdata_a jsonb,
  jdata_s jsonb,
  jdata_t text[],
  jmeta jsonb,
  jdata_as jsonb,
  jdata_af jsonb,
  jdata_ad jsonb,
 CONSTRAINT agg_datum_hourly_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CREATE TABLE solaragg.aud_datum_hourly (
  ts_start timestamp with time zone NOT NULL,
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  datum_count integer NOT NULL DEFAULT 0,
  prop_count integer NOT NULL DEFAULT 0,
  datum_q_count integer NOT NULL DEFAULT 0,
  CONSTRAINT aud_datum_hourly_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CREATE TABLE solaragg.agg_datum_daily (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  jdata_i jsonb,
  jdata_a jsonb,
  jdata_s jsonb,
  jdata_t text[],
  jmeta jsonb,
  jdata_as jsonb,
  jdata_af jsonb,
  jdata_ad jsonb,
 CONSTRAINT agg_datum_daily_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

-- keep track of requests to calculate day/month audit counts, so parallel jobs can compute
CREATE TABLE solaragg.aud_datum_daily_stale (
	ts_start timestamp with time zone NOT NULL,
	node_id bigint NOT NULL,
	source_id character varying(64) NOT NULL,
	aud_kind char(1) NOT NULL,
	created timestamp NOT NULL DEFAULT now(),
	CONSTRAINT aud_datum_daily_stale_pkey PRIMARY KEY (aud_kind, ts_start, node_id, source_id)
);

-- hold the daily level datum audit data
CREATE TABLE solaragg.aud_datum_daily (
	ts_start timestamp with time zone NOT NULL,
	node_id bigint NOT NULL,
	source_id character varying(64) NOT NULL,
    prop_count bigint NOT NULL DEFAULT 0,
    datum_q_count bigint NOT NULL DEFAULT 0,
	datum_count integer NOT NULL DEFAULT 0,
	datum_hourly_count smallint NOT NULL DEFAULT 0,
	datum_daily_pres BOOLEAN NOT NULL DEFAULT FALSE,
	processed_count TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	processed_hourly_count TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	processed_io_count TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT aud_datum_daily_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

-- hold the monthly level datum audit data
CREATE TABLE solaragg.aud_datum_monthly (
	ts_start timestamp with time zone NOT NULL,
	node_id bigint NOT NULL,
	source_id character varying(64) NOT NULL,
    prop_count bigint NOT NULL DEFAULT 0,
    datum_q_count bigint NOT NULL DEFAULT 0,
	datum_count integer NOT NULL DEFAULT 0,
	datum_hourly_count smallint NOT NULL DEFAULT 0,
	datum_daily_count smallint NOT NULL DEFAULT 0,
	datum_monthly_pres boolean NOT NULL DEFAULT FALSE,
	processed TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT aud_datum_monthly_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CREATE TABLE solaragg.agg_datum_monthly (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  jdata_i jsonb,
  jdata_a jsonb,
  jdata_s jsonb,
  jdata_t text[],
  jmeta jsonb,
  jdata_as jsonb,
  jdata_af jsonb,
  jdata_ad jsonb,
 CONSTRAINT agg_datum_monthly_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

-- track total accumulated counts per day
CREATE TABLE solaragg.aud_acc_datum_daily (
	ts_start timestamp with time zone NOT NULL,
	node_id bigint NOT NULL,
	source_id character varying(64) NOT NULL,
	datum_count integer NOT NULL DEFAULT 0,
	datum_hourly_count integer NOT NULL DEFAULT 0,
	datum_daily_count integer NOT NULL DEFAULT 0,
	datum_monthly_count integer NOT NULL DEFAULT 0,
	processed TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT aud_acc_datum_daily_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CREATE OR REPLACE FUNCTION solaragg.find_audit_acc_datum_daily(node bigint, source text)
	RETURNS TABLE(
		ts_start timestamp with time zone,
		node_id bigint,
		source_id character varying(64),
		datum_count integer ,
		datum_hourly_count integer,
		datum_daily_count integer,
		datum_monthly_count integer
	) LANGUAGE SQL VOLATILE AS
$$
	WITH acc AS (
		SELECT
			sum(d.datum_count) AS datum_count,
			sum(d.datum_hourly_count) AS datum_hourly_count,
			sum(d.datum_daily_count) AS datum_daily_count,
			sum(CASE d.datum_monthly_pres WHEN TRUE THEN 1 ELSE 0 END) AS datum_monthly_count
		FROM solaragg.aud_datum_monthly d
		WHERE d.node_id = node
			AND d.source_id = source
	)
	SELECT
		date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE nlt.time_zone) AT TIME ZONE nlt.time_zone,
		node,
		source,
		acc.datum_count::integer,
		acc.datum_hourly_count::integer,
		acc.datum_daily_count::integer,
		acc.datum_monthly_count::integer
	FROM solarnet.node_local_time nlt
	CROSS JOIN acc
	WHERE nlt.node_id = node
$$;

CREATE OR REPLACE FUNCTION solaragg.populate_audit_acc_datum_daily(node bigint, source text)
	RETURNS void LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaragg.aud_acc_datum_daily (ts_start, node_id, source_id,
		datum_count, datum_hourly_count, datum_daily_count, datum_monthly_count)
	SELECT
		ts_start,
		node_id,
		source_id,
		COALESCE(datum_count, 0),
		COALESCE(datum_hourly_count, 0),
		COALESCE(datum_daily_count, 0),
		COALESCE(datum_monthly_count, 0)
	FROM solaragg.find_audit_acc_datum_daily(node, source)
	ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
	SET datum_count = EXCLUDED.datum_count,
		datum_hourly_count = EXCLUDED.datum_hourly_count,
		datum_daily_count = EXCLUDED.datum_daily_count,
		datum_monthly_count = EXCLUDED.datum_monthly_count,
		processed = CURRENT_TIMESTAMP;
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_datum_hourly)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarcommon.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_datum_daily)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarcommon.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_datum_monthly)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarcommon.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE VIEW solaragg.agg_datum_hourly_data AS
  SELECT d.ts_start, d.local_date, d.node_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
  FROM solaragg.agg_datum_hourly d;

CREATE VIEW solaragg.agg_datum_daily_data AS
  SELECT d.ts_start, d.local_date, d.node_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
  FROM solaragg.agg_datum_daily d;

CREATE VIEW solaragg.agg_datum_monthly_data AS
  SELECT d.ts_start, d.local_date, d.node_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
  FROM solaragg.agg_datum_monthly d;

CREATE VIEW solaragg.da_datum_avail_hourly AS
WITH nodetz AS (
	SELECT n.node_id, COALESCE(l.time_zone, 'UTC') AS tz
	FROM solarnet.sn_node n
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
)
SELECT date_trunc('hour', d.ts at time zone nodetz.tz) at time zone nodetz.tz AS ts_start, d.node_id, d.source_id
FROM solardatum.da_datum d
INNER JOIN nodetz ON nodetz.node_id = d.node_id
GROUP BY date_trunc('hour', d.ts at time zone nodetz.tz) at time zone nodetz.tz, d.node_id, d.source_id;

CREATE VIEW solaragg.da_datum_avail_daily AS
WITH nodetz AS (
	SELECT n.node_id, COALESCE(l.time_zone, 'UTC') AS tz
	FROM solarnet.sn_node n
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
)
SELECT date_trunc('day', d.ts at time zone nodetz.tz) at time zone nodetz.tz AS ts_start, d.node_id, d.source_id
FROM solardatum.da_datum d
INNER JOIN nodetz ON nodetz.node_id = d.node_id
GROUP BY date_trunc('day', d.ts at time zone nodetz.tz) at time zone nodetz.tz, d.node_id, d.source_id;

CREATE VIEW solaragg.da_datum_avail_monthly AS
WITH nodetz AS (
	SELECT n.node_id, COALESCE(l.time_zone, 'UTC') AS tz
	FROM solarnet.sn_node n
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
)
SELECT date_trunc('month', d.ts at time zone nodetz.tz) at time zone nodetz.tz AS ts_start, d.node_id, d.source_id
FROM solardatum.da_datum d
INNER JOIN nodetz ON nodetz.node_id = d.node_id
GROUP BY date_trunc('month', d.ts at time zone nodetz.tz) at time zone nodetz.tz, d.node_id, d.source_id;

CREATE OR REPLACE FUNCTION solardatum.store_meta(
	cdate timestamp with time zone,
	node bigint,
	src text,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate timestamp with time zone := now();
	jdata_json jsonb := jdata::jsonb;
BEGIN
	INSERT INTO solardatum.da_meta(node_id, source_id, created, updated, jdata)
	VALUES (node, src, cdate, udate, jdata_json)
	ON CONFLICT (node_id, source_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;

CREATE OR REPLACE FUNCTION solardatum.find_available_sources(
	IN node bigint,
	IN st timestamp with time zone DEFAULT NULL,
	IN en timestamp with time zone DEFAULT NULL)
  RETURNS TABLE(source_id text) AS
$BODY$
DECLARE
	node_tz text;
BEGIN
	IF st IS NOT NULL OR en IS NOT NULL THEN
		-- get the node TZ for local date/time
		SELECT l.time_zone  FROM solarnet.sn_node n
		INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE n.node_id = node
		INTO node_tz;

		IF NOT FOUND THEN
			RAISE NOTICE 'Node % has no time zone, will use UTC.', node;
			node_tz := 'UTC';
		END IF;
	END IF;

	CASE
		WHEN st IS NULL AND en IS NULL THEN
			RETURN QUERY SELECT DISTINCT CAST(d.source_id AS text)
			FROM solaragg.agg_datum_daily d
			WHERE d.node_id = node;

		WHEN en IS NULL THEN
			RETURN QUERY SELECT DISTINCT CAST(d.source_id AS text)
			FROM solaragg.agg_datum_daily d
			WHERE d.node_id = node
				AND d.ts_start >= CAST(st at time zone node_tz AS DATE);

		WHEN st IS NULL THEN
			RETURN QUERY SELECT DISTINCT CAST(d.source_id AS text)
			FROM solaragg.agg_datum_daily d
			WHERE d.node_id = node
				AND d.ts_start <= CAST(en at time zone node_tz AS DATE);

		ELSE
			RETURN QUERY SELECT DISTINCT CAST(d.source_id AS text)
			FROM solaragg.agg_datum_daily d
			WHERE d.node_id = node
				AND d.ts_start >= CAST(st at time zone node_tz AS DATE)
				AND d.ts_start <= CAST(en at time zone node_tz AS DATE);
	END CASE;
END;$BODY$
  LANGUAGE plpgsql STABLE ROWS 50;

CREATE OR REPLACE FUNCTION solardatum.find_reportable_interval(
	IN node bigint,
	IN src text DEFAULT NULL,
	OUT ts_start timestamp with time zone,
	OUT ts_end timestamp with time zone,
	OUT node_tz TEXT,
	OUT node_tz_offset INTEGER)
  RETURNS RECORD AS
$BODY$
BEGIN
	CASE
		WHEN src IS NULL THEN
			SELECT min(ts) FROM solardatum.da_datum WHERE node_id = node
			INTO ts_start;
		ELSE
			SELECT min(ts) FROM solardatum.da_datum WHERE node_id = node AND source_id = src
			INTO ts_start;
	END CASE;

	CASE
		WHEN src IS NULL THEN
			SELECT max(ts) FROM solardatum.da_datum WHERE node_id = node
			INTO ts_end;
		ELSE
			SELECT max(ts) FROM solardatum.da_datum WHERE node_id = node AND source_id = src
			INTO ts_end;
	END CASE;

	SELECT
		l.time_zone,
		CAST(EXTRACT(epoch FROM z.utc_offset) / 60 AS INTEGER)
	FROM solarnet.sn_node n
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	INNER JOIN pg_timezone_names z ON z.name = l.time_zone
	WHERE n.node_id = node
	INTO node_tz, node_tz_offset;

	IF NOT FOUND THEN
		node_tz := 'UTC';
		node_tz_offset := 0;
	END IF;

END;$BODY$
  LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION solardatum.populate_updated()
  RETURNS "trigger" AS
$BODY$
BEGIN
	NEW.updated := now();
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_updated
  BEFORE INSERT OR UPDATE
  ON solardatum.da_meta
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.populate_updated();




/**
 * Find source IDs matching a datum metadata search filter.
 *
 * Search filters are specified using LDAP filter syntax, e.g. <code>(/m/foo=bar)</code>.
 *
 * @param nodes				array of node IDs
 * @param criteria			the search filter
 *
 * @returns All matching source IDs.
 */
CREATE OR REPLACE FUNCTION solardatum.find_sources_for_meta(
    IN nodes bigint[],
    IN criteria text
  )
  RETURNS TABLE(node_id bigint, source_id text)
  LANGUAGE plv8 ROWS 100 STABLE AS
$BODY$
'use strict';

var objectPathMatcher = require('util/objectPathMatcher').default,
	searchFilter = require('util/searchFilter').default;

var filter = searchFilter(criteria),
	stmt,
	curs,
	rec,
	meta,
	matcher,
	resultRec = {};

if ( !filter.rootNode ) {
	plv8.elog(NOTICE, 'Malformed search filter:', criteria);
	return;
}

stmt = plv8.prepare('SELECT node_id, source_id, jdata FROM solardatum.da_meta WHERE node_id = ANY($1)', ['bigint[]']);
curs = stmt.cursor([nodes]);

while ( rec = curs.fetch() ) {
	meta = rec.jdata;
	matcher = objectPathMatcher(meta);
	if ( matcher.matchesFilter(filter) ) {
		resultRec.node_id = rec.node_id;
		resultRec.source_id = rec.source_id;
		plv8.return_next(resultRec);
	}
}

curs.close();
stmt.free();

$BODY$;

/**
 * Count the properties in a datum JSON object.
 *
 * @param jdata				the datum JSON
 *
 * @returns The property count.
 */
CREATE OR REPLACE FUNCTION solardatum.datum_prop_count(IN jdata jsonb)
  RETURNS INTEGER
  LANGUAGE plv8
  IMMUTABLE AS
$BODY$
'use strict';
var count = 0, prop, val;
if ( jdata ) {
	for ( prop in jdata ) {
		val = jdata[prop];
		if ( Array.isArray(val) ) {
			count += val.length;
		} else {
			count += Object.keys(val).length;
		}
	}
}
return count;
$BODY$;

/**
 * Project the values of a datum at a specific point in time, by deriving from the previous and next values
 * from the same source ID.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts` column will
 * simply be `reading_ts`. The `jdata_i` column will be computed as an average of the previous/next rows,
 * and `jdata_a` will be time-projected based on the previous/next readings.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param reading_ts	the timestamp to calculate the value of each datum at
 * @param span			a maximum range before and after `reading_ts` to consider when looking for the previous/next datum
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_at(
	nodes bigint[], sources text[], reading_ts timestamptz, span interval default '1 month')
RETURNS TABLE(
  ts timestamp with time zone,
  node_id bigint,
  source_id text,
  jdata_i jsonb,
  jdata_a jsonb
) LANGUAGE SQL STABLE AS $$
	WITH slice AS (
		SELECT
			d.ts,
			CASE
				WHEN d.ts <= reading_ts THEN last_value(d.ts) OVER win
				ELSE first_value(d.ts) OVER win
			END AS slot_ts,
			lead(d.ts) OVER win_full AS next_ts,
			EXTRACT(epoch FROM (reading_ts - d.ts))
				/ EXTRACT(epoch FROM (lead(d.ts) OVER win_full - d.ts)) AS weight,
			d.node_id,
			d.source_id,
			d.jdata_i,
			d.jdata_a
		FROM solardatum.da_datum d
		WHERE d.node_id = ANY(nodes)
			AND d.source_id = ANY(sources)
			AND d.ts >= reading_ts - span
			AND d.ts < reading_ts + span
		WINDOW win AS (PARTITION BY d.node_id, d.source_id, CASE WHEN d.ts <= reading_ts
			THEN 0 ELSE 1 END ORDER BY d.ts RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING),
			win_full AS (PARTITION BY d.node_id, d.source_id)
		ORDER BY d.node_id, d.source_id, d.ts
	)
	SELECT reading_ts AS ts,
		node_id,
		source_id,
		CASE solarcommon.first(ts ORDER BY ts)
			-- we have exact timestamp (improbable!)
			WHEN reading_ts THEN solarcommon.first(jdata_i ORDER BY ts)

			-- more likely, project prop values based on linear difference between start/end samples
			ELSE solarcommon.jsonb_avg_object(jdata_i)
		END AS jdata_i,
		CASE solarcommon.first(ts ORDER BY ts)
			-- we have exact timestamp (improbable!)
			WHEN reading_ts THEN solarcommon.first(jdata_a ORDER BY ts)

			-- more likely, project prop values based on linear difference between start/end samples
			ELSE solarcommon.jsonb_weighted_proj_object(jdata_a, weight)
		END AS jdata_a
	FROM slice
	WHERE ts = slot_ts
	GROUP BY node_id, source_id
	HAVING count(*) > 1 OR solarcommon.first(ts ORDER BY ts) = reading_ts OR solarcommon.first(ts ORDER BY ts DESC) = reading_ts
	ORDER BY node_id, source_id
$$;

/**
 * Project the values of a datum at a specific point in node-local time, by deriving from the previous and next values
 * from the same source ID.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts` column will
 * be `reading_ts` at the time zone for each node. The `jdata_i` column will be computed as an average of the previous/next rows,
 * and `jdata_a` will be time-projected based on the previous/next readings.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param reading_ts	the timestamp to calculate the value of each datum at
 * @param span			a maximum range before and after `reading_ts` to consider when looking for the previous/next datum
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_at_local(
	nodes bigint[], sources text[], reading_ts timestamp, span interval default '1 month')
RETURNS TABLE(
  ts timestamp with time zone,
  node_id bigint,
  source_id text,
  jdata_i jsonb,
  jdata_a jsonb
) LANGUAGE SQL STABLE AS $$
	WITH t AS (
		SELECT node_id, reading_ts AT TIME ZONE time_zone AS ts
		FROM solarnet.node_local_time
		WHERE node_id = ANY(nodes)
	), slice AS (
		SELECT
			d.ts,
			t.ts AS ts_slot,
			CASE
				WHEN d.ts <= t.ts THEN last_value(d.ts) OVER win
				ELSE first_value(d.ts) OVER win
			END AS slot_ts,
			lead(d.ts) OVER win_full AS next_ts,
			EXTRACT(epoch FROM (t.ts - d.ts))
				/ EXTRACT(epoch FROM (lead(d.ts) OVER win_full - d.ts)) AS weight,
			d.node_id,
			d.source_id,
			d.jdata_i,
			d.jdata_a
		FROM solardatum.da_datum d
		INNER JOIN t ON t.node_id = d.node_id
		WHERE d.node_id = ANY(nodes)
			AND d.source_id = ANY(sources)
			AND d.ts >= t.ts - span
			AND d.ts < t.ts + span
		WINDOW win AS (PARTITION BY d.node_id, d.source_id, CASE WHEN d.ts <= t.ts
			THEN 0 ELSE 1 END ORDER BY d.ts RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING),
			win_full AS (PARTITION BY d.node_id, d.source_id)
		ORDER BY d.node_id, d.source_id, d.ts
	)
	SELECT
		ts_slot AS ts,
		node_id,
		source_id,
		CASE solarcommon.first(ts ORDER BY ts)
			-- we have exact timestamp (improbable!)
			WHEN ts_slot THEN solarcommon.first(jdata_i ORDER BY ts)

			-- more likely, project prop values based on linear difference between start/end samples
			ELSE solarcommon.jsonb_avg_object(jdata_i)
		END AS jdata_i,
		CASE solarcommon.first(ts ORDER BY ts)
			-- we have exact timestamp (improbable!)
			WHEN ts_slot THEN solarcommon.first(jdata_a ORDER BY ts)

			-- more likely, project prop values based on linear difference between start/end samples
			ELSE solarcommon.jsonb_weighted_proj_object(jdata_a, weight)
		END AS jdata_a
	FROM slice
	WHERE ts = slot_ts
	GROUP BY ts_slot, node_id, source_id
	HAVING count(*) > 1 OR solarcommon.first(ts ORDER BY ts) = ts_slot OR solarcommon.first(ts ORDER BY ts DESC) = ts_slot
	ORDER BY ts_slot, node_id, source_id
$$;

/**
 * Calculate the difference between the accumulating properties of datum between a time range.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata_a` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diff_object()` aggregate function.
 *
 * The `solardatum.da_datum_aux` table will be considered, for `Reset` type rows.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range
 * @param ts_max		the timestamp of the end of the time range
 * @param tolerance		a maximum range before `ts_min` to consider when looking for the datum
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff(
	nodes bigint[], sources text[], ts_min timestamptz, ts_max timestamptz, tolerance interval default interval '1 month')
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	-- find records closest to, but not after, min date
	-- also considering reset records, using their STARTING sample value
	WITH latest_before_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT DISTINCT ON (d.node_id, d.source_id) d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.da_datum d 
				WHERE d.node_id = ANY(nodes)
					AND d.source_id = ANY(sources)
					AND d.ts <= ts_min
					AND d.ts > ts_min - tolerance
				ORDER BY d.node_id, d.source_id, d.ts DESC
			)
			UNION
			(
				SELECT DISTINCT ON (aux.node_id, aux.source_id) aux.ts, aux.node_id, aux.source_id, aux.jdata_as AS jdata_a
				FROM solardatum.da_datum_aux aux
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.node_id = ANY(nodes)
					AND aux.source_id = ANY(sources)
					AND aux.ts <= ts_min
					AND aux.ts > ts_min - tolerance
				ORDER BY aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- find records closest to, but not after max date (could be same as latest_before_start or earliest_after_start)
	-- also considering reset records, using their FINAL sample value
	, latest_before_end AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT DISTINCT ON (d.node_id, d.source_id) d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.da_datum d
				WHERE d.node_id = ANY(nodes)
					AND d.source_id = ANY(sources)
					AND d.ts <= ts_max
					AND d.ts > ts_max - tolerance
				ORDER BY d.node_id, d.source_id, d.ts DESC
			)
			UNION
			(
				SELECT DISTINCT ON (aux.node_id, aux.source_id) aux.ts, aux.node_id, aux.source_id, aux.jdata_af AS jdata_a
				FROM solardatum.da_datum_aux aux
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.node_id = ANY(nodes)
					AND aux.source_id = ANY(sources)
					AND aux.ts <= ts_max
					AND aux.ts > ts_max - tolerance
				ORDER BY aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- narrow data to [start, final] pairs of rows by node,source by choosing
	-- latest_before_start in preference to earliest_after_start
	, d AS (
		SELECT * FROM latest_before_start
		UNION
		SELECT * FROM latest_before_end
	)
	-- begin search for reset records WITHIN [start, final] date ranges via table of found [start, final] dates
	, ranges AS (
		SELECT node_id
			, source_id
			, min(ts) AS sdate
			, max(ts) AS edate
		FROM d
		GROUP BY node_id, source_id
	)
	-- find all reset records per node, source within [start, final] date ranges, producing pairs
	-- of rows for each matching record, of [FINAL, STARTING] data
	, resets AS (
		SELECT aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM ranges
		INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ranges.node_id AND aux.source_id = ranges.source_id
			AND aux.ts > ranges.sdate AND aux.ts < ranges.edate
		WHERE atype = 'Reset'::solardatum.da_datum_aux_type
	)
	-- combine [start, final] pairs with reset pairs
	, combined AS (
		SELECT * FROM d
		UNION
		SELECT * FROM resets
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(nlt.time_zone) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;


/**
 * Calculate the difference between the accumulating properties of datum between a time range.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata_a` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diff_object()` aggregate function.
 *
 * The `solardatum.da_datum_aux` table will be considered, for `Reset` type rows.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range
 * @param ts_max		the timestamp of the end of the time range
 * @param tolerance		a maximum range before `ts_min` to consider when looking for the datum
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_local(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp, tolerance interval default interval '1 month')
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	-- generate rows of nodes grouped by time zone, get absolute start/end dates for all nodes
	-- but grouped into as few rows as possible to minimize subsequent query times
	WITH tz AS (
		SELECT nlt.time_zone,
			ts_min AT TIME ZONE nlt.time_zone AS sdate,
			ts_max AT TIME ZONE nlt.time_zone AS edate,
			array_agg(DISTINCT nlt.node_id) AS nodes,
			array_agg(DISTINCT s.source_id) AS sources
		FROM solarnet.node_local_time nlt
		CROSS JOIN (
			SELECT unnest(sources) AS source_id
		) s
		WHERE nlt.node_id = ANY(nodes)
		GROUP BY nlt.time_zone
	)
	-- find records closest to, but not after, min date
	-- also considering reset records, using their STARTING sample value
	, latest_before_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT DISTINCT ON (d.node_id, d.source_id) tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum d ON d.node_id = ANY(tz.nodes) AND d.source_id = ANY(tz.sources)
				WHERE d.node_id = ANY(tz.nodes)
					AND d.source_id = ANY(tz.sources)
					AND d.ts <= tz.sdate
					AND d.ts > tz.sdate - tolerance
				ORDER BY d.node_id, d.source_id, d.ts DESC
			)
			UNION
			(
				SELECT DISTINCT ON (tz.time_zone, aux.node_id, aux.source_id)
					tz.time_zone, aux.ts, aux.node_id, aux.source_id, aux.jdata_as AS jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ANY(tz.nodes) AND aux.source_id = ANY(tz.sources)
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.ts < tz.sdate
				ORDER BY tz.time_zone, aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- find records closest to, but not after max date (could be same as latest_before_start or earliest_after_start)
	-- also considering reset records, using their FINAL sample value
	, latest_before_end AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT DISTINCT ON (d.node_id, d.source_id) tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum d ON d.node_id = ANY(tz.nodes) AND d.source_id = ANY(tz.sources)
				WHERE d.node_id = ANY(tz.nodes)
					AND d.source_id = ANY(tz.sources)
					AND d.ts <= tz.edate
					AND d.ts > tz.edate - tolerance
				ORDER BY d.node_id, d.source_id, d.ts DESC
			)
			UNION
			(
				SELECT DISTINCT ON (tz.time_zone, aux.node_id, aux.source_id)
					tz.time_zone, aux.ts, aux.node_id, aux.source_id, aux.jdata_af AS jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ANY(tz.nodes) AND aux.source_id = ANY(tz.sources)
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.ts < tz.edate
				ORDER BY tz.time_zone, aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- narrow data to [start, final] pairs of rows by node,source by choosing
	-- latest_before_start in preference to earliest_after_start
	, d AS (
		SELECT * FROM latest_before_start
		UNION
		SELECT * FROM latest_before_end
	)
	-- begin search for reset records WITHIN [start, final] date ranges via table of found [start, final] dates
	, ranges AS (
		SELECT time_zone
			, node_id
			, source_id
			, min(ts) AS sdate
			, max(ts) AS edate
		FROM d
		GROUP BY time_zone, node_id, source_id
	)
	-- find all reset records per node, source within [start, final] date ranges, producing pairs
	-- of rows for each matching record, of [FINAL, STARTING] data
	, resets AS (
		SELECT ranges.time_zone
			, aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM ranges
		INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ranges.node_id AND aux.source_id = ranges.source_id
			AND aux.ts > ranges.sdate AND aux.ts < ranges.edate
		WHERE atype = 'Reset'::solardatum.da_datum_aux_type
	)
	-- combine [start, final] pairs with reset pairs
	, combined AS (
		SELECT * FROM d
		UNION
		SELECT * FROM resets
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(d.time_zone) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;


/**
 * Find the latest available data before a specific point in time.
 *
 * This function returns at most one row per node+source combination. It also relies
 * on the `solaragg.agg_datum_daily` table to perform an initial narrowing of all possible
 * datum to a single day (for each node+source combination). This means that the query
 * results depend on aggregate data processing to populate the `solaragg.agg_datum_daily`
 * table in order for results to be returned.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_max		the maximum timestamp to search for (inclusive)
 */
CREATE OR REPLACE FUNCTION solardatum.find_latest_before(
	nodes bigint[], sources text[], ts_max timestamptz)
RETURNS TABLE(
  ts timestamp with time zone,
  node_id bigint,
  source_id character varying(64)
) LANGUAGE sql STABLE AS $$
	-- first find max day quickly for each node+source
	WITH max_dates AS (
		SELECT max(ts_start) AS ts_start, node_id, source_id
		FROM solaragg.agg_datum_daily
		WHERE node_id = ANY(nodes)
			AND source_id = ANY(sources)
			AND ts_start < ts_max
		GROUP BY node_id, source_id
	)
	, -- then group by day (start of day), so we can batch day+node+source queries together
	max_date_groups AS (
		SELECT ts_start AS ts_start, array_agg(DISTINCT node_id) AS nodes, array_agg(DISTINCT source_id) AS sources
		FROM max_dates
		GROUP BY ts_start
	)
	-- now for each day+node+source find maximum exact date
	SELECT max(d.ts) AS ts, d.node_id, d.source_id
	FROM max_date_groups mdg
	INNER JOIN solardatum.da_datum d ON d.node_id = ANY(mdg.nodes) AND d.source_id = ANY(mdg.sources)
	WHERE d.ts >= mdg.ts_start
		AND d.ts <= ts_max
	GROUP BY d.node_id, d.source_id
$$;


/**
 * Find the earliest available data after a specific point in time.
 *
 * This function returns at most one row per node+source combination. It also relies
 * on the `solaragg.agg_datum_daily` table to perform an initial narrowing of all possible
 * datum to a single day (for each node+source combination). This means that the query
 * results depend on aggregate data processing to populate the `solaragg.agg_datum_daily`
 * table in order for results to be returned.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the minimum timestamp to search for (inclusive)
 */
CREATE OR REPLACE FUNCTION solardatum.find_earliest_after(
	nodes bigint[], sources text[], ts_min timestamptz)
RETURNS TABLE(
  ts timestamp with time zone,
  node_id bigint,
  source_id character varying(64)
) LANGUAGE sql STABLE AS $$
	-- first find min day quickly for each node+source
	WITH min_dates AS (
		SELECT min(ts_start) AS ts_start, node_id, source_id
		FROM solaragg.agg_datum_daily
		WHERE node_id = ANY(nodes)
			AND source_id = ANY(sources)
			AND ts_start >= ts_min
		GROUP BY node_id, source_id
	)
	, -- then group by day (start of day), so we can batch day+node+source queries together
	min_date_groups AS (
		SELECT ts_start AS ts_start, array_agg(DISTINCT node_id) AS nodes, array_agg(DISTINCT source_id) AS sources
		FROM min_dates
		GROUP BY ts_start
	)
	-- now for each day+node+source find minimum exact date
	SELECT min(d.ts) AS ts, d.node_id, d.source_id
	FROM min_date_groups mdg
	INNER JOIN solardatum.da_datum d ON d.node_id = ANY(mdg.nodes) AND d.source_id = ANY(mdg.sources)
	WHERE d.ts >= ts_min
		AND d.ts <= mdg.ts_start + interval '1 day'
	GROUP BY d.node_id, d.source_id
$$;


/**
 * Calculate the difference between the accumulating properties of datum over a time range.
 *
 * This returns at most one row. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diffsum_jdata()` aggregate function.
 *
 * @param node 			the node ID to find
 * @param source 		the source ID to find
 * @param ts_min		the timestamp of the start of the time range (inclusive)
 * @param ts_max		the timestamp of the end of the time range (exclusive)
 * @param tolerance 	the maximum time span to look backwards for the previous reading record; smaller == faster
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_over(
	node bigint, source text, ts_min timestamptz, ts_max timestamptz, tolerance interval default interval '1 year')
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata jsonb
) LANGUAGE sql STABLE ROWS 1 AS $$
	WITH latest_before_start AS (
		SELECT ts, node_id, source_id, jdata_a FROM (
			(
				-- find latest before
				SELECT ts, node_id, source_id, jdata_a, 0 AS rr
				FROM solardatum.da_datum
				WHERE node_id = node
					AND source_id = source
					AND ts < ts_min
					AND ts >= ts_min - tolerance
				ORDER BY ts DESC 
				LIMIT 1
			)
			UNION
			(
				-- find latest before reset
				SELECT ts, node_id, source_id, jdata_as AS jdata_a, 1 AS rr
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = node
					AND source_id = source
					AND ts < ts_min
					AND ts >= ts_min - tolerance
				ORDER BY ts DESC
				LIMIT 1
			)
		) d
		-- add order by rr so that when datum & reset have equivalent ts, reset has priority
		ORDER BY d.ts DESC, rr DESC
		LIMIT 1
	)
	, earliest_after_start AS (
		SELECT ts, node_id, source_id, jdata_a FROM (
			(
				-- find earliest on/after
				SELECT ts, node_id, source_id, jdata_a, 0 AS rr
				FROM solardatum.da_datum
				WHERE node_id = node
					AND source_id = source
					AND ts >= ts_min
					AND ts < ts_max
				ORDER BY ts 
				LIMIT 1
			)
			UNION ALL
			(
				-- find earliest on/after reset
				SELECT ts, node_id, source_id, jdata_as AS jdata_a, 1 AS rr
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = node
					AND source_id = source
					AND ts >= ts_min
					AND ts < ts_max
				ORDER BY ts
				LIMIT 1
			)
		) d
		-- add order by rr so that when datum & reset have equivalent ts, reset has priority
		ORDER BY d.ts, rr DESC
		LIMIT 1
	)
	, latest_before_end AS (
		SELECT ts, node_id, source_id, jdata_a FROM (
			(
				-- find latest before
				SELECT ts, node_id, source_id, jdata_a, 0 AS rr
				FROM solardatum.da_datum
				WHERE node_id = node
					AND source_id = source
					AND ts < ts_max
					AND ts >= ts_min
				ORDER BY ts DESC 
				LIMIT 1
			)
			UNION ALL
			(
				-- find latest before reset
				SELECT ts, node_id, source_id, jdata_af AS jdata_a, 1 AS rr
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = node
					AND source_id = source
					AND ts < ts_max
					AND ts >= ts_min
				ORDER BY ts DESC
				LIMIT 1
			)
		) d
		-- add order by rr so that when datum & reset have equivalent ts, reset has priority
		ORDER BY d.ts DESC, rr DESC
		LIMIT 1
	)
	, d AS (
		(
			SELECT *
			FROM (
				SELECT * FROM latest_before_start
				UNION
				SELECT * FROM earliest_after_start
			) d
			ORDER BY d.ts
			LIMIT 1
		)
		UNION ALL
		(
			SELECT * FROM latest_before_end
		)
	)
	, ranges AS (
		SELECT min(ts) AS sdate
			, max(ts) AS edate
		FROM d
	)
	, combined AS (
		SELECT * FROM d
	
		UNION ALL
		SELECT aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM ranges, solardatum.da_datum_aux aux 
		WHERE atype = 'Reset'::solardatum.da_datum_aux_type
			AND aux.node_id = node 
			AND aux.source_id = source
			AND aux.ts > ranges.sdate
			AND aux.ts < ranges.edate
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(COALESCE(nlt.time_zone, 'UTC')) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_jdata(d.jdata_a ORDER BY d.ts) AS jdata
	FROM combined d
	LEFT OUTER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
$$;


/**
 * Calculate the difference between the accumulating properties of datum over a time range.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata_a` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diff_object()` aggregate function.
 *
 * The `solardatum.da_datum_aux` table will be considered, for `Reset` type rows.
 * 
 * This function makes use of `solardatum.find_latest_before()` and `solardatum.find_earliest_after`
 * and thus has the same restrictions as those, and as a consequence will consider all data before
 * the given `ts_max` as possible data.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range (inclusive)
 * @param ts_max		the timestamp of the end of the time range (inclusive)
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_over(
	nodes bigint[], sources text[], ts_min timestamptz, ts_max timestamptz)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	-- find records closest to, but not after, min date
	-- also considering reset records, using their STARTING sample value
	WITH latest_before_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			SELECT d.ts, d.node_id, d.source_id, d.jdata_a
			FROM  solardatum.find_latest_before(nodes, sources, ts_min) dates
			INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			UNION
			SELECT DISTINCT ON (node_id, source_id) ts, node_id, source_id, jdata_as AS jdata_a
			FROM solardatum.da_datum_aux
			WHERE atype = 'Reset'::solardatum.da_datum_aux_type
				AND node_id = ANY(nodes)
				AND source_id = ANY(sources)
				AND ts < ts_min
			ORDER BY node_id, source_id, ts DESC
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- in case no data before min date, find closest to min date or after
	-- also considering reset records, using their STARTING sample value
	, earliest_after_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.find_earliest_after(nodes, sources, ts_min) dates
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			)
			UNION
			(
				SELECT DISTINCT ON (node_id, source_id) ts, node_id, source_id, jdata_as AS jdata_a
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = ANY(nodes)
					AND source_id = ANY(sources)
					AND ts >= ts_min
				ORDER BY node_id, source_id, ts
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts
	)
	-- find records closest to, but not after max date (could be same as latest_before_start or earliest_after_start)
	-- also considering reset records, using their FINAL sample value
	, latest_before_end AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.find_latest_before(nodes, sources, ts_max) dates
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			)
			UNION
			(
				SELECT DISTINCT ON (node_id, source_id) ts, node_id, source_id, jdata_af AS jdata_a
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = ANY(nodes)
					AND source_id = ANY(sources)
					AND ts < ts_max
				ORDER BY node_id, source_id, ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- narrow data to [start, final] pairs of rows by node,source by choosing
	-- latest_before_start in preference to earliest_after_start
	, d AS (
		SELECT * FROM (
			SELECT DISTINCT ON (d.node_id, d.source_id) d.*
			FROM (
				SELECT * FROM latest_before_start
				UNION
				SELECT * FROM earliest_after_start
			) d
			ORDER BY d.node_id, d.source_id, d.ts
		) earliest
		UNION 
		SELECT * FROM latest_before_end
	)
	-- begin search for reset records WITHIN [start, final] date ranges via table of found [start, final] dates
	, ranges AS (
		SELECT node_id
			, source_id
			, min(ts) AS sdate
			, max(ts) AS edate
		FROM d
		GROUP BY node_id, source_id
	)
	-- find all reset records per node, source within [start, final] date ranges, producing pairs
	-- of rows for each matching record, of [FINAL, STARTING] data
	, resets AS (
		SELECT aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM ranges
		INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ranges.node_id AND aux.source_id = ranges.source_id
			AND aux.ts > ranges.sdate AND aux.ts < ranges.edate
		WHERE atype = 'Reset'::solardatum.da_datum_aux_type
	)
	-- combine [start, final] pairs with reset pairs
	, combined AS (
		SELECT * FROM d
		UNION
		SELECT * FROM resets
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(COALESCE(nlt.time_zone, 'UTC')) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	LEFT OUTER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;


/**
 * Calculate the difference between the accumulating properties of datum over a time range.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata_a` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diff_object()` aggregate function.
 *
 * The `solardatum.da_datum_aux` table will be considered, for `Reset` type rows.
 * 
 * This function makes use of `solardatum.find_latest_before()` and `solardatum.find_earliest_after`
 * and thus has the same restrictions as those, and as a consequence will consider all data before
 * the given `ts_max` as possible data.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range (inclusive), in node local time
 * @param ts_max		the timestamp of the end of the time range (inclusive), in node local time
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_over_local(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	-- generate rows of nodes grouped by time zone, get absolute start/end dates for all nodes
	-- but grouped into as few rows as possible to minimize subsequent query times
	WITH tz AS (
		SELECT nlt.time_zone,
			ts_min AT TIME ZONE nlt.time_zone AS sdate,
			ts_max AT TIME ZONE nlt.time_zone AS edate,
			array_agg(DISTINCT nlt.node_id) AS nodes,
			array_agg(DISTINCT s.source_id) AS sources
		FROM solarnet.node_local_time nlt
		CROSS JOIN (
			SELECT unnest(sources) AS source_id
		) s
		WHERE nlt.node_id = ANY(nodes)
		GROUP BY nlt.time_zone
	)
	-- find records closest to, but not after, min date
	-- also considering reset records, using their STARTING sample value
	, latest_before_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
				FROM tz
				INNER JOIN solardatum.find_latest_before(tz.nodes, tz.sources, tz.sdate) dates ON dates.node_id = ANY(tz.nodes) AND dates.source_id = ANY(tz.sources)
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			)
			UNION
			(
				SELECT DISTINCT ON (tz.time_zone, aux.node_id, aux.source_id)
					tz.time_zone, aux.ts, aux.node_id, aux.source_id, aux.jdata_as AS jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ANY(tz.nodes) AND aux.source_id = ANY(tz.sources)
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.ts < tz.sdate
				ORDER BY tz.time_zone, aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- in case no data before min date, find closest to min date or after
	-- also considering reset records, using their STARTING sample value
	, earliest_after_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
				FROM tz
				INNER JOIN solardatum.find_earliest_after(tz.nodes, tz.sources, tz.sdate) dates ON dates.node_id = ANY(tz.nodes) AND dates.source_id = ANY(tz.sources)
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			)
			UNION
			(
				SELECT DISTINCT ON (tz.time_zone, aux.node_id, aux.source_id)
					tz.time_zone, aux.ts, aux.node_id, aux.source_id, aux.jdata_as AS jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ANY(tz.nodes) AND aux.source_id = ANY(tz.sources)
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.ts >= tz.sdate
				ORDER BY tz.time_zone, aux.node_id, aux.source_id, aux.ts
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts
	)
	-- find records closest to, but not after max date (could be same as latest_before_start or earliest_after_start)
	-- also considering reset records, using their FINAL sample value
	, latest_before_end AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
				FROM tz
				INNER JOIN solardatum.find_latest_before(tz.nodes, tz.sources, tz.edate) dates ON dates.node_id = ANY(tz.nodes) AND dates.source_id = ANY(tz.sources)
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			)
			UNION
			(
				SELECT DISTINCT ON (tz.time_zone, aux.node_id, aux.source_id)
					tz.time_zone, aux.ts, aux.node_id, aux.source_id, aux.jdata_af AS jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ANY(tz.nodes) AND aux.source_id = ANY(tz.sources)
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.ts < tz.edate
				ORDER BY tz.time_zone, aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- narrow data to [start, final] pairs of rows by node,source by choosing
	-- latest_before_start in preference to earliest_after_start
	, d AS (
		SELECT * FROM (
			SELECT DISTINCT ON (d.node_id, d.source_id) d.*
			FROM (
				SELECT * FROM latest_before_start
				UNION
				SELECT * FROM earliest_after_start
			) d
			ORDER BY d.node_id, d.source_id, d.ts
		) earliest
		UNION 
		SELECT * FROM latest_before_end
	)
	-- begin search for reset records WITHIN [start, final] date ranges via table of found [start, final] dates
	, ranges AS (
		SELECT time_zone
			, node_id
			, source_id
			, min(ts) AS sdate
			, max(ts) AS edate
		FROM d
		GROUP BY time_zone, node_id, source_id
	)
	-- find all reset records per node, source within [start, final] date ranges, producing pairs
	-- of rows for each matching record, of [FINAL, STARTING] data
	, resets AS (
		SELECT ranges.time_zone
			, aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM ranges
		INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ranges.node_id AND aux.source_id = ranges.source_id
			AND aux.ts > ranges.sdate AND aux.ts < ranges.edate
		WHERE atype = 'Reset'::solardatum.da_datum_aux_type
	)
	-- combine [start, final] pairs with reset pairs
	, combined AS (
		SELECT * FROM d
		UNION
		SELECT * FROM resets
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(d.time_zone) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;


/**
 * Calculate the count of rows matching a set of nodes, sources, and a local date range.
 *
 * The time zones of each node are used to calculate absolute date ranges for each node.
 * 
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date, or the current time if not provided
 * @param ts_max the ending local date, or the current time if not provided
 */
CREATE OR REPLACE FUNCTION solardatum.datum_record_counts(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
	query_date timestamptz, 
	datum_count bigint, 
	datum_hourly_count integer, 
	datum_daily_count integer, 
	datum_monthly_count integer
) LANGUAGE plpgsql STABLE AS $$
DECLARE
	all_source_ids boolean := sources IS NULL OR array_length(sources, 1) < 1;
	start_date timestamp := COALESCE(ts_min, CURRENT_TIMESTAMP);
	end_date timestamp := COALESCE(ts_max, CURRENT_TIMESTAMP);
BEGIN
	-- raw count
	WITH nlt AS (
		SELECT time_zone, ts_start, ts_end, node_ids, source_ids
		FROM solarnet.node_source_time_ranges_local(nodes, sources, start_date, end_date)
	)
	SELECT count(*)
	FROM solardatum.da_datum d, nlt
	WHERE 
		d.ts >= nlt.ts_start
		AND d.ts < nlt.ts_end
		AND d.node_id = ANY(nlt.node_ids)
		AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	INTO datum_count;

	-- count hourly data
	WITH nlt AS (
		SELECT time_zone, ts_start, ts_end, node_ids, source_ids
		FROM solarnet.node_source_time_ranges_local(nodes, sources, start_date, end_date)
	)
	SELECT count(*)
	FROM solaragg.agg_datum_hourly d, nlt
	WHERE 
		d.ts_start >= nlt.ts_start
		AND d.ts_start < date_trunc('hour', nlt.ts_end)
		AND d.node_id = ANY(nlt.node_ids)
		AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	INTO datum_hourly_count;

	-- count daily data
	WITH nlt AS (
		SELECT time_zone, ts_start, ts_end, node_ids, source_ids
		FROM solarnet.node_source_time_ranges_local(nodes, sources, start_date, end_date)
	)
	SELECT count(*)
	FROM solaragg.agg_datum_daily d, nlt
	WHERE 
		d.ts_start >= nlt.ts_start
		AND d.ts_start < date_trunc('day', nlt.ts_end)
		AND d.node_id = ANY(nlt.node_ids)
		AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	INTO datum_daily_count;

	-- count daily data
	WITH nlt AS (
		SELECT time_zone, ts_start, ts_end, node_ids, source_ids
		FROM solarnet.node_source_time_ranges_local(nodes, sources, start_date, end_date)
	)
	SELECT count(*)
	FROM solaragg.agg_datum_monthly d, nlt
	WHERE 
		d.ts_start >= nlt.ts_start
		AND d.ts_start < date_trunc('month', nlt.ts_end)
		AND d.node_id = ANY(nlt.node_ids)
		AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	INTO datum_monthly_count;

	query_date = CURRENT_TIMESTAMP;
	RETURN NEXT;
END
$$;

/**
 * Calculate the count of rows matching a set of nodes, sources, and a local date range.
 *
 * The `jfilter` parameter must provide the following items:
 * 
 * * `nodeIds` - array of node IDs
 * * `sourceIds` - (optional) array of source IDs
 * * `localStartDate` - (optional) string date, yyyy-MM-dd HH:mm format; current time used if not provided
 * * `localEndDate` - (optional) string date, yyyy-MM-dd HH:mm format; current time used if not provided
 * 
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date
 * @param ts_max the ending local date
 */
CREATE OR REPLACE FUNCTION solardatum.datum_record_counts_for_filter(jfilter jsonb)
RETURNS TABLE(
	query_date timestamptz, 
	datum_count bigint, 
	datum_hourly_count integer, 
	datum_daily_count integer, 
	datum_monthly_count integer
) LANGUAGE plpgsql STABLE AS $$
DECLARE
	node_ids bigint[] := solarcommon.jsonb_array_to_bigint_array(jfilter->'nodeIds');
	source_ids text[] := solarcommon.json_array_to_text_array(jfilter->'sourceIds');
	ts_min timestamp := jfilter->>'localStartDate';
	ts_max timestamp := jfilter->>'localEndDate';
BEGIN
	RETURN QUERY
	SELECT * FROM solardatum.datum_record_counts(node_ids, source_ids, ts_min, ts_max);
END
$$;

/**
 * Delete datum rows matching a set of nodes, sources, and a local date range.
 *
 * The time zones of each node are used to calculate absolute date ranges for each node.
 * 
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date, or the current time if not provided
 * @param ts_max the ending local date, or the current time if not provided
 */
CREATE OR REPLACE FUNCTION solardatum.delete_datum(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS $$
DECLARE
	all_source_ids boolean := sources IS NULL OR array_length(sources, 1) < 1;
	start_date timestamp := COALESCE(ts_min, CURRENT_TIMESTAMP);
	end_date timestamp := COALESCE(ts_max, CURRENT_TIMESTAMP);
	total_count bigint := 0;
BEGIN
	WITH nlt AS (
		SELECT time_zone, ts_start, ts_end, node_ids, source_ids
		FROM solarnet.node_source_time_ranges_local(nodes, sources, start_date, end_date)
	)
	DELETE FROM solardatum.da_datum d
	USING nlt
	WHERE 
		d.ts >= nlt.ts_start
		AND d.ts < nlt.ts_end
		AND d.node_id = ANY(nlt.node_ids)
		AND (all_source_ids OR d.source_id = ANY(nlt.source_ids));
	GET DIAGNOSTICS total_count = ROW_COUNT;

	RETURN total_count;
END
$$;

/**
 * Delete rows matching a set of nodes, sources, and a local date range.
 *
 * The `jfilter` parameter must provide the following items:
 * 
 * * `nodeIds` - array of node IDs
 * * `sourceIds` - (optional) array of source IDs
 * * `localStartDate` - (optional) string date, yyyy-MM-dd HH:mm format; current time used if not provided
 * * `localEndDate` - (optional) string date, yyyy-MM-dd HH:mm format; current time used if not provided
 * 
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date
 * @param ts_max the ending local date
 */
CREATE OR REPLACE FUNCTION solardatum.delete_datum_for_filter(jfilter jsonb)
RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS $$
DECLARE
	node_ids bigint[] := solarcommon.jsonb_array_to_bigint_array(jfilter->'nodeIds');
	source_ids text[] := solarcommon.json_array_to_text_array(jfilter->'sourceIds');
	ts_min timestamp := jfilter->>'localStartDate';
	ts_max timestamp := jfilter->>'localEndDate';
	total_count bigint := 0;
BEGIN
	SELECT solardatum.delete_datum(node_ids, source_ids, ts_min, ts_max) INTO total_count;
	RETURN total_count;
END
$$;
