CREATE SEQUENCE solaruser.user_expire_seq;

CREATE TABLE solaruser.user_expire_data_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_expire_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	expire_days		integer NOT NULL,
	sprops			jsonb,
	filter			jsonb,
	CONSTRAINT user_expire_data_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_expire_data_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_expire_data_conf_user_idx ON solaruser.user_expire_data_conf (user_id);

CREATE OR REPLACE FUNCTION solaruser.expire_datum_for_policy(userid bigint, jpolicy jsonb, age interval)
  RETURNS bigint LANGUAGE plpgsql VOLATILE AS
$BODY$
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
		AND d.ts_start < nlt.older_than
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
				(date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
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

	RETURN total_count;
END;
$BODY$;
