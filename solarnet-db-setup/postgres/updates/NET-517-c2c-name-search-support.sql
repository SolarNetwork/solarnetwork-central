DROP VIEW solardin.cin_datum_stream_info;
DROP VIEW solardin.cin_datum_stream_prop_info;
DROP VIEW solardin.cin_datum_stream_rake_info;

ALTER TABLE solardin.cin_datum_stream
	ALTER COLUMN cname TYPE citext,
	ADD CONSTRAINT cin_datum_stream_cname_len CHECK (length(cname) <= 64)
	;
ALTER TABLE solardin.cin_integration
	ALTER COLUMN cname TYPE citext,
	ADD CONSTRAINT cin_integration_cname_len CHECK (length(cname) <= 64)
	;


CREATE VIEW solardin.cin_datum_stream_info AS
SELECT cds.user_id
	, cds.id
	, cds.created
	, cds.modified
	, cds.enabled
	, cds.cname
	, cds.sident
	, cds.schedule
	, cds.kind
	, cds.obj_id
	, cds.source_id
	, cds.sprops
	, cds.map_id
	, cdsm.cname AS map_name
	, cdsm.int_id
	, cin.enabled AS int_enabled
	, cin.cname AS int_name
	, cin.sident AS int_sident
	, cin.sprops AS int_sprops
	, cdsp.status AS poll_status
	, cdsp.exec_at AS poll_exec_at
	, cdsp.start_at AS poll_start_at
	, cdsp.message AS poll_message
	, cdsp.sprops AS poll_sprops
FROM solardin.cin_datum_stream cds
LEFT OUTER JOIN solardin.cin_datum_stream_map cdsm ON cdsm.id = cds.map_id AND cdsm.user_id = cds.user_id
LEFT OUTER JOIN solardin.cin_integration cin ON cin.id = cdsm.int_id AND cin.user_id = cdsm.user_id
LEFT OUTER JOIN solardin.cin_datum_stream_poll_task cdsp ON cdsp.ds_id = cds.id AND cdsp.user_id = cds.user_id
;

CREATE VIEW solardin.cin_datum_stream_prop_info AS
SELECT cds.user_id
	, cds.id
	, cds.created
	, cds.modified
	, cds.enabled
	, cds.cname
	, cds.sident
	, cds.schedule
	, cds.kind
	, cds.obj_id
	, cds.source_id
	, cds.sprops
	, cds.map_id
	, cdsm.cname AS map_name
	, cin.enabled AS int_enabled
	, cdsm.int_id
	, cin.cname AS int_name
	, cin.sident AS int_sident
	, cin.sprops AS int_sprops
	, cdsp.status AS poll_status
	, cdsp.exec_at AS poll_exec_at
	, cdsp.start_at AS poll_start_at
	, cdsp.message AS poll_message
	, cdsp.sprops AS poll_sprops
	, cdsprop.idx AS prop_idx
	, cdsprop.enabled AS prop_enabled
	, cdsprop.ptype AS prop_ptype
	, cdsprop.pname AS prop_pname
	, cdsprop.vtype AS prop_vtype
	, cdsprop.vref AS prop_vref
	, cdsprop.mult AS prop_mult
	, cdsprop.scale AS prop_scale
FROM solardin.cin_datum_stream cds
LEFT OUTER JOIN solardin.cin_datum_stream_map cdsm ON cdsm.id = cds.map_id AND cdsm.user_id = cds.user_id
LEFT OUTER JOIN solardin.cin_integration cin ON cin.id = cdsm.int_id AND cin.user_id = cdsm.user_id
LEFT OUTER JOIN solardin.cin_datum_stream_poll_task cdsp ON cdsp.ds_id = cds.id AND cdsp.user_id = cds.user_id
LEFT OUTER JOIN solardin.cin_datum_stream_prop cdsprop ON cdsprop.map_id = cdsm.id AND cdsprop.user_id = cdsm.user_id
;

CREATE VIEW solardin.cin_datum_stream_rake_info AS
SELECT cds.user_id
	, cds.id
	, cds.created
	, cds.modified
	, cds.enabled
	, cds.cname
	, cds.sident
	, cds.schedule
	, cds.kind
	, cds.obj_id
	, cds.source_id
	, cds.sprops
	, cds.map_id
	, cdsm.cname AS map_name
	, cdsm.int_id
	, cin.enabled AS int_enabled
	, cin.cname AS int_name
	, cin.sident AS int_sident
	, cin.sprops AS int_sprops
	, cdsr.id AS rake_id
	, cdsr.status AS rake_status
	, cdsr.exec_at AS rake_exec_at
	, cdsr.start_offset AS rake_start_offset
	, cdsr.message AS rake_message
	, cdsr.sprops AS rake_sprops
FROM solardin.cin_datum_stream cds
LEFT OUTER JOIN solardin.cin_datum_stream_map cdsm ON cdsm.id = cds.map_id AND cdsm.user_id = cds.user_id
LEFT OUTER JOIN solardin.cin_integration cin ON cin.id = cdsm.int_id AND cin.user_id = cdsm.user_id
LEFT OUTER JOIN solardin.cin_datum_stream_rake_task cdsr ON cdsr.ds_id = cds.id AND cdsr.user_id = cds.user_id
;
