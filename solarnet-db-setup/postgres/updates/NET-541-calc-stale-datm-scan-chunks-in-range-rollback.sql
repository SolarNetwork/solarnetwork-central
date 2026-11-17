/* ============================================================================
 * NET-541 rollback: restore `solardatm.calc_stale_datm()` as it was before
 * NET-541-calc-stale-datm-scan-chunks-in-range.sql, with its two lookups ordered by `stream_id`
 * and then `ts`, which scans the index of every `da_datm` chunk when the plan is cached.
 *
 * This is the function as the setup DDL in postgres-init-datm-in.sql defined it before NET-541,
 * copied unchanged.
 *
 * The function signature is unchanged, so `CREATE OR REPLACE` keeps its existing owner and ACLs.
 * The function returns the same rows either way, so nothing computed in between needs redoing.
 * ============================================================================
 */

/**
 * Calculate "stale datum" rows for a given datum primary key that has changed.
 *
 * This function will return 1-4 rows representing stale rows that must be re-calculated.
 * It is designed so that the results can be inserted into `solardatm.agg_stale_datm`, like:
 *
 * 	INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
 * 	SELECT stream_id, ts_start, 'h' AS agg_kind
 * 	FROM solardatm.calc_stale_datm('11111'::uuid,'2020-10-07 08:00:40+13'::timestamptz)
 * 	ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;
 *
 * @param sid 				the stream ID of the datum that has been changed (inserted, deleted)
 * @param ts_in				the date of the datum that has changed
 * @param tolerance 		the maximum time to look forward/backward for adjacent datm
 */
CREATE OR REPLACE FUNCTION solardatm.calc_stale_datm(
		sid 		UUID,
		ts_in 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT interval '3 months'
	) RETURNS TABLE (
		stream_id	UUID,
		ts_start 	TIMESTAMP WITH TIME ZONE
	) LANGUAGE SQL STABLE ROWS 4 AS
$$
	WITH prev_datum_hour AS (
		SELECT date_trunc('hour', d.ts) AS ts
		FROM solardatm.da_datm d
		WHERE d.stream_id = sid
			AND d.ts < ts_in
			AND d.ts > ts_in - tolerance
		ORDER BY d.stream_id, d.ts DESC
		LIMIT 1
	)
	, b AS (
		-- prev datum hour
		(
			SELECT ts FROM prev_datum_hour
		)
		-- curr hour
		UNION ALL
		(
			SELECT date_trunc('hour', ts_in) AS ts
		)
		-- prev hour, if datum exactly on hour and prev datum exists
		UNION ALL
		(
			SELECT date_trunc('hour', ts_in) - INTERVAL 'PT1H' AS ts
			FROM prev_datum_hour
			WHERE date_trunc('hour', ts_in) = ts_in
		)
		UNION ALL
		-- next datum hour, or next datum hour - 1 if next datum exactly on hour
		(
			SELECT CASE WHEN date_trunc('hour', d.ts) = d.ts
				THEN d.ts - INTERVAL 'PT1H'
				ELSE date_trunc('hour', d.ts)
				END AS ts
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts > ts_in
				AND d.ts < ts_in + tolerance
			ORDER BY d.stream_id, d.ts
			LIMIT 1
		)
	)
	SELECT DISTINCT ON (ts) sid, ts FROM b
$$;
