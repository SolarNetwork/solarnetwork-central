-- solardatum location datum functions that rely on other namespaces like solaragg

/**
 * Add or update a location datum record. The data is stored in the <code>solardatum.da_loc_datum</code> table.
 *
 * @param cdate The datum creation date.
 * @param loc The location ID.
 * @param src The source ID.
 * @param pdate The date the datum was posted to SolarNet.
 * @param jdata The datum JSON document.
 */
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
	INSERT INTO solardatum.da_loc_datum(ts, loc_id, source_id, posted, jdata_i, jdata_a, jdata_s, jdata_t)
	VALUES (ts_crea, loc, src, ts_post, jdata_json->'i', jdata_json->'a', jdata_json->'s', solarcommon.json_array_to_text_array(jdata_json->'t'))
	ON CONFLICT (loc_id, ts, source_id) DO UPDATE
	SET jdata_i = EXCLUDED.jdata_i,
		jdata_a = EXCLUDED.jdata_a,
		jdata_s = EXCLUDED.jdata_s,
		jdata_t = EXCLUDED.jdata_t,
		posted = EXCLUDED.posted;

	INSERT INTO solaragg.aud_loc_datum_hourly (
		ts_start, loc_id, source_id, prop_count)
	VALUES (ts_post_hour, loc, src, jdata_prop_count)
	ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
	SET prop_count = aud_loc_datum_hourly.prop_count + EXCLUDED.prop_count;
END;
$BODY$;
