/**
 * Format a timestamp to RFC 1123 syntax using the GMT time zone.
 *
 * This is the form used by SNWS2 signatures and HTTP date headers.
 *
 * @param d the timestamp to format
 * @returns the formatted date, e.g. `Tue, 25 Apr 2017 14:30:00 GMT`
 */
CREATE OR REPLACE FUNCTION solarcommon.to_rfc1123_utc(d timestamptz)
RETURNS text LANGUAGE SQL STRICT IMMUTABLE AS
$$
	SELECT to_char(d at time zone 'UTC', 'Dy, FMDD Mon YYYY HH24:MI:SS "GMT"');
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
	SELECT req_date BETWEEN CURRENT_TIMESTAMP - tolerance AND CURRENT_TIMESTAMP + tolerance
$$;

/**
 * View of all valid node IDs for a given token.
 *
 * This will filter out any node IDs not present on the token policy `nodeIds` array.
 * Additionally, archived nodes are filtered out.
 *
 * Typical query is:
 *
 *     SELECT node_id` FROM solaruser.nodes_for_auth_token
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
