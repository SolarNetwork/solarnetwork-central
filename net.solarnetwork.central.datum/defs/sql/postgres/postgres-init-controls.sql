CREATE SEQUENCE solarnet.hardware_control_seq;

CREATE TABLE solarnet.sn_hardware (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	manufact		CHARACTER VARYING(256) NOT NULL,
	model			CHARACTER VARYING(256) NOT NULL,
	revision		INTEGER DEFAULT 0 NOT NULL,
  	fts_default 	tsvector,
	CONSTRAINT sn_hardware_pkey PRIMARY KEY (id),
	CONSTRAINT sn_hardware_unq UNIQUE (manufact, model, revision)
);

CREATE INDEX sn_hardware_fts_default_idx ON solarnet.sn_hardware
USING gin(fts_default);

DROP TRIGGER IF EXISTS maintain_fts ON solarnet.sn_hardware;
CREATE TRIGGER maintain_fts 
  BEFORE INSERT OR UPDATE ON solarnet.sn_hardware FOR EACH ROW EXECUTE PROCEDURE 
  tsvector_update_trigger(fts_default, 'pg_catalog.english', manufact, model);

CREATE TABLE solarnet.sn_hardware_control (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	hw_id			BIGINT NOT NULL,
	ctl_name		CHARACTER VARYING(128) NOT NULL,
	unit			CHARACTER VARYING(16),
	CONSTRAINT sn_hardware_control_pkey PRIMARY KEY (id),
	CONSTRAINT sn_hardware_control_unq UNIQUE (hw_id, ctl_name),
	CONSTRAINT sn_hardware_control_hardware_fk
		FOREIGN KEY (hw_id) REFERENCES solarnet.sn_hardware (id)
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE solarnet.sn_hardware_control_datum (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.hardware_control_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	node_id			BIGINT NOT NULL,
	source_id		CHARACTER VARYING(128) NOT NULL,
	int_val			INTEGER,
	float_val		REAL,
	CONSTRAINT sn_hardware_control_datum_pkey PRIMARY KEY (id),
	CONSTRAINT sn_hardware_control_datum_node_unq UNIQUE (created,node_id,source_id),
	CONSTRAINT sn_hardware_control_datum_node_fk
		FOREIGN KEY (node_id) REFERENCES solarnet.sn_node (node_id)
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- this index is used for foreign key validation in other tables
CREATE INDEX sn_hardware_control_datum_node_idx ON solarnet.sn_hardware_control_datum (node_id,created);

CLUSTER solarnet.sn_hardware_control_datum USING sn_hardware_control_datum_node_unq;

CREATE TABLE solaruser.user_node_hardware_control (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.solaruser_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	node_id			BIGINT NOT NULL,
	source_id		CHARACTER VARYING(128) NOT NULL,
	hwc_id			BIGINT NOT NULL,
	disp_name		CHARACTER VARYING(128),
	CONSTRAINT user_node_hardware_control_pkey PRIMARY KEY (id),
	CONSTRAINT user_node_hardware_control_node_unq UNIQUE (node_id,source_id),
	CONSTRAINT user_node_hardware_control_node_fk
		FOREIGN KEY (node_id) REFERENCES solarnet.sn_node (node_id)
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT user_node_hardware_control_hardware_control_fk
		FOREIGN KEY (hwc_id) REFERENCES solarnet.sn_hardware_control (id)
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

