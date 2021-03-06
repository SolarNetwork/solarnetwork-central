CREATE VIEW solardatum.da_loc_datum_data AS
    SELECT d.ts, d.loc_id, d.source_id, d.posted, solardatum.jdata_from_datum(d) AS jdata
    FROM solardatum.da_loc_datum d;

CREATE VIEW solaragg.agg_loc_datum_hourly_data AS
    SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
    FROM solaragg.agg_loc_datum_hourly d;

CREATE VIEW solaragg.agg_loc_datum_daily_data AS
    SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
    FROM solaragg.agg_loc_datum_daily d;

CREATE VIEW solaragg.agg_loc_datum_monthly_data AS
    SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d) AS jdata
    FROM solaragg.agg_loc_datum_monthly d;

