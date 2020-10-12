-- mark agg rows stale when da_datum_aux records change
CREATE TRIGGER aa_agg_stale_datum_aux
    BEFORE INSERT OR DELETE OR UPDATE
    ON solardatum.da_datum_aux
    FOR EACH ROW
    EXECUTE PROCEDURE solardatum.trigger_agg_stale_datum();

/**
 * After trigger to call `solaragg.handle_curr_change('h')` if the
 * changed row is for the current hour.
 */
CREATE TRIGGER curr_change
  AFTER INSERT OR UPDATE
  ON solaragg.agg_datum_hourly
  FOR EACH ROW
  WHEN (NEW.ts_start = date_trunc('hour', CURRENT_TIMESTAMP))
  EXECUTE PROCEDURE solaragg.handle_curr_change('h');

/**
 * After trigger to call `solaragg.handle_curr_change('d')` if the
 * changed row is for the current day in the rows's local time zone.
 */
CREATE TRIGGER curr_change
  AFTER INSERT OR UPDATE
  ON solaragg.agg_datum_daily
  FOR EACH ROW
  WHEN (date_trunc('day', CURRENT_TIMESTAMP - (NEW.ts_start - NEW.local_date))::date = NEW.local_date)
  EXECUTE PROCEDURE solaragg.handle_curr_change('d');

/**
 * After trigger to call `solaragg.handle_curr_change('m')` if the
 * changed row is for the current month in the rows's local time zone.
 */
CREATE TRIGGER curr_change
  AFTER INSERT OR UPDATE
  ON solaragg.agg_datum_monthly
  FOR EACH ROW
  WHEN (date_trunc('month', CURRENT_TIMESTAMP - (NEW.ts_start - NEW.local_date))::date = NEW.local_date)
  EXECUTE PROCEDURE solaragg.handle_curr_change('M');
