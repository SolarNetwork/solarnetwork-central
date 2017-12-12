ALTER TABLE solaruser.user_meta
  ALTER COLUMN jdata SET DATA TYPE jsonb;

CREATE OR REPLACE FUNCTION solaruser.store_user_meta(
	cdate timestamp with time zone,
	userid BIGINT,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate timestamp with time zone := now();
	jdata_json jsonb := jdata::jsonb;
BEGIN
	INSERT INTO solaruser.user_meta(user_id, created, updated, jdata)
	VALUES (userid, cdate, udate, jdata_json)
	ON CONFLICT (user_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;

DROP FUNCTION solaruser.find_most_recent_datum_for_user(bigint[]);
CREATE OR REPLACE FUNCTION solaruser.find_most_recent_datum_for_user(users bigint[])
  RETURNS SETOF solardatum.da_datum AS
$BODY$
	SELECT r.*
	FROM (SELECT node_id FROM solaruser.user_node WHERE user_id = ANY(users)) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent(n.node_id)) AS r
	ORDER BY r.node_id, r.source_id;
$BODY$
  LANGUAGE sql STABLE;
