ALTER TABLE solardin.din_endpoint ADD COLUMN incl_res_body BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE solardin.din_endpoint ADD COLUMN req_type CHARACTER VARYING(96);

ALTER TABLE solardin.inin_endpoint ADD COLUMN req_type CHARACTER VARYING(96);
ALTER TABLE solardin.inin_endpoint ADD COLUMN res_type CHARACTER VARYING(96);
