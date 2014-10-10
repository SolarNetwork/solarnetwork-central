ALTER TABLE solarnet.sn_loc ALTER COLUMN latitude TYPE numeric(9,6);
ALTER TABLE solarnet.sn_loc ALTER COLUMN longitude TYPE numeric(9,6);
ALTER TABLE solarnet.sn_loc ADD COLUMN elevation numeric(8,3);

DROP TRIGGER maintain_fts ON solarnet.sn_loc;
CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solarnet.sn_loc
  FOR EACH ROW EXECUTE PROCEDURE 
  tsvector_update_trigger(fts_default, 'pg_catalog.english', 
  	country, region, state_prov, locality, postal_code, address);

ALTER TABLE solarnet.sn_loc DROP COLUMN loc_name;
