CREATE SCHEMA IF NOT EXISTS solarcommon;

CREATE DOMAIN solarcommon.node_id
  AS bigint
  NOT NULL;

CREATE DOMAIN solarcommon.source_id
  AS character varying(32)
  DEFAULT ''
  NOT NULL;

CREATE DOMAIN solarcommon.ts
  AS timestamp with time zone
  DEFAULT now()
  NOT NULL;
