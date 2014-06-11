CREATE SCHEMA solarrep;

/* =========================================================================
   =========================================================================
   STALE DATA
   =========================================================================
   ========================================================================= */

/**************************************************************************************************
 * TABLE solarrep.rep_stale_node_datum
 * 
 * A table to hold references to "stale" aggregate data that is associated with a particular
 * BIGINT key such as a 'node_id' or 'loc_id' value. This table is populated by trigger functions
 * on raw datum tables when rows are inserted, updated, or deleted, with the assumption that 
 * inserting into this table should be quick. Later on some other process is expected to query 
 * this table and perform some aggregate calculations based on the details of each row. The 
 * results of the calculation are stored in some associated reporting table, and then the row
 * in this table can be deleted. In essence the reporting table is like a materialized view
 * of aggregate data on some raw source table.
 */
CREATE TABLE solarrep.rep_stale_node_datum (
	ts			TIMESTAMP WITH TIME ZONE NOT NULL,
	node_id 	BIGINT NOT NULL,
	agg_kind 	CHARACTER(1) NOT NULL,
	datum_kind 	CHARACTER VARYING(64) NOT NULL,
	PRIMARY KEY (ts, node_id, agg_kind, datum_kind)
);
COMMENT ON TABLE solarrep.rep_stale_node_datum IS 
'The node_id column in this table is inappropriately named. Really it holds any datum table
BIGINT key value, for example a loc_id value from sn_price_datum.';

CREATE TABLE solarrep.rep_stale_datum (
	ts			TIMESTAMP WITH TIME ZONE NOT NULL,
	agg_kind 	CHARACTER(1) NOT NULL,
	datum_kind 	CHARACTER VARYING(64) NOT NULL,
	PRIMARY KEY (ts, agg_kind, datum_kind)
);

/* =========================================================================
   =========================================================================
   CONSUMPTION REPORTING
   =========================================================================
   ========================================================================= */

CREATE TABLE solarrep.rep_consum_datum_hourly (
  created_hour 	timestamp without time zone NOT NULL,
  node_id 		bigint NOT NULL,
  source_id 	character varying(255) NOT NULL,
  watts 		integer,
  watt_hours 	double precision,
  cost_amt 		real,
  cost_currency	character varying(10),
  PRIMARY KEY (created_hour, node_id, source_id)
);

COMMENT ON TABLE solarrep.rep_consum_datum_hourly IS 'To refresh this table, call something like this:

select solarrep.populate_rep_consum_datum_hourly(c) from solarnet.sn_consum_datum c
where c.id in (
	select max(id) from solarnet.sn_consum_datum
	group by date_trunc(''hour'', created), node_id, source_id
)';

-- this index is used for foreign key validation in other tables
CREATE INDEX rep_consum_datum_hourly_node_idx ON solarrep.rep_consum_datum_hourly (node_id,created_hour);

CLUSTER solarrep.rep_consum_datum_hourly USING rep_consum_datum_hourly_pkey;

CREATE TABLE solarrep.rep_consum_datum_daily (
  created_day 	date NOT NULL,
  node_id 		bigint NOT NULL,
  source_id 	character varying(255) NOT NULL,
  watts 		integer,
  watt_hours 	double precision,
  cost_amt 		double precision,
  cost_currency	character varying(10),
  PRIMARY KEY (created_day, node_id, source_id)
);

COMMENT ON TABLE solarrep.rep_consum_datum_daily IS 'To refresh this table, call something like this:

select solarrep.populate_rep_consum_datum_daily(c) from solarnet.sn_consum_datum c
where c.id in (
	select max(id) from solarnet.sn_consum_datum
	group by date_trunc(''day'', created), node_id, source_id
)';

-- this index is used for foreign key validation in other tables
CREATE INDEX rep_consum_datum_daily_node_idx ON solarrep.rep_consum_datum_daily (node_id,created_day);

CLUSTER solarrep.rep_consum_datum_daily USING rep_consum_datum_daily_pkey;

/* =========================================================================
   =========================================================================
   POWER REPORTING
   =========================================================================
   ========================================================================= */

CREATE TABLE solarrep.rep_power_datum_hourly (
  created_hour 	timestamp without time zone NOT NULL,
  node_id 		BIGINT NOT NULL,
  source_id		CHARACTER VARYING(255) NOT NULL,
  watts			INTEGER,
  bat_volts		REAL,
  watt_hours 	DOUBLE PRECISION,
  cost_amt 		REAL,
  cost_currency	CHARACTER VARYING(10),
  PRIMARY KEY (created_hour, node_id, source_id)
);

COMMENT ON TABLE solarrep.rep_power_datum_hourly IS 
'To refresh this table, call something like this:

select solarrep.populate_rep_power_datum_hourly(c) from solarnet.sn_power_datum c
where c.id in (
	select max(id) from solarnet.sn_power_datum
	group by date_trunc(''hour'', created), node_id, source_id
)';

-- this index is used for foreign key validation in other tables
CREATE INDEX rep_power_datum_hourly_node_idx ON solarrep.rep_power_datum_hourly (node_id,created_hour);

CLUSTER solarrep.rep_power_datum_hourly USING rep_power_datum_hourly_pkey;

CREATE TABLE solarrep.rep_net_power_datum_hourly (
  created_hour timestamp without time zone NOT NULL,
  watt_hours double precision,
  CONSTRAINT rep_net_power_datum_hourly_pkey PRIMARY KEY (created_hour)
);

CLUSTER solarrep.rep_net_power_datum_hourly USING rep_net_power_datum_hourly_pkey;

CREATE TABLE solarrep.rep_power_datum_daily (
  created_day 	date NOT NULL,
  node_id 		BIGINT NOT NULL,
  source_id		CHARACTER VARYING(255) NOT NULL,
  watts 		INTEGER,
  bat_volts		REAL,
  watt_hours 	DOUBLE PRECISION,
  cost_amt 		REAL,
  cost_currency	CHARACTER VARYING(10),
  PRIMARY KEY (created_day, node_id, source_id)
);

COMMENT ON TABLE solarrep.rep_power_datum_daily IS 
'To refresh this table, call something like this:

select solarrep.populate_rep_power_datum_daily(c) from solarnet.sn_power_datum c
where c.id in (
	select max(id) from solarnet.sn_power_datum
	group by date_trunc(''day'', created), node_id, source_id
)';

-- this index is used for foreign key validation in other tables
CREATE INDEX rep_power_datum_daily_node_idx ON solarrep.rep_power_datum_daily (node_id,created_day);

CLUSTER solarrep.rep_power_datum_daily USING rep_power_datum_daily_pkey;

CREATE TABLE solarrep.rep_net_power_datum_daily (
  created_day date NOT NULL,
  watt_hours double precision,
  CONSTRAINT rep_net_power_datum_daily_pkey PRIMARY KEY (created_day)
);

CLUSTER solarrep.rep_net_power_datum_daily USING rep_net_power_datum_daily_pkey;

/* =========================================================================
   =========================================================================
   PRICE REPORTING
   =========================================================================
   ========================================================================= */
  
CREATE TABLE solarrep.rep_price_datum_hourly (
  created_hour 	TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  loc_id		BIGINT NOT NULL,
  price			REAL,
  CONSTRAINT rep_price_datum_hourly_pkey PRIMARY KEY (created_hour, loc_id)
);

COMMENT ON TABLE solarrep.rep_price_datum_hourly IS 'To refresh this table, call something like this:

select solarrep.populate_rep_price_datum_hourly(c) from solarnet.sn_price_datum c
where c.id in (
	select max(id) from solarnet.sn_price_datum
	group by date_trunc(''hour'', created), loc_id
)';

-- this index is used for foreign key validation in other tables
CREATE INDEX rep_price_datum_hourly_node_idx ON solarrep.rep_price_datum_hourly (loc_id,created_hour);

CLUSTER solarrep.rep_price_datum_hourly USING rep_price_datum_hourly_pkey;

CREATE TABLE solarrep.rep_price_datum_daily (
  created_day 	DATE NOT NULL,
  loc_id		BIGINT NOT NULL,
  price			REAL,
  CONSTRAINT rep_price_datum_daily_pkey PRIMARY KEY (created_day, loc_id)
);

COMMENT ON TABLE solarrep.rep_price_datum_daily IS 'To refresh this table, call something like this:

select solarrep.populate_rep_price_datum_daily(c) from solarnet.sn_price_datum c
where c.id in (
	select max(id) from solarnet.sn_price_datum
	group by date_trunc(''hour'', created), loc_id
)';

-- this index is used for foreign key validation in other tables
CREATE INDEX rep_price_datum_daily_node_idx ON solarrep.rep_price_datum_daily (loc_id,created_day);

CLUSTER solarrep.rep_price_datum_daily USING rep_price_datum_daily_pkey;
