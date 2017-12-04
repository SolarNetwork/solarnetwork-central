ALTER TABLE solardatum.da_loc_datum
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t jsonb;

ALTER TABLE solardatum.da_loc_meta
  ALTER COLUMN jdata SET DATA TYPE jsonb;

ALTER TABLE solaragg.agg_loc_datum_hourly
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t jsonb;

ALTER TABLE solaragg.agg_loc_datum_daily
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t jsonb;

ALTER TABLE solaragg.agg_loc_datum_monthly
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t jsonb;

CREATE OR REPLACE FUNCTION solardatum.jdata_from_datum(datum solardatum.da_loc_datum)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_loc_datum_hourly)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_loc_datum_daily)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_loc_datum_monthly)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

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

CREATE OR REPLACE FUNCTION solardatum.store_loc_meta(
	cdate timestamp with time zone,
	loc bigint,
	src text,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate timestamp with time zone := now();
	jdata_json jsonb := jdata::jsonb;
BEGIN
	INSERT INTO solardatum.da_loc_meta(loc_id, source_id, created, updated, jdata)
	VALUES (loc, src, cdate, udate, jdata_json)
	ON CONFLICT (loc_id, source_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;
