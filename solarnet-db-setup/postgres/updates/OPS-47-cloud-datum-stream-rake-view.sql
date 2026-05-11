/**
 * VIEW solardin.cin_datum_stream_rake_info
 *
 * View of datum streams combined with mapping, integration, and rake tasks.
 */
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
