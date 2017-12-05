ALTER TABLE solardatum.da_loc_datum
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t text[];

ALTER TABLE solardatum.da_loc_meta
  ALTER COLUMN jdata SET DATA TYPE jsonb;

DROP VIEW solaragg.da_loc_datum_avail_hourly;
DROP VIEW solaragg.da_loc_datum_avail_daily;
DROP VIEW solaragg.da_loc_datum_avail_monthly;

ALTER TABLE solaragg.agg_loc_datum_hourly
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t text[];

ALTER TABLE solaragg.agg_loc_datum_daily
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t text[];

ALTER TABLE solaragg.agg_loc_datum_monthly
	ADD COLUMN jdata_i jsonb,
	ADD COLUMN jdata_a jsonb,
	ADD COLUMN jdata_s jsonb,
	ADD COLUMN jdata_t text[];

CREATE OR REPLACE FUNCTION solardatum.jdata_from_datum(datum solardatum.da_loc_datum)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_loc_datum_hourly)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_loc_datum_daily)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solaragg.jdata_from_datum(datum solaragg.agg_loc_datum_monthly)
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT solarnet.jdata_from_components(datum.jdata_i, datum.jdata_a, datum.jdata_s, datum.jdata_t);
$$;

CREATE OR REPLACE FUNCTION solardatum.store_loc_meta(
	cdate timestamp with time zone,
	loc bigint,
	src text,
	jdata text)
  RETURNS void LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	udate timestamp with time zone := now();
	jdata_json jsonb := jdata::jsonb;
BEGIN
	INSERT INTO solardatum.da_loc_meta(loc_id, source_id, created, updated, jdata)
	VALUES (loc, src, cdate, udate, jdata_json)
	ON CONFLICT (loc_id, source_id) DO UPDATE
	SET jdata = EXCLUDED.jdata, updated = EXCLUDED.updated;
END;
$BODY$;

DROP FUNCTION solardatum.find_loc_most_recent(bigint, text[]);
CREATE OR REPLACE FUNCTION solardatum.find_loc_most_recent(
	loc bigint,
	sources text[] DEFAULT NULL)
  RETURNS SETOF solardatum.da_loc_datum AS
$BODY$
BEGIN
	IF sources IS NULL OR array_length(sources, 1) < 1 THEN
		RETURN QUERY
		SELECT dd.* FROM solardatum.da_loc_datum dd
		INNER JOIN (
			-- to speed up query for sources (which can be very slow when queried directly on da_loc_datum),
			-- we find the most recent hour time slot in agg_loc_datum_hourly, and then join to da_loc_datum with that narrow time range
			WITH days AS (
				SELECT max(d.ts_start) as ts_start, d.source_id FROM solaragg.agg_loc_datum_hourly d
				INNER JOIN (SELECT solardatum.find_loc_available_sources(loc) AS source_id) AS s ON s.source_id = d.source_id
				WHERE d.loc_id = loc
				GROUP BY d.source_id
			)
			SELECT max(d.ts) as ts, d.source_id FROM solardatum.da_loc_datum d
			INNER JOIN days ON days.source_id = d.source_id
			WHERE d.loc_id = loc
				AND d.ts >= days.ts_start
				AND d.ts < days.ts_start + interval '1 hour'
			GROUP BY d.source_id
		) AS r ON r.ts = dd.ts AND r.source_id = dd.source_id AND dd.loc_id = loc
		ORDER BY dd.source_id ASC;
	ELSE
		RETURN QUERY
		SELECT dd.* FROM solardatum.da_loc_datum dd
		INNER JOIN (
			WITH days AS (
				SELECT max(d.ts_start) as ts_start, d.source_id FROM solaragg.agg_loc_datum_hourly d
				INNER JOIN (SELECT unnest(sources) AS source_id) AS s ON s.source_id = d.source_id
				WHERE d. loc_id = loc
				GROUP BY d.source_id
			)
			SELECT max(d.ts) as ts, d.source_id FROM solardatum.da_loc_datum d
			INNER JOIN days ON days.source_id = d.source_id
			WHERE d.loc_id = loc
				AND d.ts >= days.ts_start
				AND d.ts < days.ts_start + interval '1 hour'
			GROUP BY d.source_id
		) AS r ON r.ts = dd.ts AND r.source_id = dd.source_id AND dd.loc_id = loc
		ORDER BY dd.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;
