ALTER TABLE solardatum.da_datum
	DROP COLUMN jdata;

ALTER TABLE solaragg.agg_datum_hourly
	DROP COLUMN jdata;

ALTER TABLE solaragg.agg_datum_daily
	DROP COLUMN jdata;

ALTER TABLE solaragg.agg_datum_monthly
	DROP COLUMN jdata;


ALTER TABLE solardatum.da_loc_datum
	DROP COLUMN jdata;

ALTER TABLE solaragg.agg_loc_datum_hourly
	DROP COLUMN jdata;

ALTER TABLE solaragg.agg_loc_datum_daily
	DROP COLUMN jdata;

ALTER TABLE solaragg.agg_loc_datum_monthly
	DROP COLUMN jdata;
