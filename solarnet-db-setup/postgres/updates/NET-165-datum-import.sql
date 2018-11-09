/**************************************************************************************************
 * TABLE solarnet.sn_datum_import_job
 *
 * Holds records for datum import jobs, where `status` represents the execution status
 * of the task and `config` holds a complete import configuration document.
 */
CREATE TABLE solarnet.sn_datum_import_job (
	id				uuid NOT NULL,
	user_id			BIGINT NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	import_date		TIMESTAMP WITH TIME ZONE NOT NULL,
	state			CHARACTER(1) NOT NULL,
	config			jsonb NOT NULL,
	success 		BOOLEAN,
	message			TEXT,
	completed 		TIMESTAMP WITH TIME ZONE,
	CONSTRAINT datum_import_job_pkey PRIMARY KEY (user_id, id)
);
