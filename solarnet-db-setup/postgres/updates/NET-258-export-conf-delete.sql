ALTER TABLE solaruser.user_export_task
	DROP CONSTRAINT user_export_task_user_export_datum_conf_fk,
	ADD CONSTRAINT user_export_task_user_export_datum_conf_fk FOREIGN KEY (conf_id)
		REFERENCES solaruser.user_export_datum_conf (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE;
