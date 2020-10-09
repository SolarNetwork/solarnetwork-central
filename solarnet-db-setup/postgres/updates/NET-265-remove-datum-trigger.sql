DROP TRIGGER IF EXISTS aa_agg_stale_datum ON solardatum.da_datum;
DROP FUNCTION IF EXISTS solardatum.trigger_agg_stale_datum;

/**
 * Calculate "stale datum" rows for a given datum primary key that has changed.
 *
 * This function will return 1-3 rows representing stale rows that must be re-calculated.
 * It is designed so that the results can be inserted into `solaragg.agg_stale_datum`, like:
 *
 * 	INSERT INTO solaragg.agg_stale_datum (agg_kind, node_id, ts_start, source_id)
 * 	SELECT 'h' AS agg_kind, node_id, ts_start, source_id
 * 	FROM solardatum.calculate_stale_datum(179,'Mock Energy Meter','2020-10-07 08:00:40+13'::timestamptz)
 * 	ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING;
 *
 * @param node 		the node ID of the datum that has been changed (inserted, deleted)
 * @param source 	the source ID of the datum that has been changed
 * @param ts_in		the date of the datum that has changed
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_stale_datum(
		node 		BIGINT,
		source 		CHARACTER VARYING(64),
		ts_in 		TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		node_id		BIGINT,
		source_id 	CHARACTER VARYING(64),
		ts_start 	TIMESTAMP WITH TIME ZONE
	) LANGUAGE PLPGSQL STABLE ROWS 3 AS
$$
DECLARE
	orig_slot	TIMESTAMP WITH TIME ZONE 	:= date_trunc('hour', ts_in);
	dur_back 	INTERVAL 					:= interval '1 hour';
	dur_fwd  	INTERVAL 					:= interval '3 months';
BEGIN
	node_id 	:= node;
	source_id 	:= source;
	ts_start 	:= orig_slot;
	RETURN NEXT;

	-- prev slot; if the previous record for this source falls on the previous hour, we have to mark that hour as stale as well

	SELECT d.node_id, d.source_id, date_trunc('hour', d.ts) AS ts_start
		FROM solardatum.da_datum d
		WHERE d.ts < ts_in
			AND d.ts > ts_in - dur_back
			AND d.node_id = node
			AND d.source_id = source
		ORDER BY d.ts DESC
		LIMIT 1
		INTO node_id, source_id, ts_start;

	IF FOUND AND ts_start < orig_slot THEN
		RETURN NEXT;
	END IF;

	-- next slot; if there is another record in a future hour, we have to mark that hour as stale as well

	SELECT d.node_id, d.source_id, date_trunc('hour', d.ts) AS ts_start
		FROM solardatum.da_datum d
		WHERE d.ts > ts_in
			AND d.ts < ts_in + dur_fwd
			AND d.node_id = node
			AND d.source_id = source
		ORDER BY d.ts ASC
		LIMIT 1
		INTO node_id, source_id, ts_start;

	IF FOUND AND ts_start > orig_slot THEN
		RETURN NEXT;
	END IF;

	RETURN;
END;
$$;

/**
 * Calculate a date range for stale datum given a node, source, and lower and upper dates.
 *
 * Note that the returned `ts_max` represents an **exclusive** date value, suitable for passing
 * to the `solaragg.mark_datum_stale_hour_slots` function.
 *
 * @param node 		the node ID of the datum that has been changed (inserted, deleted)
 * @param source 	the source ID of the datum that has been changed
 * @param ts_lower	the lower date of the datum that has changed
 * @param ts_upper	the upper date of the datum that has changed
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_stale_datum_range(
		node 		BIGINT,
		source 		CHARACTER VARYING(64),
		ts_lower 	TIMESTAMP WITH TIME ZONE,
		ts_upper 	TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		ts_min 		TIMESTAMP WITH TIME ZONE,
		ts_max		TIMESTAMP WITH TIME ZONE
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	WITH d AS (
		SELECT ts_start
		FROM solardatum.calculate_stale_datum(node, source, ts_lower)
		UNION ALL
		SELECT ts_start
		FROM solardatum.calculate_stale_datum(node, source, ts_upper)
	)
	SELECT min(ts_start) AS ts_min, max(ts_start) + interval '1 hour' AS ts_max
	FROM d
$$;

/**
 * Find hours with datum data in them based on a node, source, and date range; mark them as "stale"
 * for aggregate processing.
 *
 * This function will insert into the `solaragg.agg_stale_datum` table records for all hours
 * of available data matching the given criteria.
 *
 * @param node 		the node ID of the datum that has been changed (inserted, deleted)
 * @param source 	the source ID of the datum that has been changed
 * @param ts_lower	the lower date of the datum that has changed
 * @param ts_upper	the upper date of the datum that has changed
 */
CREATE OR REPLACE FUNCTION solaragg.mark_datum_stale_hour_slots_range(
		node 		BIGINT,
		source 		CHARACTER VARYING(64),
		ts_lower 	TIMESTAMP WITH TIME ZONE,
		ts_upper 	TIMESTAMP WITH TIME ZONE
	) RETURNS VOID LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
	SELECT ts_start, node, source, 'h'
	FROM solardatum.calculate_stale_datum(node, source, ts_lower)
	UNION
	SELECT ts_start, node, source, 'h'
	FROM solardatum.calculate_stale_datum(node, source, ts_upper)
	ON CONFLICT DO NOTHING
$$;

/**
 * Add or update a datum record. The data is stored in the `solardatum.da_datum` table.
 *
 * @param cdate The datum creation date.
 * @param node The node ID.
 * @param src The source ID.
 * @param pdate The date the datum was posted to SolarNet.
 * @param jdata The datum JSON document.
 * @param track_recent if `TRUE` then also insert results of `solardatum.calculate_stale_datum()`
 *                     into the `solaragg.agg_stale_datum` table and call
 *                     `solardatum.update_datum_range_dates()` to keep the
 *                     `solardatum.da_datum_range` table up-to-date
 */
CREATE OR REPLACE FUNCTION solardatum.store_datum(
	cdate 			TIMESTAMP WITH TIME ZONE,
	node 			BIGINT,
	src 			TEXT,
	pdate 			TIMESTAMP WITH TIME ZONE,
	jdata 			TEXT,
	track_recent 	BOOLEAN DEFAULT TRUE)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_crea 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(cdate, now());
	ts_post 			TIMESTAMP WITH TIME ZONE	:= COALESCE(pdate, now());
	jdata_json 			JSONB 						:= jdata::jsonb;
	jdata_prop_count 	INTEGER 					:= solardatum.datum_prop_count(jdata_json);
	ts_post_hour 		TIMESTAMP WITH TIME ZONE 	:= date_trunc('hour', ts_post);
	is_insert 			BOOLEAN 					:= false;
BEGIN
	INSERT INTO solardatum.da_datum(ts, node_id, source_id, posted, jdata_i, jdata_a, jdata_s, jdata_t)
	VALUES (ts_crea, node, src, ts_post, jdata_json->'i', jdata_json->'a', jdata_json->'s', solarcommon.json_array_to_text_array(jdata_json->'t'))
	ON CONFLICT (node_id, ts, source_id) DO UPDATE
	SET jdata_i = EXCLUDED.jdata_i,
		jdata_a = EXCLUDED.jdata_a,
		jdata_s = EXCLUDED.jdata_s,
		jdata_t = EXCLUDED.jdata_t,
		posted = EXCLUDED.posted
	RETURNING (xmax = 0)
	INTO is_insert;

	INSERT INTO solaragg.aud_datum_hourly (
		ts_start, node_id, source_id, datum_count, prop_count)
	VALUES (ts_post_hour, node, src, 1, jdata_prop_count)
	ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
	SET datum_count = aud_datum_hourly.datum_count + (CASE is_insert WHEN TRUE THEN 1 ELSE 0 END),
		prop_count = aud_datum_hourly.prop_count + EXCLUDED.prop_count;

	IF track_recent THEN
		INSERT INTO solaragg.agg_stale_datum (agg_kind, node_id, ts_start, source_id)
		SELECT 'h' AS agg_kind, node_id, ts_start, source_id
		FROM solardatum.calculate_stale_datum(node, src, cdate)
		ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING;

		IF is_insert THEN
			PERFORM solardatum.update_datum_range_dates(node, src, cdate);
		END IF;
	END IF;
END;
$$;

-- update to delete aggregate data as well
CREATE OR REPLACE FUNCTION solardatum.delete_datum(
	nodes 		BIGINT[],
	sources 	TEXT[],
	ts_min 		TIMESTAMP,
	ts_max 		TIMESTAMP
) RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS $$
DECLARE
	all_source_ids boolean := sources IS NULL OR array_length(sources, 1) < 1;
	start_date timestamp := COALESCE(ts_min, CURRENT_TIMESTAMP);
	end_date timestamp := COALESCE(ts_max, CURRENT_TIMESTAMP);
	total_count bigint := 0;
	--stale_count bigint := 0;
BEGIN
	WITH nlt AS (
		SELECT time_zone, ts_start, ts_end, node_ids, source_ids
		FROM solarnet.node_source_time_ranges_local(nodes, sources, start_date, end_date)
	)
	, audit AS (
		UPDATE solaragg.aud_datum_daily d
		SET datum_count = 0, datum_daily_pres = FALSE
		FROM nlt
		WHERE
			-- whole days only; partial to be handled by stale processing
			d.ts_start >= CASE
				WHEN date_trunc('day', nlt.ts_start) = nlt.ts_start THEN nlt.ts_start
				ELSE date_trunc('day', nlt.ts_start) + interval '1 day'
				END
			AND d.ts_start < date_trunc('day', nlt.ts_end)
			AND d.node_id = ANY(nlt.node_ids)
			AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	)
	, hourly AS (
		DELETE FROM solaragg.agg_datum_hourly d
		USING nlt
		WHERE
			-- whole hours (ceil) only; partial to be handled by stale processing
			d.ts_start >= date_trunc('hour', nlt.ts_start) + interval '1 hour'
			AND d.ts_start < date_trunc('hour', nlt.ts_end)
			AND d.node_id = ANY(nlt.node_ids)
			AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	)
	, daily AS (
		DELETE FROM solaragg.agg_datum_daily d
		USING nlt
		WHERE
			-- whole days only; partial to be handled by stale processing
			d.ts_start >= CASE
				WHEN date_trunc('day', nlt.ts_start) = nlt.ts_start THEN nlt.ts_start
				ELSE date_trunc('day', nlt.ts_start) + interval '1 day'
				END
			AND d.ts_start < date_trunc('day', nlt.ts_end)
			AND d.node_id = ANY(nlt.node_ids)
			AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	)
	, monthly AS (
		DELETE FROM solaragg.agg_datum_monthly d
		USING nlt
		WHERE
			-- whole months only; partial to be handled by stale processing
			d.ts_start >= CASE
				WHEN date_trunc('month', nlt.ts_start) = nlt.ts_start THEN nlt.ts_start
				ELSE date_trunc('month', nlt.ts_start) + interval '1 month'
				END
			AND d.ts_start < date_trunc('month', nlt.ts_end)
			AND d.node_id = ANY(nlt.node_ids)
			AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	)
	DELETE FROM solardatum.da_datum d
	USING nlt
	WHERE
		d.ts >= nlt.ts_start
		AND d.ts < nlt.ts_end
		AND d.node_id = ANY(nlt.node_ids)
		AND (all_source_ids OR d.source_id = ANY(nlt.source_ids));
	GET DIAGNOSTICS total_count = ROW_COUNT;

	-- mark remaining hourly aggregates as stale, so partial hours/days/months recalculated
	WITH nlt AS (
		SELECT nlt.node_id
			, source_id
			, start_date AT TIME ZONE nlt.time_zone AS ts_start
		FROM solarnet.node_local_time nlt, UNNEST(sources) AS source_id
		WHERE nlt.node_id = ANY(nodes)
		UNION ALL
		SELECT nlt.node_id
			, source_id
			, end_date AT TIME ZONE nlt.time_zone AS ts_start
		FROM solarnet.node_local_time nlt, UNNEST(sources) AS source_id
		WHERE nlt.node_id = ANY(nodes)
	)
	INSERT INTO solaragg.agg_stale_datum(node_id, source_id, ts_start, agg_kind)
	SELECT s.node_id, s.source_id, s.ts_start, 'h'
	FROM nlt, solardatum.calculate_stale_datum(nlt.node_id, nlt.source_id, nlt.ts_start) s
	ON CONFLICT DO NOTHING;

	--GET DIAGNOSTICS stale_count = ROW_COUNT;
	--RAISE NOTICE 'INSERTED % solaragg.agg_stale_datum rows after delete.', stale_count;

	RETURN total_count;
END
$$;

--
-- LOCATION DATUM
--


DROP TRIGGER IF EXISTS aa_agg_stale_loc_datum ON solardatum.da_loc_datum;
DROP FUNCTION IF EXISTS solardatum.trigger_agg_stale_loc_datum();

/**
 * Calculate "stale location datum" rows for a given location datum primary key that has changed.
 *
 * This function will return 1-3 rows representing stale rows that must be re-calculated.
 * It is designed so that the results can be inserted into `solaragg.agg_stale_loc_datum`, like:
 *
 * 	INSERT INTO solaragg.agg_stale_loc_datum (agg_kind, loc_id, ts_start, source_id)
 * 	SELECT 'h' AS agg_kind, loc_id, ts_start, source_id
 * 	FROM solardatum.calculate_stale_loc_datum(123,'Weather','2020-10-07 08:00:40+13'::timestamptz)
 * 	ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;
 *
 * @param loc 		the location ID of the datum that has been changed (inserted, deleted)
 * @param source 	the source ID of the datum that has been changed
 * @param ts_in		the date of the datum that has changed
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_stale_loc_datum(
		loc 		BIGINT,
		source 		CHARACTER VARYING(64),
		ts_in 		TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		loc_id		BIGINT,
		source_id 	CHARACTER VARYING(64),
		ts_start 	TIMESTAMP WITH TIME ZONE
	) LANGUAGE PLPGSQL STABLE ROWS 3 AS
$$
DECLARE
	orig_slot	TIMESTAMP WITH TIME ZONE 	:= date_trunc('hour', ts_in);
	dur_back 	INTERVAL 					:= interval '1 hour';
	dur_fwd  	INTERVAL 					:= interval '3 months';
BEGIN
	loc_id 		:= loc;
	source_id 	:= source;
	ts_start 	:= orig_slot;
	RETURN NEXT;

	-- prev slot; if the previous record for this source falls on the previous hour, we have to mark that hour as stale as well

	SELECT d.loc_id, d.source_id, date_trunc('hour', d.ts) AS ts_start
		FROM solardatum.da_loc_datum d
		WHERE d.ts < ts_in
			AND d.ts > ts_in - dur_back
			AND d.loc_id = loc
			AND d.source_id = source
		ORDER BY d.ts DESC
		LIMIT 1
		INTO loc_id, source_id, ts_start;

	IF FOUND AND ts_start < orig_slot THEN
		RETURN NEXT;
	END IF;

	-- next slot; if there is another record in a future hour, we have to mark that hour as stale as well

	SELECT d.loc_id, d.source_id, date_trunc('hour', d.ts) AS ts_start
		FROM solardatum.da_loc_datum d
		WHERE d.ts > ts_in
			AND d.ts < ts_in + dur_fwd
			AND d.loc_id = loc
			AND d.source_id = source
		ORDER BY d.ts ASC
		LIMIT 1
		INTO loc_id, source_id, ts_start;

	IF FOUND AND ts_start > orig_slot THEN
		RETURN NEXT;
	END IF;

	RETURN;
END;
$$;

/**
 * Add or update a location datum record. The data is stored in the `solardatum.da_loc_datum` table.
 *
 * This function also updates the `prop_count` column in the `solaragg.aud_loc_datum_hourly` table
 * for the appropriate row, and inserts the results of the `solardatum.calculate_stale_loc_datum()`
 * function into the `solaragg.agg_stale_loc_datum` table.
 *
 * @param cdate The datum creation date.
 * @param loc The location ID.
 * @param src The source ID.
 * @param pdate The date the datum was posted to SolarNet.
 * @param jdata The datum JSON document.
 */
CREATE OR REPLACE FUNCTION solardatum.store_loc_datum(
	cdate 	TIMESTAMP WITH TIME ZONE,
	loc 	BIGINT,
	src 	TEXT,
	pdate 	TIMESTAMP WITH TIME ZONE,
	jdata 	TEXT)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ts_crea 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(cdate, now());
	ts_post 			TIMESTAMP WITH TIME ZONE 	:= COALESCE(pdate, now());
	jdata_json 			JSONB 						:= jdata::jsonb;
	jdata_prop_count 	INTEGER 					:= solardatum.datum_prop_count(jdata_json);
	ts_post_hour 		TIMESTAMP WITH TIME ZONE 	:= date_trunc('hour', ts_post);
BEGIN
	INSERT INTO solardatum.da_loc_datum(ts, loc_id, source_id, posted, jdata_i, jdata_a, jdata_s, jdata_t)
	VALUES (ts_crea, loc, src, ts_post, jdata_json->'i', jdata_json->'a', jdata_json->'s', solarcommon.json_array_to_text_array(jdata_json->'t'))
	ON CONFLICT (loc_id, ts, source_id) DO UPDATE
	SET jdata_i = EXCLUDED.jdata_i,
		jdata_a = EXCLUDED.jdata_a,
		jdata_s = EXCLUDED.jdata_s,
		jdata_t = EXCLUDED.jdata_t,
		posted = EXCLUDED.posted;

	INSERT INTO solaragg.aud_loc_datum_hourly (
		ts_start, loc_id, source_id, prop_count)
	VALUES (ts_post_hour, loc, src, jdata_prop_count)
	ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
	SET prop_count = aud_loc_datum_hourly.prop_count + EXCLUDED.prop_count;

	INSERT INTO solaragg.agg_stale_loc_datum (agg_kind, loc_id, ts_start, source_id)
	SELECT 'h' AS agg_kind, loc_id, ts_start, source_id
	FROM solardatum.calculate_stale_loc_datum(loc, src, cdate)
	ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;
END;
$$;

/**
 * Calculate the minimum number of absolute time spans required for a given set of locations.
 *
 * The time zones of each location are used to group them into rows where all locations have the
 * same absolute start/end dates.
 *
 * @param locs the list of location to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date
 * @param ts_max the ending local date
 */
CREATE OR REPLACE FUNCTION solarnet.loc_source_time_ranges_local(
	locs bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  loc_ids bigint[],
  source_ids character varying(64)[]
) LANGUAGE sql STABLE AS $$
	SELECT ts_min AT TIME ZONE l.time_zone AS sdate,
		ts_max AT TIME ZONE l.time_zone AS edate,
		l.time_zone AS time_zone,
		array_agg(DISTINCT l.id) AS loc_ids,
		array_agg(DISTINCT s.source_id::character varying(64)) FILTER (WHERE s.source_id IS NOT NULL) AS sources
	FROM solarnet.sn_loc l
	LEFT JOIN (
		SELECT unnest(sources) AS source_id
	) s ON TRUE
	WHERE l.id = ANY(locs)
	GROUP BY time_zone
$$;

/**
 * Delete location datum and associated aggregates and insert appropriate "stale loc datum" rows
 * into the `solargg.agg_stale_loc_datum` table.
 *
 * @param locs the list of locations to delete
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date
 * @param ts_max the ending local date
 */
CREATE OR REPLACE FUNCTION solardatum.delete_loc_datum(
	locs 		BIGINT[],
	sources 	TEXT[],
	ts_min 		TIMESTAMP,
	ts_max 		TIMESTAMP
) RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS $$
DECLARE
	all_source_ids boolean := sources IS NULL OR array_length(sources, 1) < 1;
	start_date timestamp := COALESCE(ts_min, CURRENT_TIMESTAMP);
	end_date timestamp := COALESCE(ts_max, CURRENT_TIMESTAMP);
	total_count bigint := 0;
	--stale_count bigint := 0;
BEGIN
	WITH llt AS (
		SELECT time_zone, ts_start, ts_end, loc_ids, source_ids
		FROM solarnet.loc_source_time_ranges_local(locs, sources, start_date, end_date)
	)
	, hourly AS (
		DELETE FROM solaragg.agg_loc_datum_hourly d
		USING llt
		WHERE
			-- whole hours (ceil) only; partial to be handled by stale processing
			d.ts_start >= date_trunc('hour', llt.ts_start) + interval '1 hour'
			AND d.ts_start < date_trunc('hour', llt.ts_end)
			AND d.loc_id = ANY(llt.loc_ids)
			AND (all_source_ids OR d.source_id = ANY(llt.source_ids))
	)
	, daily AS (
		DELETE FROM solaragg.agg_loc_datum_daily d
		USING llt
		WHERE
			-- whole days only; partial to be handled by stale processing
			d.ts_start >= CASE
				WHEN date_trunc('day', llt.ts_start) = llt.ts_start THEN llt.ts_start
				ELSE date_trunc('day', llt.ts_start) + interval '1 day'
				END
			AND d.ts_start < date_trunc('day', llt.ts_end)
			AND d.loc_id = ANY(llt.loc_ids)
			AND (all_source_ids OR d.source_id = ANY(llt.source_ids))
	)
	, monthly AS (
		DELETE FROM solaragg.agg_loc_datum_monthly d
		USING llt
		WHERE
			-- whole months only; partial to be handled by stale processing
			d.ts_start >= CASE
				WHEN date_trunc('month', llt.ts_start) = llt.ts_start THEN llt.ts_start
				ELSE date_trunc('month', llt.ts_start) + interval '1 month'
				END
			AND d.ts_start < date_trunc('month', llt.ts_end)
			AND d.loc_id = ANY(llt.loc_ids)
			AND (all_source_ids OR d.source_id = ANY(llt.source_ids))
	)
	DELETE FROM solardatum.da_loc_datum d
	USING llt
	WHERE
		d.ts >= llt.ts_start
		AND d.ts < llt.ts_end
		AND d.loc_id = ANY(llt.loc_ids)
		AND (all_source_ids OR d.source_id = ANY(llt.source_ids));
	GET DIAGNOSTICS total_count = ROW_COUNT;

	-- mark remaining hourly aggregates as stale, so partial hours/days/months recalculated
	WITH llt AS (
		SELECT l.id AS loc_id
			, source_id
			, start_date AT TIME ZONE l.time_zone AS ts_start
		FROM solarnet.sn_loc l, UNNEST(sources) AS source_id
		WHERE l.id = ANY(locs)
		UNION ALL
		SELECT l.id AS loc_id
			, source_id
			, end_date AT TIME ZONE l.time_zone AS ts_start
		FROM solarnet.sn_loc l, UNNEST(sources) AS source_id
		WHERE l.id = ANY(locs)
	)
	INSERT INTO solaragg.agg_stale_loc_datum(loc_id, source_id, ts_start, agg_kind)
	SELECT s.loc_id, s.source_id, s.ts_start, 'h'
	FROM llt, solardatum.calculate_stale_loc_datum(llt.loc_id, llt.source_id, llt.ts_start) s
	ON CONFLICT DO NOTHING;

	--GET DIAGNOSTICS stale_count = ROW_COUNT;
	--RAISE NOTICE 'INSERTED % solaragg.agg_stale_loc_datum rows after delete.', stale_count;

	RETURN total_count;
END
$$;
