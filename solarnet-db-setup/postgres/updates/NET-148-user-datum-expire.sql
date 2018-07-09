CREATE SEQUENCE solaruser.user_expire_seq;

CREATE TABLE solaruser.user_expire_data_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_expire_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	sprops			jsonb,
	filter			jsonb,
	CONSTRAINT user_expire_data_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_expire_data_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_expire_data_conf_user_idx ON solaruser.user_expire_data_conf (user_id);

CREATE OR REPLACE FUNCTION solaruser.expire_datum_for_policy(userid bigint, jpolicy jsonb, older_than timestamptz)
  RETURNS bigint LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	total_count bigint := 0;
	one_count bigint := 0;
	node_ids bigint[];
	have_source_ids boolean := jpolicy->'sourceIds' IS NULL;
	source_id_regexs text[];
	agg_kind text := jpolicy->>'aggregationKey';
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
	DELETE FROM solardatum.da_datum d
	WHERE d.node_id = ANY (node_ids)
		AND d.ts < older_than
		AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
	GET DIAGNOSTICS total_count = ROW_COUNT;

	IF agg_kind IN ('h', 'd', 'm') THEN
		-- delete hourly data
		DELETE FROM solaragg.agg_datum_hourly d
		WHERE d.node_id = ANY (node_ids)
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;
	END IF;

	IF agg_kind IN ('d', 'm') THEN
		-- delete hourly data
		DELETE FROM solaragg.agg_datum_daily d
		WHERE d.node_id = ANY (node_ids)
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;
	END IF;

	IF agg_kind = 'm' THEN
		-- delete hourly data
		DELETE FROM solaragg.agg_datum_monthly d
		WHERE d.node_id = ANY (node_ids)
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;
	END IF;

	RETURN total_count;
END;
$BODY$;
