ALTER TABLE solarnet.sn_node_instruction ADD COLUMN expire_date TIMESTAMP WITH TIME ZONE;

CREATE INDEX sn_node_instruction_exp_idx ON solarnet.sn_node_instruction
	(expire_date) WHERE deliver_state NOT IN (
		'Declined'::solarnet.instruction_delivery_state
		, 'Completed'::solarnet.instruction_delivery_state);
