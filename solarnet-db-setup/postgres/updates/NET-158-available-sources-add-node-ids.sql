DROP FUNCTION IF EXISTS solaragg.find_available_sources(bigint[]);
CREATE OR REPLACE FUNCTION solaragg.find_available_sources(nodes bigint[])
	RETURNS TABLE(node_id bigint, source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT DISTINCT d.node_id, CAST(d.source_id AS text)
	FROM solaragg.agg_datum_daily d
	WHERE d.node_id = ANY(nodes)
	ORDER BY source_id, node_id
$$;

DROP FUNCTION IF EXISTS solaragg.find_available_sources_since(bigint[], timestamp with time zone);
CREATE OR REPLACE FUNCTION solaragg.find_available_sources_since(nodes bigint[], sdate timestamp with time zone)
	RETURNS TABLE(node_id bigint, source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT DISTINCT d.node_id, CAST(d.source_id AS text)
	FROM solaragg.agg_datum_daily d
	WHERE d.node_id = ANY(nodes)
		AND d.ts_start >= sdate
	ORDER BY source_id, node_id
$$;

DROP FUNCTION IF EXISTS solaragg.find_available_sources_before(bigint[], timestamp with time zone);
CREATE OR REPLACE FUNCTION solaragg.find_available_sources_before(nodes bigint[], edate timestamp with time zone)
	RETURNS TABLE(node_id bigint, source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT DISTINCT d.node_id, CAST(d.source_id AS text)
	FROM solaragg.agg_datum_daily d
	WHERE d.node_id = ANY(nodes)
		AND d.ts_start < edate
	ORDER BY source_id, node_id
$$;

DROP FUNCTION IF EXISTS solaragg.find_available_sources(bigint[], timestamp with time zone, timestamp with time zone);
CREATE OR REPLACE FUNCTION solaragg.find_available_sources(
		nodes bigint[],
		sdate timestamp with time zone,
		edate timestamp with time zone)
	RETURNS TABLE(node_id bigint, source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT DISTINCT d.node_id, CAST(d.source_id AS text)
	FROM solaragg.agg_datum_daily d
	WHERE d.node_id = ANY(nodes)
		AND d.ts_start >= sdate
		AND d.ts_start < edate
	ORDER BY source_id, node_id
$$;
