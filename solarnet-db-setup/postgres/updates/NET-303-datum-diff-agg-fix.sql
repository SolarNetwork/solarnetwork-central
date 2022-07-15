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
		INNER JOIN solarcommon.reduce_dim(agg_state.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
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
		INNER JOIN solarcommon.reduce_dim(agg_state.read_a) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
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
