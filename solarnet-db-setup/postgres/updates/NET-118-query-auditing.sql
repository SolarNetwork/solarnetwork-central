ALTER TABLE solaragg.aud_datum_hourly
	ALTER COLUMN prop_count SET DEFAULT 0,
	ADD COLUMN datum_q_count integer NOT NULL DEFAULT 0;

CREATE OR REPLACE FUNCTION solaragg.aud_inc_datum_query_count(
	qdate timestamp with time zone,
	node bigint,
	source text,
	dcount integer)
	RETURNS void LANGUAGE sql VOLATILE AS
$BODY$
	INSERT INTO solaragg.aud_datum_hourly(ts_start, node_id, source_id, datum_q_count)
	VALUES (date_trunc('hour', qdate), node, source, dcount)
	ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
	SET datum_q_count = aud_datum_hourly.datum_q_count + EXCLUDED.datum_q_count;
$BODY$;
