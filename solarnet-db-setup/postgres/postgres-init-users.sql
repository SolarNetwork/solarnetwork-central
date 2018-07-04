/* === USER ================================================================ */

CREATE SCHEMA solaruser;

CREATE SEQUENCE solaruser.solaruser_seq;

/**
 * user_user: main table for user information.
 */
CREATE TABLE solaruser.user_user (
	id					BIGINT NOT NULL DEFAULT nextval('solaruser.solaruser_seq'),
	created				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	disp_name			CHARACTER VARYING(128) NOT NULL,
	email				citext NOT NULL,
	password			CHARACTER VARYING(128) NOT NULL,
	enabled				BOOLEAN NOT NULL DEFAULT TRUE,
	loc_id				BIGINT,
	jdata				jsonb,
	CONSTRAINT user_user_pkey PRIMARY KEY (id),
	CONSTRAINT user_user_email_unq UNIQUE (email),
	CONSTRAINT user_user_loc_fk FOREIGN KEY (loc_id)
		REFERENCES solarnet.sn_loc (id)
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX user_user_jdata_idx ON solaruser.user_user
	USING GIN (jdata jsonb_path_ops);

/**
 * Add or update the internal data for a user.
 *
 * @param user_id The ID of the user to update.
 * @param json_obj The JSON object to add. All <code>null</code> values will be removed from the resulting object.
 */
CREATE OR REPLACE FUNCTION solaruser.store_user_data(
	user_id bigint,
	json_obj jsonb)
  RETURNS void LANGUAGE sql VOLATILE AS
$BODY$
	UPDATE solaruser.user_user
	SET jdata = jsonb_strip_nulls(COALESCE(jdata, '{}'::jsonb) || json_obj)
	WHERE id = user_id
$BODY$;

/**
 * user_meta: JSON metadata specific to a user.
 */
CREATE TABLE solaruser.user_meta (
  user_id 			BIGINT NOT NULL,
  created 			timestamp with time zone NOT NULL,
  updated 			timestamp with time zone NOT NULL,
  jdata				jsonb NOT NULL,
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
	cdate timestamp with time zone,
	userid BIGINT,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate timestamp with time zone := now();
	jdata_json jsonb := jdata::jsonb;
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
	jpolicy			jsonb,
	CONSTRAINT user_auth_token_pkey PRIMARY KEY (auth_token),
	CONSTRAINT user_auth_token_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/**
 * View of active tokens with associated user, token, and policy details.
 */
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

/**
 * View of granted roles for tokens.
 */
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

/**
 * View of all valid node IDs for a given token.
 *
 * This will filter out any node IDs not present on the token policy `nodeIds` array.
 * Additionally, archived nodes are filtered out.
 *
 * Typical query is:
 *
 *     SELECT node_id FROM solaruser.user_auth_token_nodes
 *     WHERE auth_token = 'token-id'
 */
CREATE OR REPLACE VIEW solaruser.user_auth_token_nodes AS
	SELECT t.auth_token, un.node_id
	FROM solaruser.user_auth_token t
	INNER JOIN solaruser.user_node un ON un.user_id = t.user_id
	WHERE
		un.archived = FALSE
		AND t.status = 'Active'::solaruser.user_auth_token_status
		AND (
			t.jpolicy->'nodeIds' IS NULL
			OR t.jpolicy->'nodeIds' @> un.node_id::text::jsonb
		);

/**
 * View of all valid node source IDs for a given token.
 *
 * This will filter out any node IDs not present on the token policy `nodeIds` array,
 * and then any source IDs not present in the token policy `sourceIds` array. Note
 * that the `sourceIds` array holds wildcard patterns, which are converted into
 * regular expressions for filtering purposes.Additionally, archived nodes are filtered
 * out.
 *
 * Note that the output of this view is NOT distinct. That is left to the
 * caller. Additionally this view provides the `ts` column so that the sources
 * can be narrowed down to a specific date range. Because source IDs are extracted
 * from the datum data itself, providing date range criteria when selecting from
 * this view can greatly speed up the query.
 *
 * Typical query is:
 *
 *     SELECT DISTINCT node_id, source_id FROM solaruser.user_auth_token_sources
 *     WHERE auth_token = 'token-id' AND ts > CURRENT_DATE - interval '1 month'
 */
CREATE OR REPLACE VIEW solaruser.user_auth_token_sources AS
	SELECT t.auth_token, un.node_id, d.source_id::text, d.ts_start AS ts
	FROM solaruser.user_auth_token t
	INNER JOIN solaruser.user_node un ON un.user_id = t.user_id
	INNER JOIN solaragg.agg_datum_daily d ON d.node_id = un.node_id
	LEFT OUTER JOIN LATERAL (
		SELECT solarcommon.ant_pattern_to_regexp(jsonb_array_elements_text(t.jpolicy->'sourceIds')) AS regex
		) s_regex ON TRUE
	WHERE
		un.archived = FALSE
		AND t.status = 'Active'::solaruser.user_auth_token_status
		AND (
			t.jpolicy->'nodeIds' IS NULL
			OR t.jpolicy->'nodeIds' @> un.node_id::text::jsonb
		)
		AND (
			t.jpolicy->'sourceIds' IS NULL
			OR d.source_id ~ s_regex.regex
		);

/**
 * Generate a SNWS2 signing key out of a token secret.
 *
 * @param sign_date the signing date
 * @param secret the token secret
 * @returns the key to use for signing SNWS2 messages
 */
CREATE OR REPLACE FUNCTION solaruser.snws2_signing_key(sign_date date, secret text)
RETURNS bytea LANGUAGE SQL STRICT IMMUTABLE AS $$
	SELECT hmac('snws2_request', hmac(to_char(sign_date, 'YYYYMMDD'), 'SNWS2' || secret, 'sha256'), 'sha256');
$$;

/**
 * Generate a hex-encoded SNWS2 signing key out of a token secret.
 *
 * @param sign_date the signing date
 * @param secret the token secret
 * @returns the key to use for signing SNWS2 messages, encoded as hex
 */
CREATE OR REPLACE FUNCTION solaruser.snws2_signing_key_hex(sign_date date, secret text)
RETURNS text LANGUAGE SQL STRICT IMMUTABLE AS $$
	SELECT encode(solaruser.snws2_signing_key(sign_date, secret), 'hex');
$$;

/**
 * Generate SNWS2 canonical request data for a GET request and `host` and `x-sn-date` signed headers.
 *
 * @param req_date the request date (e.g. the X-SN-Date HTTP header)
 * @param host the request host (e.g. the Host HTTP header)
 * @param path the request path
 * @returns the canonical request data
 */
CREATE OR REPLACE FUNCTION solaruser.snws2_canon_request_data(req_date timestamptz, host text, path text)
RETURNS text LANGUAGE SQL STRICT IMMUTABLE AS
$$
	SELECT E'GET\n'
		|| path || E'\n'
		|| E'\n' -- query params
		|| 'host:' || host || E'\n'
		|| 'x-sn-date:' || solarcommon.to_rfc1123_utc(req_date) || E'\n'
		|| E'host;x-sn-date\n'
		|| 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
$$;

/**
 * Generate the message data to be signed for a SNWS2 authorization header.
 *
 * @param req_date the request date
 * @param canon_request_data the canonical request data
 * @returns the message data to sign
 */
CREATE OR REPLACE FUNCTION solaruser.snws2_signature_data(req_date timestamptz, canon_request_data text)
RETURNS text LANGUAGE SQL STRICT IMMUTABLE AS
$$
	SELECT E'SNWS2-HMAC-SHA256\n'
		|| to_char(req_date at time zone 'UTC', 'YYYYMMDD"T"HH24MISS"Z"') || E'\n'
		|| encode(digest(canon_request_data, 'sha256'), 'hex');
$$;

/**
 * Compute the SNWS2 signature from the data to sign and the sign key.
 *
 * @param signature_data the data to sign, e.g. result of `solaruser.snws2_signature_data(text, timestamptz)`
 * @param sign_key the key to sign the data with, e.g. result of `solaruser.snws2_signing_key`
 * @returns the hex-encoded signature result
 */
CREATE OR REPLACE FUNCTION solaruser.snws2_signature(signature_data text, sign_key bytea)
RETURNS text LANGUAGE SQL STRICT IMMUTABLE AS
$$
	SELECT encode(hmac(convert_to(signature_data, 'UTF8'), sign_key, 'sha256'), 'hex');
$$;

/**
 * Find token details matching a given signature and associated parameters.
 *
 * This function will validate the provided signature and parameters matches
 * the token secret associated with `token_id`, by re-computing the signature
 * value using a signing date matching any date between `req_date` and 6 days
 * earlier.
 *
 * @param token_id the security token to verify
 * @param req_date the request date
 * @param host the request host (e.g. the Host HTTP header)
 * @param path the request path
 * @param signature the signature to verify
 * @returns the user ID, token type, and policy of the verified token, or an empty result if not verified
 */
CREATE OR REPLACE FUNCTION solaruser.snws2_find_verified_token_details(
	token_id text,
	req_date timestamptz,
	host text,
	path text,
	signature text)
RETURNS TABLE (user_id bigint, token_type solaruser.user_auth_token_type, jpolicy jsonb)
LANGUAGE SQL STRICT STABLE ROWS 1 AS
$$
	WITH sign_dates AS (
		SELECT CAST(generate_series(
			(req_date at time zone 'UTC')::date,
			(req_date at time zone 'UTC')::date - interval '6 days',
			-interval '1 day') at time zone 'UTC' AS DATE) as sign_date
	), canon_data AS (
		SELECT solaruser.snws2_signature_data(
			req_date,
			solaruser.snws2_canon_request_data(req_date, host, path)
		) AS sign_data
	)
	SELECT
		user_id,
		token_type,
		jpolicy
	FROM solaruser.user_auth_token auth
	INNER JOIN sign_dates sd ON TRUE
	INNER JOIN canon_data cd ON TRUE
	WHERE auth.auth_token = token_id
		AND auth.status = 'Active'::solaruser.user_auth_token_status
		AND COALESCE(to_timestamp((jpolicy->>'notAfter')::double precision / 1000), req_date) >= req_date
		AND solaruser.snws2_signature(
				sign_data,
				solaruser.snws2_signing_key(sd.sign_date, auth.auth_secret)
			) = signature;
$$;

/**
 * Validate a request date.
 *
 * @param req_date the request date
 * @param tolerance the tolerance plus/minus to allow from the current time
 * @returns `true` if the request date is within the given tolerance
 */
CREATE OR REPLACE FUNCTION solaruser.snws2_validated_request_date(
	req_date timestamptz,
	tolerance interval default interval '5 minutes')
RETURNS boolean LANGUAGE SQL STRICT STABLE AS
$$
	SELECT req_date BETWEEN CURRENT_TIMESTAMP - tolerance AND CURRENT_TIMESTAMP + tolerance;
$$;


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

/* === USER NODE TRANSFER ======================================================
 * Holds ownership transfer requests for user nodes.
 */

CREATE TABLE solaruser.user_node_xfer (
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	node_id			BIGINT NOT NULL,
	recipient		citext NOT NULL,
	CONSTRAINT user_node_xfer_pkey PRIMARY KEY (user_id, node_id),
	CONSTRAINT user_node_xfer_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX user_node_xfer_recipient_idx ON solaruser.user_node_xfer (recipient);

/**************************************************************************************************
 * FUNCTION solaruser.store_user_node_xfer(bigint, bigint, varchar, varchar)
 *
 * Insert or update a user node transfer record.
 *
 * @param node The ID of the node.
 * @param userid The ID of the user.
 * @param recip The recipient email of the requested owner.
 */
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

/**
 * Return most recent datum records for all available sources for all nodes owned by a given user ID.
 *
 * @param users An array of user IDs to return results for.
 * @returns Set of solardatum.da_datum records.
 */
CREATE OR REPLACE FUNCTION solaruser.find_most_recent_datum_for_user(users bigint[])
  RETURNS SETOF solardatum.da_datum_data AS
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
