/**
 * Delete datum rows matching a set of nodes, sources, and a local date range.
 *
 * The time zones of each node are used to calculate absolute date ranges for each node.
 *
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date, or the current time if not provided
 * @param ts_max the ending local date, or the current time if not provided
 */
CREATE OR REPLACE FUNCTION solardatm.delete_datm(
	sid			UUID,
	ts_min 		TIMESTAMP,
	ts_max 		TIMESTAMP,
	tz			TEXT
) RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS $$
DECLARE
	start_date timestamp := COALESCE(ts_min, CURRENT_TIMESTAMP);
	end_date timestamp := COALESCE(ts_max, CURRENT_TIMESTAMP);
	total_count bigint := 0;
BEGIN
	WITH audit AS (
		UPDATE solardatm.aud_datm_daily d
		SET datum_count = 0, datum_daily_pres = FALSE
		WHERE d.stream_id = sid
			-- whole days only; partial to be handled by stale processing
			AND d.ts_start >= CASE
				WHEN date_trunc('day', ts_min) = ts_min THEN ts_min AT TIME ZONE tz
				ELSE date_trunc('day', ts_min) AT TIME ZONE tz + interval '1 day'
				END
			AND d.ts_start < date_trunc('day', ts_max) AT TIME ZONE tz
	)
	, hourly AS (
		DELETE FROM solardatm.agg_datm_hourly d
		WHERE d.stream_id = sid
			-- whole hours (ceil) only; partial to be handled by stale processing
			AND d.ts_start >= date_trunc('hour', ts_min) AT TIME ZONE tz + interval '1 hour'
			AND d.ts_start < date_trunc('hour', ts_max) AT TIME ZONE tz
	)
	, daily AS (
		DELETE FROM solardatm.agg_datm_daily d
		WHERE d.stream_id = sid
			-- whole days only; partial to be handled by stale processing
			AND d.ts_start >= CASE
				WHEN date_trunc('day', ts_min) = ts_min THEN ts_min AT TIME ZONE tz
				ELSE date_trunc('day', ts_min) AT TIME ZONE tz + interval '1 day'
				END
			AND d.ts_start < date_trunc('day', ts_max) AT TIME ZONE tz
	)
	, monthly AS (
		DELETE FROM solardatm.agg_datm_monthly d
		WHERE d.stream_id = sid
			-- whole months only; partial to be handled by stale processing
			AND d.ts_start >= CASE
				WHEN date_trunc('month', ts_min) = ts_min THEN ts_min AT TIME ZONE tz
				ELSE date_trunc('month', ts_min) AT TIME ZONE tz + interval '1 month'
				END
			AND d.ts_start < date_trunc('month', ts_max)
	)
	DELETE FROM solardatm.da_datm d
	WHERE d.stream_id = sid
		AND d.ts >= ts_min AT TIME ZONE tz
		AND d.ts < ts_max AT TIME ZONE tz;
	GET DIAGNOSTICS total_count = ROW_COUNT;

	-- mark remaining hourly aggregates as stale, so partial hours/days/months recalculated
	WITH dates AS (
		SELECT sid AS stream_id, ts_min AT TIME ZONE tz AS ts_start
		UNION ALL
		SELECT sid AS stream_id, ts_max AT TIME ZONE tz AS ts_start
	)
	INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
	SELECT s.stream_id, s.ts_start, 'h' AS agg_kind
	FROM dates, solardatm.calc_stale_datm(dates.stream_id, dates.ts_start) s
	ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;

	--GET DIAGNOSTICS stale_count = ROW_COUNT;
	--RAISE NOTICE 'INSERTED % solaragg.agg_stale_datum rows after delete.', stale_count;

	RETURN total_count;
END
$$;
