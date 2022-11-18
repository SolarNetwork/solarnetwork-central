-- keep track of global tracking campaigns
CREATE TABLE solarnet.sn_metric_campaign (
	id CHARACTER VARYING(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    disp_name CHARACTER VARYING(128) NOT NULL,
    description CHARACTER VARYING(512),
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT sn_metric_campaign_pkey PRIMARY KEY (id)
);

-- keep track of global tracking campaigns
CREATE TABLE solarnet.sn_metric_campaign_optin (
	campaign_id CHARACTER VARYING(64) NOT NULL,
    node_id BIGINT NOT NULL,
    source_id CHARACTER VARYING(64) NOT NULL,
    prop_type CHARACTER(1) NOT NULL,
    prop_name CHARACTER VARYING(64) NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT sn_metric_campaign_optin_pkey 
		PRIMARY KEY (campaign_id, node_id, source_id, prop_type, prop_name),
	CONSTRAINT sn_metric_campaign_optin_campaign_fk FOREIGN KEY (campaign_id)
		REFERENCES solarnet.sn_metric_campaign (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT sn_metric_campaign_optin_node_fk FOREIGN KEY (node_id)
		REFERENCES solarnet.sn_node (node_id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- table of stale time slots for tracking campaigns
CREATE TABLE solaragg.agg_stale_metric_campaign (
	campaign_id CHARACTER VARYING(64) NOT NULL,
	ts_start TIMESTAMP WITH TIME ZONE NOT NULL,
	ts_end TIMESTAMP WITH TIME ZONE NOT NULL,
	created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT agg_stale_metric_campaign_pkey PRIMARY KEY (campaign_id, ts_start, ts_end)
);

-- table of stale time slots tasks for tracking campaigns;
-- tasks are created out of stale time slots so the work of computing the overall tracking value
-- can be broken into smaller parallel steps and combined in the end
-- NOTE: task_key is SHA1 digest of nodes + sources + prop name + prop type, generated
--       before insert so can be used in primary key
CREATE TABLE solaragg.agg_stale_metric_campaign_task (
	campaign_id CHARACTER VARYING(64) NOT NULL,
	ts_start TIMESTAMP WITH TIME ZONE NOT NULL,
	ts_end TIMESTAMP WITH TIME ZONE NOT NULL,
	task_key bytea(20) NOT NULL,
	node_ids BIGINT[] NOT NULL,
	source_ids CHARACTER VARYING(64)[] NOT NULL,
	prop_name CHARACTER VARYING(64) NOT NULL,
	prop_type CHARACTER(1) NOT NULL,
	created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT agg_stale_metric_campaign_task_pkey PRIMARY KEY (campaign_id, ts_start, ts_end, task_key)
);

-- table to store the final results for tracking campaigns
-- rem_task_count is the remaining number of tasks contributing to the result; when this
-- column reaches 0 then the result is complete
CREATE TABLE solaragg.agg_stale_metric_campaign_result (
	campaign_id CHARACTER VARYING(64) NOT NULL,
	ts_start TIMESTAMP WITH TIME ZONE NOT NULL,
	ts_end TIMESTAMP WITH TIME ZONE NOT NULL,
	res_sum NUMERIC NOT NULL DEFAULT 0,
	res_count BIGINT NOT NULL DEFAULT 0,
	rem_task_count INTEGER NOT NULL,
	created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT agg_stale_metric_campaign_result_pkey PRIMARY KEY (campaign_id, ts_start, ts_end)
);
