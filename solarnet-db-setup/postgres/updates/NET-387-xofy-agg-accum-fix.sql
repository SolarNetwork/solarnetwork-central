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
