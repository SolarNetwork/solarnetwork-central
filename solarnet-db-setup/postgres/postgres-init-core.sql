CREATE SEQUENCE solarnet.solarnet_seq;
CREATE SEQUENCE solarnet.node_seq;

/* =========================================================================
   =========================================================================
   LOCATION
   =========================================================================
   ========================================================================= */

CREATE TABLE solarnet.sn_loc (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	country			CHARACTER VARYING(2) NOT NULL,
	time_zone		CHARACTER VARYING(64) NOT NULL,
	region			CHARACTER VARYING(128),
	state_prov		CHARACTER VARYING(128),
	locality		CHARACTER VARYING(128),
	postal_code		CHARACTER VARYING(32),
	address			CHARACTER VARYING(256),
	latitude		NUMERIC(9,6),
	longitude		NUMERIC(9,6),
	elevation		NUMERIC(8,3),
	fts_default 	tsvector,
	PRIMARY KEY (id)
);

CREATE INDEX sn_loc_fts_default_idx ON solarnet.sn_loc USING gin(fts_default);

CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solarnet.sn_loc
  FOR EACH ROW EXECUTE PROCEDURE
  tsvector_update_trigger(fts_default, 'pg_catalog.english',
  	country, region, state_prov, locality, postal_code, address);

/* =========================================================================
   =========================================================================
   NODE
   =========================================================================
   ========================================================================= */

CREATE TABLE solarnet.sn_node (
	node_id			BIGINT NOT NULL DEFAULT nextval('solarnet.node_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	loc_id			BIGINT NOT NULL,
	wloc_id			BIGINT,
	node_name		CHARACTER VARYING(128),
	PRIMARY KEY (node_id),
	CONSTRAINT sn_node_loc_fk FOREIGN KEY (loc_id)
		REFERENCES solarnet.sn_loc (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

/******************************************************************************
 * VIEW solarnet.node_local_time
 *
 * View of node time zones and local time information. The local_ts and
 * local_hour_of_day columns are based on the current (transaction) time.
 */
CREATE OR REPLACE VIEW solarnet.node_local_time AS
	SELECT n.node_id,
		COALESCE(l.time_zone, 'UTC'::character varying(64)) AS time_zone,
		CURRENT_TIMESTAMP AT TIME ZONE COALESCE(l.time_zone, 'UTC') AS local_ts,
		EXTRACT(HOUR FROM CURRENT_TIMESTAMP AT TIME ZONE COALESCE(l.time_zone, 'UTC'))::integer AS local_hour_of_day
	FROM solarnet.sn_node n
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id;

/**
 * Calculate the minimum number of absolute time spans required for a given set of nodes.
 *
 * The time zones of each node are used to group them into rows where all nodes have the
 * same absolute start/end dates.
 * 
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date
 * @param ts_max the ending local date
 */
CREATE OR REPLACE FUNCTION solarnet.node_source_time_ranges_local(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_ids bigint[],
  source_ids character varying(64)[]
) LANGUAGE sql STABLE AS $$
	SELECT ts_min AT TIME ZONE nlt.time_zone AS sdate,
		ts_max AT TIME ZONE nlt.time_zone AS edate,
		nlt.time_zone AS time_zone,
		array_agg(DISTINCT nlt.node_id) AS nodes,
		array_agg(DISTINCT s.source_id::character varying(64)) FILTER (WHERE s.source_id IS NOT NULL) AS sources
	FROM solarnet.node_local_time nlt
	LEFT JOIN (
		SELECT unnest(sources) AS source_id
	) s ON TRUE
	WHERE nlt.node_id = ANY(nodes)
	GROUP BY time_zone
$$;


/**
 * Calculate the minimum number of absolute rounded dates for a given set of nodes.
 *
 * The time zones of each node are used to group them into rows where all nodes have the
 * same absolute dates.
 * 
 * @param nodes   the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param field   the Postgres date_trunc compatible field to truncate the date on, e.g. 'hour', 'day', 'month', etc.
 * @param ts      the date to truncate
 */
CREATE OR REPLACE FUNCTION solarnet.node_source_time_rounded(
	nodes bigint[], sources text[], field text , ts timestamptz)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_ids bigint[],
  source_ids character varying(64)[]
) LANGUAGE sql STABLE AS $$
	SELECT 
		date_trunc(field, ts AT TIME ZONE nlt.time_zone) AT TIME ZONE nlt.time_zone AS ts_start
		, (date_trunc(field, ts AT TIME ZONE nlt.time_zone) + ('1 '||field)::interval) AT TIME ZONE nlt.time_zone AS ts_end
		, nlt.time_zone AS time_zone
		, array_agg(DISTINCT nlt.node_id) AS nodes
		, array_agg(DISTINCT s.source_id::character varying(64)) FILTER (WHERE s.source_id IS NOT NULL) AS sources
	FROM solarnet.node_local_time nlt
	LEFT JOIN (
		SELECT unnest(sources) AS source_id
	) s ON TRUE
	WHERE nlt.node_id = ANY(nodes)
	GROUP BY time_zone
$$;


/**
 * Calculate the minimum number of absolute rounded date ranges for a given set of nodes.
 *
 * The time zones of each node are used to group them into rows where all nodes have the
 * same absolute dates.
 * 
 * @param nodes   the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param field   the Postgres date_trunc compatible field to truncate the date on, e.g. 'hour', 'day', 'month', etc.
 * @param ts_min  the start date to truncate
 * @param ts_max  the end date to truncate
 */
CREATE OR REPLACE FUNCTION solarnet.node_source_time_rounded(
	nodes bigint[], sources text[], field text , ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_ids bigint[],
  source_ids character varying(64)[]
) LANGUAGE sql STABLE AS $$
	SELECT 
		date_trunc(field, ts_min) AT TIME ZONE nlt.time_zone AS ts_start
		, date_trunc(field, ts_max) AT TIME ZONE nlt.time_zone AS ts_end
		, nlt.time_zone AS time_zone
		, array_agg(DISTINCT nlt.node_id) AS nodes
		, array_agg(DISTINCT s.source_id::character varying(64)) FILTER (WHERE s.source_id IS NOT NULL) AS sources
	FROM solarnet.node_local_time nlt
	LEFT JOIN (
		SELECT unnest(sources) AS source_id
	) s ON TRUE
	WHERE nlt.node_id = ANY(nodes)
	GROUP BY time_zone;
$$;


/******************************************************************************
 * TABLE solarnet.sn_node_meta
 *
 * Stores JSON metadata specific to a node.
 */
CREATE TABLE solarnet.sn_node_meta (
  node_id 			bigint NOT NULL,
  created 			timestamp with time zone NOT NULL,
  updated 			timestamp with time zone NOT NULL,
  jdata				jsonb NOT NULL,
  CONSTRAINT sn_node_meta_pkey PRIMARY KEY (node_id),
  CONSTRAINT sn_node_meta_node_fk FOREIGN KEY (node_id)
        REFERENCES solarnet.sn_node (node_id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE
);

/******************************************************************************
 * FUNCTION solarnet.store_node_meta(timestamptz, bigint, text)
 *
 * Add or update node metadata.
 *
 * @param cdate the creation date to use
 * @param node the node ID
 * @param jdata the metadata to store
 */
CREATE OR REPLACE FUNCTION solarnet.store_node_meta(
	cdate timestamp with time zone,
	node bigint,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate timestamp with time zone := now();
	jdata_json jsonb := jdata::jsonb;
BEGIN
	INSERT INTO solarnet.sn_node_meta(node_id, created, updated, jdata)
	VALUES (node, cdate, udate, jdata_json)
	ON CONFLICT (node_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;


/**
 * Find node IDs matching a metadata search filter.
 *
 * Search filters are specified using LDAP filter syntax, e.g. <code>(/m/foo=bar)</code>.
 *
 * @param nodes				array of node IDs
 * @param criteria			the search filter
 *
 * @returns All matching node IDs.
 */
CREATE OR REPLACE FUNCTION solarnet.find_nodes_for_meta(nodes bigint[], criteria text)
  RETURNS TABLE(node_id bigint)
  LANGUAGE plv8 ROWS 100 STABLE AS
$$
'use strict';

var objectPathMatcher = require('util/objectPathMatcher').default,
	searchFilter = require('util/searchFilter').default;

var filter = searchFilter(criteria),
	stmt,
	curs,
	rec,
	meta,
	matcher;

if ( !filter.rootNode ) {
	plv8.elog(NOTICE, 'Malformed search filter:', criteria);
	return;
}

stmt = plv8.prepare('SELECT node_id, jdata FROM solarnet.sn_node_meta WHERE node_id = ANY($1)', ['bigint[]']);
curs = stmt.cursor([nodes]);

while ( rec = curs.fetch() ) {
	meta = rec.jdata;
	matcher = objectPathMatcher(meta);
	if ( matcher.matchesFilter(filter) ) {
		plv8.return_next(rec.node_id);
	}
}

curs.close();
stmt.free();
$$;


CREATE OR REPLACE FUNCTION solarnet.get_node_local_timestamp(timestamp with time zone, bigint)
  RETURNS timestamp without time zone AS
$BODY$
	SELECT $1 AT TIME ZONE l.time_zone
	FROM solarnet.sn_node n
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE n.node_id = $2
$BODY$
  LANGUAGE 'sql' STABLE;

/******************************************************************************
 * FUNCTION solarnet.get_node_timezone(bigint)
 *
 * Return a node's time zone.
 *
 * @param bigint the node ID
 * @return time zone name, e.g. 'Pacific/Auckland'
 */
CREATE OR REPLACE FUNCTION solarnet.get_node_timezone(bigint)
  RETURNS text AS
$BODY$
	SELECT l.time_zone
	FROM solarnet.sn_node n
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE n.node_id = $1
$BODY$
  LANGUAGE 'sql' STABLE;

/* =========================================================================
   =========================================================================
   SEASON SUPPORT
   =========================================================================
   ========================================================================= */

/**************************************************************************************************
 * FUNCTION solarnet.get_season(date)
 *
 * Assign a "season" number to a date. Seasons are defined as:
 *
 * Dec,Jan,Feb = 0
 * Mar,Apr,May = 1
 * Jun,Jul,Aug = 2
 * Sep,Oct,Nov = 3
 *
 * @param date the date to calcualte the season for
 * @returns integer season constant
 */
CREATE OR REPLACE FUNCTION solarnet.get_season(date)
RETURNS INTEGER AS
$BODY$
	SELECT
	CASE EXTRACT(MONTH FROM $1)
		WHEN 12 THEN 0
		WHEN 1 THEN 0
		WHEN 2 THEN 0
		WHEN 3 THEN 1
		WHEN 4 THEN 1
		WHEN 5 THEN 1
		WHEN 6 THEN 2
		WHEN 7 THEN 2
		WHEN 8 THEN 2
		WHEN 9 THEN 3
		WHEN 10 THEN 3
		WHEN 11 THEN 3
	END AS season
$BODY$
  LANGUAGE 'sql' IMMUTABLE;


/**************************************************************************************************
 * FUNCTION solarnet.get_season_monday_start(date)
 *
 * Returns a date representing the first Monday within the provide date's season, where season
 * is defined by the solarnet.get_season(date) function. The actual returned date is meaningless
 * other than it will be a Monday and will be within the appropriate season.
 *
 * @param date the date to calcualte the Monday season date for
 * @returns date representing the first Monday within the season
 * @see solarnet.get_season(date)
 */
CREATE OR REPLACE FUNCTION solarnet.get_season_monday_start(date)
RETURNS DATE AS
$BODY$
	SELECT
	CASE solarnet.get_season($1)
		WHEN 0 THEN DATE '2000-12-04'
		WHEN 1 THEN DATE '2001-03-05'
		WHEN 2 THEN DATE '2001-06-04'
		ELSE DATE '2001-09-03'
  END AS season_monday
$BODY$
  LANGUAGE 'sql' IMMUTABLE;
