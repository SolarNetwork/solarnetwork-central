CREATE SCHEMA solarnet;

CREATE SEQUENCE solarnet.solarnet_seq;
CREATE SEQUENCE solarnet.node_seq;

CREATE DOMAIN solarnet.created
  AS timestamp with time zone
  DEFAULT CURRENT_TIMESTAMP
  NOT NULL;

CREATE DOMAIN solarnet.pk_i
  AS bigint
  DEFAULT nextval('solarnet.solarnet_seq')
  NOT NULL;

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
   WEATHER / LOCATION
   =========================================================================
   ========================================================================= */

CREATE SEQUENCE solarnet.weather_seq;

CREATE TABLE solarnet.sn_weather_source (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	sname			CHARACTER VARYING(128) NOT NULL,
	fts_default		tsvector,
	PRIMARY KEY(id)
);

CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solarnet.sn_weather_source
  FOR EACH ROW EXECUTE PROCEDURE
  tsvector_update_trigger(fts_default, 'pg_catalog.english', sname);

CREATE INDEX sn_weather_source_fts_default_idx ON solarnet.sn_weather_source USING gin(fts_default);

/* --- sn_weather_loc */

CREATE TABLE solarnet.sn_weather_loc (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	loc_id			BIGINT NOT NULL,
	source_id		BIGINT NOT NULL,
	source_data		CHARACTER VARYING(128),
	fts_default		tsvector,
	PRIMARY KEY (id),
	CONSTRAINT sn_weather_location_sn_loc_fk FOREIGN KEY (loc_id)
		REFERENCES solarnet.sn_loc (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT sn_weather_location_sn_weather_source_fk FOREIGN KEY (source_id)
		REFERENCES solarnet.sn_weather_source (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solarnet.sn_weather_loc
  FOR EACH ROW EXECUTE PROCEDURE
  tsvector_update_trigger(fts_default, 'pg_catalog.english', source_data);

CREATE INDEX sn_weather_loc_fts_default_idx ON solarnet.sn_weather_loc USING gin(fts_default);

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
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT sn_node_weather_loc_fk FOREIGN KEY (wloc_id)
		REFERENCES solarnet.sn_weather_loc (id)
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

/******************************************************************************
 * TABLE solarnet.sn_node_meta
 *
 * Stores JSON metadata specific to a node.
 */
CREATE TABLE solarnet.sn_node_meta (
  node_id 			solarcommon.node_id NOT NULL,
  created 			solarcommon.ts NOT NULL,
  updated 			solarcommon.ts NOT NULL,
  jdata 			json NOT NULL,
  CONSTRAINT sn_node_meta_pkey PRIMARY KEY (node_id)  DEFERRABLE INITIALLY IMMEDIATE,
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
	cdate solarcommon.ts,
	node solarcommon.node_id,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate solarcommon.ts := now();
	jdata_json json := jdata::json;
BEGIN
	INSERT INTO solarnet.sn_node_meta(node_id, created, updated, jdata)
	VALUES (node, cdate, udate, jdata_json)
	ON CONFLICT (node_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;

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


/* =========================================================================
   =========================================================================
   PRICE
   =========================================================================
   ========================================================================= */

CREATE SEQUENCE solarnet.price_seq;

CREATE TABLE solarnet.sn_price_source (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	sname			CHARACTER VARYING(128) NOT NULL,
	fts_default		tsvector,
	PRIMARY KEY(id)
);

CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solarnet.sn_price_source
  FOR EACH ROW EXECUTE PROCEDURE
  tsvector_update_trigger(fts_default, 'pg_catalog.english', sname);

CREATE INDEX sn_price_source_fts_default_idx ON solarnet.sn_price_source USING gin(fts_default);

CREATE TABLE solarnet.sn_price_loc (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	loc_id			BIGINT NOT NULL,
	loc_name		CHARACTER VARYING(128) NOT NULL UNIQUE,
	source_id		BIGINT NOT NULL,
	source_data		CHARACTER VARYING(128),
	currency		VARCHAR(10) NOT NULL,
	unit			VARCHAR(20) NOT NULL,
	fts_default		tsvector,
	PRIMARY KEY (id),
    CONSTRAINT sn_price_loc_loc_fk FOREIGN KEY (loc_id)
  	    REFERENCES solarnet.sn_loc (id) MATCH SIMPLE
	    ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT sn_price_loc_sn_price_source_fk FOREIGN KEY (source_id)
		REFERENCES solarnet.sn_price_source (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solarnet.sn_price_loc
  FOR EACH ROW EXECUTE PROCEDURE
  tsvector_update_trigger(fts_default, 'pg_catalog.english', loc_name, source_data, currency);

CREATE INDEX sn_price_loc_fts_default_idx ON solarnet.sn_price_loc USING gin(fts_default);
