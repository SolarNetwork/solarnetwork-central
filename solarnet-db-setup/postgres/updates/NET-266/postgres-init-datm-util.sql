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


