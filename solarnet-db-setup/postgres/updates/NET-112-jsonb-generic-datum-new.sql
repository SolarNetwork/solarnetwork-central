CREATE VIEW solardatum.da_datum_data AS
    SELECT d.ts, d.node_id, d.source_id, d.posted, solardatum.jdata_from_datum(d) AS jdata
    FROM solardatum.da_datum d;

CREATE VIEW solaragg.agg_datum_hourly_data AS
  SELECT d.ts_start, d.local_date, d.node_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
  FROM solaragg.agg_datum_hourly d;

CREATE VIEW solaragg.agg_datum_daily_data AS
  SELECT d.ts_start, d.local_date, d.node_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
  FROM solaragg.agg_datum_daily d;

CREATE VIEW solaragg.agg_datum_monthly_data AS
  SELECT d.ts_start, d.local_date, d.node_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
  FROM solaragg.agg_datum_monthly d;

