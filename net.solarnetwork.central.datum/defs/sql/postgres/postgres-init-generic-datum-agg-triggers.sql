-- NOTE the trigger name has aa_ prefix so sorts before pg_partman trigger name
CREATE TRIGGER aa_agg_stale_datum
  BEFORE INSERT OR UPDATE OR DELETE
  ON solardatum.da_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.trigger_agg_stale_datum();

