/* === USER ALERTS ===================================================== */

CREATE TYPE solaruser.user_alert_status AS ENUM 
	('Active', 'Disabled', 'Suppressed');

CREATE TYPE solaruser.user_alert_type AS ENUM 
	('NodeStaleData');

CREATE TYPE solaruser.user_alert_sit_status AS ENUM 
	('Active', 'Resolved');

CREATE SEQUENCE solaruser.user_alert_seq;

CREATE TABLE solaruser.user_alert (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_alert_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	node_id			BIGINT,
	valid_to		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	alert_type		solaruser.user_alert_type NOT NULL,
	status			solaruser.user_alert_status NOT NULL,
	alert_opt		json,
	CONSTRAINT user_alert_pkey PRIMARY KEY (id),
	CONSTRAINT user_alert_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE,
	CONSTRAINT user_alert_node_fk FOREIGN KEY (node_id)
		REFERENCES solarnet.sn_node (node_id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* Add index on node_id so we can batch process in sets of nodes. */
CREATE INDEX user_alert_node_idx ON solaruser.user_alert (node_id);

/* Add index on valid_to so we can batch process only those alerts that need validation. */
CREATE INDEX user_alert_valid_idx ON solaruser.user_alert (valid_to);

CREATE TABLE solaruser.user_alert_sit (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_alert_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	alert_id		BIGINT NOT NULL,
	status			solaruser.user_alert_sit_status NOT NULL,
	notified		TIMESTAMP WITH TIME ZONE,
	CONSTRAINT user_alert_sit_pkey PRIMARY KEY (id),
	CONSTRAINT user_alert_sit_alert_fk FOREIGN KEY (alert_id)
		REFERENCES solaruser.user_alert (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/**
 * Return most recent datum records for all available sources for a given set of node IDs.
 * 
 * @param nodes An array of node IDs to return results for.
 * @returns Set of solardatum.da_datum records.
 */
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(nodes solarcommon.node_ids)
  RETURNS SETOF solardatum.da_datum AS
$BODY$
	SELECT r.* 
	FROM (SELECT unnest(nodes) AS node_id) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent(n.node_id)) AS r
	ORDER BY r.node_id, r.source_id;
$BODY$
  LANGUAGE sql STABLE;
