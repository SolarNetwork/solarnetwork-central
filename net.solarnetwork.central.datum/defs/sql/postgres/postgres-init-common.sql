CREATE SCHEMA IF NOT EXISTS solarcommon;

CREATE DOMAIN solarcommon.node_id
  AS bigint;

CREATE DOMAIN solarcommon.node_ids
  AS bigint[];

CREATE DOMAIN solarcommon.source_id
  AS character varying(32)
  DEFAULT '';

CREATE DOMAIN solarcommon.source_ids
  AS character varying(32)[];

CREATE DOMAIN solarcommon.ts
  AS timestamp with time zone
  DEFAULT now();

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
