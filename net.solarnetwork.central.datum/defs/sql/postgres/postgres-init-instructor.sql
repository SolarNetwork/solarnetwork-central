
CREATE TYPE solarnet.instruction_delivery_state AS ENUM 
	('Unknown', 'Queued', 'Received', 'Executing', 'Declined', 'Completed');

CREATE TABLE solarnet.sn_node_instruction (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.solarnet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	node_id			BIGINT NOT NULL,
	topic			CHARACTER VARYING(128) NOT NULL,
	instr_date		TIMESTAMP WITH TIME ZONE NOT NULL,
	deliver_state	solarnet.instruction_delivery_state NOT NULL,
	CONSTRAINT sn_node_instruction_pkey PRIMARY KEY (id),
	CONSTRAINT sn_node_instruction_node_fk
		FOREIGN KEY (node_id) REFERENCES solarnet.sn_node (node_id)
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX sn_node_instruction_node_idx ON solarnet.sn_node_instruction 
	(node_id, deliver_state, instr_date);

CREATE TABLE solarnet.sn_node_instruction_param (
	instr_id		BIGINT NOT NULL,
	idx				INTEGER NOT NULL,
	pname			CHARACTER VARYING(256) NOT NULL,
	pvalue			CHARACTER VARYING(256) NOT NULL,
	CONSTRAINT sn_node_instruction_param_pkey PRIMARY KEY (instr_id, idx),
	CONSTRAINT sn_node_instruction_param_sn_node_instruction_fk
		FOREIGN KEY (instr_id) REFERENCES solarnet.sn_node_instruction (id) 
		ON UPDATE NO ACTION ON DELETE CASCADE
);
