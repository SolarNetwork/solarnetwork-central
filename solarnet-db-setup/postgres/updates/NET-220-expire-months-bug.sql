CREATE OR REPLACE FUNCTION solaruser.preview_expire_datum_for_policy(userid bigint, jpolicy jsonb, age interval)
  RETURNS TABLE(query_date timestamptz, datum_count bigint, datum_hourly_count integer, datum_daily_count integer, datum_monthly_count integer)
  LANGUAGE plpgsql STABLE AS
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
	WITH nlt AS (
		SELECT
			node_id,
			(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
		FROM solarnet.node_local_time
		WHERE node_id = ANY (node_ids)
	)
	SELECT count(*)
	FROM solardatum.da_datum d, nlt
	WHERE d.node_id = nlt.node_id
		AND d.ts < nlt.older_than
		AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs))
	INTO datum_count;

	IF agg_key IN ('h', 'd', 'M') THEN
		-- count hourly data
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		SELECT count(*)
		FROM solaragg.agg_datum_hourly d, nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs))
		INTO datum_hourly_count;
	END IF;

	IF agg_key IN ('d', 'M') THEN
		-- count daily data
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		SELECT count(*)
		FROM solaragg.agg_datum_daily d, nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs))
		INTO datum_daily_count;
	END IF;

	IF agg_key = 'M' THEN
		-- count monthly data (round down to whole months only)
		WITH nlt AS (
			SELECT
				node_id,
				date_trunc('month', (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age)) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		SELECT count(*)
		FROM solaragg.agg_datum_monthly d, nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs))
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
	WITH nlt AS (
		SELECT
			node_id,
			(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
		FROM solarnet.node_local_time
		WHERE node_id = ANY (node_ids)
	)
	DELETE FROM solardatum.da_datum d
	USING nlt
	WHERE d.node_id = nlt.node_id
		AND d.ts < nlt.older_than
		AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
	GET DIAGNOSTICS total_count = ROW_COUNT;

	-- delete any triggered stale rows
	WITH nlt AS (
		SELECT
			node_id,
			(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
		FROM solarnet.node_local_time
		WHERE node_id = ANY (node_ids)
	)
	DELETE FROM solaragg.agg_stale_datum d
	USING nlt
	WHERE d.node_id = nlt.node_id
		AND d.ts_start <= nlt.older_than
		AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs))
		AND d.agg_kind = 'h';

	-- update daily audit datum counts
	WITH nlt AS (
		SELECT
			node_id,
			(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
		FROM solarnet.node_local_time
		WHERE node_id = ANY (node_ids)
	)
	UPDATE solaragg.aud_datum_daily d
	SET datum_count = 0
	FROM nlt
	WHERE d.node_id = nlt.node_id
		AND d.ts_start < nlt.older_than
		AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));

	IF agg_key IN ('h', 'd', 'M') THEN
		-- delete hourly data
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		DELETE FROM solaragg.agg_datum_hourly d
		USING nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;

		-- update daily audit datum counts
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		UPDATE solaragg.aud_datum_daily d
		SET datum_hourly_count = 0
		FROM nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < nlt.older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
	END IF;

	IF agg_key IN ('d', 'M') THEN
		-- delete daily data
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		DELETE FROM solaragg.agg_datum_daily d
		USING nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;

		-- update daily audit datum counts
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		UPDATE solaragg.aud_datum_daily d
		SET datum_daily_pres = FALSE
		FROM nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < nlt.older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
	END IF;

	IF agg_key = 'M' THEN
		-- delete monthly data (round down to whole months only)
		WITH nlt AS (
			SELECT
				node_id,
				date_trunc('month', (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age)) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		DELETE FROM solaragg.agg_datum_monthly d
		USING nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;
	END IF;

	-- mark all monthly audit data as stale for recalculation
	IF total_count > 0 THEN
		INSERT INTO solaragg.aud_datum_daily_stale (node_id, ts_start, source_id, aud_kind)
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		SELECT d.node_id, d.ts_start, d.source_id, 'm'
		FROM solaragg.aud_datum_monthly d
		INNER JOIN nlt ON nlt.node_id = d.node_id
		WHERE d.ts_start < nlt.older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs))
		ON CONFLICT DO NOTHING;
	END IF;
	
	RETURN total_count;
END;
$$;
