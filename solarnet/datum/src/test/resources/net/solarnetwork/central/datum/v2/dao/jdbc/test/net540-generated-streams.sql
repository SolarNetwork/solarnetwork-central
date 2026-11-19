-- NET-540: 24 generated node datum streams, for testing the 15 minute rollup against the hourly one.
--
-- The streams are those of the NET-540 prototypes (setup-streams.sql and test 3): regular and
-- jittered cadences from 1 minute to 2 hours, gaps and outages, reset records on and between hour
-- and slot boundaries, pairs of reset records with no datum between them, datum that lack some or
-- all accumulating properties, and a second accumulating property that is carried only in part of
-- each hour or never. All run from 2030-03-04 00:00 UTC, for up to 3 days, except stream 10, which
-- is offline for 23 days.
--
-- Creates the temporary tables t_stream (k, sid, label) and t_hours (k, sid, hour), the latter
-- with every hour of each stream's datum, plus two hours either side, in the session time zone.
-- Both are dropped at commit, so run this inside a transaction.

SELECT setseed(0.540);

CREATE TEMP TABLE t_stream (k INT PRIMARY KEY, sid UUID NOT NULL, label TEXT NOT NULL) ON COMMIT DROP;

INSERT INTO t_stream (k, sid, label)
SELECT k, ('00000000-0000-4000-8540-' || lpad(k::text, 12, '0'))::uuid, label
FROM (VALUES
	  ( 1, '1 min, jittered')
	, ( 2, '5 min, on boundaries')
	, ( 3, '15 min, on boundaries')
	, ( 4, '20 min, jittered')
	, ( 5, '30 min, on boundaries')
	, ( 6, '60 min, jittered (55-65)')
	, ( 7, '60 min, on the hour')
	, ( 8, 'random gaps, 1 s to 90 min')
	, ( 9, '1 min with outages to 6 h')
	, (10, 'offline for 23 days')
	, (11, '1 min with reset records')
	, (12, '5 min offset, resets on boundaries')
	, (13, '2 hours')
	, (14, '45 min, exact')
	, (15, '1 min, some datum lack accumulating')
	, (16, '10 min offset, jittered')
	, (17, '1 min, each property missing at times')
	, (18, '20 min, pairs of resets between datum')
	, (19, '5 min, pairs of resets between datum')
	, (20, '2nd property only in minutes 0-19')
	, (21, '2nd property only in minutes 40-59')
	, (22, '2nd property only in minutes 20-39')
	, (23, '2nd property never')
	, (24, '2nd property zero, minutes 0-19 only')
) AS t(k, label);

INSERT INTO solardatm.da_datm_meta (stream_id, node_id, source_id, names_i, names_a, names_s)
SELECT sid, -540002, 'net540/' || k, ARRAY['w','v'], ARRAY['wh','x'], ARRAY['st']
FROM t_stream;

CREATE TEMP TABLE t_times (k INT NOT NULL, n INT NOT NULL, ts TIMESTAMPTZ NOT NULL) ON COMMIT DROP;

-- cumulative random gaps, in seconds
INSERT INTO t_times (k, n, ts)
SELECT k, n, '2030-03-04 00:00:00+00'::timestamptz + (sum(gap) OVER (PARTITION BY k ORDER BY n)) * interval '1 second'
FROM (
	SELECT 1 AS k, n, round((50 + random() * 20)::numeric, 3) AS gap FROM generate_series(1, 4400) n
	UNION ALL
	SELECT 4, n, round((900 + random() * 600)::numeric, 3) FROM generate_series(1, 220) n
	UNION ALL
	SELECT 6, n, round((3300 + random() * 600)::numeric, 3) FROM generate_series(1, 75) n
	UNION ALL
	SELECT 8, n, round((1 + random() * 5399)::numeric, 3) FROM generate_series(1, 100) n
	UNION ALL
	SELECT 9, n, CASE WHEN random() < 0.01 THEN round((1200 + random() * 20400)::numeric, 3)
			ELSE round((50 + random() * 20)::numeric, 3) END FROM generate_series(1, 3000) n
	UNION ALL
	SELECT 10, n, CASE WHEN n = 361 THEN 23 * 86400 + 7 * 3600 + 13 * 60 + 0.5
			ELSE round((50 + random() * 20)::numeric, 3) END FROM generate_series(1, 720) n
	UNION ALL
	SELECT 11, n, round((50 + random() * 20)::numeric, 3) FROM generate_series(1, 4400) n
	UNION ALL
	SELECT 15, n, round((50 + random() * 20)::numeric, 3) FROM generate_series(1, 2200) n
	UNION ALL
	SELECT 16, n, round((540 + random() * 120)::numeric, 3) FROM generate_series(1, 430) n
) g;

-- exact cadences
INSERT INTO t_times (k, n, ts)
SELECT k, n, '2030-03-04 00:00:00+00'::timestamptz + off + n * step
FROM (VALUES
	  ( 2, interval '5 minutes',  interval '0',       864)
	, ( 3, interval '15 minutes', interval '0',       288)
	, ( 5, interval '30 minutes', interval '0',       144)
	, ( 7, interval '60 minutes', interval '0',        72)
	, (12, interval '5 minutes',  interval '150 sec', 864)
	, (13, interval '2 hours',    interval '7 min',    36)
	, (14, interval '45 minutes', interval '0',        96)
) AS c(k, step, off, cnt)
, generate_series(0, c.cnt - 1) n;

INSERT INTO solardatm.da_datm (stream_id, ts, received, data_i, data_a, data_s, data_t)
SELECT s.sid, t.ts, t.ts
	, ARRAY[round((random() * 1000)::numeric, 3), round((random() * 250)::numeric, 1)]
	, ARRAY[sum(round((random() * 10)::numeric, 3)) OVER w, (t.n * 7)::numeric]
	, ARRAY[(ARRAY['on','off','idle'])[1 + floor(random() * 3)::int]]
	, CASE WHEN random() < 0.1 THEN ARRAY['t' || floor(random() * 3)::int] END
FROM t_times t
INNER JOIN t_stream s USING (k)
WINDOW w AS (PARTITION BY t.k ORDER BY t.n);

-- datum without any accumulating properties
UPDATE solardatm.da_datm SET data_a = NULL
WHERE stream_id = (SELECT sid FROM t_stream WHERE k = 15) AND random() < 0.15;

-- reset records
INSERT INTO solardatm.da_datm_aux (stream_id, ts, atype, jdata_af, jdata_as)
SELECT s.sid, '2030-03-04 00:00:00+00'::timestamptz + r.off, 'Reset'
	, jsonb_build_object('wh', r.f, 'x', r.f * 2), jsonb_build_object('wh', r.s, 'x', 0)
FROM (VALUES
	  (11, interval '5 hours',               30000, 0)      -- exactly on the hour
	, (11, interval '9 hours 15 minutes',    50000, 10)     -- exactly on a slot
	, (11, interval '13 hours 22 min 10 s',  60000, 0)
	, (11, interval '20 hours 59 min 59 s',  70000, 5)
	, (11, interval '30 hours 10 minutes',   90000, 0)      -- two in one hour
	, (11, interval '30 hours 40 minutes',     500, 0)
	, (12, interval '3 hours',               20000, 0)      -- on the hour, between datum
	, (12, interval '7 hours 45 minutes',    30000, 100)    -- on a slot, between datum
	, (12, interval '12 hours 2 min 30 s',   40000, 0)      -- same time as a datum
	, (12, interval '18 hours 58 minutes',   50000, 0)
	, (10, interval '10 days',                9000, 0)      -- during the offline gap
	, ( 9, interval '26 hours 30 minutes',   99000, 0)
	, ( 7, interval '10 hours 20 minutes',     800, 0)      -- sparse stream, inside the hour
	, ( 6, interval '31 hours',               1500, 0)      -- sparse stream, on the hour
) AS r(k, off, f, s)
INNER JOIN t_stream s USING (k);

-- each accumulating property missing at times
INSERT INTO solardatm.da_datm (stream_id, ts, received, data_i, data_a, data_s)
SELECT s.sid, g.ts, g.ts
	, ARRAY[round((random() * 1000)::numeric, 3), CASE WHEN random() < 0.2 THEN NULL ELSE round((random() * 250)::numeric, 1) END]
	, ARRAY[CASE WHEN g.r1 < 0.25 THEN NULL ELSE g.c1 END, CASE WHEN g.r2 < 0.25 THEN NULL ELSE g.n * 7 END]
	, ARRAY[(ARRAY['on','off','idle'])[1 + floor(random() * 3)::int]]
FROM t_stream s
, LATERAL (
	SELECT n, '2030-03-04 00:00:00+00'::timestamptz + (sum(round((50 + random() * 20)::numeric, 3)) OVER (ORDER BY n)) * interval '1 second' AS ts
		, sum(round((random() * 10)::numeric, 3)) OVER (ORDER BY n) AS c1
		, random() AS r1, random() AS r2
	FROM generate_series(1, 2200) n
) g
WHERE s.k = 17;

INSERT INTO solardatm.da_datm (stream_id, ts, received, data_i, data_a, data_s)
SELECT s.sid, g.ts, g.ts
	, ARRAY[round((random() * 1000)::numeric, 3), round((random() * 250)::numeric, 1)]
	, ARRAY[g.c1, g.n * 7]
	, ARRAY['on']
FROM t_stream s
, LATERAL (
	SELECT n, '2030-03-04 00:00:00+00'::timestamptz + (sum(round((CASE s.k WHEN 18 THEN 900 + random() * 600 ELSE 280 + random() * 40 END)::numeric, 3)) OVER (ORDER BY n)) * interval '1 second' AS ts
		, sum(round((random() * 10)::numeric, 3)) OVER (ORDER BY n) AS c1
	FROM generate_series(1, CASE s.k WHEN 18 THEN 220 ELSE 860 END) n
) g
WHERE s.k IN (18, 19);

-- for about one gap in six, two resets inside the gap (at 1/3 and 2/3 of it)
INSERT INTO solardatm.da_datm_aux (stream_id, ts, atype, jdata_af, jdata_as)
SELECT g.stream_id, g.ts + (g.nxt - g.ts) * f.frac, 'Reset'
	, jsonb_build_object('wh', g.val + f.add, 'x', 0), jsonb_build_object('wh', f.st, 'x', 0)
FROM (
	SELECT d.stream_id, d.ts, d.data_a[1] AS val, lead(d.ts) OVER (PARTITION BY d.stream_id ORDER BY d.ts) AS nxt, random() AS r
	FROM solardatm.da_datm d INNER JOIN t_stream s ON s.sid = d.stream_id WHERE s.k IN (18, 19)
) g
, (VALUES (1.0/3, 2, 1000000), (2.0/3, 1000004, 500)) AS f(frac, add, st)
WHERE g.r < 0.17 AND g.nxt IS NOT NULL;

-- a second accumulating property that the datum carry only in part of each hour, or never
INSERT INTO solardatm.da_datm (stream_id, ts, received, data_i, data_a, data_s)
SELECT s.sid, g.ts, g.ts
	, ARRAY[round((random() * 1000)::numeric, 3), round((random() * 250)::numeric, 1)]
	, CASE s.k
		WHEN 20 THEN CASE WHEN g.mi < 20 THEN ARRAY[g.c1, g.n * 7] ELSE ARRAY[g.c1] END
		WHEN 21 THEN CASE WHEN g.mi >= 40 THEN ARRAY[g.c1, g.n * 7] ELSE ARRAY[g.c1] END
		WHEN 22 THEN CASE WHEN g.mi >= 20 AND g.mi < 40 THEN ARRAY[g.c1, g.n * 7] ELSE ARRAY[g.c1] END
		WHEN 23 THEN ARRAY[g.c1]
		ELSE CASE WHEN g.mi < 20 THEN ARRAY[g.c1, 0] ELSE ARRAY[g.c1] END
		END
	, ARRAY['on']
FROM t_stream s
, LATERAL (
	SELECT x.n, x.ts, x.c1, extract(minute FROM x.ts)::int AS mi
	FROM (
		SELECT n
			, '2030-03-04 00:00:00+00'::timestamptz + (sum(round((50 + random() * 20)::numeric, 3)) OVER (ORDER BY n)) * interval '1 second' AS ts
			, sum(round((random() * 10)::numeric, 3)) OVER (ORDER BY n) AS c1
		FROM generate_series(1, 1500) n
	) x
) g
WHERE s.k BETWEEN 20 AND 24;

-- every hour of each stream, in the session time zone
CREATE TEMP TABLE t_hours ON COMMIT DROP AS
SELECT s.k, s.sid, h AS hour
FROM t_stream s
INNER JOIN LATERAL (
	SELECT date_trunc('hour', min(ts)) - interval '2 hours' AS a
		, date_trunc('hour', max(ts)) + interval '2 hours' AS b
	FROM solardatm.da_datm WHERE stream_id = s.sid
) r ON TRUE
INNER JOIN LATERAL generate_series(r.a, r.b, interval '1 hour') h ON TRUE;
