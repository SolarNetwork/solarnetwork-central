DELETE FROM solardatm.da_datm_alias da
WHERE da.node_id = ANY(?)
