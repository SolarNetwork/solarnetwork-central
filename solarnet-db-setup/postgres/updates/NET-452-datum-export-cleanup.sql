DROP FUNCTION IF EXISTS solaruser.purge_completed_user_export_tasks(timestamp with time zone);

DROP FUNCTION IF EXISTS solaruser.purge_completed_user_adhoc_export_tasks(timestamp with time zone);

ALTER TABLE solaruser.user_export_task DROP CONSTRAINT IF EXISTS user_export_task_datum_export_task_fk;

ALTER TABLE solaruser.user_export_task ADD CONSTRAINT user_export_task_datum_export_task_fk
		FOREIGN KEY (task_id) REFERENCES solarnet.sn_datum_export_task (id)
		ON UPDATE NO ACTION ON DELETE CASCADE;
