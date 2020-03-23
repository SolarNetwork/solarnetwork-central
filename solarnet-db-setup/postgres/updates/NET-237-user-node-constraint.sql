ALTER TABLE solaruser.user_node ADD CONSTRAINT user_node_unq UNIQUE (user_id, node_id);
DROP INDEX IF EXISTS solaruser.user_node_user_idx;

ALTER TABLE solaruser.user_node_cert 
	DROP CONSTRAINT user_cert_user_fk,
	ADD CONSTRAINT user_node_cert_user_node_fk FOREIGN KEY (user_id, node_id)
		REFERENCES solaruser.user_node (user_id, node_id) MATCH SIMPLE
		ON UPDATE CASCADE ON DELETE CASCADE;

CREATE OR REPLACE FUNCTION solaruser.node_ownership_transfer()
	RETURNS "trigger"  LANGUAGE 'plpgsql' VOLATILE AS $$
BEGIN
	UPDATE solaruser.user_node_conf
	SET user_id = NEW.user_id
	WHERE user_id = OLD.user_id
		AND node_id = NEW.node_id;

	RETURN NEW;
END;
$$;
