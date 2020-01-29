ALTER TABLE solarnet.sn_node
	DROP CONSTRAINT sn_node_weather_loc_fk,
	DROP COLUMN wloc_id;
DROP TABLE solarnet.sn_price_loc;
DROP TABLE solarnet.sn_price_source;
DROP TABLE solarnet.sn_weather_loc;
DROP TABLE solarnet.sn_weather_source;
DROP SEQUENCE IF EXISTS solarnet.price_seq;
DROP SEQUENCE IF EXISTS solarnet.weather_seq;

DROP TABLE solaruser.user_node_hardware_control;
DROP TABLE solarnet.sn_hardware_control;
DROP TABLE solarnet.sn_hardware;
DROP SEQUENCE IF EXISTS solarnet.hardware_control_seq;
