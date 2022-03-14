/**************************************************************************************************
 * FUNCTION solarnet.purge_incomplete_instructions(timestamp with time zone)
 *
 * Delete instructions regardless of state, whose instruction date is older than the given date.
 *
 * @param older_date The maximum date to delete instructions for.
 * @return The number of instructions deleted.
 */
CREATE OR REPLACE FUNCTION solarnet.purge_incomplete_instructions(older_date timestamp with time zone)
  RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	num_rows BIGINT := 0;
BEGIN
	DELETE FROM solarnet.sn_node_instruction
	WHERE instr_date < older_date;
	GET DIAGNOSTICS num_rows = ROW_COUNT;
	RETURN num_rows;
END
$$;
