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
 * 		FROM solardatum.da_datum
 * 		GROUP BY node_id, source_id
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
