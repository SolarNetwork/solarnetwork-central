ALTER TABLE solaruser.user_node
   ADD COLUMN private boolean NOT NULL DEFAULT FALSE;

CREATE TABLE solaruser.user_auth_token_node (
	auth_token CHARACTER(20) NOT NULL, 
	node_id BIGINT NOT NULL,
	CONSTRAINT user_auth_token_node_pkey PRIMARY KEY (auth_token, node_id),
	CONSTRAINT user_auth_token_node_token_fk FOREIGN KEY (auth_token)
		REFERENCES solaruser.user_auth_token(auth_token) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE,
	CONSTRAINT user_auth_token_node_node_fk FOREIGN KEY (node_id)
		REFERENCES solarnet.sn_node (node_id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE OR REPLACE VIEW solaruser.user_auth_token_login  AS
	SELECT
		t.auth_token AS username,
		t.auth_secret AS password, 
		u.enabled AS enabled,
		u.id AS user_id,
		u.disp_name AS display_name,
		CAST(t.token_type AS character varying) AS token_type,
		ARRAY(SELECT n.node_id 
			FROM solaruser.user_auth_token_node n 
			WHERE n.auth_token = t.auth_token) AS node_ids
	FROM solaruser.user_auth_token t
	INNER JOIN solaruser.user_user u ON u.id = t.user_id
	WHERE 
		t.status = CAST('Active' AS solaruser.user_auth_token_status);

