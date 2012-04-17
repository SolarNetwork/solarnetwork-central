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
	loc_name		CHARACTER VARYING(128) NOT NULL,
	country			CHARACTER(2) NOT NULL,
	time_zone		CHARACTER VARYING(64) NOT NULL,
	region			CHARACTER VARYING(128),
	state_prov		CHARACTER VARYING(128),
	locality		CHARACTER VARYING(128),
	postal_code		CHARACTER VARYING(32),
	address			CHARACTER VARYING(256),
	latitude		DOUBLE PRECISION,
	longitude		DOUBLE PRECISION,
	fts_default 	tsvector,
	PRIMARY KEY (id)
);

CREATE INDEX sn_loc_fts_default_idx ON solarnet.sn_loc USING gin(fts_default);

CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solarnet.sn_loc
  FOR EACH ROW EXECUTE PROCEDURE 
  tsvector_update_trigger(fts_default, 'pg_catalog.english', 
  	loc_name, country, region, state_prov, locality, postal_code, address);

/* =========================================================================
   =========================================================================
   WEATHER / LOCATION
   =========================================================================
   ========================================================================= */

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

/* --- sn_day_datum */

CREATE TABLE solarnet.sn_day_datum (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	loc_id 			BIGINT NOT NULL,
	day		 		DATE NOT NULL,
	sunrise			TIME NOT NULL,
	sunset			TIME NOT NULL,
	temperature_h	REAL,
	temperature_l	REAL,
	sky				TEXT,
	PRIMARY KEY (id),
	CONSTRAINT sn_day_datum_weather_loc_fk
		FOREIGN KEY (loc_id) REFERENCES solarnet.sn_weather_loc (id) 
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT sn_day_datum_loc_unq UNIQUE (day, loc_id)
);

CREATE INDEX day_datum_created_idx ON solarnet.sn_day_datum (created);

CLUSTER solarnet.sn_day_datum USING sn_day_datum_loc_unq;

/* --- sn_weather_datum */

CREATE TABLE solarnet.sn_weather_datum (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	loc_id 			BIGINT NOT NULL,
	info_date		TIMESTAMP WITH TIME ZONE NOT NULL,
	temperature		REAL NOT NULL,
	sky				TEXT,
	humidity		REAL,
	bar				REAL,
	bar_dir			TEXT,
	visibility		TEXT,
	uv_index		INTEGER,
	dew				REAL,
	PRIMARY KEY (id),
	CONSTRAINT sn_weather_datum_weather_loc_fk
		FOREIGN KEY (loc_id) REFERENCES solarnet.sn_weather_loc (id) 
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT sn_weather_datum_loc_unq UNIQUE (info_date, loc_id)
);

CREATE INDEX weather_datum_created_idx ON solarnet.sn_weather_datum (created);

CLUSTER solarnet.sn_weather_datum USING sn_weather_datum_loc_unq;

/* --- Weather stored procedures / triggers */

CREATE OR REPLACE FUNCTION solarnet.populate_high_low_temperature(datum solarnet.sn_weather_datum)
  RETURNS void AS
$BODY$
DECLARE
	node_tz text;
	dat record;
BEGIN
	SELECT l.time_zone from solarnet.sn_loc l 
	INNER JOIN solarnet.sn_weather_loc w ON w.loc_id = l.id 
	WHERE w.id = datum.loc_id
	INTO node_tz;
	
	SELECT 
		date(w.info_date at time zone node_tz) as day,
		min(w.temperature) as temp_min,
		max(w.temperature) as temp_max
	FROM solarnet.sn_weather_datum w
	WHERE 
		w.loc_id = datum.loc_id
		AND w.info_date >= date_trunc('day', datum.info_date at time zone node_tz) at time zone node_tz
		AND w.info_date < date_trunc('day', datum.info_date at time zone node_tz) at time zone node_tz + interval '1 day'
	GROUP BY date(w.info_date at time zone node_tz)
	INTO dat;

	IF FOUND THEN
		--RAISE NOTICE 'Got temperature data: %', dat;
		-- we can only update here, not insert
		UPDATE solarnet.sn_day_datum SET
			temperature_l = dat.temp_min, 
			temperature_h = dat.temp_max
		WHERE day = dat.day
			AND loc_id = datum.loc_id;
	END IF;

	-- for sn_day_datum.sky, select most frequently occuring sn_weather_datum.sky 
	-- for the daylight hours of the given day, using sn_day_datum.sunrise/sunset
	SELECT sub.day, sub.sky from (
		SELECT 
			date(w.info_date at time zone node_tz) as day,
			w.sky, 
			count(*) as cnt
		FROM solarnet.sn_weather_datum w
		INNER JOIN solarnet.sn_day_datum d on d.day = date(datum.info_date at time zone node_tz)
		WHERE 
			d.loc_id = datum.loc_id
			AND w.loc_id = datum.loc_id
			AND w.info_date >= (date_trunc('day', datum.info_date at time zone node_tz) + d.sunrise) at time zone node_tz
			AND w.info_date < (date_trunc('day', datum.info_date at time zone node_tz) + d.sunset) at time zone node_tz
			AND w.sky <> 'N/A'
			and w.sky <> ''
			AND w.sky IS NOT NULL
		GROUP BY 
			date(w.info_date at time zone node_tz),
			w.sky
	) as sub ORDER BY sub.cnt DESC LIMIT 1
	INTO dat;

	IF FOUND THEN
		--RAISE NOTICE 'Got weather data: %', dat;
		-- we can only update here, not insert
		UPDATE solarnet.sn_day_datum SET
			sky = dat.sky
		WHERE day = dat.day
			AND loc_id = datum.loc_id;
	END IF;

END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION solarnet.populate_hl_temperature()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM solarnet.populate_high_low_temperature(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_high_low_temperature
  AFTER INSERT OR UPDATE
  ON solarnet.sn_weather_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarnet.populate_hl_temperature();

/**************************************************************************************************
 * FUNCTION solarnet.find_near_sky_condition(datum solarnet.sn_weather_datum)
 * 
 * Tries to find in a "near" sky value when the passed in value is N/A or blank.
 * Seems that weather.com data often alternates between a valid value and N/A,
 * so this will over-write this with a "near" value so we don't see so many N/A's.
 */
CREATE OR REPLACE FUNCTION solarnet.find_near_sky_condition(datum solarnet.sn_weather_datum)
  RETURNS text AS
$BODY$
DECLARE
	prev_sky text;
BEGIN
	IF datum.sky <> 'N/A' AND datum.sky <> '' THEN
		RETURN NULL;
	END IF;
	
	SELECT w.sky FROM solarnet.sn_weather_datum w
	WHERE w.loc_id = datum.loc_id
		AND w.info_date < datum.info_date
		AND w.info_date >= datum.info_date - interval '5 hours'
		AND w.sky <> 'N/A'
		AND w.sky <> ''
		AND w.sky IS NOT NULL
	ORDER BY w.info_date DESC, w.created DESC LIMIT 1
	INTO prev_sky;
	
	RETURN prev_sky;
	
END;$BODY$
  LANGUAGE 'plpgsql' STABLE;

CREATE OR REPLACE FUNCTION solarnet.populate_near_sky_condition()
  RETURNS "trigger" AS
$BODY$
DECLARE
	nsky text;
BEGIN
	SELECT solarnet.find_near_sky_condition(NEW) INTO nsky;
	IF nsky IS NOT NULL THEN
		NEW.sky := nsky;
	END IF;
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_near_sky_condition
  BEFORE INSERT OR UPDATE
  ON solarnet.sn_weather_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarnet.populate_near_sky_condition();

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

CREATE OR REPLACE FUNCTION solarnet.get_node_local_timestamp(timestamp with time zone, bigint)
  RETURNS timestamp without time zone AS
$BODY$
	SELECT $1 AT TIME ZONE l.time_zone
	FROM solarnet.sn_node n
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE n.node_id = $2
$BODY$
  LANGUAGE 'sql' STABLE;

/* =========================================================================
   =========================================================================
   PRICE
   =========================================================================
   ========================================================================= */

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
	loc_name		CHARACTER VARYING(128) NOT NULL UNIQUE,
	source_id		BIGINT NOT NULL,
	source_data		CHARACTER VARYING(128),
	currency		VARCHAR(10) NOT NULL,
	unit			VARCHAR(20) NOT NULL,
	time_zone		VARCHAR(64) NOT NULL,
	fts_default		tsvector,
	PRIMARY KEY (id),
	CONSTRAINT sn_price_loc_sn_price_source_fk FOREIGN KEY (source_id)
		REFERENCES solarnet.sn_price_source (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solarnet.sn_price_loc
  FOR EACH ROW EXECUTE PROCEDURE 
  tsvector_update_trigger(fts_default, 'pg_catalog.english', loc_name, source_data, currency);

CREATE INDEX sn_price_loc_fts_default_idx ON solarnet.sn_price_loc USING gin(fts_default);

CREATE TABLE solarnet.sn_price_datum (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	posted			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	loc_id 			BIGINT NOT NULL,
	price			REAL,
	prev_datum		BIGINT,
	PRIMARY KEY (id),
    CONSTRAINT price_datum_unq UNIQUE (created, loc_id),
	CONSTRAINT sn_price_datum_price_loc_fk FOREIGN KEY (loc_id)
		REFERENCES solarnet.sn_price_loc (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX price_datum_prev_datum_idx ON solarnet.sn_price_datum (prev_datum);

-- this index is used for foreign key validation in other tables
CREATE INDEX price_datum_loc_idx ON solarnet.sn_price_datum (loc_id,created);

CLUSTER solarnet.sn_price_datum USING price_datum_unq;

CREATE OR REPLACE FUNCTION solarnet.find_prev_price_datum(solarnet.sn_price_datum, interval)
  RETURNS bigint AS
$BODY$
	SELECT c.id
	FROM solarnet.sn_price_datum c
	WHERE c.created < $1.created
		AND c.created >= ($1.created - $2)
		AND c.loc_id = $1.loc_id
	ORDER BY c.created DESC
	LIMIT 1;
$BODY$
  LANGUAGE 'sql' STABLE;

CREATE OR REPLACE FUNCTION solarnet.populate_prev_price_datum()
  RETURNS "trigger" AS
$BODY$
BEGIN
	NEW.prev_datum := solarnet.find_prev_price_datum(NEW, interval '1 hour');
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_prev_price_datum
  BEFORE INSERT
  ON solarnet.sn_price_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarnet.populate_prev_price_datum();

/**************************************************************************************************
 * FUNCTION solarnet.calc_price_per_watt_hours(real, text)
 * 
 * Calculate a price per watt hour from a price and price unit.
 * 
 * @param real			price
 * @param text			price unit (could be MWh, kWh, etc)
 */
CREATE OR REPLACE FUNCTION solarnet.calc_price_per_watt_hours(
	IN real, IN text)
RETURNS DOUBLE PRECISION AS
$BODY$
	SELECT $1 * (CASE $2 WHEN 'MWh' THEN 10^(-6) WHEN 'kWh' THEN 10^(-3) ELSE 1 END)
$BODY$
LANGUAGE 'sql' IMMUTABLE;

/* =========================================================================
   =========================================================================
   POWER
   =========================================================================
   ========================================================================= */

CREATE TABLE solarnet.sn_power_datum (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	posted			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	node_id 		BIGINT NOT NULL,
	source_id 		VARCHAR(255) NOT NULL DEFAULT '',
	price_loc_id	BIGINT,
	watts 			INTEGER,
	bat_volts		REAL,
	bat_amp_hrs		REAL,
	watt_hour		BIGINT,
	prev_datum		BIGINT,
	local_created	TIMESTAMP WITHOUT TIME ZONE NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT sn_power_datum_node_fk
		FOREIGN KEY (node_id) REFERENCES solarnet.sn_node (node_id)
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT sn_power_datum_price_loc_fk
		FOREIGN KEY (price_loc_id) REFERENCES solarnet.sn_price_loc (id)
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT power_datum_node_unq UNIQUE (created, node_id, source_id)
);

CREATE INDEX power_datum_prev_datum_idx ON solarnet.sn_power_datum (prev_datum);
CREATE INDEX power_datum_local_created_idx ON solarnet.sn_power_datum (local_created);

-- this index is used for foreign key validation in other tables
CREATE INDEX power_datum_node_idx ON solarnet.sn_power_datum (node_id,created);

CLUSTER solarnet.sn_power_datum USING power_datum_node_unq;

CREATE OR REPLACE FUNCTION solarnet.find_prev_power_datum(solarnet.sn_power_datum, interval)
  RETURNS bigint AS
$BODY$
	SELECT c.id
	FROM solarnet.sn_power_datum c
	WHERE c.created < $1.created
		AND c.created >= ($1.created - $2)
		AND c.node_id = $1.node_id
		AND c.source_id = $1.source_id
	ORDER BY c.created DESC
	LIMIT 1;
$BODY$
  LANGUAGE 'sql' STABLE;

COMMENT ON FUNCTION solarnet.find_prev_power_datum(solarnet.sn_power_datum, interval) IS 
'To manually update power datum, use a query like:

update solarnet.sn_power_datum set prev_datum = solarnet.find_prev_power_datum(sn_power_datum, 
	interval ''1 hour'');';

CREATE OR REPLACE FUNCTION solarnet.populate_prev_power_datum()
  RETURNS "trigger" AS
$BODY$
BEGIN
	NEW.prev_datum := solarnet.find_prev_power_datum(NEW, interval '1 hour');
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_prev_power_datum
  BEFORE INSERT
  ON solarnet.sn_power_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarnet.populate_prev_power_datum();

CREATE OR REPLACE FUNCTION solarnet.populate_local_created()
  RETURNS trigger AS
$BODY$
BEGIN
	NEW.local_created := solarnet.get_node_local_timestamp(NEW.created, NEW.node_id);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_local_created
   BEFORE INSERT OR UPDATE
   ON solarnet.sn_power_datum FOR EACH ROW
   EXECUTE PROCEDURE solarnet.populate_local_created();
   
/**************************************************************************************************
 * FUNCTION solarnet.calc_avg_watt_hours(integer, integer, double precision, 
 *                                       double precision, interval)
 * 
 * Calculate average watt hours between two watt values and a time interval. If
 * the Wh parameters are not null, the direct difference between these two values is 
 * returned instead of the average calculation.
 * 
 * @param integer			ending watt
 * @param integer			starting watt
 * @param double precision	ending Wh
 * @param double precision	starting Wh
 * @param interval			time interval
 */
CREATE OR REPLACE FUNCTION solarnet.calc_avg_watt_hours(integer, integer, double precision, double precision, interval)
  RETURNS double precision AS
$BODY$
	SELECT CASE 
			WHEN 
				-- Wh readings available, so use difference in Wh if end value > start value, or
				-- end value > 10, and dt is small and the % change is less than 10%, in case of anomaly e.g. NET-19
				$3 IS NOT NULL AND $4 IS NOT NULL AND (
					$3 >= $4 OR 
					($3 > 10.0 AND $5 < interval '30 minutes' AND ($4 - $3) / $3 < 0.1))
				THEN $3 - $4
			WHEN 
				-- end Wh value less than start: assume day reset on inverter and just take end value
				$3 IS NOT NULL AND $4 IS NOT NULL
				THEN $3
			ELSE 
				-- Wh not available, so calculate Wh using (watts * dt)
				ABS(($1 + $2) / 2) * ((extract('epoch' from $5)) / 3600)
		END
$BODY$
  LANGUAGE sql IMMUTABLE;

/* =========================================================================
   =========================================================================
   CONSUMPTION
   =========================================================================
   ========================================================================= */

CREATE TABLE solarnet.sn_consum_datum (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	posted			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	node_id 		BIGINT NOT NULL,
	source_id 		VARCHAR(255) NOT NULL,
	price_loc_id	BIGINT,
	watts			INTEGER,
	watt_hour		BIGINT,
	prev_datum		BIGINT,
	PRIMARY KEY (id),
	CONSTRAINT sn_consum_datum_node_fk
		FOREIGN KEY (node_id) REFERENCES solarnet.sn_node (node_id)
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT sn_consum_datum_price_loc_fk
		FOREIGN KEY (price_loc_id) REFERENCES solarnet.sn_price_loc (id)
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT consum_datum_node_unq UNIQUE (created,node_id,source_id)
);

CREATE INDEX consum_datum_prev_datum_idx ON solarnet.sn_consum_datum (prev_datum);

-- this index is used for foreign key validation in other tables
CREATE INDEX consum_datum_node_idx ON solarnet.sn_consum_datum (node_id,created);

CLUSTER solarnet.sn_consum_datum USING consum_datum_node_unq;

CREATE OR REPLACE FUNCTION solarnet.find_prev_consum_datum(solarnet.sn_consum_datum, interval)
  RETURNS bigint AS
$BODY$
	SELECT c.id
	FROM solarnet.sn_consum_datum c
	WHERE c.created < $1.created
		AND c.created >= ($1.created - $2)
		AND c.node_id = $1.node_id
		AND c.source_id = $1.source_id
	ORDER BY c.created DESC
	LIMIT 1;
$BODY$
  LANGUAGE 'sql' STABLE;

CREATE OR REPLACE FUNCTION solarnet.populate_prev_consum_datum()
  RETURNS "trigger" AS
$BODY$
BEGIN
	NEW.prev_datum := solarnet.find_prev_consum_datum(NEW, interval '1 hour');
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_prev_consum_datum
  BEFORE INSERT
  ON solarnet.sn_consum_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarnet.populate_prev_consum_datum();
