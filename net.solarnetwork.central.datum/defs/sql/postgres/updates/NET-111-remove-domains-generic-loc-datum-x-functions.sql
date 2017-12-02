DROP FUNCTION solardatum.store_loc_datum(solarcommon.ts, solarcommon.loc_id, solarcommon.source_id, solarcommon.ts, text);
CREATE OR REPLACE FUNCTION solardatum.store_loc_datum(
	cdate timestamp with time zone,
	loc bigint,
	src text,
	pdate timestamp with time zone,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	ts_crea timestamp with time zone := COALESCE(cdate, now());
	ts_post timestamp with time zone := COALESCE(pdate, now());
	jdata_json jsonb := jdata::jsonb;
	jdata_prop_count integer := solardatum.datum_prop_count(jdata_json);
	ts_post_hour timestamp with time zone := date_trunc('hour', ts_post);
BEGIN
	INSERT INTO solardatum.da_loc_datum(ts, loc_id, source_id, posted, jdata)
	VALUES (ts_crea, loc, src, ts_post, jdata_json)
	ON CONFLICT (loc_id, ts, source_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, posted = EXCLUDED.posted;

	INSERT INTO solaragg.aud_loc_datum_hourly (
		ts_start, loc_id, source_id, prop_count)
	VALUES (ts_post_hour, loc, src, jdata_prop_count)
	ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
	SET prop_count = aud_loc_datum_hourly.prop_count + EXCLUDED.prop_count;
END;
$BODY$;
