CREATE TABLE solaruser.user_auth_token (
	user_id			BIGINT NOT NULL,
	auth_token		CHARACTER(20) NOT NULL,
	auth_secret		CHARACTER VARYING(64) NOT NULL,
	status			CHAR(1) NOT NULL,
	CONSTRAINT user_auth_token_pkey PRIMARY KEY (auth_token, user_id),
	CONSTRAINT user_auth_token_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE VIEW solaruser.user_auth_token_login  AS
	SELECT
		u.email AS username,
		t.auth_token AS auth_token,
		t.auth_secret AS password, 
		u.enabled AS enabled,
		u.id AS user_id,
		u.disp_name AS display_name
	FROM solaruser.user_auth_token t
	INNER JOIN solaruser.user_user u ON u.id = t.user_id
	WHERE t.status = 'a'::bpchar;

ALTER TABLE solaruser.user_node_conf 
ADD COLUMN sec_phrase CHARACTER VARYING(128);
ALTER TABLE solaruser.user_node_conf
ALTER COLUMN sec_phrase SET NOT NULL;

CREATE VIEW solaruser.network_association  AS
	SELECT
		unc.conf_key AS conf_key,
		unc.sec_phrase AS sec_phrase
	FROM solaruser.user_node_conf unc
	INNER JOIN solaruser.user_user u ON u.id = unc.user_id;
