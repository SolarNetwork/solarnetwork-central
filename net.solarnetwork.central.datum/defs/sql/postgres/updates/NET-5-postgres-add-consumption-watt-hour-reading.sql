ALTER TABLE solarnet.sn_consum_datum ADD COLUMN watt_hour bigint;

DROP FUNCTION solarnet.calc_avg_watt_hours(real, real, real, real, real, real, interval);

CREATE OR REPLACE FUNCTION solarnet.calc_avg_watt_hours(
	real, real, real, real, double precision, double precision, interval)
  RETURNS double precision AS
$BODY$
	SELECT CASE 
			WHEN $5 IS NOT NULL AND $6 IS NOT NULL THEN ABS($6 - $5)
			ELSE ABS(($1 + $2) / 2) * (($3 + $4) / 2) * ((extract('epoch' from $7)) / 3600)
		END
$BODY$
  LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION solarrep.find_rep_net_power_datum(IN timestamp without time zone, IN interval)
  RETURNS TABLE(created timestamp without time zone, watt_hours double precision) AS
$BODY$
	SELECT
		c2.created at time zone l.time_zone as created,
		solarnet.calc_avg_watt_hours(c.pv_amps, c2.pv_amps, c.pv_volts, c2.pv_volts, 
			c.kwatt_hours * 1000, c2.kwatt_hours * 1000, (c.created - c2.created)) as watt_hours
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

CREATE OR REPLACE FUNCTION solarrep.find_rep_power_datum(IN bigint, IN text, IN timestamp without time zone, IN text, IN interval)
  RETURNS TABLE(
	created 		timestamp with time zone, 
	avg_pv_amps 	double precision, 
	avg_pv_volts 	double precision, 
	avg_bat_volts 	double precision,
	watt_hours		double precision,
	price_per_Wh	double precision,
	cost_amt		double precision,
	currency		character varying(10)
) AS
$BODY$
	SELECT DISTINCT ON (c.created)
		c2.created as created,
		(c.pv_amps + c2.pv_amps) / 2 as avg_pv_amps,
		(c.pv_volts + c2.pv_volts) / 2 as avg_pv_volts,
		(c.bat_volts + c2.bat_volts) / 2 as avg_bat_volts,
		solarnet.calc_avg_watt_hours(c.pv_amps, c2.pv_amps, c.pv_volts, c2.pv_volts, 
			c.kwatt_hours * 1000, c2.kwatt_hours * 1000, (c.created - c2.created)) as watt_hours,
		solarnet.calc_price_per_watt_hours(p.price, p.unit) as price_per_Wh,
		solarnet.calc_price_per_watt_hours(p.price, p.unit) * 
			solarnet.calc_avg_watt_hours(c.pv_amps, c2.pv_amps, c.pv_volts, c2.pv_volts, 
			c.kwatt_hours * 1000, c2.kwatt_hours * 1000, (c.created - c2.created)) as cost_amt,
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

CREATE OR REPLACE FUNCTION solarrep.find_rep_consum_datum(IN bigint, IN text, 
	IN timestamp without time zone, IN text, IN interval)
RETURNS TABLE(
	created 		timestamp with time zone, 
	avg_amps 		double precision, 
	avg_voltage 	double precision, 
	watt_hours		double precision,
	price_per_Wh	double precision,
	cost_amt		double precision,
	currency		character varying(10)
) AS
$BODY$
	SELECT DISTINCT ON (c.created)
		c2.created as created,
		(c.amps + c2.amps) / 2 as avg_amps,
		(c.voltage + c2.voltage) / 2 as avg_voltage,
		solarnet.calc_avg_watt_hours(c.amps, c2.amps, c.voltage, c2.voltage, 
			c.watt_hour, c2.watt_hour, (c.created - c2.created)) as watt_hours,
		solarnet.calc_price_per_watt_hours(p.price, p.unit) as price_per_Wh,
		solarnet.calc_price_per_watt_hours(p.price, p.unit) * 
			solarnet.calc_avg_watt_hours(c.amps, c2.amps, c.voltage, c2.voltage, 
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
