ALTER TABLE solaruser.user_user
  ADD COLUMN loc_id bigint;

ALTER TABLE solaruser.user_user
  ADD COLUMN billing_jdata jsonb;

ALTER TABLE solaruser.user_user
  ADD CONSTRAINT user_user_loc_fk FOREIGN KEY (loc_id) REFERENCES solarnet.sn_loc (id)
  ON UPDATE NO ACTION ON DELETE NO ACTION;
