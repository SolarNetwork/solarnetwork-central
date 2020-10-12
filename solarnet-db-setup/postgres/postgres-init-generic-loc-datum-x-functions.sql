-- solardatum location datum functions that rely on other namespaces like solaragg

/**
 * Add or update a location datum record. The data is stored in the `solardatum.da_loc_datum` table.
 *
 * This function also updates the `prop_count` column in the `solaragg.aud_loc_datum_hourly` table
 * for the appropriate row, and inserts the results of the `solardatum.calculate_stale_loc_datum()`
 * function into the `solaragg.agg_stale_loc_datum` table.
 *
 * @param cdate The datum creation date.
 * @param loc The location ID.
 * @param src The source ID.
 * @param pdate The date the datum was posted to SolarNet.
 * @param jdata The datum JSON document.
 */
CREATE OR REPLACE FUNCTION solardatum.store_loc_datum(
	cdate 	TIMESTAMP WITH TIME ZONE,
	loc 	BIGINT,
	src 	TEXT,
	pdate 	TIMESTAMP WITH TIME ZONE,
	jdata 	TEXT)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_crea 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(cdate, now());
	ts_post 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(pdate, now());
	jdata_json 			JSONB 						:= jdata::jsonb;
	jdata_prop_count 	INTEGER 					:= solardatum.datum_prop_count(jdata_json);
	ts_post_hour 		TIMESTAMP WITH TIME ZONE 	:= date_trunc('hour', ts_post);
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

	INSERT INTO solaragg.agg_stale_loc_datum (agg_kind, loc_id, ts_start, source_id)
	SELECT 'h' AS agg_kind, loc_id, ts_start, source_id
	FROM solardatum.calculate_stale_loc_datum(loc, src, cdate)
	ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;
END;
$$;
