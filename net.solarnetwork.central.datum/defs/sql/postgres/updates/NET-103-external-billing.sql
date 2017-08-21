ALTER TABLE solaruser.user_user
  ADD COLUMN loc_id bigint;

ALTER TABLE solaruser.user_user
  ADD COLUMN billing_jdata jsonb;

CREATE INDEX user_user_billing_jdata_idx ON solaruser.user_user
	USING GIN (billing_jdata jsonb_path_ops);

ALTER TABLE solaruser.user_user
  ADD CONSTRAINT user_user_loc_fk FOREIGN KEY (loc_id) REFERENCES solarnet.sn_loc (id)
  ON UPDATE NO ACTION ON DELETE NO ACTION;

CREATE OR REPLACE FUNCTION solaragg.find_audit_datum_interval(
	IN node solarcommon.node_id,
	IN src solarcommon.source_id DEFAULT NULL,
	OUT ts_start solarcommon.ts,
	OUT ts_end solarcommon.ts,
	OUT node_tz TEXT,
	OUT node_tz_offset INTEGER)
  RETURNS RECORD AS
$BODY$
BEGIN
	CASE
		WHEN src IS NULL THEN
			SELECT min(a.ts_start) FROM solaragg.aud_datum_hourly a WHERE node_id = node
			INTO ts_start;
		ELSE
			SELECT min(a.ts_start) FROM solaragg.aud_datum_hourly a WHERE node_id = node AND source_id = src
			INTO ts_start;
	END CASE;

	CASE
		WHEN src IS NULL THEN
			SELECT max(a.ts_start) FROM solaragg.aud_datum_hourly a WHERE node_id = node
			INTO ts_end;
		ELSE
			SELECT max(a.ts_start) FROM solaragg.aud_datum_hourly a WHERE node_id = node AND source_id = src
			INTO ts_end;
	END CASE;

	SELECT
		l.time_zone,
		CAST(EXTRACT(epoch FROM z.utc_offset) / 60 AS INTEGER)
	FROM solarnet.sn_node n
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	INNER JOIN pg_timezone_names z ON z.name = l.time_zone
	WHERE n.node_id = node
	INTO node_tz, node_tz_offset;

	IF NOT FOUND THEN
		node_tz := 'UTC';
		node_tz_offset := 0;
	END IF;

END;$BODY$
  LANGUAGE plpgsql STABLE;

