/* ============================================================================
 * NET-541: look for the datum before and after a changed datum in only the chunks that can hold
 * them.
 *
 * `solardatm.calc_stale_datm()` finds the hours to mark stale when a datum is stored or deleted.
 * It looks up the datum before and the datum after the one that changed, each within a tolerance
 * of 3 months. Both lookups were ordered by `stream_id` and then `ts`. With that ordering
 * Timescale plans each lookup as a merge of an index scan on every `da_datm` chunk, and leaves
 * out the chunks outside the tolerance only when the query is planned with the datum's time as a
 * constant. The statements that call this function are in PL/pgSQL functions, whose plans are
 * cached and reused with the time as a parameter. So every datum stored scanned the index of
 * every chunk twice: about 550 buffers for 72 chunks, where the two lookups need about 10. That
 * was a third of all buffer hits on the primary database, and most of the time taken to store a
 * datum.
 *
 * The lookups are now ordered by `ts` alone. The stream is fixed by the `WHERE` clause, so the
 * rows returned are the same, and Timescale then scans the chunks in time order and leaves out
 * the ones outside the tolerance when the time is a parameter, and when it is a column of another
 * table, as in `solardatm.delete_datm()`.
 *
 * The function signature is unchanged, so `CREATE OR REPLACE` keeps its existing owner and ACLs.
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
 * The lookups of the datum before and after are ordered by `ts` alone, not by `stream_id` and
 * then `ts`: the stream is fixed, so the result is the same, and only then does Timescale scan
 * the chunks within the tolerance instead of every chunk (see NET-541).
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
		-- by time alone, so that only the chunks within the tolerance are scanned (see NET-541)
		ORDER BY d.ts DESC
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
			-- by time alone, so that only the chunks within the tolerance are scanned (see NET-541)
			ORDER BY d.ts
			LIMIT 1
		)
	)
	SELECT DISTINCT ON (ts) sid, ts FROM b
$$;
