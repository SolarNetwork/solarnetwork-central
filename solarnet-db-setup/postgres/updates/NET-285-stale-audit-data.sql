CREATE OR REPLACE FUNCTION solardatm.audit_increment_datum_q_count(
		node	BIGINT,
		source	TEXT,
		ts 		TIMESTAMP WITH TIME ZONE,
		dcount 	INTEGER
	) RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	sid 	UUID;
	tz		TEXT;
BEGIN
	SELECT m.stream_id, COALESCE(l.time_zone, 'UTC')
	FROM solardatm.da_datm_meta m
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = m.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE m.node_id = node AND m.source_id = source
	INTO sid, tz;

	IF FOUND THEN
		INSERT INTO solardatm.aud_datm_io(stream_id, ts_start, datum_q_count)
		VALUES (sid, date_trunc('hour', ts), dcount)
		ON CONFLICT (stream_id, ts_start) DO UPDATE
		SET datum_q_count = aud_datm_io.datum_q_count + EXCLUDED.datum_q_count;

		INSERT INTO solardatm.aud_stale_datm (stream_id, ts_start, aud_kind)
		VALUES (sid, date_trunc('day', ts AT TIME ZONE tz) AT TIME ZONE tz, 'd')
		ON CONFLICT DO NOTHING;
	END IF;
END
$$;

CREATE OR REPLACE FUNCTION solardatm.audit_increment_mqtt_publish_byte_count(
	service			TEXT,
	node 			BIGINT,
	src				TEXT,
	ts_recv 		TIMESTAMP WITH TIME ZONE,
	bcount			INTEGER
	) RETURNS VOID LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	sid 	UUID;
	tz		TEXT;
BEGIN
	SELECT m.stream_id, COALESCE(l.time_zone, 'UTC')
	FROM solardatm.da_datm_meta m
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = m.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE m.node_id = node AND m.source_id = ANY(ARRAY[src, TRIM(leading '/' FROM src)])
	LIMIT 1
	INTO sid, tz;

	IF FOUND THEN
		INSERT INTO solardatm.aud_datm_io (stream_id, ts_start, flux_byte_count)
		VALUES (sid, date_trunc('hour', ts_recv), bcount)
		ON CONFLICT (stream_id, ts_start) DO UPDATE
		SET flux_byte_count = aud_datm_io.flux_byte_count + EXCLUDED.flux_byte_count;

		INSERT INTO solardatm.aud_stale_datm (stream_id, ts_start, aud_kind)
		VALUES (sid, date_trunc('day', ts_recv AT TIME ZONE tz) AT TIME ZONE tz, 'd')
		ON CONFLICT DO NOTHING;
	END IF;
END
$$;

