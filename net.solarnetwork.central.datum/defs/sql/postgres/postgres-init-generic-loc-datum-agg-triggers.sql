-- NOTE the trigger name has aa_ prefix so sorts before pg_partman trigger name
CREATE TRIGGER aa_agg_stale_loc_datum
  BEFORE INSERT OR UPDATE OR DELETE
  ON solardatum.da_loc_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.trigger_agg_stale_loc_datum();

