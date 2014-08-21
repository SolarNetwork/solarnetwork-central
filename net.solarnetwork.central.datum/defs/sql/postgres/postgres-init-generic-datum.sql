CREATE SCHEMA solardatum;

CREATE SCHEMA solaragg;

CREATE TABLE solardatum.sn_datum (
  ts solarcommon.ts,
  node_id solarcommon.node_id,
  source_id solarcommon.source_id,
  posted solarcommon.ts,
  jdata json NOT NULL,
  CONSTRAINT sn_consum_datum_pkey PRIMARY KEY (node_id, ts, source_id) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE TABLE solaragg.agg_stale_datum (
  ts_start timestamp with time zone NOT NULL,
  node_id solarcommon.node_id,
  source_id solarcommon.source_id,
  agg_kind char(1) NOT NULL,
  CONSTRAINT agg_stale_datum_pkey PRIMARY KEY (node_id, ts_start, source_id, agg_kind)
);

CREATE TABLE solaragg.agg_datum_hourly (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  local_time time without time zone NOT NULL,
  node_id solarcommon.node_id,
  source_id solarcommon.source_id,
  jdata json NOT NULL,
 CONSTRAINT agg_datum_hourly_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CREATE TABLE solaragg.agg_datum_daily (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  node_id solarcommon.node_id,
  source_id solarcommon.source_id,
  jdata json NOT NULL,
 CONSTRAINT agg_datum_daily_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CREATE OR REPLACE FUNCTION solardatum.trigger_agg_stale_datum()
  RETURNS trigger AS
$BODY$BEGIN
	CASE TG_OP
		WHEN 'INSERT', 'UPDATE' THEN
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', NEW.ts), NEW.node_id, NEW.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
			RETURN NEW;
		ELSE
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', OLD.ts), OLD.node_id, OLD.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
			RETURN OLD;
	END CASE;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE TRIGGER populate_agg_stale_datum
  AFTER INSERT OR UPDATE OR DELETE
  ON solardatum.sn_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.trigger_agg_stale_datum();
