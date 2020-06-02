-- queue for application events related to datum
CREATE TABLE solardatum.da_datum_event (
	ts 			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	node_id 	BIGINT NOT NULL,
	source_id 	CHARACTER VARYING(64) NOT NULL,
	topic text 	NOT NULL,
	jdata 		jsonb
);

CREATE UNIQUE INDEX da_datum_event_unq ON solardatum.da_datum_event 
	(topic, ts DESC, node_id, source_id);

/**
 * FUNCTION solardatum.store_datum_event
 *
 * Add datum event records.
 */
CREATE OR REPLACE FUNCTION solardatum.store_datum_event(
	event_topic CHARACTER VARYING(64),
	cdate TIMESTAMP WITH TIME ZONE,
	node BIGINT,
	src CHARACTER VARYING(64),
	event_json text)
  RETURNS void LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solardatum.da_datum_event(topic, ts, node_id, source_id, jdata)
	VALUES (event_topic, COALESCE(cdate, CURRENT_TIMESTAMP), node, src, event_json::jsonb)
	ON CONFLICT (topic, ts, node_id, source_id) DO NOTHING
$$;
