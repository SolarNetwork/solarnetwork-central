/**
 * Delete a datum row.
 *
 * Either the `sid` parameter OR both the `node` and `source` parameters must be
 * provided. If `sid` is provided then `node` and `source` are ignored.
 *
 * @param uid the user ID that owns the datum
 * @param ts_at the timestamp of the datum to delete
 * @param sid the stream ID of the datum to delete
 * @param node the node ID of the datum to delete, if no `sid` provided
 * @param source the soruce ID of the datum to delete, if no `sid` provided
 * @param track if TRUE then insert appropriate rows into the `solardatm.agg_stale_datm` table
 */
CREATE OR REPLACE FUNCTION solardatm.delete_datm(
	  uid			BIGINT
	, ts_at 		TIMESTAMP WITH TIME ZONE
	, sid			UUID DEFAULT NULL
	, node			BIGINT DEFAULT NULL
	, source		CHARACTER VARYING(64) DEFAULT NULL
	, track			BOOLEAN DEFAULT TRUE
) RETURNS TABLE(
		  stream_id 	UUID
		, ts 			TIMESTAMP WITH TIME ZONE
		, agg_kind 		CHARACTER
		, obj_id 		BIGINT
		, source_id 	CHARACTER VARYING(64)
		, kind 			CHARACTER
	) LANGUAGE plpgsql VOLATILE ROWS 1 AS $$
DECLARE
	resolved_sid UUID;
BEGIN
	ts := ts_at;
	agg_kind := '0';
	kind := 'n';

	-- if node and source IDs provided, look up stream ID
	IF node IS NOT NULL AND source IS NOT NULL AND sid IS NULL THEN
		obj_id := node;
		source_id := source;
		SELECT m.stream_id
		FROM solardatm.da_datm_meta m
		INNER JOIN solaruser.user_node un ON un.node_id = m.node_id AND un.user_id = uid
		WHERE m.node_id = node AND m.source_id = source
		INTO stream_id;
	ELSIF sid IS NOT NULL THEN
		stream_id := sid;
		SELECT m.node_id, m.source_id
		FROM solardatm.da_datm_meta m
		INNER JOIN solaruser.user_node un ON un.node_id = m.node_id AND un.user_id = uid
		WHERE m.stream_id = sid
		INTO obj_id, source_id;
	ELSE
		RAISE EXCEPTION 'No identifier provided.'
		USING ERRCODE = 'invalid_parameter_value',
			SCHEMA = 'solardatm',
			TABLE = 'da_datm',
			HINT = 'Either a stream ID or both node and source IDs must be provided.';
	END IF;

	IF FOUND THEN
		resolved_sid := stream_id; -- to avoid ambiguous ref below
		DELETE FROM solardatm.da_datm d
		WHERE d.stream_id = resolved_sid AND d.ts = ts_at;

		IF FOUND THEN
			RETURN NEXT;
			IF track THEN
				-- mark hourly aggregate as stale
				INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
				SELECT s.stream_id, s.ts_start, 'h' AS agg_kind
				FROM solardatm.calc_stale_datm(sid, ts) s
				ON CONFLICT DO NOTHING;
			END IF;
		END IF;
	END IF;

	RETURN;
END
$$;
