
-- ==========================================================================================
-- Updates
-- ==========================================================================================

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
	-- with constraints and da_datm_meta_node_source_checker trigger we assume
	-- the following query can only ever return 1 row (i.e. a node/source combo
	-- is globally unique across da_datm_meta and da_datm_alias)
	SELECT m.stream_id, COALESCE(l.time_zone, 'UTC')
	FROM solardatm.da_datm_meta_aliased m
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

CREATE OR REPLACE FUNCTION solarbill.billing_usage(userid BIGINT, ts_min TIMESTAMP, ts_max TIMESTAMP)
	RETURNS TABLE(
		  node_id BIGINT
		, prop_in BIGINT
		, datum_stored BIGINT
		, datum_out BIGINT
		, instr_issued BIGINT
		, flux_data_in BIGINT
	) LANGUAGE sql STABLE AS
$$
	WITH nodes AS (
		SELECT nlt.time_zone,
			ts_min AT TIME ZONE nlt.time_zone AS sdate,
			ts_max AT TIME ZONE nlt.time_zone AS edate,
			array_agg(DISTINCT nlt.node_id) AS nodes
		FROM solarnet.node_local_time nlt
		INNER JOIN solaruser.user_node un ON un.node_id = nlt.node_id
		WHERE un.user_id = userid
		GROUP BY nlt.time_zone
	)
	, stored AS (
		SELECT
			meta.node_id
			, SUM(acc.datum_count + acc.datum_hourly_count + acc.datum_daily_count + acc.datum_monthly_count) AS datum_count
		FROM nodes nodes
		INNER JOIN solardatm.da_datm_meta meta ON meta.node_id = ANY(nodes.nodes)
		INNER JOIN solardatm.aud_acc_datm_daily acc ON acc.stream_id = meta.stream_id
			AND acc.ts_start >= nodes.sdate AND acc.ts_start < nodes.edate
		GROUP BY meta.node_id
	)
	, datum AS (
		SELECT
			meta.node_id
			, SUM(a.prop_count)::bigint AS prop_count
			, SUM(a.datum_q_count)::bigint AS datum_q_count
			, SUM(a.flux_byte_count)::bigint AS flux_byte_count
		FROM nodes nodes
		INNER JOIN solardatm.da_datm_meta_aliased meta ON meta.node_id = ANY(nodes.nodes)
		INNER JOIN solardatm.aud_datm_daily a ON a.stream_id = meta.stream_id
			AND a.ts_start >= nodes.sdate AND a.ts_start < nodes.edate
		GROUP BY meta.node_id
	)
	, svc AS (
		SELECT
			a.node_id
			, (SUM(a.cnt) FILTER (WHERE a.service = 'inst'))::BIGINT AS instr_issued
		FROM nodes nodes
		INNER JOIN solardatm.aud_node_daily a ON a.node_id = ANY(nodes.nodes)
			AND a.ts_start >= nodes.sdate AND a.ts_start < nodes.edate
		GROUP BY a.node_id
	)
	SELECT
		  COALESCE(s.node_id, a.node_id, svc.node_id) AS node_id
		, COALESCE(a.prop_count, 0)::BIGINT AS prop_in
		, COALESCE(s.datum_count, 0)::BIGINT AS datum_stored
		, COALESCE(a.datum_q_count, 0)::BIGINT AS datum_out
		, COALESCE(svc.instr_issued, 0)::BIGINT AS instr_issued
		, COALESCE(a.flux_byte_count, 0)::BIGINT AS flux_data_in
	FROM stored s
	FULL OUTER JOIN datum a ON a.node_id = s.node_id
	FULL OUTER JOIN svc ON svc.node_id = s.node_id
$$;
