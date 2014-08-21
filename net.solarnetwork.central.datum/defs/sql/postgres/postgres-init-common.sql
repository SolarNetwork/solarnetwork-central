CREATE SCHEMA IF NOT EXISTS solarcommon AUTHORIZATION solarnet;

CREATE DOMAIN solarcommon.node_id
  AS bigint
  NOT NULL;
ALTER DOMAIN solarcommon.node_id OWNER TO solarnet;

CREATE DOMAIN solarcommon.source_id
  AS character varying(32)
  DEFAULT ''
  NOT NULL;
ALTER DOMAIN solarcommon.source_id OWNER TO solarnet;

CREATE DOMAIN solarcommon.ts
  AS timestamp with time zone
  DEFAULT now()
  NOT NULL;
ALTER DOMAIN solarcommon.ts OWNER TO solarnet;
