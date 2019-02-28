ALTER TABLE solardatum.da_datum_aux 
ADD COLUMN jmeta jsonb;

/**
 * FUNCTION solardatum.store_datum_aux(timestamp with time zone,bigint,character varying,solardatum.da_datum_aux_type,text,text,text,text)
 *
 * Add or replace datum auxiliary record data.
 */
CREATE OR REPLACE FUNCTION solardatum.store_datum_aux(
	cdate timestamp with time zone,
	node bigint,
	src character varying(64),
	aux_type solardatum.da_datum_aux_type,
	aux_notes text,
	jdata_final text,
	jdata_start text,
	jmeta text)
  RETURNS void LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solardatum.da_datum_aux(ts, node_id, source_id, atype, updated, notes, jdata_af, jdata_as, jmeta)
	VALUES (cdate, node, src, aux_type, CURRENT_TIMESTAMP, aux_notes, 
		(jdata_final::jsonb)->'a', (jdata_start::jsonb)->'a', jmeta::jsonb)
	ON CONFLICT (ts, node_id, source_id, atype) DO UPDATE
	SET notes = EXCLUDED.notes,
		jdata_af = EXCLUDED.jdata_af,
		jdata_as = EXCLUDED.jdata_as, 
		jmeta = EXCLUDED.jmeta,
		updated = EXCLUDED.updated;
$$;

/**
 * FUNCTION solardatum.move_datum_aux(timestamp with time zone,bigint,character varying,solardatum.da_datum_aux_type,timestamp with time zone,bigint,character varying,solardatum.da_datum_aux_type,text,text,text,text)
 *
 * Update and move datum auxiliary record data.
 */
CREATE OR REPLACE FUNCTION solardatum.move_datum_aux(
	cdate_from timestamp with time zone,
	node_from bigint,
	src_from character varying(64),
	aux_type_from solardatum.da_datum_aux_type,
	cdate timestamp with time zone,
	node bigint,
	src character varying(64),
	aux_type solardatum.da_datum_aux_type,
	aux_notes text,
	jdata_final text,
	jdata_start text,
	meta_json text)
  RETURNS BOOLEAN LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	del_count integer := 0;
BEGIN
	-- note we are doing DELETE/INSERT so that trigger_agg_stale_datum can correctly pick up old/new stale rows
	DELETE FROM solardatum.da_datum_aux
	WHERE ts = cdate_from AND node_id = node_from AND source_id = src_from AND atype = aux_type_from;
	
	GET DIAGNOSTICS del_count = ROW_COUNT;
	
	IF del_count > 0 THEN
		PERFORM solardatum.store_datum_aux(cdate, node, src, aux_type, aux_notes, jdata_final, jdata_start, meta_json);
	END IF;
	
	RETURN (del_count > 0);
END;
$$;
