ALTER TABLE solarnet.sn_node_instruction
	ADD COLUMN modified TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
UPDATE solarnet.sn_node_instruction SET modified = created WHERE modified IS NULL;
ALTER TABLE solarnet.sn_node_instruction ALTER COLUMN modified SET NOT NULL;
