CREATE SCHEMA IF NOT EXISTS solardatum;

CREATE SCHEMA IF NOT EXISTS solaragg;

CREATE TABLE solardatum.da_datum (
  ts solarcommon.ts NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  posted solarcommon.ts NOT NULL,
  jdata json NOT NULL,
  CONSTRAINT da_datum_pkey PRIMARY KEY (node_id, ts, source_id) DEFERRABLE INITIALLY IMMEDIATE
);

CREATE TABLE solardatum.da_meta (
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  created solarcommon.ts NOT NULL,
  updated solarcommon.ts NOT NULL,
  jdata json NOT NULL,
  CONSTRAINT da_meta_pkey PRIMARY KEY (node_id, source_id)
);

CREATE TABLE solaragg.agg_stale_datum (
  ts_start timestamp with time zone NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  agg_kind char(1) NOT NULL,
  created timestamp NOT NULL DEFAULT now(),
  CONSTRAINT agg_stale_datum_pkey PRIMARY KEY (agg_kind, node_id, ts_start, source_id)
);

CREATE TABLE solaragg.agg_messages (
  created timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  ts solarcommon.ts NOT NULL,
  msg text NOT NULL
);

CREATE INDEX agg_messages_ts_node_idx ON solaragg.agg_messages (ts, node_id);

CREATE TABLE solaragg.agg_datum_hourly (
  ts_start timestamp with time zone NOT NULL,
  local_date timestamp without time zone NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  jdata json NOT NULL,
 CONSTRAINT agg_datum_hourly_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CREATE TABLE solaragg.agg_datum_daily (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  jdata json NOT NULL,
 CONSTRAINT agg_datum_daily_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CREATE TABLE solaragg.agg_datum_monthly (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  jdata json NOT NULL,
 CONSTRAINT agg_datum_monthly_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CREATE VIEW solaragg.da_datum_avail_hourly AS
WITH nodetz AS (
	SELECT n.node_id, COALESCE(l.time_zone, 'UTC') AS tz
	FROM solarnet.sn_node n
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
)
SELECT date_trunc('hour', d.ts at time zone nodetz.tz) at time zone nodetz.tz AS ts_start, d.node_id, d.source_id
FROM solardatum.da_datum d
INNER JOIN nodetz ON nodetz.node_id = d.node_id
GROUP BY date_trunc('hour', d.ts at time zone nodetz.tz) at time zone nodetz.tz, d.node_id, d.source_id;

CREATE VIEW solaragg.da_datum_avail_daily AS
WITH nodetz AS (
	SELECT n.node_id, COALESCE(l.time_zone, 'UTC') AS tz
	FROM solarnet.sn_node n
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
)
SELECT date_trunc('day', d.ts at time zone nodetz.tz) at time zone nodetz.tz AS ts_start, d.node_id, d.source_id
FROM solardatum.da_datum d
INNER JOIN nodetz ON nodetz.node_id = d.node_id
GROUP BY date_trunc('day', d.ts at time zone nodetz.tz) at time zone nodetz.tz, d.node_id, d.source_id;

CREATE VIEW solaragg.da_datum_avail_monthly AS
WITH nodetz AS (
	SELECT n.node_id, COALESCE(l.time_zone, 'UTC') AS tz
	FROM solarnet.sn_node n
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
)
SELECT date_trunc('month', d.ts at time zone nodetz.tz) at time zone nodetz.tz AS ts_start, d.node_id, d.source_id
FROM solardatum.da_datum d
INNER JOIN nodetz ON nodetz.node_id = d.node_id
GROUP BY date_trunc('month', d.ts at time zone nodetz.tz) at time zone nodetz.tz, d.node_id, d.source_id;

CREATE OR REPLACE FUNCTION solardatum.store_datum(
	cdate solarcommon.ts, 
	node solarcommon.node_id, 
	src solarcommon.source_id, 
	pdate solarcommon.ts, 
	jdata text)
  RETURNS void AS
$BODY$
DECLARE
	ts_post solarcommon.ts := (CASE WHEN pdate IS NULL THEN now() ELSE pdate END);
	jdata_json json := jdata::json;
BEGIN
	BEGIN
		INSERT INTO solardatum.da_datum(ts, node_id, source_id, posted, jdata)
		VALUES (cdate, node, src, ts_post, jdata_json);
	EXCEPTION WHEN unique_violation THEN
		-- We mostly expect inserts, but we allow updates
		UPDATE solardatum.da_datum SET 
			jdata = jdata_json, 
			posted = ts_post
		WHERE
			node_id = node
			AND ts = cdate
			AND source_id = src;
	END;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solardatum.store_meta(
	cdate solarcommon.ts, 
	node solarcommon.node_id, 
	src solarcommon.source_id, 
	jdata text)
  RETURNS void AS
$BODY$
DECLARE
	udate solarcommon.ts := now();
	jdata_json json := jdata::json;
BEGIN
	BEGIN
		INSERT INTO solardatum.da_meta(node_id, source_id, created, updated, jdata)
		VALUES (node, src, cdate, udate, jdata_json);
	EXCEPTION WHEN unique_violation THEN
		-- We mostly expect inserts, but we allow updates
		UPDATE solardatum.da_meta SET 
			jdata = jdata_json, 
			updated = udate
		WHERE
			node_id = node
			AND source_id = src;
	END;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solardatum.find_available_sources(
	IN node solarcommon.node_id, 
	IN st solarcommon.ts DEFAULT NULL, 
	IN en solarcommon.ts DEFAULT NULL)
  RETURNS TABLE(source_id solarcommon.source_id) AS
$BODY$
DECLARE
	node_tz text;
BEGIN
	IF st IS NOT NULL OR en IS NOT NULL THEN
		-- get the node TZ for local date/time
		SELECT l.time_zone  FROM solarnet.sn_node n
		INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE n.node_id = node
		INTO node_tz;

		IF NOT FOUND THEN
			RAISE NOTICE 'Node % has no time zone, will use UTC.', node;
			node_tz := 'UTC';
		END IF;
	END IF;
	
	CASE
		WHEN st IS NULL AND en IS NULL THEN
			RETURN QUERY SELECT DISTINCT d.source_id
			FROM solaragg.agg_datum_daily d
			WHERE d.node_id = node;
		
		WHEN st IS NULL THEN
			RETURN QUERY SELECT DISTINCT d.source_id
			FROM solaragg.agg_datum_daily d
			WHERE d.node_id = node
				AND d.ts_start >= CAST(st at time zone node_tz AS DATE);
				
		ELSE
			RETURN QUERY SELECT DISTINCT d.source_id
			FROM solaragg.agg_datum_daily d
			WHERE d.node_id = node
				AND d.ts_start >= CAST(st at time zone node_tz AS DATE)
				AND d.ts_start <= CAST(en at time zone node_tz AS DATE);
	END CASE;	
END;$BODY$
  LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION solardatum.find_reportable_interval(
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
			SELECT min(ts) FROM solardatum.da_datum WHERE node_id = node
			INTO ts_start;
		ELSE
			SELECT min(ts) FROM solardatum.da_datum WHERE node_id = node AND source_id = src
			INTO ts_start;
	END CASE;
	
	CASE
		WHEN src IS NULL THEN
			SELECT max(ts) FROM solardatum.da_datum WHERE node_id = node
			INTO ts_end;
		ELSE
			SELECT max(ts) FROM solardatum.da_datum WHERE node_id = node AND source_id = src
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

CREATE OR REPLACE FUNCTION solardatum.find_most_recent(
	node solarcommon.node_id, 
	sources solarcommon.source_ids DEFAULT NULL)
  RETURNS SETOF solardatum.da_datum AS
$BODY$
BEGIN
	IF sources IS NULL OR array_length(sources, 1) < 1 THEN
		RETURN QUERY
		SELECT dd.* FROM solardatum.da_datum dd
		INNER JOIN (
			-- to speed up query for sources (which can be very slow when queried directly on da_datum), 
			-- we find the most recent hour time slot in agg_datum_hourly, and then join to da_datum with that narrow time range
			WITH days AS (
				SELECT max(d.ts_start) as ts_start, d.source_id FROM solaragg.agg_datum_hourly d 
				INNER JOIN (SELECT solardatum.find_available_sources(node) AS source_id) AS s ON s.source_id = d.source_id
				WHERE d. node_id = node
				GROUP BY d.source_id
			)
			SELECT max(d.ts) as ts, d.source_id FROM solardatum.da_datum d 
			INNER JOIN days ON days.source_id = d.source_id 
			WHERE d.node_id = node
				AND d.ts >= days.ts_start
				AND d.ts < days.ts_start + interval '1 hour'
			GROUP BY d.source_id
		) AS r ON r.ts = dd.ts AND r.source_id = dd.source_id AND dd.node_id = node
		ORDER BY dd.source_id ASC;
	ELSE
		RETURN QUERY
		SELECT dd.* FROM solardatum.da_datum dd
		INNER JOIN (
			WITH days AS (
				SELECT max(d.ts_start) as ts_start, d.source_id FROM solaragg.agg_datum_hourly d 
				INNER JOIN (SELECT unnest(sources) AS source_id) AS s ON s.source_id = d.source_id
				WHERE d. node_id = node
				GROUP BY d.source_id
			)
			SELECT max(d.ts) as ts, d.source_id FROM solardatum.da_datum d 
			INNER JOIN days ON days.source_id = d.source_id 
			WHERE d.node_id = node
				AND d.ts >= days.ts_start
				AND d.ts < days.ts_start + interval '1 hour'
			GROUP BY d.source_id
		) AS r ON r.ts = dd.ts AND r.source_id = dd.source_id AND dd.node_id = node
		ORDER BY dd.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;

/**
 * Return most recent datum records for all available sources for a given set of node IDs.
 * 
 * @param nodes An array of node IDs to return results for.
 * @returns Set of solardatum.da_datum records.
 */
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(nodes solarcommon.node_ids)
  RETURNS SETOF solardatum.da_datum AS
$BODY$
	SELECT r.* 
	FROM (SELECT unnest(nodes) AS node_id) AS n,
	LATERAL (SELECT * FROM solardatum.find_most_recent(n.node_id)) AS r
	ORDER BY r.node_id, r.source_id;
$BODY$
  LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION solardatum.populate_updated()
  RETURNS "trigger" AS
$BODY$
BEGIN
	NEW.updated := now();
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_updated
  BEFORE INSERT OR UPDATE
  ON solardatum.da_meta
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.populate_updated();
