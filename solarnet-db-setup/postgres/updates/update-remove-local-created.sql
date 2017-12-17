DROP INDEX solarnet.power_datum_local_created_idx;
DROP TRIGGER populate_local_created ON solarnet.sn_power_datum;
DROP FUNCTION solarrep.find_rep_net_power_datum(IN timestamp without time zone, IN interval);
DROP FUNCTION solarnet.populate_local_created();
ALTER TABLE solarnet.sn_power_datum DROP COLUMN local_created;
DROP FUNCTION solarrep.populate_rep_net_power_datum_hourly(datum solarnet.sn_power_datum);
DROP FUNCTION solarrep.populate_rep_net_power_datum_daily(datum solarnet.sn_power_datum);
