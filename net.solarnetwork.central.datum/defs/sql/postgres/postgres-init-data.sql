/* Location data */

INSERT INTO solarnet.sn_loc (loc_name, country, region, time_zone) 
	VALUES ('Unknown', '--', '--', 'UTC');
INSERT INTO solarnet.sn_loc (loc_name, country, region, time_zone, latitude, longitude) 
	VALUES ('Auckland', 'NZ', 'Auckland', 'Pacific/Auckland', -36.85, 174.77);
INSERT INTO solarnet.sn_loc (loc_name, country, region, time_zone, latitude, longitude) 
	VALUES ('Wellington', 'NZ', 'Wellington', 'Pacific/Auckland', -41.29, 174.78);
INSERT INTO solarnet.sn_loc (loc_name, country, region, time_zone, latitude, longitude) 
	VALUES ('Sydney', 'AU', 'Sydney', 'Australia/Sydney', -33.82, 151);

/* Weather location data */

INSERT INTO solarnet.sn_weather_source (sname) VALUES ('Unknown');
INSERT INTO solarnet.sn_weather_source (sname) VALUES ('weather.com');
INSERT INTO solarnet.sn_weather_source (sname) VALUES ('NZ MetService');

INSERT INTO solarnet.sn_weather_loc (loc_id, source_id) 
	VALUES (
		(SELECT id FROM solarnet.sn_loc WHERE loc_name = 'Unknown'),
		(SELECT id FROM solarnet.sn_weather_source WHERE sname = 'Unknown'));
INSERT INTO solarnet.sn_weather_loc (loc_id, source_id, source_data) 
	VALUES (
		(SELECT id FROM solarnet.sn_loc WHERE loc_name = 'Auckland'),
		(SELECT id FROM solarnet.sn_weather_source WHERE sname = 'weather.com'),
		'NZXX0003');
INSERT INTO solarnet.sn_weather_loc (loc_id, source_id, source_data) 
	VALUES (
		(SELECT id FROM solarnet.sn_loc WHERE loc_name = 'Wellington'),
		(SELECT id FROM solarnet.sn_weather_source WHERE sname = 'weather.com'),
		'NZXX0049');
INSERT INTO solarnet.sn_weather_loc (loc_id, source_id, source_data) 
	VALUES (
		(SELECT id FROM solarnet.sn_loc WHERE loc_name = 'Sydney'),
		(SELECT id FROM solarnet.sn_weather_source WHERE sname = 'weather.com'),
		'ASXX0087');
INSERT INTO solarnet.sn_weather_loc (loc_id, source_id, source_data) 
	VALUES (
		(SELECT id FROM solarnet.sn_loc WHERE loc_name = 'Auckland'),
		(SELECT id FROM solarnet.sn_weather_source WHERE sname = 'NZ MetService'),
		'Auckland');
INSERT INTO solarnet.sn_weather_loc (loc_id, source_id, source_data) 
	VALUES (
		(SELECT id FROM solarnet.sn_loc WHERE loc_name = 'Wellington'),
		(SELECT id FROM solarnet.sn_weather_source WHERE sname = 'NZ MetService'),
		'Wellington');

/* Price location data */

INSERT INTO solarnet.sn_price_source (sname) VALUES ('Unknown');
INSERT INTO solarnet.sn_price_source (sname) VALUES ('electricityinfo.co.nz');

INSERT INTO solarnet.sn_price_loc (loc_name, currency, unit, time_zone, source_id) 
	VALUES ('Unknown', '--', '--', 'UTC', 
	(SELECT id FROM solarnet.sn_price_source WHERE sname = 'Unknown'));
INSERT INTO solarnet.sn_price_loc (loc_name, currency, unit, time_zone, source_id) 
	VALUES ('HAY2201', 'NZD', 'MWh', 'Pacific/Auckland',
	(SELECT id FROM solarnet.sn_price_source WHERE sname = 'electricityinfo.co.nz'));

