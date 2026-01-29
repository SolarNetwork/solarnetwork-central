-- see https://www.postgresql.org/docs/17/collation.html#ICU-COLLATION-SETTINGS
CREATE COLLATION solarcommon.naturalsort (provider = icu, locale = 'und-u-kf-upper-kn');

ALTER TABLE solardatm.da_datm_meta ALTER COLUMN source_id TYPE CHARACTER VARYING(64) COLLATE solarcommon.naturalsort;
ALTER TABLE solardatm.da_loc_datm_meta ALTER COLUMN source_id TYPE CHARACTER VARYING(64) COLLATE solarcommon.naturalsort;
