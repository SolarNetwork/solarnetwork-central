/**
 * View of active tokens owned by enabled users, with associated user, token, and policy details.
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
	WHERE t.status = 'Active'::solaruser.user_auth_token_status
		AND u.enabled = TRUE;

/**
 * View of all valid node IDs, as an array, for a given token.
 *
 * This will filter out any node IDs not present on the token policy `nodeIds` array.
 * Additionally, archived nodes are filtered out, and only active tokens owned by
 * enabled users are included.
 *
 * Typical query is:
 *
 *     SELECT node_id FROM solaruser.user_auth_token_node_ids
 *     WHERE auth_token = 'token-id'
 */
CREATE OR REPLACE VIEW solaruser.user_auth_token_node_ids AS
	SELECT t.auth_token,
		t.user_id,
		t.token_type,
		t.jpolicy,
		array_agg(un.node_id) AS node_ids
	FROM solaruser.user_auth_token t
	JOIN solaruser.user_node un ON un.user_id = t.user_id
	JOIN solaruser.user_user u ON u.id = t.user_id
	WHERE un.archived = FALSE
		AND u.enabled = TRUE
		AND t.status = 'Active'::solaruser.user_auth_token_status
		AND (
			(t.jpolicy->'nodeIds') IS NULL
			OR (t.jpolicy->'nodeIds') @> un.node_id::text::jsonb
		)
	GROUP BY t.auth_token, t.user_id;

/**
 * Find token details matching a given signature and associated parameters.
 *
 * This function will validate the provided signature and parameters matches
 * the token secret associated with `token_id`, by re-computing the signature
 * value using a signing date matching any date between `req_date` and 6 days
 * earlier. Only active tokens owned by enabled users can be verified.
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
		auth.user_id,
		auth.token_type,
		auth.jpolicy
	FROM solaruser.user_auth_token auth
	INNER JOIN solaruser.user_user u ON u.id = auth.user_id
	INNER JOIN sign_dates sd ON TRUE
	INNER JOIN canon_data cd ON TRUE
	WHERE auth.auth_token = token_id
		AND auth.status = 'Active'::solaruser.user_auth_token_status
		AND u.enabled = TRUE
		AND COALESCE(to_timestamp((auth.jpolicy->>'notAfter')::double precision / 1000), req_date) >= req_date
		AND solaruser.snws2_signature(
				sign_data,
				solaruser.snws2_signing_key(sd.sign_date, auth.auth_secret)
			) = signature;
$$;
