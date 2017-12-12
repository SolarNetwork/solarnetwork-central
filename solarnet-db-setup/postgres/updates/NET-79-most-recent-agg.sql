CREATE OR REPLACE FUNCTION solaragg.find_most_recent_hourly(
	node solarcommon.node_id,
	sources solarcommon.source_ids DEFAULT NULL)
  RETURNS SETOF solaragg.agg_datum_hourly AS
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
		SELECT d.* FROM solaragg.agg_datum_hourly d
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
		SELECT d.* FROM solaragg.agg_datum_hourly d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;

CREATE OR REPLACE FUNCTION solaragg.find_most_recent_daily(
	node solarcommon.node_id,
	sources solarcommon.source_ids DEFAULT NULL)
  RETURNS SETOF solaragg.agg_datum_daily AS
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
		SELECT d.* FROM solaragg.agg_datum_daily d
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
		SELECT d.* FROM solaragg.agg_datum_daily d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;

CREATE OR REPLACE FUNCTION solaragg.find_most_recent_monthly(
	node solarcommon.node_id,
	sources solarcommon.source_ids DEFAULT NULL)
  RETURNS SETOF solaragg.agg_datum_monthly AS
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
		SELECT d.* FROM solaragg.agg_datum_monthly d
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
		SELECT d.* FROM solaragg.agg_datum_monthly d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;
