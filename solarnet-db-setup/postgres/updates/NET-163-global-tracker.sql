-- keep track of global tracking campaigns
CREATE TABLE solarnet.sn_metric_campaign (
	id CHARACTER VARYING(64) NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    disp_name CHARACTER VARYING(128) NOT NULL,
    description CHARACTER VARYING(512),
	CONSTRAINT sn_metric_campaign_pkey PRIMARY KEY (id)
);
