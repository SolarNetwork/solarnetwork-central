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
SELECT jsonb_strip_nulls(jsonb_set(jsonb_set(jsonb_set(jsonb_set(
				'{"t":null}'::jsonb, '{t}', COALESCE(to_jsonb(jdata_t), 'null'::jsonb)),
			'{s}', COALESCE(jdata_s, 'null'::jsonb)),
		'{a}', COALESCE(jdata_a, 'null'::jsonb)),
	'{i}', COALESCE(jdata_i, 'null'::jsonb)));
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
