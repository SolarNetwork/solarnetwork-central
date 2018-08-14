-- drop dependent foreign key constraints
ALTER TABLE solarnet.sn_weather_loc DROP CONSTRAINT sn_weather_location_sn_loc_fk;
ALTER TABLE solarnet.sn_node DROP CONSTRAINT sn_node_loc_fk;
ALTER TABLE solarnet.sn_price_loc DROP CONSTRAINT sn_price_loc_loc_fk;
ALTER TABLE solaruser.user_user DROP CONSTRAINT user_user_loc_fk;

-- drop primary key
ALTER TABLE solarnet.sn_loc DROP CONSTRAINT sn_loc_pkey;

-- adding tz/country to index  for index-only scan support
ALTER TABLE solarnet.sn_loc ADD PRIMARY KEY (id, time_zone, country);

-- recreate foreign key constraints
ALTER TABLE solarnet.sn_weather_loc ADD
	CONSTRAINT sn_weather_location_sn_loc_fk FOREIGN KEY (loc_id)
		REFERENCES solarnet.sn_loc (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE solarnet.sn_node ADD
	CONSTRAINT sn_node_loc_fk FOREIGN KEY (loc_id)
		REFERENCES solarnet.sn_loc (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE solarnet.sn_price_loc ADD
    CONSTRAINT sn_price_loc_loc_fk FOREIGN KEY (loc_id)
  	    REFERENCES solarnet.sn_loc (id) MATCH SIMPLE
	    ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE solaruser.user_user ADD
	CONSTRAINT user_user_loc_fk FOREIGN KEY (loc_id)
		REFERENCES solarnet.sn_loc (id)
		ON UPDATE NO ACTION ON DELETE NO ACTION;
