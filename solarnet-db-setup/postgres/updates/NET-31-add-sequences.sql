
CREATE SEQUENCE solarnet.weather_seq;
SELECT setval('solarnet.weather_seq', (SELECT COALESCE(MAX(id), 1) FROM (
	SELECT MAX(id) AS id FROM solarnet.sn_day_datum
	UNION
	SELECT MAX(id) AS id FROM solarnet.sn_weather_datum
) AS ids), true);
ALTER TABLE solarnet.sn_day_datum ALTER COLUMN id SET DEFAULT nextval('solarnet.weather_seq');

CREATE SEQUENCE solarnet.hardware_control_seq;
SELECT setval('solarnet.hardware_control_seq', (SELECT COALESCE(MAX(id), 1) FROM solarnet.sn_hardware_control_datum), true);
ALTER TABLE solarnet.sn_hardware_control_datum ALTER COLUMN id SET DEFAULT nextval('solarnet.hardware_control_seq');

CREATE SEQUENCE solarnet.price_seq;
SELECT setval('solarnet.price_seq', (SELECT COALESCE(MAX(id), 1) FROM solarnet.sn_price_datum), true);
ALTER TABLE solarnet.sn_price_datum ALTER COLUMN id SET DEFAULT nextval('solarnet.price_seq');

CREATE SEQUENCE solarnet.power_seq;
SELECT setval('solarnet.power_seq', (SELECT COALESCE(MAX(id), 1) FROM solarnet.sn_power_datum), true);
ALTER TABLE solarnet.sn_power_datum ALTER COLUMN id SET DEFAULT nextval('solarnet.power_seq');

CREATE SEQUENCE solarnet.consum_seq;
SELECT setval('solarnet.consum_seq', (SELECT COALESCE(MAX(id), 1) FROM solarnet.sn_consum_datum), true);
ALTER TABLE solarnet.sn_consum_datum ALTER COLUMN id SET DEFAULT nextval('solarnet.consum_seq');

