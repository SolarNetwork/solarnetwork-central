/* =========================================================================
   =========================================================================
   STALE DATA TRIGGERS
   =========================================================================
   ========================================================================= */

/**************************************************************************************************
 * FUNCTION solarrep.populate_rep_stale_datum(timestamp with time zone, bigint, varchar)
 * 
 * Insert records into the rep_stale_datum table for asynchronously aggregating updated data later.
 * 
 * @param ts the date of the changed data
 * @param node_id the node ID (or NULL if not node-specific)
 * @param datum_kind the type of datum, e.g. 'power', 'consumption', etc.
 */
CREATE OR REPLACE FUNCTION solarrep.populate_rep_stale_datum(
	ts timestamp with time zone, 
	node_id bigint, 
	datum_kind character varying(64))
  RETURNS void AS
$BODY$
DECLARE
	agg_kinds CHARACTER[] = ARRAY['h', 'd'];
	a CHARACTER(1);
	agg_ts timestamp with time zone;
BEGIN
	FOREACH a IN ARRAY agg_kinds LOOP
		CASE a 
			WHEN 'h' THEN
				agg_ts := date_trunc('hour', ts);
			ELSE 
				agg_ts := date_trunc('day', ts);
		END CASE;
		BEGIN
			IF node_id IS NULL THEN
				INSERT INTO solarrep.rep_stale_datum (ts, agg_kind, datum_kind)
				VALUES (agg_ts, a, datum_kind);
			ELSE
				INSERT INTO solarrep.rep_stale_node_datum (ts, node_id, agg_kind, datum_kind)
				VALUES (agg_ts, node_id, a, datum_kind);
			END IF;
		EXCEPTION WHEN unique_violation THEN
            -- Nothing to do... just continue
        END;
	END LOOP;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION solarrep.trigger_rep_stale_node_datum()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM solarrep.populate_rep_stale_datum(NEW.created, NEW.node_id, TG_TABLE_NAME::text);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

-- NOTE: populates "loc_id" into "node_id" column.
CREATE OR REPLACE FUNCTION solarrep.trigger_rep_stale_loc_datum()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM solarrep.populate_rep_stale_datum(NEW.created, NEW.loc_id, TG_TABLE_NAME::text);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

DROP TRIGGER IF EXISTS populate_rep_stale_datum ON solarnet.sn_consum_datum;
CREATE TRIGGER populate_rep_stale_datum
  AFTER INSERT OR UPDATE
  ON solarnet.sn_consum_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarrep.trigger_rep_stale_node_datum();

DROP TRIGGER IF EXISTS populate_rep_stale_datum ON solarnet.sn_power_datum;
CREATE TRIGGER populate_rep_stale_datum
  AFTER INSERT OR UPDATE
  ON solarnet.sn_power_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarrep.trigger_rep_stale_node_datum();

DROP TRIGGER IF EXISTS populate_rep_stale_datum ON solarnet.sn_price_datum;
CREATE TRIGGER populate_rep_stale_datum
  AFTER INSERT OR UPDATE
  ON solarnet.sn_price_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarrep.trigger_rep_stale_loc_datum();

/**************************************************************************************************
 * FUNCTION solarrep.process_one_rep_stale_node_datum()
 * 
 * Process a single row from the rep_stale_node_datum table, calling the appropriate aggregation
 * query based on the row data. This function works by naming conventions. 
 * The rep_stale_node_datum.datum_kind values are assumed to be named 'sn_X', referring to table
 * names in the solarnet schema. A corresponding 'populate_rep_X_Y' function will be called, 
 * where Y is derived from rep_stale_node_datum.agg_kind:
 * 
 *   d -> 'daily'
 *   h -> 'hourly'
 *
 * The function is expected to accept a RECORD type of the table 'solarnet.X'.
 * 
 * For example, a datum_kind value of 'sn_power_datum' and agg_kind 'd' would result in a function
 * named 'populate_rep_power_datum_daily', which will be passed rows from the 
 * 'solarnet.sn_power_datum' table.
 * 
 * @return count of rows processed (i.e. 0 or 1)
 */
CREATE OR REPLACE FUNCTION solarrep.process_one_rep_stale_node_datum()
  RETURNS INTEGER AS
$BODY$
DECLARE
	stale solarrep.rep_stale_node_datum;
	curs CURSOR FOR SELECT * FROM solarrep.rep_stale_node_datum 
					ORDER BY agg_kind DESC, ts ASC, node_id ASC, datum_kind ASC
					FOR UPDATE;
	func_name text;
	func_agg text;
	trunc_kind text;
	sql_call text;
	max_date timestamp with time zone;
	result integer := 0;
	group_kind text;
	key_kind text;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO stale;
	
	IF FOUND THEN
		CASE stale.agg_kind
			WHEN 'h' THEN
				func_agg := '_hourly';
				trunc_kind := 'hour';
				max_date := stale.ts + INTERVAL '1 hour';
			ELSE
				func_agg := '_daily';
				trunc_kind := 'day';
				max_date := stale.ts + INTERVAL '1 day';
		END CASE;
		CASE stale.datum_kind
			WHEN 'sn_price_datum' THEN
				key_kind := 'loc_id';
				group_kind := 'loc_id';
			ELSE
				key_kind := 'node_id';
				group_kind := 'node_id, source_id';
		END CASE;
		func_name := 'populate_rep_' || substring(stale.datum_kind FROM 4) || func_agg;
		-- find all records in aggregate range, grouped by source, to run with
		sql_call := 'SELECT solarrep.' || func_name || '(c) FROM solarnet.' ||stale.datum_kind || ' c'
			|| ' where c.id in (select max(id) from solarnet.' || stale.datum_kind 
			|| ' where ' || key_kind || ' = $1 and created >= $2 and created < $3 and prev_datum IS NOT NULL'
			|| ' group by date_trunc(''' || trunc_kind || ''', created), ' || group_kind || ')';
	
		--RAISE NOTICE 'Calling aggregate SQL %; %, %, %', sql_call, stale.node_id, stale.ts, max_date;
		EXECUTE sql_call USING stale.node_id, stale.ts, max_date;
	
		DELETE FROM solarrep.rep_stale_node_datum WHERE CURRENT OF curs;
		result := 1;
	END IF;
	
	CLOSE curs;
	RETURN result;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;

/**************************************************************************************************
 * FUNCTION solarrep.process_rep_stale_node_datum()
 * 
 * Process all rows in rep_stale_node_datum by repeatedly calling 
 * solarrep.process_one_rep_stale_node_datum() until no rows remain.
 */
CREATE OR REPLACE FUNCTION solarrep.process_rep_stale_node_datum()
  RETURNS void AS
$BODY$
DECLARE
	result_count INTEGER;
BEGIN
	LOOP
		SELECT * INTO result_count FROM solarrep.process_one_rep_stale_node_datum();
		IF result_count < 1 THEN
			RETURN;
		END IF;
	END LOOP;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;

/* =========================================================================
   =========================================================================
   CONSUMPTION REPORTING TRIGGERS
   =========================================================================
   ========================================================================= */

/**************************************************************************************************
 * FUNCTION solarrep.find_rep_consum_datum(bigint, text, timestamp, text, interval)
 * 
 * SQL function to return a set of solarnet.sn_consum_datum records for a specific node within a
 * given date range.
 * 
 * @param bigint		the node ID
 * @param text			the source ID
 * @param timestamp		the starting date
 * @param text			a valid time zone ID (e.g. 'Pacific/Auckland')
 * @param interval		an interval to calculate the end date from the starting date
 */
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

/**************************************************************************************************
 * FUNCTION solarrep.populate_rep_consum_datum_hourly(solarnet.sn_consum_datum)
 * 
 * Pl/PgSQL function to aggreage solarnet.sn_consum_datum rows at an hourly level and populate the results
 * into the solarrep.rep_consum_datum_hourly table. This function is designed to be exectued via a 
 * trigger function on the solarnet.sn_consum_datum table and thus treat the rep_consum_datum_hourly
 * table like a materialized view.
 * 
 * The hour to update is derived from the created column of the input record's "previous" record
 * (the sn_consum_datum.prev_datum ID).
 * 
 * @param solarnet.sn_consum_datum		the solarnet.sn_consum_datum row to update aggregated data for
 */
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

/**************************************************************************************************
 * FUNCTION solarrep.populate_rep_consum_datum_daily(solarnet.sn_consum_datum)
 * 
 * Pl/PgSQL function to aggreage ConsumptionDatum rows at an daily level and populate the results
 * into the solarrep.rep_consum_datum_daily table. This function is designed to be exectued via a 
 * trigger function on the solarnet.sn_consum_datum table and thus treat the rep_consum_datum_daily
 * table like a materialized view.
 * 
 * The hour to update is derived from the created column of the input record's "previous" record
 * (the sn_consum_datum.prev_datum ID).
 * 
 * @param solarnet.sn_consum_datum		the ConsumptionDatum row to update aggregated data for
 */
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


/* =========================================================================
   =========================================================================
   POWER REPORTING TRIGGERS
   =========================================================================
   ========================================================================= */

/**************************************************************************************************
 * FUNCTION solarrep.find_rep_net_power_datum(timestamp, interval)
 * 
 * SQL function to return a set of reporting solarnet.sn_power_datum records for all nodes within a 
 * given date range.
 * 
 * @param timestamp		the starting date
 * @param interval		an interval to calculate the end date from the starting date
 */
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

/**************************************************************************************************
 * FUNCTION solarrep.find_rep_power_datum(bigint, text, timestamp, text, interval)
 * 
 * SQL function to return a set of reporting solarnet.sn_power_datum records for a specific node
 * within a given date range.
 * 
 * @param bigint		the node ID
 * @param text			the source ID
 * @param timestamp		the starting date
 * @param text			a valid time zone ID (e.g. 'Pacific/Auckland')
 * @param interval		an interval to calculate the end date from the starting date
 */
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

/**************************************************************************************************
 * FUNCTION solarrep.populate_rep_power_datum_hourly(solarnet.sn_power_datum)
 * 
 * Pl/PgSQL function to aggreage solarnet.sn_power_datum rows at an hourly level and populate the 
 * results into the solarrep.rep_power_datum_hourly table. This function is designed to be exectued
 * via a trigger function on the solarnet.sn_power_datum table and thus treat the 
 * rep_power_datum_hourly table like a materialized view.
 * 
 * The hour to update is derived from the created column of the input record's "previous" record
 * (the sn_power_datum.prev_datum ID).
 * 
 * @param datum the solarnet.sn_power_datum row to update aggregated data for
 */
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

/**************************************************************************************************
 * FUNCTION solarrep.populate_rep_net_power_datum_hourly(solarnet.sn_power_datum)
 * 
 * Pl/PgSQL function to aggreage solarnet.sn_power_datum rows at an hourly level and populate the
 * results into the solarrep.rep_net_power_datum_hourly table. This function is designed to be
 * exectued via a trigger function on the solarnet.sn_power_datum table and thus treat the 
 * rep_net_power_datum_hourly table like a materialized view.
 * 
 * The hour to update is derived from the created column of the input record's "previous" record
 * (the sn_power_datum.prev_datum ID).
 * 
 * @param datum	the solarnet.sn_power_datum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION solarrep.populate_rep_net_power_datum_hourly(datum solarnet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	data record;
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
		datum.node_id as node_id,
		sum(sub.watt_hours) as watt_hours
	FROM solarrep.find_rep_net_power_datum(chour, interval '1 hour') AS sub
	GROUP BY date_trunc('hour', sub.created)
	ORDER BY date_trunc('hour', sub.created)
	INTO data;

	IF NOT FOUND THEN
		RAISE NOTICE 'Datum % has insufficient data.', datum;
		RETURN;
	END IF;
	
	<<insert_update>>
	LOOP
		UPDATE solarrep.rep_net_power_datum_hourly SET
			watt_hours = data.watt_hours
		WHERE created_hour = data.created_hour;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarrep.rep_net_power_datum_hourly (
			created_hour, watt_hours)
		VALUES (data.created_hour, data.watt_hours);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;

/**************************************************************************************************
 * FUNCTION solarrep.populate_rep_power_datum_daily(solarnet.sn_power_datum)
 * 
 * Pl/PgSQL function to aggreage solarnet.sn_power_datum rows at an daily level and populate the
 * results into the solarrep.rep_power_datum_daily table. This function is designed to be exectued
 * via a trigger function on the solarnet.sn_power_datum table and thus treat the 
 * rep_power_datum_daily table like a materialized view.
 * 
 * The day to update is derived from the created column of the input record's "previous" record
 * (the sn_power_datum.prev_datum ID).
 * 
 * @param datum the solarnet.sn_power_datum row to update aggregated data for
 */
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

/**************************************************************************************************
 * FUNCTION solarrep.populate_rep_net_power_datum_daily(solarnet.sn_power_datum)
 * 
 * Pl/PgSQL function to aggreage solarnet.sn_power_datum rows at an daily level and populate the
 * results into the solarrep.rep_net_power_datum_daily table. This function is designed to be
 * exectued via a trigger function on the solarnet.sn_power_datum table and thus treat the 
 * rep_net_power_datum_daily table like a materialized view.
 * 
 * The day to update is derived from the created column of the input record's "previous" record
 * (the sn_power_datum.prev_datum ID).
 * 
 * @param datum	the solarnet.sn_power_datum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION solarrep.populate_rep_net_power_datum_daily(datum solarnet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	data record;
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
	FROM solarrep.find_rep_net_power_datum(chour, interval '1 day') AS sub
	GROUP BY date(sub.created)
	ORDER BY date(sub.created)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE solarrep.rep_net_power_datum_daily SET
			watt_hours = data.watt_hours
		WHERE created_day = data.created_day;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarrep.rep_net_power_datum_daily (
			created_day, watt_hours)
		VALUES (data.created_day, data.watt_hours);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION solarrep.populate_rep_net_power_daily()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM solarrep.populate_rep_net_power_datum_daily(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

DROP TRIGGER IF EXISTS populate_rep_net_power_daily ON solarnet.sn_power_datum;
CREATE TRIGGER populate_rep_net_power_daily
  AFTER INSERT OR UPDATE
  ON solarnet.sn_power_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarrep.populate_rep_net_power_daily();

-- NET-20: disable trigger until different implementation can be done
ALTER TABLE solarnet.sn_power_datum DISABLE TRIGGER populate_rep_net_power_daily;
  
/* =========================================================================
   =========================================================================
   PRICE REPORTING TRIGGERS
   =========================================================================
   ========================================================================= */
  
/**************************************************************************************************
 * FUNCTION solarrep.find_rep_price_datum(bigint timestamp, interval)
 * 
 * SQL function to return a set of reporting solarnet.sn_price_datum records for a specific node 
 * within a given date range.
 * 
 * @param bigint		the location ID
 * @param timestamp		the starting date
 * @param interval		an interval to calculate the end date from the starting date
 */
CREATE OR REPLACE FUNCTION solarrep.find_rep_price_datum(IN bigint, IN timestamp with time zone, IN interval)
  RETURNS TABLE(
	created 		timestamp with time zone, 
	avg_price 		double precision
) AS
$BODY$
	SELECT 
		c2.created as created,
		(c.price + c2.price) / 2 as avg_price
	FROM solarnet.sn_price_datum c
	INNER JOIN solarnet.sn_price_datum c2 ON c2.id = c.prev_datum
	WHERE 
		c2.loc_id = $1
		AND c2.created >= $2
		AND c2.created < $2 + $3
	ORDER BY c.created
$BODY$
LANGUAGE 'sql' STABLE;

/**************************************************************************************************
 * FUNCTION solarrep.populate_rep_price_datum_hourly(solarnet.sn_price_datum)
 * 
 * Pl/PgSQL function to aggreage solarnet.sn_price_datum rows at an hourly level and populate the
 * results into the solarrep.rep_price_datum_hourly table. This function is designed to be exectued
 * via a trigger function on the solarnet.sn_price_datum table and thus treat the rep_price_datum_hourly
 * table like a materialized view.
 * 
 * The hour to update is derived from the created column of the input record's "previous" record
 * (the sn_price_datum.prev_datum ID).
 * 
 * @param datum the solarnet.sn_price_datum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION solarrep.populate_rep_price_datum_hourly(datum solarnet.sn_price_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp with time zone;
	data record;
BEGIN
	SELECT date_trunc('hour', c.created)
	FROM solarnet.sn_price_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date_trunc('hour', sub.created) as created_hour,
		datum.loc_id,
		avg(sub.avg_price) as price
	FROM solarrep.find_rep_price_datum(datum.loc_id, chour, interval '1 hour') AS sub
	GROUP BY date_trunc('hour', sub.created)
	ORDER BY date_trunc('hour', sub.created)
	INTO data;
	
	IF NOT FOUND THEN
		RAISE NOTICE 'Datum % has insufficient data.', datum;
		RETURN;
	END IF;
	
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE solarrep.rep_price_datum_hourly SET
			price = data.price
		WHERE created_hour = data.created_hour
			AND loc_id = data.loc_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarrep.rep_price_datum_hourly (
			created_hour, loc_id, price)
		VALUES (data.created_hour, data.loc_id, data.price);

		EXIT insert_update;
	END LOOP insert_update;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;

/**************************************************************************************************
 * FUNCTION solarrep.populate_rep_price_datum_daily(solarnet.sn_price_datum)
 * 
 * Pl/PgSQL function to aggreage solarnet.sn_price_datum rows at an daily level and populate the
 * results into the solarrep.rep_price_datum_daily table. This function is designed to be exectued
 * via a trigger function on the solarnet.sn_price_datum table and thus treat the 
 * rep_price_datum_daily table like a materialized view.
 * 
 * The day to update is derived from the created column of the input record's "previous" record
 * (the sn_price_datum.prev_datum ID).
 * 
 * @param datum the solarnet.sn_price_datum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION solarrep.populate_rep_price_datum_daily(datum solarnet.sn_price_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp with time zone;
	data record;
BEGIN
	SELECT date_trunc('day', c.created)
	FROM solarnet.sn_price_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date(sub.created) as created_day,
		datum.loc_id,
		avg(sub.avg_price) as price
	FROM solarrep.find_rep_price_datum(datum.loc_id, chour, interval '1 day') AS sub
	GROUP BY date(sub.created)
	ORDER BY date(sub.created)
	INTO data;
	
	IF NOT FOUND THEN
		RAISE NOTICE 'Datum % has insufficient data.', datum;
		RETURN;
	END IF;
	
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE solarrep.rep_price_datum_daily SET
			price = data.price
		WHERE created_day = data.created_day
			AND loc_id = data.loc_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solarrep.rep_price_datum_daily (
			created_day, loc_id, price)
		VALUES (data.created_day, data.loc_id, data.price);

		EXIT insert_update;
	END LOOP insert_update;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;
