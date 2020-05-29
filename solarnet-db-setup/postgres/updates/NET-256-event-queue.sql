-- queue for application events related to datum
CREATE TABLE solardatum.da_datum_event (
	created 	TIMESTAMP NOT NULL DEFAULT now(),
	node_id 	bigint NOT NULL,
	source_id 	CHARACTER VARYING(64) NOT NULL,
	topic text 	NOT NULL,
	jdata 		jsonb
);

CREATE UNIQUE INDEX da_datum_event_unq ON solardatum.da_datum_event 
	(topic, created DESC, node_id, source_id);
