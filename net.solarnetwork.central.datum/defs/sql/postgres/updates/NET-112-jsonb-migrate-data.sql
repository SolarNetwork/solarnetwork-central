UPDATE solardatum.da_datum
	SET jdata_i = jdata->'i', jdata_a = jdata->'a', jdata_s = jdata->'s',
		jdata_t = solarnet.json_array_to_text_arrary(jdata->'t');

UPDATE solaragg.agg_datum_hourly
	SET jdata_i = jdata->'i', jdata_a = jdata->'a', jdata_s = jdata->'s',
		jdata_t = solarnet.json_array_to_text_arrary(jdata->'t');

UPDATE solaragg.agg_datum_daily
	SET jdata_i = jdata->'i', jdata_a = jdata->'a', jdata_s = jdata->'s',
		jdata_t = solarnet.json_array_to_text_arrary(jdata->'t');

UPDATE solaragg.agg_datum_monthly
	SET jdata_i = jdata->'i', jdata_a = jdata->'a', jdata_s = jdata->'s',
		jdata_t = solarnet.json_array_to_text_arrary(jdata->'t');


UPDATE solardatum.da_loc_datum
	SET jdata_i = jdata->'i', jdata_a = jdata->'a', jdata_s = jdata->'s',
		jdata_t = solarnet.json_array_to_text_arrary(jdata->'t');

UPDATE solaragg.agg_loc_datum_hourly
	SET jdata_i = jdata->'i', jdata_a = jdata->'a', jdata_s = jdata->'s',
		jdata_t = solarnet.json_array_to_text_arrary(jdata->'t');

UPDATE solaragg.agg_loc_datum_daily
	SET jdata_i = jdata->'i', jdata_a = jdata->'a', jdata_s = jdata->'s',
		jdata_t = solarnet.json_array_to_text_arrary(jdata->'t');

UPDATE solaragg.agg_loc_datum_monthly
	SET jdata_i = jdata->'i', jdata_a = jdata->'a', jdata_s = jdata->'s',
		jdata_t = solarnet.json_array_to_text_arrary(jdata->'t');

