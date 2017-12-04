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

CREATE OR REPLACE FUNCTION solarnet.jdata_from_components(jdata_i jsonb, jdata_a jsonb, jdata_s jsonb, jdata_t jsonb)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
SELECT jsonb_set(jsonb_set(jsonb_set(jsonb_set(
				'{"t":null}'::jsonb, '{t}', COALESCE(jdata_t, 'null'::jsonb)),
			'{s}', COALESCE(jdata_s, 'null'::jsonb)),
		'{a}', COALESCE(jdata_a, 'null'::jsonb)),
	'{i}', COALESCE(jdata_i, 'null'::jsonb));
$$;

CREATE OR REPLACE FUNCTION solarnet.components_from_jdata(
	IN jdata jsonb,
	OUT jdata_i jsonb,
	OUT jdata_a jsonb,
	OUT jdata_s jsonb,
	OUT jdata_t jsonb)
	LANGUAGE SQL IMMUTABLE AS
$$
SELECT jdata->'i', jdata->'a', jdata->'s', jdata->'t'
$$;
