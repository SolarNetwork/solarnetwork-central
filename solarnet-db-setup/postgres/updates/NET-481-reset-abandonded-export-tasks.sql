CREATE OR REPLACE FUNCTION solarnet.purge_completed_datum_export_tasks(older_date TIMESTAMP WITH TIME ZONE)
  RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	num_rows BIGINT := 0;
BEGIN
	DELETE FROM solarnet.sn_datum_export_task
	WHERE completed < older_date AND status = 'c';
	GET DIAGNOSTICS num_rows = ROW_COUNT;

	-- reset very old abandonded tasks to Completed with error
	UPDATE solarnet.sn_datum_export_task
	SET completed = CURRENT_TIMESTAMP
		, success = FALSE
		, status = 'c'
		, message = 'Abandoned'
	WHERE created < (CURRENT_TIMESTAMP - INTERVAL 'P10D')
	AND status IN ('p', 'e');

	RETURN num_rows;
END;
$$;
