/* Found that the tsvector_update_trigger() function in 9.6 is throwing an error when passing in a character(2)
 * column, such as sn_loc.country. Changing to character varying(2) works around the issue.
 */
ALTER TABLE solarnet.sn_loc ALTER COLUMN country TYPE character varying(2);

CREATE OR REPLACE FUNCTION solardatum.find_most_recent(
	node solarcommon.node_id,
	sources solarcommon.source_ids DEFAULT NULL)
  RETURNS SETOF solardatum.da_datum AS
$BODY$
	SELECT dd.* FROM solardatum.da_datum dd
	INNER JOIN (
		-- to speed up query for sources (which can be very slow when queried directly on da_datum),
		-- we find the most recent hour time slot in agg_datum_hourly, and then join to da_datum with that narrow time range
		SELECT max(d.ts) as ts, d.source_id FROM solardatum.da_datum d
		INNER JOIN (SELECT node_id, ts_start, source_id FROM solaragg.find_most_recent_hourly(node, sources)) AS days
			ON days.node_id = d.node_id
				AND days.ts_start <= d.ts
				AND days.ts_start + interval '1 hour' > d.ts
				AND days.source_id = d.source_id
		GROUP BY d.source_id
	) AS r ON r.ts = dd.ts AND r.source_id = dd.source_id AND dd.node_id = node
	ORDER BY dd.source_id ASC;
$BODY$
  LANGUAGE sql STABLE
  ROWS 20;
