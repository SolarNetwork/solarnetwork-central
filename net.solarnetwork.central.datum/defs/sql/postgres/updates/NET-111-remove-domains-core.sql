\echo Removing domains from node meta...

ALTER TABLE solarnet.sn_node_meta
  ALTER COLUMN node_id SET DATA TYPE bigint,
  ALTER COLUMN created SET DATA TYPE timestamp with time zone,
  ALTER COLUMN updated SET DATA TYPE timestamp with time zone;

DROP FUNCTION solarnet.store_node_meta(solarcommon.ts, solarcommon.node_id, text);
CREATE OR REPLACE FUNCTION solarnet.store_node_meta(
	cdate timestamp with time zone,
	node bigint,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate timestamp with time zone := now();
	jdata_json json := jdata::json;
BEGIN
	INSERT INTO solarnet.sn_node_meta(node_id, created, updated, jdata)
	VALUES (node, cdate, udate, jdata_json)
	ON CONFLICT (node_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;
