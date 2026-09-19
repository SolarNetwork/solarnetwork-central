ALTER TYPE solardatm.da_datm_aux_type ADD VALUE 'Mark';
ALTER TYPE solardatm.da_datm_aux_type ADD VALUE 'Annotation';
ALTER TYPE solardatm.da_datm_aux_type ADD VALUE 'Flag';
ALTER TYPE solardatm.da_datm_aux_type ADD VALUE 'Note';

CREATE OR REPLACE FUNCTION solardatm.store_datum_aux(
		sid				UUID,
		ddate 			TIMESTAMP WITH TIME ZONE,
		aux_type 		solardatm.da_datm_aux_type,
		aux_notes 		TEXT,
		jdata_final 	JSONB,
		jdata_start 	JSONB,
		jmeta 			JSONB
	) RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
BEGIN
	INSERT INTO solardatm.da_datm_aux (stream_id, ts, atype, notes, jdata_af, jdata_as, jmeta)
	VALUES (sid, ddate, aux_type, aux_notes, jdata_final->'a', jdata_start->'a', jmeta)
	ON CONFLICT (stream_id, ts, atype) DO UPDATE
		SET updated = CURRENT_TIMESTAMP,
			notes = EXCLUDED.notes,
			jdata_af = EXCLUDED.jdata_af,
			jdata_as = EXCLUDED.jdata_as,
			jmeta = EXCLUDED.jmeta;

	IF aux_type = 'Reset'::solardatm.da_datm_aux_type THEN
		-- insert stale record for updated row
		INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
		SELECT stream_id, ts_start, 'h' AS agg_kind
		FROM solardatm.calc_stale_datm(sid, ddate)
		ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
		
	END IF;
END
$$;

CREATE OR REPLACE FUNCTION solardatm.delete_datum_aux(
		sid				UUID,
		ddate 			TIMESTAMP WITH TIME ZONE,
		aux_type 		solardatm.da_datm_aux_type
	) RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
BEGIN
	DELETE FROM solardatm.da_datm_aux
	WHERE stream_id = sid AND ts = ddate AND atype = aux_type;

	IF aux_type = 'Reset'::solardatm.da_datm_aux_type THEN
		-- insert stale record for deleted row
		INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
		SELECT stream_id, ts_start, 'h' AS agg_kind
		FROM solardatm.calc_stale_datm(sid, ddate)
		ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
	END IF;
END
$$;

CREATE OR REPLACE FUNCTION solardatm.move_datum_aux(
		sid_from		UUID,
		ts_from			TIMESTAMP WITH TIME ZONE,
		aux_type_from	solardatm.da_datm_aux_type,

		sid_to			UUID,
		ts_to			TIMESTAMP WITH TIME ZONE,
		atype_to 		solardatm.da_datm_aux_type,
		notes_to 		TEXT,
		jdata_final_to 	JSONB,
		jdata_start_to 	JSONB,
		jmeta_to 		JSONB
	) RETURNS BOOLEAN LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	del_count 	INTEGER := 0;
BEGIN
	DELETE FROM solardatm.da_datm_aux
	WHERE stream_id = sid_from
		AND ts = ts_from
		AND atype = aux_type_from;

	GET DIAGNOSTICS del_count = ROW_COUNT;

	IF del_count > 0 THEN
		IF aux_type_from = 'Reset'::solardatm.da_datm_aux_type OR atype_to = 'Reset'::solardatm.da_datm_aux_type THEN
			-- insert stale record for deleted row
			INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
			SELECT stream_id, ts_start, 'h' AS agg_kind
			FROM solardatm.calc_stale_datm(sid_from, ts_from)
			ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
		END IF;

		-- insert new row
		INSERT INTO solardatm.da_datm_aux (stream_id, ts, atype, notes, jdata_af, jdata_as, jmeta)
		VALUES (sid_to, ts_to, atype_to, notes_to, jdata_final_to->'a', jdata_start_to->'a', jmeta_to)
		ON CONFLICT (stream_id, ts, atype) DO UPDATE
			SET updated = CURRENT_TIMESTAMP,
				notes = EXCLUDED.notes,
				jdata_af = EXCLUDED.jdata_af,
				jdata_as = EXCLUDED.jdata_as,
				jmeta = EXCLUDED.jmeta;

		IF aux_type_from = 'Reset'::solardatm.da_datm_aux_type OR atype_to = 'Reset'::solardatm.da_datm_aux_type THEN
			-- insert stale record for new row
			INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
			SELECT stream_id, ts_start, 'h' AS agg_kind
			FROM solardatm.calc_stale_datm(sid_to, ts_to)
			ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
		END IF;
	END IF;

	RETURN (del_count > 0);
END
$$;
