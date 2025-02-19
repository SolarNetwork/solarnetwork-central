CREATE OR REPLACE FUNCTION solarcommon.ant_pattern_to_regexp(pat text)
RETURNS text LANGUAGE SQL STRICT IMMUTABLE AS
$$
	SELECT '^' ||
		regexp_replace(
			regexp_replace(
				regexp_replace(
					regexp_replace(
						regexp_replace(pat, '([!$()+.:<=>[\\\]^{|}-])', '\\\1', 'g'),
					E'[?]', E'[^/]', 'g'),
				E'(?<![*])[*](?![*])', E'[^/]*', 'g'),
			E'[*]{2}', '.*', 'g'),
		E'/[.][*]/', '(/|/.*/)', 'g')
		|| '$';
$$;
