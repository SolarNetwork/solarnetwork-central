/**
 * TABLE solaragg.agg_stale_flux
 *
 * Holds records for stale aggregate SolarFlux publishing support. There is no time component to
 * this table because SolarFlux cares only about the "most recent" value. Thus a record in this
 * table means the "most recent" data for the associated node + source + agg_kind needs to be
 * published to SolarFlux.
 *
 * This table serves as a queue for updates. Any number of workers are expected to read from this
 * table and publish the updated data to SolarFlux.
 */
CREATE TABLE IF NOT EXISTS solaragg.agg_stale_flux (
  agg_kind  char(1) NOT NULL,
  node_id   bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  created   timestamp NOT NULL DEFAULT now(),
  CONSTRAINT agg_stale_flux_pkey PRIMARY KEY (agg_kind, node_id, source_id)
);

/**
 * Trigger function to handle a changed (inserted, updated) aggregate datum row.
 *
 * The trigger must be passed the aggregate type as the first trigger argument. It then inserts
 * a row into the `solaragg.agg_stale_flux` for clients to pull from. 
 */
CREATE OR REPLACE FUNCTION solaragg.handle_curr_change()
  RETURNS trigger LANGUAGE 'plpgsql' AS
$$
BEGIN
	INSERT INTO solaragg.agg_stale_flux (agg_kind, node_id, source_id)
	VALUES (TG_ARGV[0], NEW.node_id, NEW.source_id)
	ON CONFLICT (agg_kind, node_id, source_id) DO NOTHING;
	RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS curr_change ON solaragg.agg_datum_hourly;

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

DROP TRIGGER IF EXISTS curr_change ON solaragg.agg_datum_daily;

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

DROP TRIGGER IF EXISTS curr_change ON solaragg.agg_datum_monthly;

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
