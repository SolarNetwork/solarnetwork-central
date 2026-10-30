
-- ==========================================================================================
-- New
-- ==========================================================================================

/**
 * Test if a datum stream alias may resolve to its original stream.
 *
 * Current node ownership is always used, so the result follows node ownership transfers. If
 * either node has no owner the alias may not resolve, unless the alias is on the original node.
 *
 * @param orig_node 	the original stream node ID
 * @param orig_source 	the original stream source ID
 * @param host_node 	the alias node ID
 * @returns `TRUE` if the alias may resolve to the original stream
 */
CREATE OR REPLACE FUNCTION solardatm.da_datm_alias_allowed(
		orig_node 	BIGINT,
		orig_source TEXT,
		host_node 	BIGINT
	) RETURNS BOOLEAN LANGUAGE plpgsql STABLE PARALLEL SAFE AS
$$
DECLARE
	orig_user 	BIGINT;
	host_user 	BIGINT;
BEGIN
	IF orig_node = host_node THEN
		RETURN TRUE;
	END IF;

	SELECT user_id INTO orig_user FROM solaruser.user_node WHERE node_id = orig_node;
	SELECT user_id INTO host_user FROM solaruser.user_node WHERE node_id = host_node;

	RETURN COALESCE(orig_user = host_user, FALSE);
END
$$;

/**
 * View combining da_datm_meta and all da_datm_alias rows, to support accounting on alias streams
 * that may no longer resolve. Use `solardatm.da_datm_meta_aliased` for anything else.
 */
CREATE OR REPLACE VIEW solardatm.da_datm_meta_aliased_all AS
SELECT s.stream_id
	, s.node_id
	, s.source_id
	, s.names_i
	, s.names_a
	, s.names_s
	, s.jdata
	, s.stream_id AS orig_stream_id
	, FALSE AS is_alias
	, s.created
	, s.updated
FROM solardatm.da_datm_meta s
UNION ALL
SELECT a.stream_id
	, a.alias_node_id AS node_id
	, a.alias_source_id AS source_id
	, s.names_i
	, s.names_a
	, s.names_s
	, s.jdata
	, s.stream_id AS orig_stream_id
	, TRUE AS is_alias
	, a.created
	, a.modified AS updated
FROM solardatm.da_datm_alias a
INNER JOIN solardatm.da_datm_meta s ON s.node_id = a.node_id AND s.source_id = a.source_id
;

/**
 * View combining da_datm_meta and the da_datm_alias rows that may resolve, to ease querying.
 */
CREATE OR REPLACE VIEW solardatm.da_datm_meta_aliased AS
SELECT s.stream_id
	, s.node_id
	, s.source_id
	, s.names_i
	, s.names_a
	, s.names_s
	, s.jdata
	, s.stream_id AS orig_stream_id
	, FALSE AS is_alias
	, s.created
	, s.updated
FROM solardatm.da_datm_meta s
UNION ALL
SELECT a.stream_id
	, a.alias_node_id AS node_id
	, a.alias_source_id AS source_id
	, s.names_i
	, s.names_a
	, s.names_s
	, s.jdata
	, s.stream_id AS orig_stream_id
	, TRUE AS is_alias
	, a.created
	, a.modified AS updated
FROM solardatm.da_datm_alias a
INNER JOIN solardatm.da_datm_meta s ON s.node_id = a.node_id AND s.source_id = a.source_id
WHERE solardatm.da_datm_alias_allowed(a.node_id, a.source_id::TEXT, a.alias_node_id)
;

/**
 * Disallow saving a datum stream alias with the ID of a node or location datum stream.
 */
CREATE OR REPLACE FUNCTION solardatm.da_datm_alias_validate_stream_id()
	RETURNS "trigger"  LANGUAGE 'plpgsql' VOLATILE AS $$
BEGIN
	IF EXISTS (SELECT 1 FROM solardatm.da_datm_meta WHERE stream_id = NEW.stream_id)
			OR EXISTS (SELECT 1 FROM solardatm.da_loc_datm_meta WHERE stream_id = NEW.stream_id) THEN
		RAISE EXCEPTION 'Stream ID % already exists as a datum stream.', NEW.stream_id
		USING ERRCODE = 'integrity_constraint_violation',
			SCHEMA = 'solardatm',
			TABLE = 'da_datm_alias',
			HINT = 'Use a different stream ID.';
	END IF;
	RETURN NEW;
END;
$$;

CREATE TRIGGER da_datm_alias_stream_id_checker
    BEFORE INSERT OR UPDATE OF stream_id
    ON solardatm.da_datm_alias
    FOR EACH ROW
    EXECUTE PROCEDURE solardatm.da_datm_alias_validate_stream_id();

/**
 * Node IDs of deleted datum stream aliases, so usage audited on those streams can still be
 * attributed to the alias node.
 *
 * @param stream_id 		the alias stream ID
 * @param alias_node_id 	the alias node ID
 * @param deleted 			the deletion date
 */
CREATE TABLE solardatm.da_datm_alias_del (
	stream_id			UUID NOT NULL,
	alias_node_id		BIGINT NOT NULL,
	deleted				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT da_datm_alias_del_pk PRIMARY KEY (stream_id)
);

CREATE INDEX da_datm_alias_del_alias_node_idx ON solardatm.da_datm_alias_del (alias_node_id);

/**
 * Record deleted datum stream aliases.
 */
CREATE OR REPLACE FUNCTION solardatm.da_datm_alias_record_delete()
	RETURNS "trigger"  LANGUAGE 'plpgsql' VOLATILE AS $$
BEGIN
	INSERT INTO solardatm.da_datm_alias_del (stream_id, alias_node_id)
	VALUES (OLD.stream_id, OLD.alias_node_id)
	ON CONFLICT (stream_id) DO UPDATE
	SET alias_node_id = EXCLUDED.alias_node_id,
		deleted = EXCLUDED.deleted;
	RETURN NULL;
END;
$$;

CREATE TRIGGER da_datm_alias_delete_recorder
    AFTER DELETE
    ON solardatm.da_datm_alias
    FOR EACH ROW
    EXECUTE PROCEDURE solardatm.da_datm_alias_record_delete();

/**
 * Find metadata for a stream, for accounting, where an alias stream is included even if it may no
 * longer resolve. Use `solardatm.find_metadata_for_stream()` for anything else.
 *
 * @param sid the stream ID to find metadata for
 * @see solardatm.find_metadata_for_stream()
 */
CREATE OR REPLACE FUNCTION solardatm.find_metadata_for_stream_all(
		sid 		UUID
	) RETURNS TABLE(
		stream_id 	UUID,
		obj_id		BIGINT,
		source_id	CHARACTER VARYING(64),
		created		TIMESTAMP WITH TIME ZONE,
		updated		TIMESTAMP WITH TIME ZONE,
		names_i		TEXT[],
		names_a		TEXT[],
		names_s		TEXT[],
		jdata		JSONB,
		kind		CHARACTER,
		time_zone	CHARACTER VARYING(64)
	) LANGUAGE plpgsql STABLE ROWS 1 AS
$$
BEGIN
	RETURN QUERY
	SELECT m.stream_id, m.node_id AS obj_id, m.source_id, m.created, m.updated
		, m.names_i, m.names_a, m.names_s, m.jdata, 'n'::CHARACTER AS kind
		, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta_aliased_all m
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = m.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE m.stream_id = sid
	LIMIT 1;

	IF NOT FOUND THEN
		RETURN QUERY
		SELECT m.stream_id, m.loc_id AS obj_id, m.source_id, m.created, m.updated
			, m.names_i, m.names_a, m.names_s, m.jdata, 'l'::CHARACTER AS kind
			, COALESCE(l.time_zone, 'UTC') AS time_zone
		FROM solardatm.da_loc_datm_meta m
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = m.loc_id
		WHERE m.stream_id = sid
		LIMIT 1;
	END IF;
END;
$$;


-- ==========================================================================================
-- Updates
-- ==========================================================================================

CREATE OR REPLACE FUNCTION solardatm.process_one_aud_stale_datm(kind CHARACTER)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					solardatm.aud_stale_datm;
	meta					record;
	tz						TEXT;
	result_cnt 				INTEGER := 0;
BEGIN
	-- use a limited delete here to immediately lock the row and block future concurrent
	-- updates that insert same row back into solardatm.aud_stale_datm
	WITH del AS (
		SELECT stream_id, ts_start, aud_kind
		FROM solardatm.aud_stale_datm
		WHERE aud_kind = kind
		FOR UPDATE SKIP LOCKED
		LIMIT 1
	)
	DELETE FROM solardatm.aud_stale_datm d
	USING del
	WHERE d.stream_id = del.stream_id
		AND d.ts_start = del.ts_start
		AND d.aud_kind = del.aud_kind
	RETURNING d.stream_id, d.ts_start, d.aud_kind
	INTO stale.stream_id, stale.ts_start, stale.aud_kind;

	IF FOUND THEN
		-- get stream time zone; will determine if node or location stream
		SELECT * FROM solardatm.find_metadata_for_stream_all(stale.stream_id) INTO meta;
		tz := COALESCE(meta.time_zone, 'UTC');

		CASE kind
			WHEN '0' THEN
				-- raw data counts
				INSERT INTO solardatm.aud_datm_daily (stream_id, ts_start, datum_count, processed_count)
				SELECT stream_id, ts_start, datum_count, CURRENT_TIMESTAMP AS processed_count
				FROM solardatm.calc_audit_datm_raw(
					stale.stream_id, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz)
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					processed_count = EXCLUDED.processed_count;

			WHEN 'h' THEN
				-- hour data counts
				INSERT INTO solardatm.aud_datm_daily (stream_id, ts_start, datum_hourly_count, processed_hourly_count)
				SELECT stream_id, ts_start, datum_hourly_count, CURRENT_TIMESTAMP AS processed_hourly_count
				FROM solardatm.calc_audit_datm_hourly(
					stale.stream_id, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz)
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_hourly_count = EXCLUDED.datum_hourly_count,
					processed_hourly_count = EXCLUDED.processed_hourly_count;

			WHEN 'd' THEN
				-- day data counts, including sum of hourly audit prop_count, datum_q_count, flux_byte_count
				INSERT INTO solardatm.aud_datm_daily (stream_id, ts_start,
					datum_daily_pres, prop_count, prop_u_count, datum_q_count, flux_byte_count, processed_io_count)
				SELECT stream_id, ts_start, datum_daily_pres, prop_count, prop_u_count, datum_q_count,
					flux_byte_count, CURRENT_TIMESTAMP AS processed_io_count
				FROM solardatm.calc_audit_datm_daily(
					stale.stream_id, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz)
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_daily_pres = EXCLUDED.datum_daily_pres,
					prop_count = EXCLUDED.prop_count,
					prop_u_count = EXCLUDED.prop_u_count,
					datum_q_count = EXCLUDED.datum_q_count,
					flux_byte_count = EXCLUDED.flux_byte_count,
					processed_io_count = EXCLUDED.processed_io_count;

			ELSE
				-- month data counts
				INSERT INTO solardatm.aud_datm_monthly (stream_id, ts_start,
					datum_count, datum_hourly_count, datum_daily_count, datum_monthly_pres,
					prop_count, prop_u_count, datum_q_count, flux_byte_count, processed)
				SELECT stream_id, ts_start, datum_count, datum_hourly_count, datum_daily_count,
					datum_monthly_pres, prop_count, prop_u_count, datum_q_count,
					flux_byte_count, CURRENT_TIMESTAMP AS processed
				FROM solardatm.calc_audit_datm_monthly(
					stale.stream_id, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz)
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					datum_hourly_count = EXCLUDED.datum_hourly_count,
					datum_daily_count = EXCLUDED.datum_daily_count,
					datum_monthly_pres = EXCLUDED.datum_monthly_pres,
					prop_count = EXCLUDED.prop_count,
					prop_u_count = EXCLUDED.prop_u_count,
					datum_q_count = EXCLUDED.datum_q_count,
					flux_byte_count = EXCLUDED.flux_byte_count,
					processed = EXCLUDED.processed;
		END CASE;

		CASE kind
			WHEN 'M' THEN
				-- in case node tz changed, remove record(s) from other zone
				-- monthly records clean 1 month on either side
				DELETE FROM solardatm.aud_datm_monthly a
				WHERE a.stream_id = stale.stream_id
					AND a.ts_start > (stale.ts_start AT TIME ZONE tz - interval '1 month') AT TIME ZONE tz
					AND a.ts_start < (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz
					AND a.ts_start <> stale.ts_start;

				-- recalculate full accumulated audit counts for today
				INSERT INTO solardatm.aud_acc_datm_daily (stream_id, ts_start,
					datum_count, datum_hourly_count, datum_daily_count, datum_monthly_count,
					processed)
				SELECT stream_id, ts_start,
					COALESCE(datum_count, 0) AS datum_count,
					COALESCE(datum_hourly_count, 0) AS datum_hourly_count,
					COALESCE(datum_daily_count, 0) AS datum_daily_count,
					COALESCE(datum_monthly_count, 0) AS datum_monthly_count,
					CURRENT_TIMESTAMP
				FROM solardatm.calc_audit_datm_acc(stale.stream_id)
				ON CONFLICT (stream_id, ts_start) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					datum_hourly_count = EXCLUDED.datum_hourly_count,
					datum_daily_count = EXCLUDED.datum_daily_count,
					datum_monthly_count = EXCLUDED.datum_monthly_count,
					processed = EXCLUDED.processed;
			ELSE
				-- in case node tz changed, remove record(s) from other zone
				-- daily records clean 1 day on either side
				DELETE FROM solardatm.aud_datm_daily
				WHERE stream_id = stale.stream_id
					AND ts_start > (stale.ts_start AT TIME ZONE tz - interval '1 day') AT TIME ZONE tz
					AND ts_start < (stale.ts_start AT TIME ZONE tz + interval '1 day') AT TIME ZONE tz
					AND ts_start <> stale.ts_start;

				-- recalculate monthly audit based on updated daily values
				INSERT INTO solardatm.aud_stale_datm (stream_id, ts_start, aud_kind)
				VALUES (
					stale.stream_id,
					date_trunc('month', stale.ts_start AT TIME ZONE tz) AT TIME ZONE tz,
					'M')
				ON CONFLICT DO NOTHING;
		END CASE;

		-- remove processed stale record
		result_cnt := 1;
	END IF;
	RETURN result_cnt;
END;
$$;

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
	FROM solardatm.da_datm_meta_aliased_all m
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
		INNER JOIN (
			SELECT stream_id, node_id FROM solardatm.da_datm_meta_aliased_all
			UNION ALL
			SELECT stream_id, alias_node_id AS node_id FROM solardatm.da_datm_alias_del
		) meta ON meta.node_id = ANY(nodes.nodes)
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
