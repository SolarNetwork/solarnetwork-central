/**
 * Delete datum auxiliary record data.
 *
 * @param sid 			the stream ID
 * @param ddate 		the datum timestamp
 * @param aux_type		the auxiliary type; must cast to solardatm.da_datm_aux_type, e.g. 'Reset'
 */
CREATE OR REPLACE FUNCTION solardatm.delete_datum_aux(
		sid				UUID,
		ddate 			TIMESTAMP WITH TIME ZONE,
		aux_type 		solardatm.da_datm_aux_type
	) RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
BEGIN
	DELETE FROM solardatm.da_datm_aux
	WHERE stream_id = sid AND ts = ddate AND atype = aux_type;

	INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
	SELECT stream_id, ts_start, 'h' AS agg_kind
	FROM solardatm.calc_stale_datm(sid, ddate)
	ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
END
$$;
