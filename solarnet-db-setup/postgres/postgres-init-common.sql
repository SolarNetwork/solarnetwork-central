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
 * @param pat the Ant path pattern to convert
 * @returns the equivalent regular expression
 */
CREATE OR REPLACE FUNCTION solarcommon.ant_pattern_to_regexp(pat text)
RETURNS text LANGUAGE SQL STRICT IMMUTABLE AS
$$
	SELECT '^' ||
		regexp_replace(
			regexp_replace(
				regexp_replace(
					regexp_replace(pat, '([!$()+.:<=>[\\\]^{|}-])', '\\\1', 'g'),
				E'[?]', E'[^/]', 'g'),
			E'(?<![*])[*](?![*])', E'[^/]*', 'g'),
		E'[*]{2}', '(?<=/|^).*(?=/|$)', 'g')
		|| '$';
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
 * Extract the timestamp from a v7 UUID.
 *
 * See https://www.ietf.org/archive/id/draft-peabody-dispatch-new-uuid-format-04.html#section-5.2
 * No validation is performed to check that the provided UUID is type 7.
 * Any UUID that encodes a 48-bit millisecond Unix epoch in the highest
 * 6 bytes of the UUID can be decoded by this function.
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
	SELECT to_timestamp((
	      (get_byte(bu, 0)::BIGINT << 40)
		+ (get_byte(bu, 1)::BIGINT << 32)
		+ (get_byte(bu, 2)::BIGINT << 24)
		+ (get_byte(bu, 3)::BIGINT << 16)
		+ (get_byte(bu, 4)::BIGINT << 8)
		+  get_byte(bu, 5)::BIGINT
		) / 1000.0)
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
	) || 'B000000000000000', 32, '0')::uuid
$$;
