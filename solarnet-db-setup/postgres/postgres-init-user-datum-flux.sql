/**
 * Resolve SolarFlux publish settings for a user node and source.
 *
 * @param userid the ID of the user
 * @param node the ID of the node
 * @param source the ID of the source
 */
CREATE OR REPLACE FUNCTION solaruser.flux_agg_pub_settings(
		userid		BIGINT,
		node 		BIGINT,
		source 		CHARACTER VARYING(64)
	)
	RETURNS SETOF solardatm.flux_pub_settings
	LANGUAGE SQL STABLE STRICT ROWS 1 AS
$$
WITH matches AS (
	SELECT -1 AS weight
		, 0 AS id
		, def.publish
		, def.retain
	FROM solaruser.user_flux_default_agg_pub_settings def
	WHERE def.user_id = userid

	UNION ALL

	SELECT CASE
			WHEN fap.node_ids IS NULL OR fap.source_ids IS NULL THEN 0
			ELSE 1
		  END AS weight
		, fap.id
		, fap.publish
		, fap.retain
	FROM solaruser.user_node un
	INNER JOIN solaruser.user_flux_agg_pub_settings fap ON un.user_id = fap.user_id
	WHERE un.user_id = userid
		AND un.node_id = node
		AND (fap.node_ids IS NULL OR fap.node_ids @> ARRAY[node])
		AND (fap.source_ids IS NULL OR fap.source_ids @> ARRAY[source])
)
SELECT node
	, source
	, COALESCE(solarcommon.first(publish ORDER BY weight DESC, id DESC), FALSE) AS publish
	, COALESCE(solarcommon.first(retain ORDER BY weight DESC, id DESC), FALSE) AS retain
FROM matches
$$;


/**
 * Resolve SolarFlux publish settings for a user node and source.
 *
 * @param node the ID of the node
 * @param source the ID of the source
 */
CREATE OR REPLACE FUNCTION solardatm.flux_agg_pub_settings(
		node 		BIGINT,
		source 		CHARACTER VARYING(64)
	)
	RETURNS SETOF solardatm.flux_pub_settings
	LANGUAGE SQL STABLE STRICT ROWS 1 AS
$$
WITH usernode AS (
	SELECT user_id, node_id
	FROM solaruser.user_node
	WHERE node_id = node
)
SELECT s.*
FROM usernode, solaruser.flux_agg_pub_settings(usernode.user_id, usernode.node_id, source) AS s
$$;
