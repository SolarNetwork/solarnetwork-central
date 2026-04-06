WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id, s.orig_stream_id
	FROM solardatm.da_datm_meta_aliased s 
	WHERE s.node_id = ANY(?)
)
, datum AS (
	SELECT (solardatm.calc_datm_at(d, ?)).*
	FROM s, solardatm.find_datm_around(s.orig_stream_id, ?, ?) d
	GROUP BY d.stream_id
)
SELECT s.stream_id
	, datum.ts
	, datum.received
	, datum.data_i
	, datum.data_a
	, datum.data_s
	, datum.data_t
FROM s
INNER JOIN datum ON datum.stream_id = s.orig_stream_id
