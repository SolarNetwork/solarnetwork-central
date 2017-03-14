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
 * user_meta: JSON metadata specific to a user.
 */
CREATE TABLE solaruser.user_meta (
  user_id 			BIGINT NOT NULL,
  created 			solarcommon.ts NOT NULL,
  updated 			solarcommon.ts NOT NULL,
  jdata 			json NOT NULL,
  CONSTRAINT user_meta_pkey PRIMARY KEY (user_id),
  CONSTRAINT user_meta_user_fk FOREIGN KEY (user_id)
        REFERENCES solaruser.user_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE
);

/******************************************************************************
 * FUNCTION solaruser.store_meta(timestamptz, bigint, text)
 *
 * Add or update user metadata.
 *
 * @param cdate the creation date to use
 * @param userid the user ID
 * @param jdata the metadata to store
 */
CREATE OR REPLACE FUNCTION solaruser.store_user_meta(
	cdate solarcommon.ts,
	userid BIGINT,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate solarcommon.ts := now();
	jdata_json json := jdata::json;
BEGIN
	INSERT INTO solaruser.user_meta(user_id, created, updated, jdata)
	VALUES (userid, cdate, udate, jdata_json)
	ON CONFLICT (user_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;

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
	jpolicy			json,
	CONSTRAINT user_auth_token_pkey PRIMARY KEY (auth_token),
	CONSTRAINT user_auth_token_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE OR REPLACE VIEW solaruser.user_auth_token_login AS
	SELECT t.auth_token AS username,
		t.auth_secret AS password,
		u.enabled,
		u.id AS user_id,
		u.disp_name AS display_name,
		t.token_type::character varying AS token_type,
		t.jpolicy
	 FROM solaruser.user_auth_token t
		 JOIN solaruser.user_user u ON u.id = t.user_id
	WHERE t.status = 'Active'::solaruser.user_auth_token_status;

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
	archived		BOOLEAN NOT NULL DEFAULT FALSE,
	CONSTRAINT user_node_pkey PRIMARY KEY (node_id),
	CONSTRAINT user_node_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT user_node_node_fk FOREIGN KEY (node_id)
		REFERENCES solarnet.sn_node (node_id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* Add index on user_node to assist finding all nodes for a given user. */
CREATE INDEX user_node_user_idx ON solaruser.user_node (user_id);

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

/* === USER NODE TRANSFER ======================================================
 * Holds ownership transfer requests for user nodes.
 */

CREATE TABLE solaruser.user_node_xfer (
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	node_id			solarcommon.node_id,
	recipient		citext NOT NULL,
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

/**
 * Return most recent datum records for all available sources for all nodes owned by a given user ID.
 *
 * @param users An array of user IDs to return results for.
 * @returns Set of solardatum.da_datum records.
 */
CREATE OR REPLACE FUNCTION solaruser.find_most_recent_datum_for_user(users bigint[])
  RETURNS SETOF solardatum.da_datum AS
$BODY$
	SELECT r.*
	FROM (SELECT node_id FROM solaruser.user_node WHERE user_id = ANY(users)) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent(n.node_id)) AS r
	ORDER BY r.node_id, r.source_id;
$BODY$
  LANGUAGE sql STABLE;

/**
 * TRIGGER function that automatically transfers rows related to a user_node to
 * the new owner when the user_id value is changed. Expected record is solaruser.uesr_node.
 */
CREATE OR REPLACE FUNCTION solaruser.node_ownership_transfer()
  RETURNS "trigger" AS
$BODY$
BEGIN
	UPDATE solaruser.user_node_cert
	SET user_id = NEW.user_id
	WHERE user_id = OLD.user_id
		AND node_id = NEW.node_id;

	UPDATE solaruser.user_node_conf
	SET user_id = NEW.user_id
	WHERE user_id = OLD.user_id
		AND node_id = NEW.node_id;

	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER node_ownership_transfer
  BEFORE UPDATE
  ON solaruser.user_node
  FOR EACH ROW
  WHEN (OLD.user_id IS DISTINCT FROM NEW.user_id)
  EXECUTE PROCEDURE solaruser.node_ownership_transfer();
