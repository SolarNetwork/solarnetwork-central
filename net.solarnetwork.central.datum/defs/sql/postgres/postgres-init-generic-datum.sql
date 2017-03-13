CREATE SCHEMA IF NOT EXISTS solardatum;

CREATE SCHEMA IF NOT EXISTS solaragg;

CREATE TABLE solardatum.da_datum (
  ts solarcommon.ts NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  posted solarcommon.ts NOT NULL,
  jdata json NOT NULL,
  CONSTRAINT da_datum_pkey PRIMARY KEY (node_id, ts, source_id) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE TABLE solardatum.da_meta (
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  created solarcommon.ts NOT NULL,
  updated solarcommon.ts NOT NULL,
  jdata json NOT NULL,
  CONSTRAINT da_meta_pkey PRIMARY KEY (node_id, source_id)
);

CREATE TABLE solaragg.agg_stale_datum (
  ts_start timestamp with time zone NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  agg_kind char(1) NOT NULL,
  created timestamp NOT NULL DEFAULT now(),
  CONSTRAINT agg_stale_datum_pkey PRIMARY KEY (agg_kind, node_id, ts_start, source_id)
);

CREATE TABLE solaragg.agg_messages (
  created timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  ts solarcommon.ts NOT NULL,
  msg text NOT NULL
);

CREATE INDEX agg_messages_ts_node_idx ON solaragg.agg_messages (ts, node_id);

CREATE TABLE solaragg.agg_datum_hourly (
  ts_start timestamp with time zone NOT NULL,
  local_date timestamp without time zone NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  jdata json NOT NULL,
 CONSTRAINT agg_datum_hourly_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CREATE TABLE solaragg.aud_datum_hourly (
  ts_start timestamp with time zone NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  prop_count integer NOT NULL,
  CONSTRAINT aud_datum_hourly_pkey PRIMARY KEY (node_id, ts_start, source_id) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE TABLE solaragg.agg_datum_daily (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  jdata json NOT NULL,
 CONSTRAINT agg_datum_daily_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CREATE TABLE solaragg.agg_datum_monthly (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  jdata json NOT NULL,
 CONSTRAINT agg_datum_monthly_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

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
	cdate solarcommon.ts,
	node solarcommon.node_id,
	src solarcommon.source_id,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate solarcommon.ts := now();
	jdata_json json := jdata::json;
BEGIN
	INSERT INTO solardatum.da_meta(node_id, source_id, created, updated, jdata)
	VALUES (node, src, cdate, udate, jdata_json)
	ON CONFLICT (node_id, source_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;

CREATE OR REPLACE FUNCTION solardatum.find_available_sources(
	IN node solarcommon.node_id,
	IN st solarcommon.ts DEFAULT NULL,
	IN en solarcommon.ts DEFAULT NULL)
  RETURNS TABLE(source_id solarcommon.source_id) AS
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
			RETURN QUERY SELECT DISTINCT d.source_id
			FROM solaragg.agg_datum_daily d
			WHERE d.node_id = node
			ORDER BY d.source_id;

		WHEN en IS NULL THEN
			RETURN QUERY SELECT DISTINCT d.source_id
			FROM solaragg.agg_datum_daily d
			WHERE d.node_id = node
				AND d.ts_start >= CAST(st at time zone node_tz AS DATE)
			ORDER BY d.source_id;

		WHEN st IS NULL THEN
			RETURN QUERY SELECT DISTINCT d.source_id
			FROM solaragg.agg_datum_daily d
			WHERE d.node_id = node
				AND d.ts_start <= CAST(en at time zone node_tz AS DATE)
			ORDER BY d.source_id;

		ELSE
			RETURN QUERY SELECT DISTINCT d.source_id
			FROM solaragg.agg_datum_daily d
			WHERE d.node_id = node
				AND d.ts_start >= CAST(st at time zone node_tz AS DATE)
				AND d.ts_start <= CAST(en at time zone node_tz AS DATE)
			ORDER BY d.source_id;
	END CASE;
END;$BODY$
  LANGUAGE plpgsql STABLE ROWS 50;

CREATE OR REPLACE FUNCTION solardatum.find_reportable_interval(
	IN node solarcommon.node_id,
	IN src solarcommon.source_id DEFAULT NULL,
	OUT ts_start solarcommon.ts,
	OUT ts_end solarcommon.ts,
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
  RETURNS TABLE(node_id solarcommon.node_id, source_id solarcommon.source_id)
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
CREATE OR REPLACE FUNCTION solardatum.datum_prop_count(IN jdata json)
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
