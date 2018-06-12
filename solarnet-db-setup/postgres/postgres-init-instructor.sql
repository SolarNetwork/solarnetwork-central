CREATE SEQUENCE solarnet.instruction_seq;

CREATE TYPE solarnet.instruction_delivery_state AS ENUM
	('Unknown', 'Queued', 'Queuing', 'Received', 'Executing', 'Declined', 'Completed');

CREATE TABLE solarnet.sn_node_instruction (
	id				BIGINT NOT NULL DEFAULT nextval('solarnet.instruction_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	node_id			BIGINT NOT NULL,
	topic			CHARACTER VARYING(128) NOT NULL,
	instr_date		TIMESTAMP WITH TIME ZONE NOT NULL,
	deliver_state	solarnet.instruction_delivery_state NOT NULL,
	jresult_params	json,
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

/**************************************************************************************************
 * FUNCTION solarnet.purge_completed_instructions(timestamp with time zone)
 * 
 * Delete instructions that have reached the Declined or Completed state, and whose 
 * instruction date is older than the given date.
 * 
 * @param older_date The maximum date to delete instructions for.
 * @return The number of instructions deleted.
 */
CREATE OR REPLACE FUNCTION solarnet.purge_completed_instructions(older_date timestamp with time zone)
  RETURNS BIGINT AS
$BODY$
DECLARE
	num_rows BIGINT := 0;
BEGIN
	DELETE FROM solarnet.sn_node_instruction
	WHERE instr_date < older_date
		AND deliver_state IN (
			'Declined'::solarnet.instruction_delivery_state, 
			'Completed'::solarnet.instruction_delivery_state);
	GET DIAGNOSTICS num_rows = ROW_COUNT;
	RETURN num_rows;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;
