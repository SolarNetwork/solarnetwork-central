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
	PRIMARY KEY (id)
);

INSERT INTO solarnet.sn_loc (id, created, loc_name, country, region, time_zone, latitude, longitude)
SELECT id, created, loc_name, country, region, time_zone, latitude, longitude
FROM solarnet.sn_weather_loc;

UPDATE solarnet.sn_loc SET time_zone = 'GMT' WHERE time_zone = '--';

ALTER TABLE solarnet.sn_weather_loc ADD COLUMN loc_id BIGINT;
ALTER TABLE solarnet.sn_weather_loc ADD 
	CONSTRAINT sn_weather_location_sn_loc_fk FOREIGN KEY (loc_id)
		REFERENCES solarnet.sn_loc (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION;
UPDATE solarnet.sn_weather_loc SET loc_id = id;
ALTER TABLE solarnet.sn_weather_loc DROP COLUMN loc_name;
ALTER TABLE solarnet.sn_weather_loc DROP COLUMN country;
ALTER TABLE solarnet.sn_weather_loc DROP COLUMN region;
ALTER TABLE solarnet.sn_weather_loc DROP COLUMN time_zone;
ALTER TABLE solarnet.sn_weather_loc DROP COLUMN latitude;
ALTER TABLE solarnet.sn_weather_loc DROP COLUMN longitude;

ALTER TABLE solarnet.sn_node DROP COLUMN time_zone;
ALTER TABLE solarnet.sn_node DROP CONSTRAINT sn_node_weather_loc_fk;
ALTER TABLE solarnet.sn_node ADD 
	CONSTRAINT sn_node_loc_fk FOREIGN KEY (loc_id)
		REFERENCES solarnet.sn_loc (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE solarnet.sn_node ADD COLUMN wloc_id BIGINT;
ALTER TABLE solarnet.sn_node ADD 
	CONSTRAINT sn_node_weather_loc_fk FOREIGN KEY (wloc_id)
		REFERENCES solarnet.sn_weather_loc (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION;
UPDATE solarnet.sn_node SET wloc_id = loc_id;

CREATE OR REPLACE FUNCTION solarnet.get_node_local_timestamp(timestamp with time zone, bigint)
  RETURNS timestamp without time zone AS
$BODY$
	SELECT $1 AT TIME ZONE l.time_zone
	FROM solarnet.sn_node n
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE n.node_id = $2
$BODY$
  LANGUAGE 'sql' STABLE;

CREATE OR REPLACE FUNCTION solarnet.populate_high_low_temperature(datum solarnet.sn_weather_datum)
  RETURNS void AS
$BODY$
DECLARE
	node_tz text;
	dat record;
BEGIN
	SELECT time_zone from solarnet.sn_loc where id = datum.loc_id
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

CREATE OR REPLACE FUNCTION solarnet.populate_rep_consum_datum_hourly(datum solarnet.sn_consum_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	node_tz text;
	data solarnet.rep_consum_datum_hourly;
BEGIN
	SELECT l.time_zone 
	FROM solarnet.sn_node n
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE n.node_id = datum.node_id
	INTO node_tz;
	
	SELECT date_trunc('hour', c.created at time zone node_tz)
	FROM solarnet.sn_consum_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date_trunc('hour', sub.created at time zone node_tz) as created_hour,
		datum.node_id,
		datum.source_id,
		avg(sub.avg_amps) as amps,
		avg(sub.avg_voltage) as voltage,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM solarnet.find_rep_consum_datum(datum.node_id, datum.source_id, chour, node_tz, interval '1 hour') AS sub
	GROUP BY date_trunc('hour', sub.created at time zone node_tz)
	ORDER BY date_trunc('hour', sub.created at time zone node_tz)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	IF NOT FOUND THEN
		RAISE NOTICE 'Datum % has insufficient data.', datum;
		RETURN;
	END IF;
	
	<<insert_update>>
	LOOP
		UPDATE solarnet.rep_consum_datum_hourly SET
			amps = data.amps, 
			voltage = data.voltage,
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_hour = data.created_hour
			AND node_id = data.node_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarnet.rep_consum_datum_hourly (
			created_hour, node_id, source_id, amps, voltage, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_hour, data.node_id, data.source_id, 	
			data.amps, data.voltage, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION solarnet.populate_rep_consum_datum_daily(datum solarnet.sn_consum_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	node_tz text;
	data solarnet.rep_consum_datum_daily;
BEGIN
	SELECT l.time_zone 
	FROM solarnet.sn_node n
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE n.node_id = datum.node_id
	INTO node_tz;
	
	SELECT date_trunc('day', c.created at time zone node_tz)
	FROM solarnet.sn_consum_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date(sub.created at time zone node_tz) as created_day,
		datum.node_id,
		datum.source_id,
		avg(sub.avg_amps) as amps,
		avg(sub.avg_voltage) as voltage,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM solarnet.find_rep_consum_datum(datum.node_id, datum.source_id, chour, node_tz, interval '1 day') AS sub
	GROUP BY date(sub.created at time zone node_tz)
	ORDER BY date(sub.created at time zone node_tz)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE solarnet.rep_consum_datum_daily SET
			amps = data.amps, 
			voltage = data.voltage,
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_day = data.created_day
			AND node_id = data.node_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarnet.rep_consum_datum_daily (
			created_day, node_id, source_id, amps, voltage, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_day, data.node_id, data.source_id, 	
			data.amps, data.voltage, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;
	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION solarnet.populate_rep_power_datum_hourly(datum solarnet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	node_tz text;
	data solarnet.rep_power_datum_hourly;
BEGIN
	SELECT l.time_zone 
	FROM solarnet.sn_node n
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE n.node_id = datum.node_id
	INTO node_tz;
	
	SELECT date_trunc('hour', c.created at time zone node_tz)
	FROM solarnet.sn_power_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date_trunc('hour', sub.created at time zone node_tz) as created_hour,
		datum.node_id,
		datum.source_id,
		avg(sub.avg_pv_volts) as pv_volts,
		avg(sub.avg_pv_amps) as pv_amps,
		avg(sub.avg_bat_volts) as bat_volts,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM solarnet.find_rep_power_datum(datum.node_id, datum.source_id, chour, node_tz, interval '1 hour') AS sub
	GROUP BY date_trunc('hour', sub.created at time zone node_tz)
	ORDER BY date_trunc('hour', sub.created at time zone node_tz)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	IF NOT FOUND THEN
		RAISE NOTICE 'Datum % has insufficient data.', datum;
		RETURN;
	END IF;
	
	<<insert_update>>
	LOOP
		UPDATE solarnet.rep_power_datum_hourly SET
			pv_amps = data.pv_amps, 
			pv_volts = data.pv_volts,
			bat_volts = data.bat_volts,
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_hour = data.created_hour
			AND node_id = data.node_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarnet.rep_power_datum_hourly (
			created_hour, node_id, source_id, pv_amps, pv_volts, bat_volts, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_hour, data.node_id, data.source_id,	
			data.pv_amps, data.pv_volts, data.bat_volts, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION solarnet.populate_rep_power_datum_daily(datum solarnet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	node_tz text;
	data solarnet.rep_power_datum_daily;
BEGIN
	SELECT l.time_zone 
	FROM solarnet.sn_node n
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE n.node_id = datum.node_id
	INTO node_tz;
	
	SELECT date_trunc('day', c.created at time zone node_tz)
	FROM solarnet.sn_power_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date(sub.created at time zone node_tz) as created_day,
		datum.node_id,
		datum.source_id,
		avg(sub.avg_pv_volts) as pv_volts,
		avg(sub.avg_pv_amps) as pv_amps,
		avg(sub.avg_bat_volts) as bat_volts,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM solarnet.find_rep_power_datum(datum.node_id, datum.source_id, chour, node_tz, interval '1 day') AS sub
	GROUP BY date(sub.created at time zone node_tz)
	ORDER BY date(sub.created at time zone node_tz)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE solarnet.rep_power_datum_daily SET
			pv_amps = data.pv_amps, 
			pv_volts = data.pv_volts,
			bat_volts = data.bat_volts,
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_day = data.created_day
			AND node_id = data.node_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarnet.rep_power_datum_daily (
			created_day, node_id, source_id, pv_amps, pv_volts, bat_volts, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_day, data.node_id, data.source_id,
			data.pv_amps, data.pv_volts, data.bat_volts, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION solarnet.find_rep_net_power_datum(IN timestamp without time zone, IN interval)
  RETURNS TABLE(created timestamp without time zone, watt_hours double precision) AS
$BODY$
	SELECT
		c2.created at time zone l.time_zone as created,
		solarnet.calc_avg_watt_hours(c.pv_amps, c2.pv_amps, c.pv_volts, c2.pv_volts, 
			c.kwatt_hours, c2.kwatt_hours, (c.created - c2.created)) as watt_hours
	FROM solarnet.sn_power_datum c
	INNER JOIN solarnet.sn_node n ON n.node_id = c.node_id
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	LEFT OUTER JOIN solarnet.sn_power_datum c2 ON c2.id = c.prev_datum
	WHERE 
		c2.local_created >= $1
		and c2.local_created < $1 + $2
	ORDER BY c.created, c.node_id
$BODY$
  LANGUAGE 'sql' STABLE;

CREATE OR REPLACE FUNCTION solarnet.populate_rep_net_power_datum_hourly(datum solarnet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	data solarnet.rep_net_power_datum_hourly;
BEGIN
	SELECT date_trunc('hour', c.created at time zone l.time_zone)
	FROM solarnet.sn_power_datum c
	INNER JOIN solarnet.sn_node n ON n.node_id = datum.node_id
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date_trunc('hour', sub.created) as created_hour,
		datum.node_id,
		sum(sub.watt_hours) as watt_hours
	FROM solarnet.find_rep_net_power_datum(chour, interval '1 hour') AS sub
	GROUP BY date_trunc('hour', sub.created)
	ORDER BY date_trunc('hour', sub.created)
	INTO data;

	IF NOT FOUND THEN
		RAISE NOTICE 'Datum % has insufficient data.', datum;
		RETURN;
	END IF;
	
	<<insert_update>>
	LOOP
		UPDATE solarnet.rep_net_power_datum_hourly SET
			watt_hours = data.watt_hours
		WHERE created_hour = data.created_hour;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarnet.rep_net_power_datum_hourly (
			created_hour, watt_hours)
		VALUES (data.created_hour, data.watt_hours);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION solarnet.populate_rep_net_power_datum_daily(datum solarnet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	data solarnet.rep_net_power_datum_daily;
BEGIN
	SELECT date_trunc('day', c.created at time zone l.time_zone)
	FROM solarnet.sn_power_datum c
	INNER JOIN solarnet.sn_node n ON n.node_id = datum.node_id
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date(sub.created) as created_day,
		sum(sub.watt_hours) as watt_hours
	FROM solarnet.find_rep_net_power_datum(chour, interval '1 day') AS sub
	GROUP BY date(sub.created)
	ORDER BY date(sub.created)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE solarnet.rep_net_power_datum_daily SET
			watt_hours = data.watt_hours
		WHERE created_day = data.created_day;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarnet.rep_net_power_datum_daily (
			created_day, watt_hours)
		VALUES (data.created_day, data.watt_hours);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;
