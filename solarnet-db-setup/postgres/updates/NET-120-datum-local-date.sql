CREATE OR REPLACE VIEW solardatum.da_datum_data AS
	SELECT
		d.ts,
		d.node_id,
		d.source_id,
		d.posted,
		solardatum.jdata_from_datum(d.*) AS jdata,
		d.ts AT TIME ZONE COALESCE(l.time_zone, 'UTC') AS local_date
	FROM solardatum.da_datum d
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = d.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id;

DROP FUNCTION solaragg.find_most_recent_hourly(bigint, text[]);
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_hourly(
	node bigint,
	sources text[] DEFAULT NULL)
  RETURNS SETOF solaragg.agg_datum_hourly_data AS
$BODY$
BEGIN
	IF sources IS NULL OR array_length(sources, 1) < 1 THEN
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_hourly d
			INNER JOIN (SELECT solardatum.find_available_sources(node) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_hourly_data d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	ELSE
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_hourly d
			INNER JOIN (SELECT unnest(sources) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_hourly_data d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;

DROP FUNCTION solaragg.find_most_recent_daily(bigint, text[]);
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_daily(
	node bigint,
	sources text[] DEFAULT NULL)
  RETURNS SETOF solaragg.agg_datum_daily_data AS
$BODY$
BEGIN
	IF sources IS NULL OR array_length(sources, 1) < 1 THEN
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_daily d
			INNER JOIN (SELECT solardatum.find_available_sources(node) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_daily_data d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	ELSE
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_daily d
			INNER JOIN (SELECT unnest(sources) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_daily_data d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;

DROP FUNCTION solaragg.find_most_recent_monthly(bigint, text[]);
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_monthly(
	node bigint,
	sources text[] DEFAULT NULL)
  RETURNS SETOF solaragg.agg_datum_monthly_data AS
$BODY$
BEGIN
	IF sources IS NULL OR array_length(sources, 1) < 1 THEN
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_monthly d
			INNER JOIN (SELECT solardatum.find_available_sources(node) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_monthly_data d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	ELSE
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_monthly d
			INNER JOIN (SELECT unnest(sources) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_monthly_data d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;
