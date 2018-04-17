/**************************************************************************************************
 * TABLE solarnet.sn_datum_export_task
 *
 * Holds records for datum export tasks, where `status` represents the execution status
 * of the task and `config` holds a complete export configuration document.
 */
CREATE TABLE solarnet.sn_datum_export_task (
	id				uuid NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	export_date		TIMESTAMP WITH TIME ZONE NOT NULL,
	status			CHARACTER(1) NOT NULL,
	config			jsonb NOT NULL,
	CONSTRAINT datum_export_task_pkey PRIMARY KEY (id)
);

/**************************************************************************************************
 * FUNCTION solarnet.add_datum_export_task(uuid, timestamp with time zone, jsonb)
 *
 * Insert a new datum export task record.
 *
 * @param uid the UUID of the task
 * @param ex_date the export date of the task
 * @param cfg the complete export configuration document
 * @return the status value of the inserted record
 */
CREATE OR REPLACE FUNCTION solarnet.add_datum_export_task(
	uid uuid,
	ex_date TIMESTAMP WITH TIME ZONE,
	cfg jsonb
  ) RETURNS CHARACTER(1) LANGUAGE plpgsql VOLATILE AS
$BODY$
BEGIN
	INSERT INTO solarnet.sn_datum_export_task
		(id, created, export_date, config, status)
	VALUES
		(uid, CURRENT_TIMESTAMP, ex_date, cfg, 'q');
	RETURN 'q';
END;
$BODY$;
