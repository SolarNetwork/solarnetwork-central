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
