-- Remove solarrep schema. 
DROP SCHEMA IF EXISTS solarrep CASCADE;

-- Remove legacy datum tables
DROP TABLE IF EXISTS solarnet.sn_consum_datum_most_recent;
DROP TABLE IF EXISTS solarnet.sn_consum_datum CASCADE;
DROP TABLE IF EXISTS solarnet.sn_day_datum CASCADE;
DROP TABLE IF EXISTS solarnet.sn_hardware_control_datum_most_recent;
DROP TABLE IF EXISTS solarnet.sn_hardware_control_datum CASCADE;
DROP TABLE IF EXISTS solarnet.sn_power_datum_most_recent;
DROP TABLE IF EXISTS solarnet.sn_power_datum CASCADE;
DROP TABLE IF EXISTS solarnet.sn_price_datum_most_recent;
DROP TABLE IF EXISTS solarnet.sn_price_datum CASCADE;
DROP TABLE IF EXISTS solarnet.sn_weather_datum CASCADE;

-- Unused sequences
DROP SEQUENCE IF EXISTS solarnet.consum_seq;
DROP SEQUENCE IF EXISTS solarnet.hardware_control_seq;
DROP SEQUENCE IF EXISTS solarnet.power_seq;
DROP SEQUENCE IF EXISTS solarnet.price_seq;
DROP SEQUENCE IF EXISTS solarnet.weather_seq;

-- Unused functions
DROP FUNCTION IF EXISTS solarnet.calc_avg_watt_hours(integer, integer, double precision, double precision, interval);
DROP FUNCTION IF EXISTS solarnet.calc_price_per_watt_hours(real, text);
DROP FUNCTION IF EXISTS solarnet.maintain_datum_most_recent(text, bigint, text, bigint, timestamp with time zone);
DROP FUNCTION IF EXISTS solarnet.maintain_loc_datum_most_recent(text, bigint, bigint, timestamp with time zone);

-- Trigger functions
DROP FUNCTION IF EXISTS solardatum.mig_consum_datum();
DROP FUNCTION IF EXISTS solardatum.mig_day_datum();
DROP FUNCTION IF EXISTS solardatum.mig_hardware_control_datum();
DROP FUNCTION IF EXISTS solardatum.mig_power_datum();
DROP FUNCTION IF EXISTS solardatum.mig_price_datum();
DROP FUNCTION IF EXISTS solardatum.mig_weather_datum();
DROP FUNCTION IF EXISTS solarnet.populate_consum_datum_most_recent();
DROP FUNCTION IF EXISTS solarnet.populate_hardware_control_datum_most_recent();
DROP FUNCTION IF EXISTS solarnet.populate_hl_temperature();
DROP FUNCTION IF EXISTS solarnet.populate_near_sky_condition();
DROP FUNCTION IF EXISTS solarnet.populate_power_datum_most_recent();
DROP FUNCTION IF EXISTS solarnet.populate_prev_consum_datum();
DROP FUNCTION IF EXISTS solarnet.populate_prev_power_datum();
DROP FUNCTION IF EXISTS solarnet.populate_prev_price_datum();
DROP FUNCTION IF EXISTS solarnet.populate_price_datum_most_recent();
