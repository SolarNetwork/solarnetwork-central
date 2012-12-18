CREATE TYPE solaruser.user_auth_token_status AS ENUM 
	('Active', 'Disabled');

CREATE TYPE solaruser.user_auth_token_type AS ENUM 
	('User', 'ReadNodeData');

CREATE TABLE solaruser.user_auth_token (
	auth_token		CHARACTER(20) NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	auth_secret		CHARACTER VARYING(32) NOT NULL,
	status			solaruser.user_auth_token_status NOT NULL,
	token_type		solaruser.user_auth_token_type NOT NULL,
	CONSTRAINT user_auth_token_pkey PRIMARY KEY (auth_token),
	CONSTRAINT user_auth_token_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE VIEW solaruser.user_auth_token_login  AS
	SELECT
		t.auth_token AS username,
		t.auth_secret AS password, 
		u.enabled AS enabled,
		u.id AS user_id,
		u.disp_name AS display_name,
		CAST(t.token_type AS character varying) AS token_type
	FROM solaruser.user_auth_token t
	INNER JOIN solaruser.user_user u ON u.id = t.user_id
	WHERE 
		t.status = CAST('Active' AS solaruser.user_auth_token_status)
		AND t.token_type = CAST('User' AS solaruser.user_auth_token_type);

CREATE VIEW solaruser.user_auth_token_role AS
	SELECT
		t.auth_token AS username,
		'ROLE_' || upper(CAST(t.token_type AS character varying)) AS authority
	FROM solaruser.user_auth_token t;
		
ALTER TABLE solaruser.user_node_conf 
ADD COLUMN sec_phrase CHARACTER VARYING(128);
ALTER TABLE solaruser.user_node_conf
ALTER COLUMN sec_phrase SET NOT NULL;

ALTER TABLE solaruser.user_node_conf
ALTER COLUMN node_id DROP NOT NULL;

ALTER TABLE solaruser.user_node_conf
DROP COLUMN conf_val;

CREATE VIEW solaruser.network_association  AS
	SELECT
		unc.conf_key AS conf_key,
		unc.sec_phrase AS sec_phrase
	FROM solaruser.user_node_conf unc
	INNER JOIN solaruser.user_user u ON u.id = unc.user_id;

CREATE TABLE solaruser.user_node_cert (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.solaruser_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	conf_key		CHARACTER(64) NOT NULL,
	node_id			BIGINT NOT NULL,
	status			CHAR(1) NOT NULL,
	cert			bytea,
	CONSTRAINT user_node_cert_pkey PRIMARY KEY (id),
	CONSTRAINT user_cert_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT user_node_cert_unq UNIQUE (user_id, conf_key)
);
