ALTER TABLE solarnet.sn_consum_datum DROP CONSTRAINT IF EXISTS consum_datum_node_unq;
ALTER TABLE solarnet.sn_consum_datum
  ADD CONSTRAINT consum_datum_node_unq UNIQUE(node_id, created, source_id)
  DEFERRABLE INITIALLY IMMEDIATE;
DROP INDEX IF EXISTS solarnet.consum_datum_node_idx;
  
ALTER TABLE solarnet.sn_day_datum DROP CONSTRAINT IF EXISTS sn_day_datum_loc_unq;
ALTER TABLE solarnet.sn_day_datum
  ADD CONSTRAINT sn_day_datum_loc_unq UNIQUE(loc_id, day)
  DEFERRABLE INITIALLY IMMEDIATE;
DROP INDEX IF EXISTS solarnet.day_datum_created_idx;

ALTER TABLE solarnet.sn_hardware_control_datum DROP CONSTRAINT IF EXISTS sn_hardware_control_datum_node_unq;
ALTER TABLE solarnet.sn_hardware_control_datum
  ADD CONSTRAINT sn_hardware_control_datum_node_unq UNIQUE(node_id, created, source_id)
  DEFERRABLE INITIALLY IMMEDIATE;
DROP INDEX IF EXISTS solarnet.sn_hardware_control_datum_node_idx;

ALTER TABLE solarnet.sn_power_datum DROP CONSTRAINT IF EXISTS power_datum_node_unq;
ALTER TABLE solarnet.sn_power_datum
  ADD CONSTRAINT power_datum_node_unq UNIQUE(node_id, created, source_id)
  DEFERRABLE INITIALLY IMMEDIATE;
DROP INDEX IF EXISTS solarnet.power_datum_node_idx;

ALTER TABLE solarnet.sn_price_datum DROP CONSTRAINT IF EXISTS price_datum_unq;
ALTER TABLE solarnet.sn_price_datum
  ADD CONSTRAINT price_datum_unq UNIQUE(loc_id, created)
  DEFERRABLE INITIALLY IMMEDIATE;
DROP INDEX IF EXISTS solarnet.price_datum_loc_idx;

ALTER TABLE solarnet.sn_weather_datum DROP CONSTRAINT IF EXISTS sn_weather_datum_loc_unq;
ALTER TABLE solarnet.sn_weather_datum
  ADD CONSTRAINT sn_weather_datum_loc_unq UNIQUE(loc_id, info_date)
  DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE solarrep.rep_consum_datum_daily DROP CONSTRAINT IF EXISTS rep_consum_datum_daily_pkey;
ALTER TABLE solarrep.rep_consum_datum_daily
  ADD CONSTRAINT rep_consum_datum_daily_pkey PRIMARY KEY(node_id, created_day, source_id);
DROP INDEX solarrep.rep_consum_datum_daily_node_idx;

ALTER TABLE solarrep.rep_consum_datum_hourly DROP CONSTRAINT IF EXISTS rep_consum_datum_hourly_pkey;
ALTER TABLE solarrep.rep_consum_datum_hourly
  ADD CONSTRAINT rep_consum_datum_hourly_pkey PRIMARY KEY(node_id, created_hour, source_id);
DROP INDEX solarrep.rep_consum_datum_hourly_node_idx;

ALTER TABLE solarrep.rep_power_datum_daily DROP CONSTRAINT IF EXISTS rep_power_datum_daily_pkey;
ALTER TABLE solarrep.rep_power_datum_daily
  ADD CONSTRAINT rep_power_datum_daily_pkey PRIMARY KEY(node_id, created_day, source_id);
DROP INDEX solarrep.rep_power_datum_daily_node_idx;

ALTER TABLE solarrep.rep_power_datum_hourly DROP CONSTRAINT IF EXISTS rep_power_datum_hourly_pkey;
ALTER TABLE solarrep.rep_power_datum_hourly
  ADD CONSTRAINT rep_power_datum_hourly_pkey PRIMARY KEY(node_id, created_hour, source_id);
DROP INDEX solarrep.rep_power_datum_hourly_node_idx;

ALTER TABLE solarrep.rep_price_datum_daily DROP CONSTRAINT IF EXISTS rep_price_datum_daily_pkey;
ALTER TABLE solarrep.rep_price_datum_daily
  ADD CONSTRAINT rep_price_datum_daily_pkey PRIMARY KEY(loc_id, created_day);
DROP INDEX solarrep.rep_price_datum_daily_node_idx;

ALTER TABLE solarrep.rep_price_datum_hourly DROP CONSTRAINT IF EXISTS rep_price_datum_hourly_pkey;
ALTER TABLE solarrep.rep_price_datum_hourly
  ADD CONSTRAINT rep_price_datum_hourly_pkey PRIMARY KEY(loc_id, created_hour);
DROP INDEX solarrep.rep_price_datum_hourly_node_idx;

CLUSTER solarnet.sn_consum_datum USING consum_datum_node_unq;
CLUSTER solarnet.sn_day_datum USING sn_day_datum_loc_unq;
CLUSTER solarnet.sn_hardware_control_datum USING sn_hardware_control_datum_node_unq;
CLUSTER solarnet.sn_power_datum USING power_datum_node_unq;
CLUSTER solarnet.sn_price_datum USING price_datum_unq;
CLUSTER solarnet.sn_weather_datum USING sn_weather_datum_loc_unq;

CLUSTER solarrep.rep_consum_datum_daily USING rep_consum_datum_daily_pkey;
CLUSTER solarrep.rep_consum_datum_hourly USING rep_consum_datum_hourly_pkey;
CLUSTER solarrep.rep_power_datum_daily USING rep_power_datum_daily_pkey;
CLUSTER solarrep.rep_power_datum_hourly USING rep_power_datum_hourly_pkey;
CLUSTER solarrep.rep_price_datum_daily USING rep_price_datum_daily_pkey;
CLUSTER solarrep.rep_price_datum_hourly USING rep_price_datum_hourly_pkey;
