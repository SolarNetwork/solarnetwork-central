CREATE SCHEMA IF NOT EXISTS solarcommon;

CREATE DOMAIN solarcommon.node_id
  AS bigint;

CREATE DOMAIN solarcommon.node_ids
  AS bigint[];

CREATE DOMAIN solarcommon.node_id[]
  AS bigint[];

CREATE DOMAIN solarcommon.source_id
  AS character varying(32)
  DEFAULT '';

CREATE DOMAIN solarcommon.source_ids
  AS character varying(32)[];

CREATE DOMAIN solarcommon.ts
  AS timestamp with time zone
  DEFAULT now();
