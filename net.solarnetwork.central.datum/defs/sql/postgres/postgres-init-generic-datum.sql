CREATE SCHEMA solardatum;

CREATE SCHEMA solaragg;

CREATE TABLE solardatum.da_datum (
  ts solarcommon.ts NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  posted solarcommon.ts NOT NULL,
  jdata json NOT NULL,
  CONSTRAINT da_datum_pkey PRIMARY KEY (node_id, ts, source_id) DEFERRABLE INITIALLY IMMEDIATE
);

CLUSTER solardatum.da_datum USING da_datum_pkey;

CREATE TABLE solaragg.agg_stale_datum (
  ts_start timestamp with time zone NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  agg_kind char(1) NOT NULL,
  created timestamp NOT NULL DEFAULT now(),
  CONSTRAINT agg_stale_datum_pkey PRIMARY KEY (agg_kind, node_id, ts_start, source_id)
);

CREATE TABLE solaragg.agg_datum_hourly (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  local_time time without time zone NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  jdata json NOT NULL,
 CONSTRAINT agg_datum_hourly_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CLUSTER solaragg.agg_datum_hourly USING agg_datum_hourly_pkey;

CREATE TABLE solaragg.agg_datum_daily (
  ts_start timestamp with time zone NOT NULL,
  local_date date NOT NULL,
  node_id solarcommon.node_id NOT NULL,
  source_id solarcommon.source_id NOT NULL,
  jdata json NOT NULL,
 CONSTRAINT agg_datum_daily_pkey PRIMARY KEY (node_id, ts_start, source_id)
);

CLUSTER solaragg.agg_datum_daily USING agg_datum_daily_pkey;

CREATE OR REPLACE FUNCTION solardatum.trigger_agg_stale_datum()
  RETURNS trigger AS
$BODY$BEGIN
	CASE TG_OP
		WHEN 'INSERT', 'UPDATE' THEN
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', NEW.ts), NEW.node_id, NEW.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
			RETURN NEW;
		ELSE
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', OLD.ts), OLD.node_id, OLD.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
			RETURN OLD;
	END CASE;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

-- NOTE the trigger name has aa_ prefix so sorts before pg_partman trigger name
CREATE TRIGGER aa_agg_stale_datum
  BEFORE INSERT OR UPDATE OR DELETE
  ON solardatum.da_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.trigger_agg_stale_datum();

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

