/**
 * Count the properties in a datum JSON object.
 *
 * @param jdata				the datum JSON
 *
 * @returns The property count.
 */
CREATE OR REPLACE FUNCTION solardatum.datum_prop_count(IN jdata json)
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

CREATE TABLE solaragg.aud_datum_hourly (
  ts_start timestamp with time zone NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  prop_count integer NOT NULL,
  CONSTRAINT aud_datum_hourly_pkey PRIMARY KEY (node_id, ts_start, source_id) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE OR REPLACE FUNCTION solardatum.store_datum(
	cdate solarcommon.ts,
	node solarcommon.node_id,
	src solarcommon.source_id,
	pdate solarcommon.ts,
	jdata text)
  RETURNS void AS
$BODY$
DECLARE
	ts_crea solarcommon.ts := COALESCE(cdate, now());
	ts_post solarcommon.ts := COALESCE(pdate, now());
	jdata_json json := jdata::json;
	jdata_prop_count integer := solardatum.datum_prop_count(jdata_json);
	ts_post_hour timestamp with time zone := date_trunc('hour', ts_post);
BEGIN
	BEGIN
		INSERT INTO solardatum.da_datum(ts, node_id, source_id, posted, jdata)
		VALUES (ts_crea, node, src, ts_post, jdata_json);
	EXCEPTION WHEN unique_violation THEN
		-- We mostly expect inserts, but we allow updates
		UPDATE solardatum.da_datum SET 
			jdata = jdata_json, 
			posted = ts_post
		WHERE
			node_id = node
			AND ts = ts_crea
			AND source_id = src;
	END;

	-- for auditing we mostly expect updates
	<<update_audit>>
	LOOP
		UPDATE solaragg.aud_datum_hourly 
		SET prop_count = prop_count + jdata_prop_count
		WHERE
			node_id = node
			AND source_id = src
			AND ts_start = ts_post_hour;

		EXIT update_audit WHEN FOUND;

		INSERT INTO solaragg.aud_datum_hourly (
			ts_start, node_id, source_id, prop_count)
		VALUES (
			ts_post_hour,
			node,
			src,
			jdata_prop_count
		);
		EXIT update_audit;
	END LOOP update_audit;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE TABLE solaragg.aud_loc_datum_hourly (
  ts_start timestamp with time zone NOT NULL,
  loc_id solarcommon.loc_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  prop_count integer NOT NULL,
  CONSTRAINT aud_loc_datum_hourly_pkey PRIMARY KEY (loc_id, ts_start, source_id) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE OR REPLACE FUNCTION solardatum.store_loc_datum(
	cdate solarcommon.ts,
	loc solarcommon.loc_id,
	src solarcommon.source_id,
	pdate solarcommon.ts,
	jdata text)
  RETURNS void AS
$BODY$
DECLARE
	ts_crea solarcommon.ts := COALESCE(cdate, now());
	ts_post solarcommon.ts := COALESCE(pdate, now());
	jdata_json json := jdata::json;
	jdata_prop_count integer := solardatum.datum_prop_count(jdata_json);
	ts_post_hour timestamp with time zone := date_trunc('hour', ts_post);
BEGIN
	BEGIN
		INSERT INTO solardatum.da_loc_datum(ts, loc_id, source_id, posted, jdata)
		VALUES (ts_crea, loc, src, ts_post, jdata_json);
	EXCEPTION WHEN unique_violation THEN
		-- We mostly expect inserts, but we allow updates
		UPDATE solardatum.da_loc_datum SET
			jdata = jdata_json,
			posted = ts_post
		WHERE
			loc_id = loc
			AND ts = ts_crea
			AND source_id = src;
	END;
	
	-- for auditing we mostly expect updates
	<<update_audit>>
	LOOP
		UPDATE solaragg.aud_loc_datum_hourly 
		SET prop_count = prop_count + jdata_prop_count
		WHERE
			loc_id = loc
			AND source_id = src
			AND ts_start = ts_post_hour;

		EXIT update_audit WHEN FOUND;

		INSERT INTO solaragg.aud_loc_datum_hourly (
			ts_start, loc_id, source_id, prop_count)
		VALUES (
			ts_post_hour,
			loc,
			src,
			jdata_prop_count
		);
		EXIT update_audit;
	END LOOP update_audit;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE;
