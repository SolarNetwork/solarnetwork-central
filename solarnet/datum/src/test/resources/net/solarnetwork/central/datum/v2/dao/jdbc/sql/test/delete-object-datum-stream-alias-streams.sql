DELETE FROM solardatm.da_datm_alias da
WHERE (
	da.stream_id = ANY(?)
	OR da.orig_stream_id = ANY(?)
)

