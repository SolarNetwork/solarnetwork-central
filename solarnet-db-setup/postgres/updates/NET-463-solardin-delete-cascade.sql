ALTER TABLE solardin.din_endpoint
	DROP CONSTRAINT din_endpoint_xform_fk,
	ADD CONSTRAINT din_endpoint_xform_fk FOREIGN KEY (user_id, xform_id)
		REFERENCES solardin.din_xform (user_id, id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE solardin.inin_endpoint
	DROP CONSTRAINT inin_endpoint_req_xform_fk,
	DROP CONSTRAINT inin_endpoint_res_xform_fk,
	ADD CONSTRAINT inin_endpoint_req_xform_fk FOREIGN KEY (user_id, req_xform_id)
		REFERENCES solardin.inin_req_xform (user_id, id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE,
	ADD CONSTRAINT inin_endpoint_res_xform_fk FOREIGN KEY (user_id, res_xform_id)
		REFERENCES solardin.inin_res_xform (user_id, id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE solardin.cin_datum_stream
	DROP CONSTRAINT cin_datum_stream_map_fk,
	ADD CONSTRAINT cin_datum_stream_map_fk FOREIGN KEY (user_id, map_id)
    REFERENCES solardin.cin_datum_stream_map (user_id, id) MATCH SIMPLE
    ON UPDATE NO ACTION ON DELETE CASCADE;
