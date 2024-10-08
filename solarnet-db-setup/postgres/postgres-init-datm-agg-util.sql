/**
 * Aggregate datum rollup state transition function, to rollup aggregate datum into a higher-level
 * aggregate datum.
 *
 * @see solardatm.rollup_agg_data_ffunc()
 */
CREATE OR REPLACE FUNCTION solardatm.rollup_agg_data_sfunc(agg_state solardatm.agg_data, el solardatm.agg_data)
RETURNS solardatm.agg_data LANGUAGE plpgsql IMMUTABLE AS
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
		INNER JOIN solarcommon.reduce_dim(agg_state.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
		UNION ALL
		SELECT
			  p.idx
			, (p.val * s.stat[1]) AS val
			, s.stat[1] AS cnt
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(el.data_i) WITH ORDINALITY AS p(val,idx)
		INNER JOIN solarcommon.reduce_dim(el.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
	)
	, di AS (
		SELECT
			idx
			, sum(val) AS val
			, sum(cnt) AS cnt
			, min(val_min) AS val_min
			, max(val_max) AS val_max
		FROM wi
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
			, 0::SMALLINT AS rtype
			, s.stat[1] AS rdiff
			, s.stat[2] AS rstart
			, s.stat[3] AS rend
		FROM unnest(agg_state.data_a) WITH ORDINALITY AS p(val, idx)
		-- allow for NULL read_a input, e.g. from Minute agg
		LEFT OUTER JOIN solarcommon.reduce_dim(agg_state.read_a) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
		UNION ALL
		SELECT
			  p.idx
			, p.val
			, 1::SMALLINT AS rtype
			, s.stat[1] AS rdiff
			, s.stat[2] AS rstart
			, s.stat[3] AS rend
		FROM unnest(el.data_a) WITH ORDINALITY AS p(val,idx)
		-- allow for NULL read_a input, e.g. from Minute agg
		LEFT OUTER JOIN solarcommon.reduce_dim(el.read_a) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			idx
			, sum(val) AS val
			, sum(rdiff) AS rdiff
			, solarcommon.first(rstart ORDER BY rtype) AS rstart
			, solarcommon.first(rend ORDER BY rtype DESC) AS rend
		FROM wa
		GROUP BY idx
	)
	-- join data_a and read_a property values back into arrays
	, da_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_a
			, array_agg(
				ARRAY[rdiff, rstart, rend] ORDER BY idx
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
		  di_ary.data_i
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
 * Aggregate datum rollup final transition function, to rollup aggregate datum into a higher-level
 * aggregate datum.
 *
 * @see solardatm.rollup_agg_data_sfunc()
 */
CREATE OR REPLACE FUNCTION solardatm.rollup_agg_data_ffunc(agg_state solardatm.agg_data)
RETURNS solardatm.agg_data LANGUAGE plpgsql STRICT IMMUTABLE AS
$$
BEGIN
	WITH di AS (
		SELECT
			  p.idx
			, p.val / s.stat[1] AS val
			, s.stat[1] AS cnt
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(agg_state.data_i) WITH ORDINALITY AS p(val, idx)
		INNER JOIN solarcommon.reduce_dim(agg_state.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
	)
	, di_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val ORDER BY idx)) AS data_i
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
			) AS stat_i
		FROM di
	)
	, da AS (
		SELECT
			  p.idx
			, p.val
		FROM unnest(agg_state.data_a) WITH ORDINALITY AS p(val, idx)
	)
	, da_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val ORDER BY idx)) AS data_a
		FROM da
	)
	SELECT
		  di_ary.data_i
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
 * Aggregate datum rollup aggregate, to rollup aggregate datum into a higher-level aggregate datum.
 *
 * The `TIMESTAMP` argument is used as the output `ts_start` column value.
 *
 * Note that the aggregate should be used with an `ORDER BY ts_start` clause for the reading
 * data to be calculated correctly.
 *
 * NOTE: using this aggregate is slower than calling solardatm.rollup_agg_data_for_time_span()
 *       but can be used for other specialised queries like HOD and DOW aggregates so they don't
 *       have to duplicate all the aggregation logic involved
 */
CREATE AGGREGATE solardatm.rollup_agg_data(solardatm.agg_data) (
    stype 		= solardatm.agg_data,
    sfunc 		= solardatm.rollup_agg_data_sfunc,
    finalfunc 	= solardatm.rollup_agg_data_ffunc
);


/**
 * Aggregate datum rollup state transition function, to average aggregate datum into a higher-level
 * aggregate datum.
 *
 * Note that the `data_s` column are not supported. The `data_r` column is handled differently
 * in that each 3-element sub-array contains the average, min, and max accumulated value (instead
 * of the difference, start, and end accumulated value).
 *
 * @see solardatm.avg_agg_data_ffunc()
 */
CREATE OR REPLACE FUNCTION solardatm.avg_agg_data_sfunc(agg_state solardatm.agg_data, el solardatm.agg_data)
RETURNS solardatm.agg_data LANGUAGE plpgsql IMMUTABLE AS
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
		INNER JOIN solarcommon.reduce_dim(agg_state.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
		UNION ALL
		SELECT
			  p.idx
			, (p.val * s.stat[1]) AS val
			, s.stat[1] AS cnt
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(el.data_i) WITH ORDINALITY AS p(val,idx)
		INNER JOIN solarcommon.reduce_dim(el.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
	)
	, di AS (
		SELECT
			idx
			, sum(val) AS val
			, sum(cnt) AS cnt
			, min(val_min) AS val_min
			, max(val_max) AS val_max
		FROM wi
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
			, s.stat[1] AS cnt
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(agg_state.data_a) WITH ORDINALITY AS p(val, idx)
		LEFT OUTER JOIN solarcommon.reduce_dim(agg_state.read_a) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
		UNION ALL
		SELECT
			  p.idx
			, p.val
			, 1 AS cnt
			, p.val AS val_min
			, p.val AS val_max
		FROM unnest(el.data_a) WITH ORDINALITY AS p(val,idx)
		LEFT OUTER JOIN solarcommon.reduce_dim(agg_state.read_a) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			idx
			, sum(val) AS val
			, sum(cnt) AS cnt
			, min(val_min) AS val_min
			, max(val_max) AS val_max
		FROM wa
		GROUP BY idx
	)
	-- join data_a and read_a property values back into arrays
	, da_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_a
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
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
		  di_ary.data_i
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
 * Aggregate datum rollup final transition function, to rollup aggregate datum into a higher-level
 * aggregate datum.
 *
 * @see solardatm.avg_agg_data_sfunc()
 */
CREATE OR REPLACE FUNCTION solardatm.avg_agg_data_ffunc(agg_state solardatm.agg_data)
RETURNS solardatm.agg_data LANGUAGE plpgsql STRICT IMMUTABLE AS
$$
BEGIN
	WITH di AS (
		SELECT
			  p.idx
			, p.val / s.stat[1] AS val
			, s.stat[1] AS cnt
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(agg_state.data_i) WITH ORDINALITY AS p(val, idx)
		INNER JOIN solarcommon.reduce_dim(agg_state.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
	)
	, di_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val ORDER BY idx)) AS data_i
			, array_agg(
				vec_trim_scale(ARRAY[cnt, val_min, val_max]) ORDER BY idx
			) AS stat_i
		FROM di
	)
	, da AS (
		SELECT
			  p.idx
			, p.val / s.stat[1] AS val
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(agg_state.data_a) WITH ORDINALITY AS p(val, idx)
		INNER JOIN solarcommon.reduce_dim(agg_state.read_a) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
	)
	, da_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val ORDER BY idx)) AS data_a
			, array_agg(
				vec_trim_scale(ARRAY[val, val_min, val_max]) ORDER BY idx
			) AS read_a
		FROM da
	)
	SELECT
		  di_ary.data_i
		, da_ary.data_a
		, agg_state.data_s
		, agg_state.data_t
		, di_ary.stat_i
		, da_ary.read_a
	FROM di_ary, da_ary
	INTO agg_state;

	return agg_state;
END;
$$;


/**
 * Aggregate datum rollup aggregate, to rollup aggregate datum into a higher-level aggregate datum.
 *
 * The `TIMESTAMP` argument is used as the output `ts_start` column value.
 *
 * Note that the aggregate should be used with an `ORDER BY ts_start` clause for the reading
 * data to be calculated correctly.
 *
 * NOTE: using this aggregate is slower than calling solardatm.avg_agg_data_for_time_span()
 *       but can be used for other specialised queries like HOD and DOW aggregates so they don't
 *       have to duplicate all the aggregation logic involved
 */
CREATE AGGREGATE solardatm.avg_agg_data(solardatm.agg_data) (
    stype 		= solardatm.agg_data,
    sfunc 		= solardatm.avg_agg_data_sfunc,
    finalfunc 	= solardatm.avg_agg_data_ffunc
);
