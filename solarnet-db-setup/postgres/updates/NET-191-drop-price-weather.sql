ALTER TABLE solarnet.sn_node
	DROP CONSTRAINT sn_node_weather_loc_fk,
	DROP COLUMN wloc_id;
DROP TABLE solarnet.sn_price_loc;
DROP TABLE solarnet.sn_price_source;
DROP TABLE solarnet.sn_weather_loc;
DROP TABLE solarnet.sn_weather_source;
DROP SEQUENCE solarnet.price_seq;
DROP SEQUENCE solarnet.weather_seq;

DROP TABLE solaruser.user_node_hardware_control;
DROP TABLE solarnet.sn_hardware_control;
DROP TABLE solarnet.sn_hardware;
DROP SEQUENCE solarnet.hardware_control_seq;
