ALTER TABLE solardatum.da_datum
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t text[];

ALTER TABLE solardatum.da_meta
  ALTER COLUMN jdata SET DATA TYPE jsonb;

DROP VIEW solaragg.da_datum_avail_hourly;
DROP VIEW solaragg.da_datum_avail_daily;
DROP VIEW solaragg.da_datum_avail_monthly;

ALTER TABLE solaragg.agg_datum_hourly
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t text[];

ALTER TABLE solaragg.agg_datum_daily
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t text[];

ALTER TABLE solaragg.agg_datum_monthly
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t text[];

CREATE OR REPLACE FUNCTION solardatum.jdata_from_datum(datum solardatum.da_datum)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_datum_hourly)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_datum_daily)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_datum_monthly)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solardatum.store_meta(
	cdate timestamp with time zone,
	node bigint,
	src text,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate timestamp with time zone := now();
	jdata_json jsonb := jdata::jsonb;
BEGIN
	INSERT INTO solardatum.da_meta(node_id, source_id, created, updated, jdata)
	VALUES (node, src, cdate, udate, jdata_json)
	ON CONFLICT (node_id, source_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;

DROP FUNCTION solardatum.datum_prop_count(json);
CREATE OR REPLACE FUNCTION solardatum.datum_prop_count(IN jdata jsonb)
  RETURNS INTEGER
  LANGUAGE plv8
  IMMUTABLE AS
$BODY$
'use strict';
var count = 0, prop, val;
if ( jdata ) {
	for ( prop in jdata ) {
		val = jdata[prop];
		if ( Array.isArray(val) ) {
			count += val.length;
		} else {
			count += Object.keys(val).length;
		}
	}
}
return count;
$BODY$;
