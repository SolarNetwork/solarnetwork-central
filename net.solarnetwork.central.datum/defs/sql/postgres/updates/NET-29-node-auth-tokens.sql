CREATE TABLE solaruser.user_node_auth_token (
	auth_token		CHARACTER(20) NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	node_id			BIGINT NOT NULL,
	auth_secret		CHARACTER VARYING(32) NOT NULL,
	status			CHAR(1) NOT NULL,
	CONSTRAINT user_node_auth_token_pkey PRIMARY KEY (auth_token),
	CONSTRAINT user_node_auth_token_node_fk FOREIGN KEY (node_id)
		REFERENCES solaruser.user_node (node_id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

DROP VIEW solaruser.node_auth_token_login;
CREATE VIEW solaruser.node_auth_token_login  AS
	SELECT
		n.node_id AS node_id,
		t.auth_token AS auth_token,
		t.auth_secret AS password,
		n.user_id AS user_id,
		u.email AS username,
		u.disp_name AS display_name,
		ut.auth_token AS user_auth_token,
		ut.auth_secret AS user_auth_secret
	FROM solaruser.user_node n
	INNER JOIN solaruser.user_user u ON u.id = n.user_id
	LEFT OUTER JOIN solaruser.user_node_auth_token t 
		ON t.node_id = n.node_id AND t.status = 'v'::bpchar
	LEFT OUTER JOIN solaruser.user_auth_token ut 
		ON ut.user_id = n.user_id AND t.status = 'v'::bpchar
	WHERE 
		u.enabled = TRUE

ALTER TABLE solaruser.user_node
   ADD COLUMN private boolean NOT NULL DEFAULT FALSE;
