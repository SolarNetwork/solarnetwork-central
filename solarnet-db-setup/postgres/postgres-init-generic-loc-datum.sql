CREATE TABLE solardatum.da_loc_datum (
  ts timestamp with time zone NOT NULL,
  loc_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  posted timestamp with time zone NOT NULL,
  jdata_i jsonb,
  jdata_a jsonb,
  jdata_s jsonb,
  jdata_t text[],
  CONSTRAINT da_loc_datum_pkey PRIMARY KEY (loc_id, ts, source_id)
);

CREATE OR REPLACE FUNCTION solardatum.jdata_from_datum(datum solardatum.da_loc_datum)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarcommon.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE VIEW solardatum.da_loc_datum_data AS
    SELECT d.ts, d.loc_id, d.source_id, d.posted, solardatum.jdata_from_datum(d) AS jdata
    FROM solardatum.da_loc_datum d;

CREATE TABLE solardatum.da_loc_meta (
  loc_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  created timestamp with time zone NOT NULL,
  updated timestamp with time zone NOT NULL,
  jdata jsonb NOT NULL,
  CONSTRAINT da_loc_meta_pkey PRIMARY KEY (loc_id, source_id)
);

CREATE TABLE solaragg.agg_stale_loc_datum (
  ts_start timestamp with time zone NOT NULL,
  loc_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  agg_kind char(1) NOT NULL,
  created timestamp NOT NULL DEFAULT now(),
  CONSTRAINT agg_stale_loc_datum_pkey PRIMARY KEY (agg_kind, ts_start, loc_id, source_id)
);

CREATE TABLE solaragg.agg_loc_messages (
  created timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  loc_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  ts timestamp with time zone NOT NULL,
  msg text NOT NULL
);

CREATE INDEX agg_loc_messages_ts_loc_idx ON solaragg.agg_loc_messages (ts, loc_id);

CREATE TABLE solaragg.agg_loc_datum_hourly (
  ts_start timestamp with time zone NOT NULL,
  local_date timestamp without time zone NOT NULL,
  loc_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  jdata_i jsonb,
  jdata_a jsonb,
  jdata_s jsonb,
  jdata_t text[],
  jmeta jsonb,
  CONSTRAINT agg_loc_datum_hourly_pkey PRIMARY KEY (loc_id, ts_start, source_id)
);

CREATE TABLE solaragg.aud_loc_datum_hourly (
  ts_start timestamp with time zone NOT NULL,
  loc_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  prop_count integer NOT NULL,
  CONSTRAINT aud_loc_datum_hourly_pkey PRIMARY KEY (loc_id, ts_start, source_id)
);

CREATE TABLE solaragg.agg_loc_datum_daily (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  loc_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  jdata_i jsonb,
  jdata_a jsonb,
  jdata_s jsonb,
  jdata_t text[],
  jmeta jsonb,
  CONSTRAINT agg_loc_datum_daily_pkey PRIMARY KEY (loc_id, ts_start, source_id)
);

CREATE TABLE solaragg.agg_loc_datum_monthly (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  loc_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  jdata_i jsonb,
  jdata_a jsonb,
  jdata_s jsonb,
  jdata_t text[],
  jmeta jsonb,
  CONSTRAINT agg_loc_datum_monthly_pkey PRIMARY KEY (loc_id, ts_start, source_id)
);

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_loc_datum_hourly)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarcommon.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_loc_datum_daily)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarcommon.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_loc_datum_monthly)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarcommon.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE VIEW solaragg.agg_loc_datum_hourly_data AS
    SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
    FROM solaragg.agg_loc_datum_hourly d;

CREATE VIEW solaragg.agg_loc_datum_daily_data AS
    SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
    FROM solaragg.agg_loc_datum_daily d;

CREATE VIEW solaragg.agg_loc_datum_monthly_data AS
    SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
    FROM solaragg.agg_loc_datum_monthly d;

CREATE VIEW solaragg.da_loc_datum_avail_hourly AS
WITH loctz AS (
	SELECT l.id AS loc_id, COALESCE(l.time_zone, 'UTC') AS tz
	FROM solarnet.sn_loc l
)
SELECT date_trunc('hour', d.ts at time zone loctz.tz) at time zone loctz.tz AS ts_start, d.loc_id, d.source_id
FROM solardatum.da_loc_datum d
INNER JOIN loctz ON loctz.loc_id = d.loc_id
GROUP BY date_trunc('hour', d.ts at time zone loctz.tz) at time zone loctz.tz, d.loc_id, d.source_id;

CREATE VIEW solaragg.da_loc_datum_avail_daily AS
WITH loctz AS (
	SELECT l.id AS loc_id, COALESCE(l.time_zone, 'UTC') AS tz
	FROM solarnet.sn_loc l
)
SELECT date_trunc('day', d.ts at time zone loctz.tz) at time zone loctz.tz AS ts_start, d.loc_id, d.source_id
FROM solardatum.da_loc_datum d
INNER JOIN loctz ON loctz.loc_id = d.loc_id
GROUP BY date_trunc('day', d.ts at time zone loctz.tz) at time zone loctz.tz, d.loc_id, d.source_id;

CREATE VIEW solaragg.da_loc_datum_avail_monthly AS
WITH loctz AS (
	SELECT l.id AS loc_id, COALESCE(l.time_zone, 'UTC') AS tz
	FROM solarnet.sn_loc l
)
SELECT date_trunc('month', d.ts at time zone loctz.tz) at time zone loctz.tz AS ts_start, d.loc_id, d.source_id
FROM solardatum.da_loc_datum d
INNER JOIN loctz ON loctz.loc_id = d.loc_id
GROUP BY date_trunc('month', d.ts at time zone loctz.tz) at time zone loctz.tz, d.loc_id, d.source_id;

CREATE OR REPLACE FUNCTION solardatum.store_loc_meta(
	cdate timestamp with time zone,
	loc bigint,
	src text,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate timestamp with time zone := now();
	jdata_json jsonb := jdata::jsonb;
BEGIN
	INSERT INTO solardatum.da_loc_meta(loc_id, source_id, created, updated, jdata)
	VALUES (loc, src, cdate, udate, jdata_json)
	ON CONFLICT (loc_id, source_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;

CREATE OR REPLACE FUNCTION solardatum.find_loc_available_sources(
    IN loc bigint,
    IN st timestamp with time zone DEFAULT NULL::timestamp with time zone,
    IN en timestamp with time zone DEFAULT NULL::timestamp with time zone)
  RETURNS TABLE(source_id text) AS
$BODY$
DECLARE
	loc_tz text;
BEGIN
	IF st IS NOT NULL OR en IS NOT NULL THEN
		-- get the node TZ for local date/time
		SELECT l.time_zone FROM solarnet.sn_loc l
		WHERE l.id = loc
		INTO loc_tz;

		IF NOT FOUND THEN
			RAISE NOTICE 'Loc % has no time zone, will use UTC.', node;
			loc_tz := 'UTC';
		END IF;
	END IF;

	CASE
		WHEN st IS NULL AND en IS NULL THEN
			RETURN QUERY SELECT DISTINCT CAST(d.source_id AS text)
			FROM solaragg.agg_loc_datum_daily d
			WHERE d.loc_id = loc;

		WHEN st IS NULL THEN
			RETURN QUERY SELECT DISTINCT CAST(d.source_id AS text)
			FROM solaragg.agg_loc_datum_daily d
			WHERE d.loc_id = loc
				AND d.ts_start >= CAST(st at time zone loc_tz AS DATE);

		ELSE
			RETURN QUERY SELECT DISTINCT CAST(d.source_id AS text)
			FROM solaragg.agg_loc_datum_daily d
			WHERE d.loc_id = loc
				AND d.ts_start >= CAST(st at time zone loc_tz AS DATE)
				AND d.ts_start <= CAST(en at time zone loc_tz AS DATE);
	END CASE;
END;$BODY$
  LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION solardatum.find_loc_reportable_interval(
	IN loc bigint,
	IN src text DEFAULT NULL,
	OUT ts_start timestamp with time zone,
	OUT ts_end timestamp with time zone,
	OUT loc_tz text,
	OUT loc_tz_offset INTEGER)
  RETURNS RECORD AS
$BODY$
BEGIN
	CASE
		WHEN src IS NULL THEN
			SELECT min(ts) FROM solardatum.da_loc_datum WHERE loc_id = loc
			INTO ts_start;
		ELSE
			SELECT min(ts) FROM solardatum.da_loc_datum WHERE loc_id = loc AND source_id = src
			INTO ts_start;
	END CASE;

	CASE
		WHEN src IS NULL THEN
			SELECT max(ts) FROM solardatum.da_loc_datum WHERE loc_id = loc
			INTO ts_end;
		ELSE
			SELECT max(ts) FROM solardatum.da_loc_datum WHERE loc_id = loc AND source_id = src
			INTO ts_end;
	END CASE;

	SELECT
		l.time_zone,
		CAST(EXTRACT(epoch FROM z.utc_offset) / 60 AS INTEGER)
	FROM solarnet.sn_loc l
	INNER JOIN pg_timezone_names z ON z.name = l.time_zone
	WHERE l.id = loc
	INTO loc_tz, loc_tz_offset;

	IF NOT FOUND THEN
		loc_tz := 'UTC';
		loc_tz_offset := 0;
	END IF;

END;$BODY$
  LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION solardatum.find_loc_most_recent(
	loc bigint,
	sources text[] DEFAULT NULL)
  RETURNS SETOF solardatum.da_loc_datum_data AS
$BODY$
BEGIN
	IF sources IS NULL OR array_length(sources, 1) < 1 THEN
		RETURN QUERY
		SELECT dd.* FROM solardatum.da_loc_datum_data dd
		INNER JOIN (
			-- to speed up query for sources (which can be very slow when queried directly on da_loc_datum),
			-- we find the most recent hour time slot in agg_loc_datum_hourly, and then join to da_loc_datum with that narrow time range
			WITH days AS (
				SELECT max(d.ts_start) as ts_start, d.source_id FROM solaragg.agg_loc_datum_hourly d
				INNER JOIN (SELECT solardatum.find_loc_available_sources(loc) AS source_id) AS s ON s.source_id = d.source_id
				WHERE d.loc_id = loc
				GROUP BY d.source_id
			)
			SELECT max(d.ts) as ts, d.source_id FROM solardatum.da_loc_datum d
			INNER JOIN days ON days.source_id = d.source_id
			WHERE d.loc_id = loc
				AND d.ts >= days.ts_start
				AND d.ts < days.ts_start + interval '1 hour'
			GROUP BY d.source_id
		) AS r ON r.ts = dd.ts AND r.source_id = dd.source_id AND dd.loc_id = loc
		ORDER BY dd.source_id ASC;
	ELSE
		RETURN QUERY
		SELECT dd.* FROM solardatum.da_loc_datum_data dd
		INNER JOIN (
			WITH days AS (
				SELECT max(d.ts_start) as ts_start, d.source_id FROM solaragg.agg_loc_datum_hourly d
				INNER JOIN (SELECT unnest(sources) AS source_id) AS s ON s.source_id = d.source_id
				WHERE d. loc_id = loc
				GROUP BY d.source_id
			)
			SELECT max(d.ts) as ts, d.source_id FROM solardatum.da_loc_datum d
			INNER JOIN days ON days.source_id = d.source_id
			WHERE d.loc_id = loc
				AND d.ts >= days.ts_start
				AND d.ts < days.ts_start + interval '1 hour'
			GROUP BY d.source_id
		) AS r ON r.ts = dd.ts AND r.source_id = dd.source_id AND dd.loc_id = loc
		ORDER BY dd.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;

CREATE TRIGGER populate_updated
  BEFORE INSERT OR UPDATE
  ON solardatum.da_loc_meta
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.populate_updated();


/**
 * Find source IDs matching a location metadata search filter.
 *
 * Search filters are specified using LDAP filter syntax, e.g. <code>(/m/foo=bar)</code>.
 *
 * @param locs				array of location IDs
 * @param criteria			the search filter
 *
 * @returns All matching source IDs.
 */
CREATE OR REPLACE FUNCTION solardatum.find_sources_for_loc_meta(
    IN locs bigint[],
    IN criteria text
  )
  RETURNS TABLE(loc_id bigint, source_id text)
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

stmt = plv8.prepare('SELECT loc_id, source_id, jdata FROM solardatum.da_loc_meta WHERE loc_id = ANY($1)', ['bigint[]']);
curs = stmt.cursor([locs]);

while ( rec = curs.fetch() ) {
	meta = rec.jdata;
	matcher = objectPathMatcher(meta);
	if ( matcher.matchesFilter(filter) ) {
		resultRec.loc_id = rec.loc_id;
		resultRec.source_id = rec.source_id;
		plv8.return_next(resultRec);
	}
}

curs.close();
stmt.free();

$BODY$;
