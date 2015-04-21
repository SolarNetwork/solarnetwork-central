/* === USER ================================================================ */

CREATE SCHEMA solaruser;

CREATE SEQUENCE solaruser.solaruser_seq;

/**
 * user_user: main table for user information.
 */
CREATE TABLE solaruser.user_user (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.solaruser_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	disp_name		CHARACTER VARYING(128) NOT NULL,
	email			citext NOT NULL,
	password		CHARACTER VARYING(128) NOT NULL,
	enabled			BOOLEAN NOT NULL DEFAULT TRUE,
	CONSTRAINT user_user_pkey PRIMARY KEY (id),
	CONSTRAINT user_user_email_unq UNIQUE (email)
);

/**
 * user_role: user granted login roles
 */
CREATE TABLE solaruser.user_role (
	user_id			BIGINT NOT NULL,
	role_name		CHARACTER VARYING(128) NOT NULL,
	CONSTRAINT user_role_pkey PRIMARY KEY (user_id, role_name),
	CONSTRAINT fk_user_role_user_id FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/**
 * user_login: view used by UI for login authentication purposes
 */
CREATE VIEW solaruser.user_login AS
	SELECT
		email::text AS username, 
		password AS password, 
		enabled AS enabled,
		id AS user_id,
		disp_name AS display_name
	FROM solaruser.user_user;

/**
 * user_login_role: view used by UI for login authorization purposes
 */
CREATE VIEW solaruser.user_login_role AS
	SELECT u.email::text AS username, r.role_name AS authority
	FROM solaruser.user_user u
	INNER JOIN solaruser.user_role r ON r.user_id = u.id;

/* === USER AUTH TOKEN ===================================================== */

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

CREATE VIEW solaruser.user_auth_token_login  AS
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

CREATE VIEW solaruser.user_auth_token_role AS
	SELECT 
		t.auth_token AS username,
		'ROLE_'::text || upper(t.token_type::character varying::text) AS authority
	FROM solaruser.user_auth_token t
	UNION
	SELECT 
		t.auth_token AS username,
		r.role_name AS authority
	FROM solaruser.user_auth_token t
	JOIN solaruser.user_role r ON r.user_id = t.user_id AND t.token_type = 'User'::solaruser.user_auth_token_type;

/* === USER NODE =========================================================== */

CREATE TABLE solaruser.user_node (
	node_id			BIGINT NOT NULL,
	user_id			BIGINT NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	disp_name		CHARACTER VARYING(128),
	description		CHARACTER VARYING(512),
	private 		BOOLEAN NOT NULL DEFAULT FALSE,
	CONSTRAINT user_node_pkey PRIMARY KEY (node_id),
	CONSTRAINT user_node_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT user_node_node_fk FOREIGN KEY (node_id)
		REFERENCES solarnet.sn_node (node_id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);


/* === USER NODE CONF ======================================================
 * Note the node_id is NOT a foreign key to the node table, because the ID
 * is assigned before the node is created (and may never be created if not
 * confirmed by the user).
 */

CREATE TABLE solaruser.user_node_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.solaruser_seq'),
	user_id			BIGINT NOT NULL,
	node_id			BIGINT,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	conf_key		CHARACTER VARYING(1024) NOT NULL,
	conf_date		TIMESTAMP WITH TIME ZONE,
	sec_phrase 		CHARACTER VARYING(128) NOT NULL,
	country			CHARACTER(2) NOT NULL,
	time_zone		CHARACTER VARYING(64) NOT NULL,
	CONSTRAINT user_node_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_node_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT user_node_conf_unq UNIQUE (user_id, conf_key)
);


/* === NETWORK ASSOCIATION VIEW ============================================
 * Supporting view for the network association process.
 */

CREATE VIEW solaruser.network_association  AS
	SELECT
		u.email::text AS username,
		unc.conf_key AS conf_key,
		unc.sec_phrase AS sec_phrase
	FROM solaruser.user_node_conf unc
	INNER JOIN solaruser.user_user u ON u.id = unc.user_id;


/* === USER NODE CERT ======================================================
 * Holds user node certificates.
 */

CREATE TABLE solaruser.user_node_cert (
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	node_id			BIGINT NOT NULL,
	status			CHAR(1) NOT NULL,
	request_id		VARCHAR(32) NOT NULL,
	keystore		bytea,
	CONSTRAINT user_node_cert_pkey PRIMARY KEY (user_id, node_id),
	CONSTRAINT user_cert_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

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
