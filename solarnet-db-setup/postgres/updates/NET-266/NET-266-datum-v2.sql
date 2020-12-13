-- this view called into solaragg.agg_datum_daily table; replaced by datum metadata query
DROP VIEW IF EXISTS solaruser.user_auth_token_sources;

-- tweak to add STRICT and ROWS
CREATE OR REPLACE FUNCTION solarcommon.reduce_dim(anyarray)
	RETURNS SETOF anyarray LANGUAGE plpgsql IMMUTABLE STRICT ROWS 20 AS
$$
DECLARE
	s $1%TYPE;
BEGIN
	FOREACH s SLICE 1 IN ARRAY $1 LOOP
		RETURN NEXT s;
	END LOOP;
	RETURN;
END
$$;

-- update user expire functions that delete from datum tables

CREATE OR REPLACE FUNCTION solaruser.preview_expire_datum_for_policy(userid bigint, jpolicy jsonb, age interval)
	RETURNS TABLE(
		query_date 			TIMESTAMP WITH TIME ZONE,
		datum_count 		BIGINT,
		datum_hourly_count 	INTEGER,
		datum_daily_count 	INTEGER,
		datum_monthly_count INTEGER
	) LANGUAGE plpgsql STABLE AS
$$
DECLARE
	node_ids bigint[];
	have_source_ids boolean := jpolicy->'sourceIds' IS NULL;
	source_id_regexs text[];
	agg_key text := jpolicy->>'aggregationKey';
BEGIN
	-- filter node IDs to only those owned by user
	SELECT ARRAY(SELECT node_id
				 FROM solaruser.user_node un
				 WHERE un.user_id = userid
					AND (
						jpolicy->'nodeIds' IS NULL
						OR jpolicy->'nodeIds' @> un.node_id::text::jsonb
					)
				)
	INTO node_ids;

	-- get array of source ID regexs
	SELECT ARRAY(SELECT solarcommon.ant_pattern_to_regexp(jsonb_array_elements_text(jpolicy->'sourceIds')))
	INTO source_id_regexs;

	-- count raw data
	WITH s AS (
		SELECT s.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
		FROM solardatm.da_datm_meta s
		INNER JOIN solarnet.sn_node n ON n.node_id = s.node_id
		INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE s.node_id = ANY(node_ids)
			AND (have_source_ids OR s.source_id ~ ANY(source_id_regexs))
	)
	SELECT COUNT(*)
	FROM s
	INNER JOIN solardatm.da_datm d ON d.stream_id = s.stream_id
	WHERE d.ts < (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age) AT TIME ZONE s.time_zone
	INTO datum_count;

	IF agg_key IN ('h', 'd', 'M') THEN
		-- count hourly data
		WITH s AS (
			SELECT s.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
			FROM solardatm.da_datm_meta s
			INNER JOIN solarnet.sn_node n ON n.node_id = s.node_id
			INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
			WHERE s.node_id = ANY(node_ids)
				AND (have_source_ids OR s.source_id ~ ANY(source_id_regexs))
		)
		SELECT COUNT(*)
		FROM s
		INNER JOIN solardatm.agg_datm_hourly d ON d.stream_id = s.stream_id
		WHERE d.ts_start < (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age) AT TIME ZONE s.time_zone
		INTO datum_hourly_count;
	END IF;

	IF agg_key IN ('d', 'M') THEN
		-- count daily data
		WITH s AS (
			SELECT s.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
			FROM solardatm.da_datm_meta s
			INNER JOIN solarnet.sn_node n ON n.node_id = s.node_id
			INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
			WHERE s.node_id = ANY(node_ids)
				AND (have_source_ids OR s.source_id ~ ANY(source_id_regexs))
		)
		SELECT COUNT(*)
		FROM s
		INNER JOIN solardatm.agg_datm_daily d ON d.stream_id = s.stream_id
		WHERE d.ts_start < (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age) AT TIME ZONE s.time_zone
		INTO datum_daily_count;
	END IF;

	IF agg_key = 'M' THEN
		-- count monthly data (round down to whole months only)
		WITH s AS (
			SELECT s.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
			FROM solardatm.da_datm_meta s
			INNER JOIN solarnet.sn_node n ON n.node_id = s.node_id
			INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
			WHERE s.node_id = ANY(node_ids)
				AND (have_source_ids OR s.source_id ~ ANY(source_id_regexs))
		)
		SELECT COUNT(*)
		FROM s
		INNER JOIN solardatm.agg_datm_monthly d ON d.stream_id = s.stream_id
		WHERE d.ts_start < date_trunc('month', (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age)) AT TIME ZONE s.time_zone
		INTO datum_monthly_count;
	END IF;

	query_date = date_trunc('day', CURRENT_TIMESTAMP);
	RETURN NEXT;
END;
$$;

CREATE OR REPLACE FUNCTION solaruser.expire_datum_for_policy(userid bigint, jpolicy jsonb, age interval)
  RETURNS bigint LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	total_count bigint := 0;
	one_count bigint := 0;
	node_ids bigint[];
	have_source_ids boolean := jpolicy->'sourceIds' IS NULL;
	source_id_regexs text[];
	agg_key text := jpolicy->>'aggregationKey';
BEGIN
	-- filter node IDs to only those owned by user
	SELECT ARRAY(SELECT node_id
				 FROM solaruser.user_node un
				 WHERE un.user_id = userid
					AND (
						jpolicy->'nodeIds' IS NULL
						OR jpolicy->'nodeIds' @> un.node_id::text::jsonb
					)
				)
	INTO node_ids;

	-- get array of source ID regexs
	SELECT ARRAY(SELECT solarcommon.ant_pattern_to_regexp(jsonb_array_elements_text(jpolicy->'sourceIds')))
	INTO source_id_regexs;

	-- delete raw data
	WITH s AS (
		SELECT s.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
		FROM solardatm.da_datm_meta s
		INNER JOIN solarnet.sn_node n ON n.node_id = s.node_id
		INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE s.node_id = ANY(node_ids)
			AND (have_source_ids OR s.source_id ~ ANY(source_id_regexs))
	)
	, audit AS (
		UPDATE solardatm.aud_datm_daily d
		SET datum_count = 0
		FROM s
		WHERE d.stream_id = s.stream_id
			AND d.ts_start < (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age) AT TIME ZONE s.time_zone
	)
	DELETE FROM solardatm.da_datm d
	USING s
	WHERE d.stream_id = s.stream_id
		AND d.ts < (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age) AT TIME ZONE s.time_zone;
	GET DIAGNOSTICS total_count = ROW_COUNT;

	IF agg_key IN ('h', 'd', 'M') THEN
		-- delete hourly data
		WITH s AS (
			SELECT s.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
			FROM solardatm.da_datm_meta s
			INNER JOIN solarnet.sn_node n ON n.node_id = s.node_id
			INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
			WHERE s.node_id = ANY(node_ids)
				AND (have_source_ids OR s.source_id ~ ANY(source_id_regexs))
		)
		, audit AS (
			UPDATE solardatm.aud_datm_daily d
			SET datum_hourly_count = 0
			FROM s
			WHERE d.stream_id = s.stream_id
				AND d.ts_start < (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age) AT TIME ZONE s.time_zone
		)
		DELETE FROM solardatm.agg_datm_hourly d
		USING s
		WHERE d.stream_id = s.stream_id
			AND d.ts_start < (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age) AT TIME ZONE s.time_zone;
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;
	END IF;

	IF agg_key IN ('d', 'M') THEN
		-- delete daily data
		WITH s AS (
			SELECT s.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
			FROM solardatm.da_datm_meta s
			INNER JOIN solarnet.sn_node n ON n.node_id = s.node_id
			INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
			WHERE s.node_id = ANY(node_ids)
				AND (have_source_ids OR s.source_id ~ ANY(source_id_regexs))
		)
		, audit AS (
			UPDATE solardatm.aud_datm_daily d
			SET datum_daily_pres = FALSE
			FROM s
			WHERE d.stream_id = s.stream_id
				AND d.ts_start < (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age) AT TIME ZONE s.time_zone
		)
		DELETE FROM solardatm.agg_datm_daily d
		USING s
		WHERE d.stream_id = s.stream_id
			AND d.ts_start < (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age) AT TIME ZONE s.time_zone;
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;
	END IF;

	IF agg_key = 'M' THEN
		-- delete monthly data (round down to whole months only)
		WITH s AS (
			SELECT s.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
			FROM solardatm.da_datm_meta s
			INNER JOIN solarnet.sn_node n ON n.node_id = s.node_id
			INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
			WHERE s.node_id = ANY(node_ids)
				AND (have_source_ids OR s.source_id ~ ANY(source_id_regexs))
		)
		DELETE FROM solardatm.agg_datm_monthly d
		USING s
		WHERE d.stream_id = s.stream_id
			AND d.ts_start < date_trunc('month', (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age)) AT TIME ZONE s.time_zone;
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;
	END IF;

	-- mark all monthly audit data as stale for recalculation
	IF total_count > 0 THEN
		INSERT INTO solardatm.aud_stale_datm (stream_id, ts_start, aud_kind)
		WITH s AS (
			SELECT s.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
			FROM solardatm.da_datm_meta s
			INNER JOIN solarnet.sn_node n ON n.node_id = s.node_id
			INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
			WHERE s.node_id = ANY(node_ids)
				AND (have_source_ids OR s.source_id ~ ANY(source_id_regexs))
		)
		SELECT s.stream_id, d.ts_start, 'M'
		FROM s
		INNER JOIN solardatm.aud_datm_monthly d ON d.stream_id = s.stream_id
		WHERE d.ts_start < (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE s.time_zone) - age) AT TIME ZONE s.time_zone
		ON CONFLICT DO NOTHING;
	END IF;

	RETURN total_count;
END;
$$;
