ALTER TABLE solarnet.sn_consum_datum ADD COLUMN watts INTEGER;
ALTER TABLE solarnet.sn_consum_datum DISABLE TRIGGER USER;
UPDATE solarnet.sn_consum_datum SET watts = round(amps * voltage);
ALTER TABLE solarnet.sn_consum_datum ENABLE TRIGGER USER;
ALTER TABLE solarnet.sn_consum_datum DROP COLUMN amps;
ALTER TABLE solarnet.sn_consum_datum DROP COLUMN voltage;

ALTER TABLE solarrep.rep_consum_datum_hourly ADD COLUMN watts integer;
ALTER TABLE solarrep.rep_consum_datum_hourly DISABLE TRIGGER USER;
UPDATE solarrep.rep_consum_datum_hourly SET watts = round(amps * voltage);
ALTER TABLE solarrep.rep_consum_datum_hourly ENABLE TRIGGER USER;
ALTER TABLE solarrep.rep_consum_datum_hourly DROP COLUMN amps;
ALTER TABLE solarrep.rep_consum_datum_hourly DROP COLUMN voltage;

ALTER TABLE solarrep.rep_consum_datum_daily ADD COLUMN watts integer;
ALTER TABLE solarrep.rep_consum_datum_daily DISABLE TRIGGER USER;
UPDATE solarrep.rep_consum_datum_daily SET watts = round(amps * voltage);
ALTER TABLE solarrep.rep_consum_datum_daily ENABLE TRIGGER USER;
ALTER TABLE solarrep.rep_consum_datum_daily DROP COLUMN amps;
ALTER TABLE solarrep.rep_consum_datum_daily DROP COLUMN voltage;

ALTER TABLE solarnet.sn_power_datum ADD COLUMN watts INTEGER;
ALTER TABLE solarnet.sn_power_datum DISABLE TRIGGER USER;
UPDATE solarnet.sn_power_datum SET watts = round(pv_amps * pv_volts);
ALTER TABLE solarnet.sn_power_datum ENABLE TRIGGER USER;
ALTER TABLE solarnet.sn_power_datum DROP COLUMN pv_amps;
ALTER TABLE solarnet.sn_power_datum DROP COLUMN pv_volts;

ALTER TABLE solarrep.rep_power_datum_hourly ADD COLUMN watts INTEGER;
ALTER TABLE solarrep.rep_power_datum_hourly DISABLE TRIGGER USER;
UPDATE solarrep.rep_power_datum_hourly SET watts = round(pv_amps * pv_volts);
ALTER TABLE solarrep.rep_power_datum_hourly ENABLE TRIGGER USER;
ALTER TABLE solarrep.rep_power_datum_hourly DROP COLUMN pv_amps;
ALTER TABLE solarrep.rep_power_datum_hourly DROP COLUMN pv_volts;

ALTER TABLE solarrep.rep_power_datum_daily ADD COLUMN watts INTEGER;
ALTER TABLE solarrep.rep_power_datum_daily DISABLE TRIGGER USER;
UPDATE solarrep.rep_power_datum_daily SET watts = round(pv_amps * pv_volts);
ALTER TABLE solarrep.rep_power_datum_daily ENABLE TRIGGER USER;
ALTER TABLE solarrep.rep_power_datum_daily DROP COLUMN pv_amps;
ALTER TABLE solarrep.rep_power_datum_daily DROP COLUMN pv_volts;

DROP FUNCTION solarnet.calc_avg_watt_hours(real, real, real, real, 
	double precision, double precision, interval);
	
CREATE OR REPLACE FUNCTION solarnet.calc_avg_watt_hours(integer, integer, 
	double precision, double precision, interval)
  RETURNS double precision AS
$BODY$
	SELECT CASE 
			WHEN 
				/* Wh readings available, so use difference in Wh */
				$3 IS NOT NULL AND $4 IS NOT NULL AND $3 > $4
				THEN $3 - $4
			WHEN 
				/* Assume day reset on inverter, so Wh for day reset */
				$3 IS NOT NULL AND $4 IS NOT NULL AND $3 < $4
				THEN $3
			ELSE 
				/* Wh not available, so calculate Wh using (watts * dt) */
				ABS(($1 + $2) / 2) * ((extract('epoch' from $5)) / 3600)
		END
$BODY$
  LANGUAGE sql IMMUTABLE;

DROP FUNCTION solarrep.find_rep_consum_datum(bigint, text, 
	timestamp without time zone, text, interval);

CREATE OR REPLACE FUNCTION solarrep.find_rep_consum_datum(IN bigint, IN text, 
	IN timestamp without time zone, IN text, IN interval)
RETURNS TABLE(
	created 		timestamp with time zone, 
	avg_watts 		double precision, 
	watt_hours		double precision,
	price_per_Wh	double precision,
	cost_amt		double precision,
	currency		character varying(10)
) AS
$BODY$
	SELECT DISTINCT ON (c.created)
		c2.created as created,
		CAST((c.watts + c2.watts) / 2 as double precision) as avg_watts,
		solarnet.calc_avg_watt_hours(c.watts, c2.watts, 
			c.watt_hour, c2.watt_hour, (c.created - c2.created)) as watt_hours,
		solarnet.calc_price_per_watt_hours(p.price, p.unit) as price_per_Wh,
		solarnet.calc_price_per_watt_hours(p.price, p.unit) * 
			solarnet.calc_avg_watt_hours(c.watts, c2.watts, 
			c.watt_hour, c2.watt_hour, (c.created - c2.created)) as cost_amt,
		p.currency
	FROM solarnet.sn_consum_datum c
	LEFT OUTER JOIN solarnet.sn_consum_datum c2 ON c2.id = c.prev_datum
	LEFT OUTER JOIN (
		SELECT p.created, p.price, l.id, l.unit, l.currency
		FROM solarnet.sn_price_datum p
		INNER JOIN solarnet.sn_price_loc l ON l.id = p.loc_id
		WHERE p.created between (($3  - interval '1 hour') at time zone $4) 
				and (($3 + $5) at time zone $4)
		) AS p ON p.created BETWEEN c.created - interval '1 hour' AND c.created
			AND p.id = c.price_loc_id
	WHERE 
		c2.node_id = $1
		AND c2.source_id = $2
		AND c2.created >= $3 at time zone $4
		AND c2.created < $3 at time zone $4 + $5
	ORDER BY c.created, p.created DESC
$BODY$
LANGUAGE 'sql' STABLE;

	
CREATE OR REPLACE FUNCTION solarrep.populate_rep_consum_datum_hourly(datum solarnet.sn_consum_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	node_tz text;
	data record;
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
		datum.node_id as node_id,
		datum.source_id as source_id,
		avg(sub.avg_watts) as watts,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM solarrep.find_rep_consum_datum(datum.node_id, datum.source_id, chour, node_tz, interval '1 hour') AS sub
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
		UPDATE solarrep.rep_consum_datum_hourly SET
			watts = data.watts, 
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_hour = data.created_hour
			AND node_id = data.node_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarrep.rep_consum_datum_hourly (
			created_hour, node_id, source_id, watts, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_hour, data.node_id, data.source_id, 	
			data.watts, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION solarrep.populate_rep_consum_datum_daily(datum solarnet.sn_consum_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	node_tz text;
	data record;
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
		datum.node_id as node_id,
		datum.source_id as source_id,
		avg(sub.avg_watts) as watts,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM solarrep.find_rep_consum_datum(datum.node_id, datum.source_id, chour, node_tz, interval '1 day') AS sub
	GROUP BY date(sub.created at time zone node_tz)
	ORDER BY date(sub.created at time zone node_tz)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE solarrep.rep_consum_datum_daily SET
			watts = data.watts, 
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_day = data.created_day
			AND node_id = data.node_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarrep.rep_consum_datum_daily (
			created_day, node_id, source_id, watts, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_day, data.node_id, data.source_id, 	
			data.watts, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;
	END LOOP insert_update;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION solarrep.find_rep_net_power_datum(IN timestamp without time zone, IN interval)
  RETURNS TABLE(created timestamp without time zone, watt_hours double precision) AS
$BODY$
	SELECT
		c2.created at time zone l.time_zone as created,
		solarnet.calc_avg_watt_hours(c.watts, c2.watts, 
			c.watt_hour, c2.watt_hour, (c.created - c2.created)) as watt_hours
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

DROP FUNCTION solarrep.find_rep_power_datum(bigint, text, timestamp without time zone, text, interval);

CREATE OR REPLACE FUNCTION solarrep.find_rep_power_datum(IN bigint, IN text, IN timestamp without time zone, IN text, IN interval)
  RETURNS TABLE(
	created 		timestamp with time zone, 
	avg_watts 		double precision, 
	avg_bat_volts	double precision,
	watt_hours		double precision,
	price_per_Wh	double precision,
	cost_amt		double precision,
	currency		character varying(10)
) AS
$BODY$
	SELECT DISTINCT ON (c.created)
		c2.created as created,
		CAST((c.watts + c2.watts) / 2 as double precision) as avg_watts,
		(c.bat_volts + c2.bat_volts) / 2 as avg_bat_volts,
		solarnet.calc_avg_watt_hours(c.watts, c2.watts, 
			c.watt_hour, c2.watt_hour, (c.created - c2.created)) as watt_hours,
		solarnet.calc_price_per_watt_hours(p.price, p.unit) as price_per_Wh,
		solarnet.calc_price_per_watt_hours(p.price, p.unit) * 
			solarnet.calc_avg_watt_hours(c.watts, c2.watts, 
			c.watt_hour, c2.watt_hour, (c.created - c2.created)) as cost_amt,
		p.currency
	FROM solarnet.sn_power_datum c
	LEFT OUTER JOIN solarnet.sn_power_datum c2 ON c2.id = c.prev_datum
	LEFT OUTER JOIN (
		SELECT p.created, l.id, p.price, l.unit, l.currency
		FROM solarnet.sn_price_datum p
		INNER JOIN solarnet.sn_price_loc l ON l.id = p.loc_id
		WHERE p.created between (($3  - interval '1 hour') at time zone $4) 
			and (($3 + $5) at time zone $4)
		) AS p ON p.created BETWEEN c.created - interval '1 hour' AND c.created
			AND p.id = c.price_loc_id
	WHERE 
		c2.node_id = $1
		AND c2.source_id = $2
		AND c2.created >= $3 at time zone $4
		AND c2.created < $3 at time zone $4 + $5
	ORDER BY c.created, p.created DESC
$BODY$
LANGUAGE 'sql' STABLE;

CREATE OR REPLACE FUNCTION solarrep.populate_rep_power_datum_hourly(datum solarnet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	node_tz text;
	data record;
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
		datum.node_id as node_id,
		datum.source_id as source_id,
		avg(sub.avg_watts) as watts,
		avg(sub.avg_bat_volts) as bat_volts,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM solarrep.find_rep_power_datum(datum.node_id, datum.source_id, chour, node_tz, interval '1 hour') AS sub
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
		UPDATE solarrep.rep_power_datum_hourly SET
			watts = data.watts, 
			bat_volts = data.bat_volts,
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_hour = data.created_hour
			AND node_id = data.node_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarrep.rep_power_datum_hourly (
			created_hour, node_id, source_id, watts, bat_volts, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_hour, data.node_id, data.source_id,	
			data.watts, data.bat_volts, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION solarrep.populate_rep_power_datum_daily(datum solarnet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	node_tz text;
	data record;
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
		datum.node_id as node_id,
		datum.source_id as source_id,
		avg(sub.avg_watts) as watts,
		avg(sub.avg_bat_volts) as bat_volts,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM solarrep.find_rep_power_datum(datum.node_id, datum.source_id, chour, node_tz, interval '1 day') AS sub
	GROUP BY date(sub.created at time zone node_tz)
	ORDER BY date(sub.created at time zone node_tz)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE solarrep.rep_power_datum_daily SET
			watts = data.watts, 
			bat_volts = data.bat_volts,
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_day = data.created_day
			AND node_id = data.node_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarrep.rep_power_datum_daily (
			created_day, node_id, source_id, watts, bat_volts, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_day, data.node_id, data.source_id,
			data.watts, data.bat_volts, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;

