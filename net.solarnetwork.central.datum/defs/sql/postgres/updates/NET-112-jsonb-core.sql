ALTER TABLE solarnet.sn_node_meta
  ALTER COLUMN jdata SET DATA TYPE jsonb;

CREATE OR REPLACE FUNCTION solarnet.store_node_meta(
	cdate timestamp with time zone,
	node bigint,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate timestamp with time zone := now();
	jdata_json jsonb := jdata::jsonb;
BEGIN
	INSERT INTO solarnet.sn_node_meta(node_id, created, updated, jdata)
	VALUES (node, cdate, udate, jdata_json)
	ON CONFLICT (node_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;
