CREATE OR REPLACE FUNCTION solardatm.rollup_agg_datm_sfunc(agg_state solardatm.agg_datm, el solardatm.agg_datm, start_ts TIMESTAMP WITH TIME ZONE)
RETURNS solardatm.agg_datm LANGUAGE plpgsql IMMUTABLE AS
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
			, (p.val * s.stat[1]) AS val
			, s.stat[1] AS cnt
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(el.data_i) WITH ORDINALITY AS p(val,idx)
		INNER JOIN solardatm.unnest_2d(el.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
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
	-- NOTE: read_a accumulation not supported as first/last calculation not implemented
	, wa AS (
		SELECT
			  p.idx
			, p.val
		FROM unnest(agg_state.data_a) WITH ORDINALITY AS p(val, idx)
		UNION ALL
		SELECT
			  p.idx
			, p.val
		FROM unnest(el.data_a) WITH ORDINALITY AS p(val,idx)
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			idx
			, sum(val) AS val
		FROM wa
		WHERE val IS NOT NULL
		GROUP BY idx
	)
	-- join data_a and read_a property values back into arrays
	, da_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_a
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
		, COALESCE(start_ts, agg_state.ts_start)
		, di_ary.data_i
		, da_ary.data_a
		, NULL::TEXT[] AS data_s
		, dt_ary.data_t
		, di_ary.stat_i
		, NULL::NUMERIC[][] AS read_a
	FROM di_ary, da_ary, dt_ary
	INTO agg_state;

	RETURN agg_state;
END;
$$;

CREATE OR REPLACE FUNCTION solardatm.rollup_agg_datm_ffunc(agg_state solardatm.agg_datm)
RETURNS solardatm.agg_datm LANGUAGE plpgsql STRICT IMMUTABLE AS
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

-- NOTE: using this aggregate is slower than calling solardatm.rollup_agg_datm_for_time_span()
--       but can be used for other specialised queries like HOD and DOW aggregates so they don't
--       have to duplicate all the aggregation logic involved
CREATE AGGREGATE solardatm.rollup_agg_datm(solardatm.agg_datm, TIMESTAMP WITH TIME ZONE) (
    stype 		= solardatm.agg_datm,
    sfunc 		= solardatm.rollup_agg_datm_sfunc,
    finalfunc 	= solardatm.rollup_agg_datm_ffunc
);
