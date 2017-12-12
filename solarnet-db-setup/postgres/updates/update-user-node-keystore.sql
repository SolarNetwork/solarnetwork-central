DELETE FROM solaruser.user_node_cert;
ALTER TABLE solaruser.user_node_cert 
	DROP CONSTRAINT user_node_cert_pkey,
	DROP CONSTRAINT user_cert_user_fk,
	DROP CONSTRAINT user_node_cert_unq,
	DROP COLUMN id,
	DROP COLUMN conf_key,
	ADD COLUMN request_id VARCHAR(32) NOT NULL;

ALTER TABLE solaruser.user_node_cert RENAME cert TO keystore;
ALTER TABLE solaruser.user_node_cert ALTER COLUMN keystore SET NOT NULL;

ALTER TABLE solaruser.user_node_cert
  ADD CONSTRAINT user_cert_user_fk FOREIGN KEY (user_id)
      REFERENCES solaruser.user_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE solaruser.user_node_cert
  ADD CONSTRAINT user_node_cert_pkey PRIMARY KEY (user_id, node_id);

CREATE OR REPLACE FUNCTION solaruser.store_user_node_cert(
	created solarcommon.ts, 
	node solarcommon.node_id, 
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

