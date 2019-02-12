CREATE SEQUENCE solaruser.user_expire_seq;

CREATE TABLE solaruser.user_expire_data_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_expire_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	expire_days		integer NOT NULL,
	enabled			BOOLEAN NOT NULL DEFAULT FALSE,
	sprops			jsonb,
	filter			jsonb,
	CONSTRAINT user_expire_data_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_expire_data_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_expire_data_conf_user_idx ON solaruser.user_expire_data_conf (user_id);

/**
 * Get a "preview" of calling the `solaruser.expire_datum_for_policy(bigint,jsonb,interval)` function
 * in the form of a record of counts for each type of datum record that would match an expiration
 * policy (and be deleted).
 *
 * The following fields are supported in the expiration policy:
 *
 * nodeIds - an array of node ID values to limit to; all nodes will be included otherwise
 * sourceIds - an array of source ID Ant path patterns to limit to; all sources will be included otherwise
 * aggregationKey - one of `h`, `d`, or `M` for hour, day, and month level records to be included;
 *                  only raw datum are included otherwise; any level automatically includes all levels
 *                  below it, e.g. `M` includes both `d` and `h`
 *
 * @param userid the ID of the user to query on
 * @pram jpolicy an expiration policy, with optional fields to limit the selected datum to
 * @param age only records older than this are included
 */
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
				(date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
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

/**
 * Delete expired datum records according to an expiration policy.
 *
 * The following fields are supported in the expiration policy:
 *
 * nodeIds - an array of node ID values to limit to; all nodes will be included otherwise
 * sourceIds - an array of source ID Ant path patterns to limit to; all sources will be included otherwise
 * aggregationKey - one of `h`, `d`, or `M` for hour, day, and month level records to be included;
 *                  only raw datum are included otherwise; any level automatically includes all levels
 *                  below it, e.g. `M` includes both `d` and `h`
 *
 * @param userid the ID of the user to query on
 * @pram jpolicy an expiration policy, with optional fields to limit the selected datum to
 * @param age only records older than this are included
 */
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

/**************************************************************************************************
 * TABLE solaruser.user_datum_delete_job
 *
 * Holds records for datum delete jobs, where `status` represents the execution status
 * of the job and `config` holds a complete delete configuration document.
 */
CREATE TABLE solaruser.user_datum_delete_job (
	id				uuid NOT NULL,
	user_id			BIGINT NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	progress		DOUBLE PRECISION NOT NULL DEFAULT 0,
	started 		TIMESTAMP WITH TIME ZONE,
	completed 		TIMESTAMP WITH TIME ZONE,
	result_count	BIGINT,
	state			CHARACTER(1) NOT NULL,
	success 		BOOLEAN,
	message			TEXT,
	config			jsonb NOT NULL,
	CONSTRAINT user_datum_delete_job_pkey PRIMARY KEY (user_id, id),
	CONSTRAINT user_datum_delete_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/**************************************************************************************************
 * FUNCTION solaruser.claim_datum_delete_job()
 *
 * "Claim" a delete job from the `solaruser.user_datum_delete_job` table that has a status of 'q'
 * and change the status to 'p' and return it. The tasks will be claimed from oldest to newest
 * based on the created column.
 *
 * @return the claimed row, if one was able to be claimed
 */
CREATE OR REPLACE FUNCTION solaruser.claim_datum_delete_job()
  RETURNS solaruser.user_datum_delete_job LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	rec solaruser.user_datum_delete_job;
	curs CURSOR FOR SELECT * FROM solaruser.user_datum_delete_job
			WHERE state = 'q'
			ORDER BY created ASC, ID ASC
			LIMIT 1
			FOR UPDATE SKIP LOCKED;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO rec;
	IF FOUND THEN
		UPDATE solaruser.user_datum_delete_job SET state = 'p' WHERE CURRENT OF curs;
	END IF;
	CLOSE curs;
	RETURN rec;
END;
$$;

/**************************************************************************************************
 * FUNCTION solaruser.purge_completed_datum_delete_jobs(timestamp with time zone)
 *
 * Delete `solaruser.user_datum_delete_job` rows that have reached the 'c' state and whose
 * completed date is older than the given date.
 *
 * @param older_date The maximum date to delete jobs for.
 * @return The number of rows deleted.
 */
CREATE OR REPLACE FUNCTION solaruser.purge_completed_datum_delete_jobs(older_date timestamp with time zone)
  RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	num_rows BIGINT := 0;
BEGIN
	DELETE FROM solaruser.user_datum_delete_job
	WHERE completed < older_date AND state = 'c';
	GET DIAGNOSTICS num_rows = ROW_COUNT;
	RETURN num_rows;
END;
$$;
