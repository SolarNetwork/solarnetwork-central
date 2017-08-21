ALTER TABLE solaruser.user_user
  ADD COLUMN loc_id bigint;

ALTER TABLE solaruser.user_user
  ADD COLUMN billing_jdata jsonb;

CREATE INDEX user_user_billing_jdata_idx ON solaruser.user_user
	USING GIN (billing_jdata jsonb_path_ops);

ALTER TABLE solaruser.user_user
  ADD CONSTRAINT user_user_loc_fk FOREIGN KEY (loc_id) REFERENCES solarnet.sn_loc (id)
  ON UPDATE NO ACTION ON DELETE NO ACTION;
