CREATE OR REPLACE FUNCTION solaragg.find_available_sources(nodes bigint[])
	RETURNS TABLE(source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT DISTINCT CAST(d.source_id AS text)
	FROM solaragg.agg_datum_daily d
	WHERE d.node_id = ANY(nodes)
	ORDER BY source_id
$$;

CREATE OR REPLACE FUNCTION solaragg.find_available_sources_since(nodes bigint[], sdate timestamp with time zone)
	RETURNS TABLE(source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT DISTINCT CAST(d.source_id AS text)
	FROM solaragg.agg_datum_daily d
	WHERE d.node_id = ANY(nodes)
		AND d.ts_start >= sdate
	ORDER BY source_id
$$;

CREATE OR REPLACE FUNCTION solaragg.find_available_sources_before(nodes bigint[], edate timestamp with time zone)
	RETURNS TABLE(source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT DISTINCT CAST(d.source_id AS text)
	FROM solaragg.agg_datum_daily d
	WHERE d.node_id = ANY(nodes)
		AND d.ts_start < edate
	ORDER BY source_id
$$;

CREATE OR REPLACE FUNCTION solaragg.find_available_sources(
		nodes bigint[],
		sdate timestamp with time zone,
		edate timestamp with time zone)
	RETURNS TABLE(source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT DISTINCT CAST(d.source_id AS text)
	FROM solaragg.agg_datum_daily d
	WHERE d.node_id = ANY(nodes)
		AND d.ts_start >= sdate
		AND d.ts_start < edate
	ORDER BY source_id
$$;
