ALTER TABLE solarnet.sn_price_loc ADD COLUMN loc_id BIGINT;
UPDATE solarnet.sn_price_loc SET loc_id = 
	(SELECT id FROM solarnet.sn_loc WHERE country = '--' LIMIT 1);

ALTER TABLE solarnet.sn_price_loc
  ADD CONSTRAINT sn_price_loc_loc_fk FOREIGN KEY (loc_id)
      REFERENCES solarnet.sn_loc (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE solarnet.sn_price_loc DROP COLUMN time_zone;
