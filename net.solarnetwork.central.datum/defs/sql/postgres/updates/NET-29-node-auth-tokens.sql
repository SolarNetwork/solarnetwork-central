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
