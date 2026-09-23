/**
 * "Natural sort" collation, to sort text with atomic number components and upper case
 * before lower case, to match Java standard sorting.
 *
 * @see https://www.postgresql.org/docs/17/collation.html#ICU-COLLATION-SETTINGS
 */
CREATE COLLATION solarcommon.naturalsort (provider = icu, locale = 'und-u-kf-upper-kn');

/**
 * Reduce a 2d array into a set of 1d arrays.
 */
CREATE OR REPLACE FUNCTION solarcommon.reduce_dim(anyarray)
	RETURNS SETOF anyarray LANGUAGE plpgsql IMMUTABLE STRICT ROWS 20 AS
$$
DECLARE
	s $1%TYPE;
BEGIN
	FOREACH s SLICE 1 IN ARRAY $1 LOOP
		RETURN NEXT s;
	END LOOP;
	RETURN;
END
$$;

CREATE OR REPLACE FUNCTION solarcommon.plainto_prefix_tsquery(config regconfig, qtext TEXT)
RETURNS tsquery AS $$
SELECT to_tsquery(config,
	regexp_replace(
			regexp_replace(
				regexp_replace(qtext, E'[^\\w ]', '', 'g'),
			E'\\M', ':*', 'g'),
		E'\\s+',' & ','g')
);
$$ LANGUAGE SQL STRICT IMMUTABLE;

CREATE OR REPLACE FUNCTION solarcommon.plainto_prefix_tsquery(qtext TEXT)
RETURNS tsquery AS $$
SELECT solarcommon.plainto_prefix_tsquery(get_current_ts_config(), qtext);
$$ LANGUAGE SQL STRICT IMMUTABLE;

/**
 * Convert a JSON array into an array of text.
 *
 * @param jdata the JSON array value to convert
 * @returns text array, or NULL if jdata is NULL
 */
CREATE OR REPLACE FUNCTION solarcommon.json_array_to_text_array(jdata jsonb)
   RETURNS text[] LANGUAGE sql IMMUTABLE AS
$$
SELECT
	CASE
		WHEN jdata IS NULL THEN NULL::text[]
		ELSE ARRAY(SELECT jsonb_array_elements_text(jdata))
	END
$$;

CREATE OR REPLACE FUNCTION solarcommon.json_array_to_text_array(jdata json)
   RETURNS text[] LANGUAGE sql IMMUTABLE AS
$$
SELECT
	CASE
		WHEN jdata IS NULL THEN NULL::text[]
		ELSE ARRAY(SELECT json_array_elements_text(jdata))
	END
$$;

/**
 * Combine "jdata" components into a single "jdata" JSON object.
 *
 * @param jdata_i the instantaneous JSON object
 * @param jdata_a the accumulating JSON object
 * @param jdata_s the status JSON object
 * @param jdata_t the tag array
 * @returns JSON object
 */
CREATE OR REPLACE FUNCTION solarcommon.jdata_from_components(
		jdata_i jsonb,
		jdata_a jsonb,
		jdata_s jsonb,
		jdata_t text[])
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
SELECT jsonb_strip_nulls(jsonb_build_object('i', jdata_i, 'a', jdata_a, 's', jdata_s, 't', to_jsonb(jdata_t)));
$$;

/**
 * Split a "jdata" JSON object into components.
 *
 * @param jdata the "jdata" JSON object
 * @returns the component values
 */
CREATE OR REPLACE FUNCTION solarcommon.components_from_jdata(
	IN jdata jsonb,
	OUT jdata_i jsonb,
	OUT jdata_a jsonb,
	OUT jdata_s jsonb,
	OUT jdata_t text[])
	LANGUAGE SQL IMMUTABLE AS
$$
SELECT jdata->'i', jdata->'a', jdata->'s', solarcommon.json_array_to_text_array(jdata->'t')
$$;


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
	SELECT to_char(d at time zone 'UTC', 'Dy, DD Mon YYYY HH24:MI:SS "GMT"');
$$;

/**
 * Convert an Ant path pattern into a regular expression.
 *
 * This can be used to convert source ID wildcard patterns into
 * regular expressions that Postgres understands.
 *
 * The result follows Spring's AntPathMatcher, so that patterns are matched the
 * same way here as they are in the application:
 *
 *   ?   matches one character within a path segment
 *   *   matches zero or more characters within a single path segment
 *   **  matches zero or more path segments, but only when it forms a complete
 *       segment; anywhere else it behaves like *
 *
 * A pattern without a leading / never matches a path with one, and vice versa.
 *
 * Patterns and paths that end with a / are not handled exactly as
 * AntPathMatcher handles them; neither source IDs nor metadata paths end with
 * one. URI template variables like {id} are treated as literal text.
 *
 * @param pat the Ant path pattern to convert
 * @returns the equivalent regular expression
 */
CREATE OR REPLACE FUNCTION solarcommon.ant_pattern_to_regexp(pat text)
RETURNS text LANGUAGE SQL STRICT IMMUTABLE AS
$$
	WITH escaped AS (
		-- drop the segment marker used below, then escape regexp metacharacters
		-- and expand "?" into one character within a segment
		SELECT regexp_replace(
			regexp_replace(
				replace(pat, E'\x01', ''),
			'([!$()+.:<=>[\\\]^{|}-])', '\\\1', 'g'),
		E'[?]', E'[^/]', 'g') AS x
	), marked AS (
		-- mark every "**" that forms a complete segment, so the next step skips it
		SELECT regexp_replace(x, E'(^|/)[*][*](?=/|$)', E'\\1\x01', 'g') AS x FROM escaped
	), segments AS (
		-- "*", and any "**" that is not a complete segment, match within one segment
		SELECT regexp_replace(x, E'[*]+', E'[^/]*', 'g') AS x FROM marked
	), collapsed AS (
		-- adjacent "**" segments are equivalent to a single one
		SELECT regexp_replace(x, E'\x01(/\x01)+', E'\x01', 'g') AS x FROM segments
	), expanded AS (
		-- a "**" segment matches zero or more segments, in any position
		SELECT regexp_replace(
			regexp_replace(
				regexp_replace(
					replace(x, E'\x01', '.*'),
				E'/[.][*]/', '(/|/.*/)', 'g'),
			E'/[.][*]$', '(/.*)?'),
		E'^[.][*]/', '(.*/)?') AS x FROM collapsed
	)
	-- a pattern only matches a path that agrees on the leading separator
	SELECT '^' || CASE WHEN left(pat, 1) = '/' THEN '' ELSE '(?![/])' END || x || '$'
	FROM expanded;
$$;

/**
 * Expand an Ant path pattern into regular expressions that match the pattern's
 * own path prefixes.
 *
 * A path matches one of these when it could still lead to a match for `pat`,
 * the way Spring's AntPathMatcher.matchStart() does. This is used to skip
 * entire object branches while pruning, without having to inspect their leaves.
 *
 * @param pat the Ant path pattern to expand
 * @returns regular expressions for every path prefix of the pattern
 */
CREATE OR REPLACE FUNCTION solarcommon.ant_pattern_prefix_regexps(pat text)
RETURNS text[] LANGUAGE SQL STRICT IMMUTABLE PARALLEL SAFE AS
$$
	SELECT ARRAY(
		SELECT DISTINCT solarcommon.ant_pattern_to_regexp(array_to_string(g.seg[1:i], '/'))
		FROM (SELECT regexp_split_to_array(pat, '/')) g(seg)
		CROSS JOIN LATERAL generate_series(1, cardinality(g.seg)) i
	);
$$;

/**
 * Prune a JSON object down to the values whose paths match a set of regular
 * expressions, preserving the shape of the tree.
 *
 * Object values are descended into when their path matches `node_re`, and all
 * other values are kept when their path matches `leaf_re`; arrays are kept
 * whole. This mirrors SecurityPolicyEnforcer.enforceMetadataPaths(), including
 * its treatment of anything that is not an object as a leaf.
 *
 * Objects left with nothing are dropped, so an object that keeps nothing at all
 * returns NULL rather than an empty object. Callers filtering rows can test the
 * result with IS NOT NULL.
 *
 * Use solarcommon.ant_pattern_to_regexp() for `leaf_re` and
 * solarcommon.ant_pattern_prefix_regexps() for `node_re`, or call
 * solarcommon.jsonb_prune_ant_paths() to do both.
 *
 * @param obj the JSON object to prune
 * @param leaf_re regular expressions matching the paths of values to keep
 * @param node_re regular expressions matching the paths to descend into
 * @param path the path of `obj` itself; pass '' (the default) for the root
 * @returns the pruned object, or NULL if nothing was kept
 */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_prune_paths(obj jsonb, leaf_re text[],
		node_re text[], path text DEFAULT '')
RETURNS jsonb LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE AS
$$
DECLARE
	result jsonb;
BEGIN
	IF jsonb_typeof(obj) <> 'object' THEN
		RETURN NULL;
	END IF;
	SELECT jsonb_object_agg(k.key, k.val) INTO result FROM (
		SELECT e.key, CASE WHEN jsonb_typeof(e.value) = 'object'
				THEN solarcommon.jsonb_prune_paths(e.value, leaf_re, node_re,
					path || '/' || e.key)
				ELSE e.value END AS val
		FROM jsonb_each(obj) e
		WHERE CASE WHEN jsonb_typeof(e.value) = 'object'
			THEN (path || '/' || e.key) ~ ANY (node_re)
			ELSE (path || '/' || e.key) ~ ANY (leaf_re) END
	) k WHERE k.val IS NOT NULL;
	RETURN result;
END
$$;

/**
 * Prune a JSON object down to the values whose paths match a set of Ant path
 * patterns, preserving the shape of the tree.
 *
 * This is the form used to apply the `userMetadataPaths` and
 * `nodeMetadataPaths` constraints of a security policy to a metadata object.
 *
 * An empty or NULL `pats` means no restriction, and returns `jdata` unchanged,
 * the same way SecurityPolicyEnforcer treats a policy with no metadata paths.
 * Note this means the caller is responsible for not applying an absent policy:
 * join the token with an INNER JOIN so that an unknown token returns no rows.
 *
 * @param jdata the JSON object to prune
 * @param pats the Ant path patterns of the values to keep
 * @returns the pruned object, `jdata` if `pats` is empty, or NULL if nothing
 *          was kept
 */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_prune_ant_paths(jdata jsonb, pats text[])
RETURNS jsonb LANGUAGE SQL IMMUTABLE PARALLEL SAFE AS
$$
	SELECT CASE WHEN jdata IS NULL OR COALESCE(cardinality(pats), 0) < 1 THEN jdata
		ELSE solarcommon.jsonb_prune_paths(jdata,
			ARRAY(SELECT solarcommon.ant_pattern_to_regexp(p) FROM unnest(pats) p),
			ARRAY(SELECT DISTINCT unnest(solarcommon.ant_pattern_prefix_regexps(p))
				FROM unnest(pats) p),
			'')
		END;
$$;

/**
 * Prune a JSON object down to the values whose paths match a set of Ant path
 * patterns held in a JSON array.
 *
 * This form takes the patterns as they are stored in a security policy, for
 * example `jpolicy -> 'userMetadataPaths'`. A NULL value, or any value that is
 * not a JSON array, means no restriction.
 *
 * @param jdata the JSON object to prune
 * @param pats a JSON array of the Ant path patterns of the values to keep
 * @returns the pruned object, `jdata` if `pats` holds no patterns, or NULL if
 *          nothing was kept
 */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_prune_ant_paths(jdata jsonb, pats jsonb)
RETURNS jsonb LANGUAGE SQL IMMUTABLE PARALLEL SAFE AS
$$
	SELECT CASE WHEN jsonb_typeof(pats) <> 'array' THEN jdata
		ELSE solarcommon.jsonb_prune_ant_paths(jdata,
			ARRAY(SELECT jsonb_array_elements_text(pats)))
		END;
$$;

/** Aggregate helper function that always returns the first non-NULL item. */
CREATE OR REPLACE FUNCTION solarcommon.first_sfunc(anyelement, anyelement)
RETURNS anyelement LANGUAGE SQL IMMUTABLE STRICT AS $$
    SELECT $1;
$$;

/**
 * First aggregate value.
 *
 * This aggregate will return the first value encountered in the aggregate group. Given
 * the order within the group is generally undefined, using an `ORDER BY` clause is usually
 * needed. For example, to select the first timestamp for each datum by node and source:
 *
 * 		SELECT solarcommon.first(ts ORDER BY ts) AS ts_start
 */
CREATE AGGREGATE solarcommon.first(
	sfunc    = solarcommon.first_sfunc,
	basetype = anyelement,
    stype    = anyelement
);

/**
 * Cast a JSON array to a BIGINT array.
 *
 * @param json the JSON array to cast
 */
CREATE OR REPLACE FUNCTION solarcommon.json_array_to_bigint_array(json)
RETURNS bigint[] LANGUAGE sql IMMUTABLE AS $$
    SELECT array_agg(x)::bigint[] || ARRAY[]::bigint[] FROM json_array_elements_text($1) t(x);
$$;

/**
 * Cast a JSONB array to a BIGINT array.
 *
 * @param jsonb the JSONB array to cast
 */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_array_to_bigint_array(jsonb)
RETURNS bigint[] LANGUAGE sql IMMUTABLE AS $$
    SELECT array_agg(x)::bigint[] || ARRAY[]::bigint[] FROM jsonb_array_elements_text($1) t(x);
$$;

/**
 * Table for dynamic application settings.
 */
CREATE TABLE solarcommon.app_setting (
	created		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	skey		VARCHAR(255) NOT NULL,
	stype		VARCHAR(255) NOT NULL,
	svalue		VARCHAR(4096) NOT NULL,
	CONSTRAINT app_settings_pk PRIMARY KEY (skey, stype)
);

/**
 * Table for verioned localized messages.
 */
CREATE TABLE solarcommon.messages (
	vers			TIMESTAMP WITH TIME ZONE NOT NULL,
	bundle			CHARACTER VARYING(128) NOT NULL,
	locale			CHARACTER VARYING(8) NOT NULL,
	msg_key			CHARACTER VARYING(128) NOT NULL,
	msg_val			TEXT,
	CONSTRAINT messages_pkey PRIMARY KEY (bundle,locale,vers,msg_key)
);

/**
 * Decode an integer string of a specific radix.
 *
 * No validation is performed to check the input is a valid value for the given radix.
 *
 * @param s the string to decode
 * @param radix the radix; defaults to `16`
 */
CREATE OR REPLACE FUNCTION solarcommon.baseX_integer(s text, radix INTEGER DEFAULT 16) RETURNS BIGINT LANGUAGE SQL IMMUTABLE AS
$$
	WITH chars AS (
		SELECT * FROM UNNEST(string_to_array(UPPER(REVERSE(s)), NULL)) WITH ORDINALITY AS a(c, p)
	)
	SELECT SUM(POWER(radix, c.p - 1)::BIGINT * CASE
									WHEN c.c BETWEEN '0' AND '9' THEN c.c::INTEGER
									ELSE (10 + ASCII(c.c) - ASCII('A'))::INTEGER
									END)::BIGINT AS v
	FROM chars c
$$;

/**
 * Convert a bigint into a base 36 string.
 *
 * @param n the number to encode
 */
CREATE OR REPLACE FUNCTION solarcommon.to_baseX(n BIGINT, radix INT DEFAULT 16) RETURNS TEXT LANGUAGE plpgsql IMMUTABLE AS
$$
DECLARE
	s TEXT := '';
BEGIN
	IF ( n < 0 ) THEN
		RETURN NULL;
	END IF;
	LOOP
		EXIT WHEN n <= 0;
		s := CHR(n % radix + CASE WHEN n % radix < 10 THEN 48 ELSE 55 END) || s;
		n := FLOOR(n / radix);
	END LOOP;
	RETURN s::BIGINT;
	EXCEPTION WHEN OTHERS THEN
	RETURN NULL;
END
$$;

/**
 * Convert a base-10 integer string into a bigint.
 *
 * This function ignores all parsing errors and returns `NULL` if `s` cannot be parsed.
 *
 * @param s the string to decode
 */
CREATE OR REPLACE FUNCTION solarcommon.to_bigint(s text) RETURNS BIGINT LANGUAGE plpgsql IMMUTABLE AS
$$
BEGIN
	RETURN s::BIGINT;
	EXCEPTION WHEN OTHERS THEN
	RETURN NULL;
END
$$;

/**
 * Transpose a two-dimensional array (swap x,y order).
 *
 * @param arr the array to transpose (must have 2 dimensions)
 */
CREATE OR REPLACE FUNCTION solarcommon.array_transpose2d(arr anyarray)
	RETURNS anyarray LANGUAGE SQL IMMUTABLE AS
$$
    SELECT array_agg(
		(SELECT array_agg(arr[i][j] ORDER BY i) FROM generate_subscripts(arr, 1) AS i)
		ORDER BY j
	)
    FROM generate_subscripts(arr, 2) as j
$$;

/**
 * Extract the timestamp from a v7 UUID with microsecond support.
 *
 * See https://www.ietf.org/archive/id/draft-peabody-dispatch-new-uuid-format-04.html#section-5.2
 * No validation is performed to check that the provided UUID is type 7.
 *
 * Any UUID that encodes a 48-bit millisecond Unix epoch in the highest
 * 6 bytes, and bits 12-2 of bytes 6 & 7 as the microseconds of the UUID
 * can be decoded by this function:
 *
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           unix_ts_ms                          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          unix_ts_ms           |  ver  |       micros      |rnd|
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |var|                        rand_b                             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                            rand_b                             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * @param u the v7 UUID to extract the timestamp from
 * @returns the extracted timestamp
 */
CREATE OR REPLACE FUNCTION solarcommon.uuid_to_timestamp_v7(u uuid)
RETURNS TIMESTAMP WITH TIME ZONE LANGUAGE SQL STRICT IMMUTABLE AS
$$
	WITH b AS (
		SELECT uuid_send(u) bu
	)
	SELECT to_timestamp(((
		-- convert first 48 bits (millis) to micros
	      (get_byte(bu, 0)::BIGINT << 40)
		+ (get_byte(bu, 1)::BIGINT << 32)
		+ (get_byte(bu, 2)::BIGINT << 24)
		+ (get_byte(bu, 3)::BIGINT << 16)
		+ (get_byte(bu, 4)::BIGINT << 8)
		+  get_byte(bu, 5)::BIGINT
		) * 1000::BIGINT
		-- add bits 52-61 (micros)
		+ ((
		    (
		      (get_byte(bu, 6)::BIGINT << 8)
		    +  get_byte(bu, 7)::BIGINT
		    ) & 4095::BIGINT
		  ) >> 2)
		) / 1000000.0::DECIMAL
	)
	FROM b
$$;

/**
 * Encode a timestamp into a v7 UUID with millisecond precision.
 *
 * See https://www.ietf.org/archive/id/draft-peabody-dispatch-new-uuid-format-04.html#section-5.2
 *
 * The timestamp will be encoded as a 48-bit millisecond Unix epoch in the highest
 * 6 bytes of the UUID and the remaining bits, which would normally be random data in a real v7
 * UUID, will be set to 0. Thus the returned UUID can be used as a boundary for date-based
 * filtering.
 *
 * @param ts the timestamp to encode
 * @returns the encoded v7 UUID
 */
CREATE OR REPLACE FUNCTION solarcommon.timestamp_to_uuid_v7_boundary(ts TIMESTAMP WITH TIME ZONE)
RETURNS UUID LANGUAGE SQL STRICT IMMUTABLE AS
$$
	SELECT lpad(to_hex(
		((EXTRACT(epoch FROM ts) * 1000)::BIGINT::BIT(48) || x'7000')::BIT(64)::BIGINT
	) || '8000000000000000', 32, '0')::uuid
$$;

/**
 * Generate a random string between a given length, using only basic ASCII characters.
 *
 * The generated value will contain only ASCII alphanumeric values, excluding some
 * common homographs like 0 and O.
 *
 * @param min_len the minimum character length to generate (inclusive)
 * @param max_len the maximum character length to generate (inclusive)
 * @returns the random string value
 */
CREATE OR REPLACE FUNCTION solarcommon.short_id(min_len INTEGER, max_len INTEGER)
RETURNS text LANGUAGE plpgsql AS
$$
DECLARE
	sid TEXT := '';
	len INTEGER := floor(random() * (max_len - min_len + 1) + min_len);
BEGIN
	LOOP
		-- generate random string, then strip out non-letter and common homographs
		sid := sid || translate(encode(gen_random_bytes(len - char_length(sid)), 'base64'), '/+0OI1l=', '');

    	IF char_length(sid) > max_len THEN
    		sid :=  SUBSTRING(sid FROM 1 FOR min_len);
    	END IF;


    	EXIT WHEN char_length(sid) >= min_len;
	END LOOP;

	RETURN sid;
END
$$;

/**
 * Assign an `hid` text column a short unique value.
 *
 * The `solarcommon.short_id()` function will be used to generate a short text ID of at least
 * 8 characters. After every 100 unsuccesful attempts to generate a unique value, the length will
 * be increased by 1.
 *
 * NOTE it is still possible for the generated value to collide, if two INSERTs at the same time
 * generate the same value. The application will have to deal with that possibility, i.e. retry
 * the INSERT that fails from a duplicate value constraint violation.
 *
 * If the row to insert already has a non-NULL `hid` value, that will be preserved and no random
 * value will be generated.
 */
CREATE OR REPLACE FUNCTION solarcommon.assign_human_id()
RETURNS TRIGGER LANGUAGE plpgsql AS
$$
DECLARE
	qry TEXT := format('SELECT TRUE FROM %I.%I WHERE hid = $1', TG_TABLE_SCHEMA, TG_TABLE_NAME);
	hid TEXT := '';
	len INTEGER := 8;
	itr INTEGER := 0;
	found BOOL;
BEGIN
	IF NEW.hid IS NULL THEN
		LOOP
			-- if we keep failing to find unique key, increase key length
			itr := itr + 1;
			IF itr % 100 = 0 THEN
				len := len + 1;
			END IF;

			-- generate short ID
			hid := solarcommon.short_id(len, len);

			-- lookup existing key
			EXECUTE qry INTO found USING hid;

			EXIT WHEN found IS NULL;
		END LOOP;

		NEW.hid = hid;
	END IF;

	RETURN NEW;
END
$$;


/**
 * Table for rate limiting buckets.
 *
 * Note the hash-based partition definitions below.
 *
 * @column id    		a 64-bit user key
 * @column state 		bucket internal state data
 * @column expires_at	epoch expiration date
 * @explicit_lock		the advisory lock ID
 */
CREATE TABLE IF NOT EXISTS solarcommon.bucket (
	  id 			BIGINT
	, state			BYTEA
	, expires_at 	BIGINT
	, explicit_lock BIGINT
	, CONSTRAINT bucket_pk PRIMARY KEY (id)
) PARTITION BY hash(id);

CREATE TABLE solarcommon.bucket_1
PARTITION OF solarcommon.bucket
    FOR VALUES WITH (MODULUS 3, REMAINDER 0);

CREATE TABLE solarcommon.bucket_2
PARTITION OF solarcommon.bucket
    FOR VALUES WITH (MODULUS 3, REMAINDER 1);

CREATE TABLE solarcommon.bucket_3
PARTITION OF solarcommon.bucket
FOR VALUES WITH (MODULUS 3, REMAINDER 2);


/**************************************************************************************************
 * FUNCTION solarcommon.jsonb_recursive_merge(jsonb,jsonb,BOOLEAN)
 *
 * Recursively merge JSON objects and arrays (if merge_arrays is TRUE).
 *
 * @param A the "source" JSON
 * @param B the JSON to merge
 * @param merge_arrays if TRUE then also merge arrays, otherwise replace them with values from B
 * @return the merged JSON
 */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_recursive_merge(A jsonb, B jsonb, merge_arrays BOOLEAN DEFAULT FALSE)
RETURNS jsonb LANGUAGE SQL AS
$$
	SELECT jsonb_object_agg(
		COALESCE(ka, kb),
		CASE
			WHEN va ISNULL THEN vb
			WHEN vb ISNULL THEN va
			WHEN merge_arrays AND jsonb_typeof(va) = 'array' AND jsonb_typeof(vb) = 'array' THEN va || vb
			WHEN jsonb_typeof(va) <> 'object' OR jsonb_typeof(vb) <> 'object' THEN vb
			ELSE solarcommon.jsonb_recursive_merge(va, vb, merge_arrays)
		END
	)
	FROM jsonb_each(A) ta(ka, va)
	FULL JOIN jsonb_each(B) tb(kb, vb) ON ka = kb
$$;

/**************************************************************************************************
 * FUNCTION solarcommon.db_migration_get_tag()
 *
 * Get the current DB migration tag.

 * @return the migration tag (if one is set)
 */
CREATE OR REPLACE FUNCTION solarcommon.db_migration_get_tag()
	RETURNS TEXT LANGUAGE SQL STABLE AS
$$
	SELECT svalue
	FROM solarcommon.app_setting
	WHERE skey = 'db'
	AND stype = 'migration'
$$;

/**************************************************************************************************
 * FUNCTION solarcommon.db_migration_set_tag()
 *
 * Mark a DB migration with a specific tag.
 *
 * @param tag the tag to set
 * @return the updated app_setting row with the tag set
 */
CREATE OR REPLACE FUNCTION solarcommon.db_migration_set_tag(tag text)
	RETURNS SETOF solarcommon.app_setting LANGUAGE SQL VOLATILE ROWS 1 AS
$$
	INSERT INTO solarcommon.app_setting (skey, stype, svalue)
	VALUES ('db', 'migration', tag)
	ON CONFLICT (skey, stype) DO UPDATE
	SET modified = CURRENT_TIMESTAMP
		, svalue = EXCLUDED.svalue
	RETURNING app_setting.created
		, app_setting.modified
		, app_setting.skey
		, app_setting.stype
		, app_setting.svalue
$$;
