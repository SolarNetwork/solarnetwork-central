/**
 * General node audit ingest data table.
 *
 * This data represents the lowest level audit data for nodes. The `service` column defines the
 * metric being counted, and is application defined.
 */
CREATE TABLE solardatm.aud_node_io (
	node_id					BIGINT NOT NULL,
	service					CHARACTER(4) NOT NULL,
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
	cnt 					INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS aud_node_io_pkey ON solardatm.aud_node_io (node_id, service, ts_start DESC);

/**
 * Node audit daily summary data table.
 *
 * This data represents a calculated summary of counts of node-related rows in other tables.
 * It must be maintained as the source data tables are updated.
 */
CREATE TABLE solardatm.aud_node_daily (
	node_id					BIGINT NOT NULL,
	service					CHARACTER(4) NOT NULL,
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
    cnt 					BIGINT NOT NULL DEFAULT 0,
	processed				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS aud_node_daily_pkey
ON solardatm.aud_node_daily (node_id, service, ts_start DESC);

/**
 * Node audit monthly summary data table.
 *
 * This data represents a calculated summary of counts of node-related rows in the
 * solardatm.aud_node_daily table. It must be maintained as the source data table is updated.
 */
CREATE TABLE solardatm.aud_node_monthly (
	node_id					BIGINT NOT NULL,
	service					CHARACTER(4) NOT NULL,
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
    cnt 					BIGINT NOT NULL DEFAULT 0,
	processed 				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS aud_node_monthly_pkey
ON solardatm.aud_node_monthly (node_id, service, ts_start DESC);

-- "stale" audit queue table
CREATE TABLE solardatm.aud_stale_node (
	node_id					BIGINT NOT NULL,
	service					CHARACTER(4),
	ts_start				TIMESTAMP WITH TIME ZONE NOT NULL,
	aud_kind 				CHARACTER NOT NULL,
	created 				TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT aud_stale_node_pkey PRIMARY KEY (aud_kind, service, ts_start, node_id)
);

/**
 * Update the `solardatm.aud_node_io` table by adding instruction counts.
 *
 * @param srvc 		the srvc name
 * @param node 		the node ID
 * @param ts_recv	the timestamp; will be truncated to 'hour' level
 * @param icount	the count to add
 */
CREATE OR REPLACE FUNCTION solardatm.audit_increment_node_count(
	node 			BIGINT,
	srvc			CHARACTER(4),
	ts_recv 		TIMESTAMP WITH TIME ZONE,
	icount			INTEGER
	) RETURNS VOID LANGUAGE PLPGSQL VOLATILE AS
$$
DECLARE
	tz						TEXT;
BEGIN
	-- get node time zone
	SELECT COALESCE(solarnet.get_node_timezone(stale.node_id), 'UTC') INTO tz;

	INSERT INTO solardatm.aud_node_io (node_id, service, ts_start, cnt)
	VALUES (node, srvc, date_trunc('hour', ts_recv), icount)
	ON CONFLICT (node_id, service, ts_start) DO UPDATE
	SET cnt = aud_node_io.cnt + EXCLUDED.cnt;

	INSERT INTO solardatm.aud_stale_node (node_id, service, ts_start, aud_kind)
	VALUES (node, srvc, date_trunc('day', ts_recv AT TIME ZONE tz) AT TIME ZONE tz, 'd')
	ON CONFLICT DO NOTHING;
END
$$;

/**
 * Calculate node daily audit values for a specific node service over a time range.
 *
 * @param node the 	node ID
 * @param srvc the 	audit service name
 * @param start_ts 	the starting time (inclusive)
 * @param end_ts 	the ending time (exclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.calc_audit_node_daily(
		node 		BIGINT,
		srvc		CHARACTER(4),
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		node_id		BIGINT,
		service		CHARACTER(4),
		ts_start 	TIMESTAMP WITH TIME ZONE,
		cnt			BIGINT
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT
		node,
		srvc,
		start_ts,
		SUM(aud.cnt) AS cnt
	FROM solardatm.aud_node_io aud
	WHERE aud.node_id = node
		AND aud.service = srvc
		AND aud.ts_start >= start_ts
		AND aud.ts_start < end_ts
	GROUP BY aud.node_id, aud.service
$$;

/**
 * Calculate node monthly audit values for a specific node service over a time range.
 *
 * @param node the 	node ID
 * @param srvc the 	audit service name
 * @param start_ts 	the starting time (inclusive)
 * @param end_ts 	the ending time (exclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.calc_audit_node_monthly(
		node 		BIGINT,
		srvc		CHARACTER(4),
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		node_id		BIGINT,
		service		CHARACTER(4),
		ts_start 	TIMESTAMP WITH TIME ZONE,
		cnt			BIGINT
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT
		node,
		srvc,
		start_ts,
		SUM(aud.cnt)::BIGINT AS cnt
	FROM solardatm.aud_node_daily aud
	WHERE aud.node_id = node
		AND aud.service = srvc
		AND aud.ts_start >= start_ts
		AND aud.ts_start < end_ts
	GROUP BY aud.node_id, aud.service
$$;

CREATE OR REPLACE FUNCTION solardatm.process_one_aud_stale_node(kind CHARACTER)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					solardatm.aud_stale_node;
	tz						TEXT;
	curs 					CURSOR FOR
							SELECT * FROM solardatm.aud_stale_node
							WHERE aud_kind = kind
							LIMIT 1 FOR UPDATE SKIP LOCKED;
	result_cnt 				INTEGER := 0;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO stale;

	IF FOUND THEN
		-- get node time zone; will determine if node or location stream
		SELECT COALESCE(solarnet.get_node_timezone(stale.node_id), 'UTC') INTO tz;

		CASE kind
			WHEN 'd' THEN
				-- day data counts summerized from hourly data
				INSERT INTO solardatm.aud_node_daily (node_id, service, ts_start, cnt, processed)
				SELECT node_id, service, ts_start, cnt, CURRENT_TIMESTAMP AS processed
				FROM solardatm.calc_audit_node_daily(
					stale.node_id, stale.service, stale.ts_start, stale.ts_start + interval '1 day')
				ON CONFLICT (node_id, service, ts_start) DO UPDATE
				SET cnt = EXCLUDED.cnt, processed = EXCLUDED.processed;

			ELSE
				-- month data counts summerized from daily data
				INSERT INTO solardatm.aud_node_monthly (node_id, service, ts_start, cnt, processed)
				SELECT node_id, service, ts_start, cnt, CURRENT_TIMESTAMP AS processed
				FROM solardatm.calc_audit_node_monthly(
					stale.node_id, stale.service, stale.ts_start, (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz)
				ON CONFLICT (node_id, service, ts_start) DO UPDATE
				SET cnt = EXCLUDED.cnt, processed = EXCLUDED.processed;
		END CASE;

		CASE kind
			WHEN 'M' THEN
				-- in case node tz changed, remove record(s) from other zone
				-- monthly records clean 1 month on either side
				DELETE FROM solardatm.aud_node_monthly a
				WHERE a.node_id = stale.node_id
					AND a.service = stale.service
					AND a.ts_start > (stale.ts_start AT TIME ZONE tz - interval '1 month') AT TIME ZONE tz
					AND a.ts_start < (stale.ts_start AT TIME ZONE tz + interval '1 month') AT TIME ZONE tz
					AND a.ts_start <> stale.ts_start;
			ELSE
				-- in case node tz changed, remove record(s) from other zone
				-- daily records clean 1 day on either side
				DELETE FROM solardatm.aud_node_daily
				WHERE node_id = stale.node_id
					AND a.service = stale.service
					AND ts_start > stale.ts_start - interval '1 day'
					AND ts_start < stale.ts_start + interval '1 day'
					AND ts_start <> stale.ts_start;

				-- recalculate monthly audit based on updated daily values
				INSERT INTO solardatm.aud_stale_datm (node_id, service, ts_start, aud_kind)
				VALUES (
					stale.node_id,
					stale.service,
					date_trunc('month', stale.ts_start AT TIME ZONE tz) AT TIME ZONE tz,
					'M')
				ON CONFLICT DO NOTHING;
		END CASE;

		-- remove processed stale record
		DELETE FROM solardatm.aud_stale_datm WHERE CURRENT OF curs;
		result_cnt := 1;
	END IF;
	CLOSE curs;
	RETURN result_cnt;
END;
$$;
