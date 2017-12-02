ALTER TABLE solaruser.user_meta
  ALTER COLUMN created SET DATA TYPE timestamp with time zone,
  ALTER COLUMN updated SET DATA TYPE timestamp with time zone;

DROP FUNCTION solaruser.store_user_meta(solarcommon.ts, bigint, text);
CREATE OR REPLACE FUNCTION solaruser.store_user_meta(
	cdate timestamp with time zone,
	userid BIGINT,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate timestamp with time zone := now();
	jdata_json json := jdata::json;
BEGIN
	INSERT INTO solaruser.user_meta(user_id, created, updated, jdata)
	VALUES (userid, cdate, udate, jdata_json)
	ON CONFLICT (user_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;

DROP FUNCTION solaruser.store_user_node_cert(solarcommon.ts, solarcommon.node_id, bigint, character, text, bytea);
CREATE OR REPLACE FUNCTION solaruser.store_user_node_cert(
	created timestamp with time zone,
	node bigint,
	userid bigint,
	stat char,
	request text,
	keydata bytea)
  RETURNS void AS
$BODY$
DECLARE
	ts TIMESTAMP WITH TIME ZONE := (CASE WHEN created IS NULL THEN now() ELSE created END);
BEGIN
	BEGIN
		INSERT INTO solaruser.user_node_cert(created, node_id, user_id, status, request_id, keystore)
		VALUES (ts, node, userid, stat, request, keydata);
	EXCEPTION WHEN unique_violation THEN
		UPDATE solaruser.user_node_cert SET
			keystore = keydata,
			status = stat,
			request_id = request
		WHERE
			node_id = node
			AND user_id = userid;
	END;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

ALTER TABLE solaruser.user_node_xfer
	ALTER COLUMN node_id SET DATA TYPE bigint;

DROP FUNCTION solaruser.store_user_node_xfer(solarcommon.node_id, bigint, character varying);
CREATE OR REPLACE FUNCTION solaruser.store_user_node_xfer(
	node bigint,
	userid bigint,
	recip CHARACTER VARYING(255))
  RETURNS void AS
$BODY$
BEGIN
	BEGIN
		INSERT INTO solaruser.user_node_xfer(node_id, user_id, recipient)
		VALUES (node, userid, recip);
	EXCEPTION WHEN unique_violation THEN
		UPDATE solaruser.user_node_xfer SET
			recipient = recip
		WHERE
			node_id = node
			AND user_id = userid;
	END;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;
