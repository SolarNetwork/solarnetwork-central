/**
 * Datum interpolation aggregate state transition function, to interpolate a datum from existing
 * datum around a specific point in time.
 *
 * @param ts_at the timestamp to interpolate the input rows at
 *
 * @see solardatm.calc_datm_at_ffunc()
 */
CREATE OR REPLACE FUNCTION solardatm.calc_datm_at_sfunc(agg_state solardatm.agg_datm_at, el solardatm.da_datm, ts_at TIMESTAMP WITH TIME ZONE)
RETURNS solardatm.agg_datm_at LANGUAGE plpgsql STRICT IMMUTABLE AS
$$
BEGIN
	-- keep track of just first/last inputs to aggregate; we expect 2 normally, but sometimes only 1
	IF el.ts IS NOT NULL THEN
		IF agg_state.ts_at IS NOT NULL THEN
			agg_state.datms := ARRAY[agg_state.datms[1], el];
		ELSE
			agg_state := (ARRAY[el], ts_at)::solardatm.agg_datm_at;
		END IF;
	END IF;
	RETURN agg_state;
END;
$$;


/**
 * Datum interpolation aggregate final transition function, to interpolate a datum from existing
 * datum around a specific point in time.
 *
 * @see solardatm.rollup_agg_datm_sfunc()
 */
CREATE OR REPLACE FUNCTION solardatm.calc_datm_at_ffunc(agg_state solardatm.agg_datm_at)
RETURNS solardatm.da_datm LANGUAGE plpgsql STRICT IMMUTABLE AS
$$
DECLARE
	result 			solardatm.da_datm;
	datum_count  	INTEGER 			:= array_length(agg_state.datms, 1);
BEGIN
	IF datum_count = 1 THEN
		result := agg_state.datms[1];
	ELSIF datum_count > 1 THEN
		WITH d AS (
			SELECT *
				, CASE
					WHEN count(*) OVER win < 2 THEN 1
					ELSE
						(EXTRACT('epoch' FROM agg_state.ts_at - first_value(ts) OVER win)
						/ EXTRACT('epoch' FROM last_value(ts) OVER win - first_value(ts) OVER win))
					END AS portion
			FROM unnest(agg_state.datms)
			WINDOW win AS (ORDER BY ts RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
		)
		-- calculate instantaneous statistis per property
		, di AS (
			SELECT
				  p.idx
				, to_char(solarcommon.first(val ORDER BY ts) +
					(solarcommon.first(val ORDER BY ts DESC) - solarcommon.first(val ORDER BY ts)) * min(portion)
					, 'FM999999999999999999990.999999999')::numeric AS val
			FROM d
			INNER JOIN unnest(d.data_i) WITH ORDINALITY AS p(val, idx) ON TRUE
			WHERE p.val IS NOT NULL
			GROUP BY p.idx
		)
		-- join data_i and stat_i property values back into arrays
		, di_ary AS (
			SELECT
				   array_agg(val ORDER BY idx) AS data_i
			FROM di
		)
		-- calculate accumulating values per property
		, da AS (
			SELECT
				  p.idx
				, to_char(solarcommon.first(val ORDER BY ts) +
					(solarcommon.first(val ORDER BY ts DESC) - solarcommon.first(val ORDER BY ts)) * min(portion)
					, 'FM999999999999999999990.999999999')::numeric AS val
			FROM d
			INNER JOIN unnest(d.data_a) WITH ORDINALITY AS p(val, idx) ON TRUE
			WHERE p.val IS NOT NULL
			GROUP BY p.idx
		)
		-- join data_a property values back into arrays
		, da_ary AS (
			SELECT
				  array_agg(val ORDER BY idx) AS data_a
			FROM da
		)
		-- calculate status statistics, as most-frequent status values
		, ds AS (
			SELECT
				  p.idx
				, mode() WITHIN GROUP (ORDER BY p.val) AS val
			FROM d
			INNER JOIN unnest(d.data_s) WITH ORDINALITY AS p(val, idx) ON TRUE
			WHERE p.val IS NOT NULL
			GROUP BY p.idx
		)
		-- join data_s property values back into arrays
		, ds_ary AS (
			SELECT
				  array_agg(val ORDER BY idx) AS data_s
			FROM ds
		)
		-- join data_t property values into mega array
		, dt_ary AS (
			SELECT
				  array_agg(p.val ORDER BY d.ts) AS data_t
			FROM d
			INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
			WHERE d.data_t IS NOT NULL
		)
		SELECT
			  agg_state.datms[1].stream_id AS stream_id
			, agg_state.ts_at AS ts
			, agg_state.ts_at AS received
			, di_ary.data_i
			, da_ary.data_a
			, ds_ary.data_s
			, dt_ary.data_t
		FROM di_ary, da_ary, ds_ary, dt_ary
		WHERE data_i IS NOT NULL OR data_a IS NOT NULL OR data_s IS NOT NULL OR data_t IS NOT NULL
		INTO result;
	END IF;
	RETURN CASE WHEN result.ts IS NOT NULL THEN result ELSE NULL END;
END;
$$;


/**
 * Aggregate datum rollup aggregate, to rollup aggregate datum into a higher-level aggregate datum.
 *
 * The `TIMESTAMP` argument is used as the output `ts_start` column value.
 *
 * NOTE: using this aggregate is slower than calling solardatm.rollup_agg_datm_for_time_span()
 *       but can be used for other specialised queries like HOD and DOW aggregates so they don't
 *       have to duplicate all the aggregation logic involved
 */
CREATE AGGREGATE solardatm.calc_datm_at(solardatm.da_datm, TIMESTAMP WITH TIME ZONE) (
    stype 		= solardatm.agg_datm_at,
    sfunc 		= solardatm.calc_datm_at_sfunc,
    finalfunc 	= solardatm.calc_datm_at_ffunc,
    initcond	= '(,)'
);


/**
 * Datum diff state transition function, to calculate differece over records.
 *
 * @see solardatm.diff_datm_ffunc()
 */
CREATE OR REPLACE FUNCTION solardatm.diff_datm_sfunc(agg_state solardatm.agg_datm_diff, el solardatm.datm_rec)
RETURNS solardatm.agg_datm_diff LANGUAGE plpgsql IMMUTABLE AS
$$
BEGIN
	WITH wi AS (
		SELECT
			  p.idx
			, p.val AS val
			, s.stat[1] AS cnt
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(agg_state.data_i) WITH ORDINALITY AS p(val, idx)
		INNER JOIN solardatm.unnest_2d(agg_state.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
		UNION ALL
		SELECT
			  p.idx
			, p.val
			, 1 AS cnt
			, p.val AS val_min
			, p.val AS val_max
		FROM unnest(el.data_i) WITH ORDINALITY AS p(val,idx)
	)
	, di AS (
		SELECT
			idx
			, sum(val) AS val
			, sum(cnt) AS cnt
			, min(val_min) AS val_min
			, max(val_max) AS val_max
		FROM wi
		WHERE val IS NOT NULL
		GROUP BY idx
	)
	, di_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_i
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
			) AS stat_i
		FROM di
	)
	-- calculate accumulating values per property
	, wa AS (
		SELECT
			  p.idx
			, p.val
			, s.stat[2] AS val_start
			, s.stat[3] AS val_end
			, 0 AS rtype
		FROM unnest(agg_state.data_a) WITH ORDINALITY AS p(val, idx)
		INNER JOIN solardatm.unnest_2d(agg_state.read_a) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
		UNION ALL
		SELECT
			  p.idx
			, p.val
			, NULL::NUMERIC AS val_start
			, NULL::NUMERIC AS val_end
			, 1 AS rtype
		FROM unnest(el.data_a) WITH ORDINALITY AS p(val,idx)
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			idx
			, CASE el.rtype
				WHEN 2 THEN
					-- reset record; just keep previous value
					COALESCE(min(val) FILTER (WHERE rtype = 0), 0)
				ELSE
					-- previous value + (curr value - last end)
					COALESCE(min(val) FILTER (WHERE rtype = 0), 0)
						+ min(val) FILTER (WHERE rtype = 1)
						- COALESCE(min(val_end) FILTER (WHERE rtype = 0), min(val) FILTER (WHERE rtype = 1))
				END AS val

			-- start value as first value
			, COALESCE(min(val_start) FILTER (WHERE rtype = 0),
				CASE el.rtype
					WHEN 1 THEN
						-- reset final record does not count as start value
						NULL::NUMERIC
					ELSE
						min(val) FILTER (WHERE rtype = 1)
				END) AS val_start

			-- end value as current value
			, min(val) FILTER (WHERE rtype = 1) AS val_end

		FROM wa
		WHERE val IS NOT NULL
		GROUP BY idx
	)
	-- join data_a and read_a property values back into arrays
	, da_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_a
			, array_agg(
				ARRAY[val, val_start, val_end] ORDER BY idx
			) AS read_a
		FROM da
	)
	-- NOTE data_s property not supported, as mode() calculation not implemented
	-- join data_t property values into mega array
	, dt_ary AS (
		SELECT array_agg(DISTINCT val) AS data_t
		FROM (
			SELECT val
			FROM unnest(agg_state.data_t) t(val)
			UNION ALL
			SELECT val
			FROM unnest(el.data_t) t(val)
		) t
		WHERE val IS NOT NULL
	)
	SELECT
		  COALESCE(agg_state.stream_id, el.stream_id)
		, COALESCE(agg_state.ts_start, el.ts)
		, el.ts AS ts_end
		, di_ary.data_i
		, da_ary.data_a
		, NULL::TEXT[] AS data_s
		, dt_ary.data_t
		, di_ary.stat_i
		, da_ary.read_a
	FROM di_ary, da_ary, dt_ary
	INTO agg_state;

	RETURN agg_state;
END;
$$;


/**
 * Datum diff final transition function, to calculate differece over records.
 *
 * @see solardatm.diff_datm_sfunc()
 */
CREATE OR REPLACE FUNCTION solardatm.diff_datm_ffunc(agg_state solardatm.agg_datm_diff)
RETURNS solardatm.agg_datm_diff LANGUAGE plpgsql STRICT IMMUTABLE AS
$$
BEGIN
	WITH di AS (
		SELECT
			  p.idx
			, to_char(p.val / s.stat[1], 'FM999999999999999999990.999999999')::numeric AS val
			, s.stat[1] AS cnt
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(agg_state.data_i) WITH ORDINALITY AS p(val, idx)
		INNER JOIN solardatm.unnest_2d(agg_state.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
	)
	, di_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_i
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
			) AS stat_i
		FROM di
	)
	, da AS (
		SELECT
			  p.idx
			, to_char(p.val, 'FM999999999999999999990.999999999')::numeric AS val
		FROM unnest(agg_state.data_a) WITH ORDINALITY AS p(val, idx)
	)
	, da_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_a
		FROM da
	)
	SELECT
		  agg_state.stream_id
		, agg_state.ts_start
		, agg_state.ts_end
		, di_ary.data_i
		, da_ary.data_a
		, agg_state.data_s
		, agg_state.data_t
		, di_ary.stat_i
		, agg_state.read_a
	FROM di_ary, da_ary
	INTO agg_state;

	return agg_state;
END;
$$;


/**
 * Datum diff aggregate, to calculate differece over records.
 */
CREATE AGGREGATE solardatm.diff_datm(solardatm.datm_rec) (
    stype 		= solardatm.agg_datm_diff,
    sfunc 		= solardatm.diff_datm_sfunc,
    finalfunc 	= solardatm.diff_datm_ffunc
);
