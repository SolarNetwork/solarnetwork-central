INSERT INTO solarrep.rep_consum_datum_hourly
SELECT * FROM solarnet.rep_consum_datum_hourly;

INSERT INTO solarrep.rep_consum_datum_daily
SELECT * FROM solarnet.rep_consum_datum_daily;

INSERT INTO solarrep.rep_power_datum_hourly
SELECT * FROM solarnet.rep_power_datum_hourly;

INSERT INTO solarrep.rep_net_power_datum_hourly
SELECT * FROM solarnet.rep_net_power_datum_hourly;

INSERT INTO solarrep.rep_power_datum_daily
SELECT * FROM solarnet.rep_power_datum_daily;

INSERT INTO solarrep.rep_net_power_datum_daily
SELECT * FROM solarnet.rep_net_power_datum_daily;

INSERT INTO solarrep.rep_price_datum_hourly
SELECT * FROM solarnet.rep_price_datum_hourly;

INSERT INTO solarrep.rep_price_datum_daily
SELECT * FROM solarnet.rep_price_datum_daily;

DROP TABLE solarnet.rep_consum_datum_hourly CASCADE;
DROP TABLE solarnet.rep_consum_datum_daily CASCADE;
DROP TABLE solarnet.rep_power_datum_hourly CASCADE;
DROP TABLE solarnet.rep_net_power_datum_hourly CASCADE;
DROP TABLE solarnet.rep_power_datum_daily CASCADE;
DROP TABLE solarnet.rep_net_power_datum_daily CASCADE;
DROP TABLE solarnet.rep_price_datum_hourly CASCADE;
DROP TABLE solarnet.rep_price_datum_daily CASCADE;

DROP FUNCTION solarnet.find_rep_consum_datum(IN bigint, IN text, 
	IN timestamp without time zone, IN text, IN interval);
DROP FUNCTION solarnet.populate_rep_consum_datum_hourly(datum solarnet.sn_consum_datum);
DROP TRIGGER populate_rep_consum_hourly ON solarnet.sn_consum_datum;
DROP FUNCTION solarnet.populate_rep_consum_hourly();

DROP FUNCTION solarnet.populate_rep_consum_datum_daily(datum solarnet.sn_consum_datum);
DROP TRIGGER populate_rep_consum_daily ON solarnet.sn_consum_datum;
DROP FUNCTION solarnet.populate_rep_consum_daily();

DROP FUNCTION solarnet.find_rep_net_power_datum(IN timestamp without time zone, IN interval);
DROP FUNCTION solarnet.find_rep_power_datum(IN bigint, IN text, IN timestamp without time zone, IN text, IN interval);
DROP FUNCTION solarnet.populate_rep_power_datum_hourly(datum solarnet.sn_power_datum);
DROP TRIGGER populate_rep_power_hourly ON solarnet.sn_power_datum;
DROP FUNCTION solarnet.populate_rep_power_hourly();

DROP FUNCTION solarnet.populate_rep_net_power_datum_hourly(datum solarnet.sn_power_datum);
DROP TRIGGER populate_rep_net_power_hourly ON solarnet.sn_power_datum;
DROP FUNCTION solarnet.populate_rep_net_power_hourly();

DROP FUNCTION solarnet.populate_rep_power_datum_daily(datum solarnet.sn_power_datum);
DROP TRIGGER populate_rep_power_daily ON solarnet.sn_power_datum;
DROP FUNCTION solarnet.populate_rep_power_daily();

DROP FUNCTION solarnet.populate_rep_net_power_datum_daily(datum solarnet.sn_power_datum);
DROP TRIGGER populate_rep_net_power_daily ON solarnet.sn_power_datum;
DROP FUNCTION solarnet.populate_rep_net_power_daily();

DROP FUNCTION solarnet.find_rep_price_datum(IN bigint, IN timestamp with time zone, IN interval);
DROP FUNCTION solarnet.populate_rep_price_datum_hourly(datum solarnet.sn_price_datum);
DROP TRIGGER populate_rep_price_hourly ON solarnet.sn_price_datum;
DROP FUNCTION solarnet.populate_rep_price_hourly();

DROP FUNCTION solarnet.populate_rep_price_datum_daily(datum solarnet.sn_price_datum);
DROP TRIGGER populate_rep_price_daily ON solarnet.sn_price_datum;
DROP FUNCTION solarnet.populate_rep_price_daily();
