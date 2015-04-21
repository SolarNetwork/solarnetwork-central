CREATE TABLE solaruser.user_node_xfer (
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	node_id			solarcommon.node_id,
	recipient		CHARACTER VARYING(255) NOT NULL,
	CONSTRAINT user_node_xfer_pkey PRIMARY KEY (user_id, node_id),
	CONSTRAINT user_node_xfer_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX user_node_xfer_recipient_idx ON solaruser.user_node_xfer (recipient);

/**************************************************************************************************
 * FUNCTION solaruser.store_user_node_xfer(solarcommon.node_id, bigint, varchar, varchar)
 * 
 * Insert or update a user node transfer record.
 * 
 * @param node The ID of the node.
 * @param userid The ID of the user.
 * @param recip The recipient email of the requested owner.
 */
CREATE OR REPLACE FUNCTION solaruser.store_user_node_xfer(
	node solarcommon.node_id, 
	userid BIGINT, 
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
