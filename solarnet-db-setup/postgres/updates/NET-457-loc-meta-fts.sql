ALTER TABLE solardatm.da_loc_datm_meta ADD COLUMN fts_default tsvector;

CREATE INDEX da_loc_datm_meta_fts_default_idx ON solardatm.da_loc_datm_meta USING gin(fts_default);

CREATE OR REPLACE FUNCTION solardatm.da_loc_datm_meta_maintain_fts()
	RETURNS "trigger"  LANGUAGE 'plpgsql' VOLATILE AS $$
BEGIN
	NEW.fts_default :=
		   to_tsvector('pg_catalog.english', COALESCE(NEW.jdata #>> '{m,name}',''));
	RETURN NEW;
END
$$;

CREATE TRIGGER da_loc_datm_meta_maintain_fts
  BEFORE INSERT OR UPDATE
  ON solardatm.da_loc_datm_meta
  FOR EACH ROW
  EXECUTE PROCEDURE solardatm.da_loc_datm_meta_maintain_fts();

-- trigger sn_loc_req_maintain_fts on all rows
UPDATE solardatm.da_loc_datm_meta SET loc_id = loc_id;
