/**
 * Node/source ID aliases for datum streams
 */
CREATE TABLE solardatm.da_datm_alias (
	stream_id		UUID NOT NULL,
	node_id			BIGINT NOT NULL,
	source_id		CHARACTER VARYING(64) NOT NULL COLLATE solarcommon.naturalsort,
	CONSTRAINT da_datm_alias_pkey PRIMARY KEY (stream_id, node_id, source_id),
	CONSTRAINT da_datm_alias_stream_fk FOREIGN KEY (stream_id)
		REFERENCES solardatm.da_datm_meta (stream_id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX da_datm_alias_node_source_idx ON solardatm.da_datm_alias (node_id, source_id);

DROP VIEW solardatm.da_datm_meta_aliased;
/**
 * View combining da_datm_meta and da_datm_alias, to ease querying.
 */
CREATE OR REPLACE VIEW solardatm.da_datm_meta_aliased AS
SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata, 0::INTEGER AS mtype
FROM solardatm.da_datm_meta s
UNION ALL
SELECT s.stream_id, a.node_id, a.source_id, s.names_i, s.names_a, s.names_s, s.jdata, 1::INTEGER AS mtype
FROM solardatm.da_datm_alias a
INNER JOIN solardatm.da_datm_meta s ON s.stream_id = a.stream_id
;
