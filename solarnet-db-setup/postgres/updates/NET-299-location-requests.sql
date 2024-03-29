/******************************************************************************
 * TABLE solarnet.sn_loc_req
 *
 * Table for location creation requests.
 */
CREATE TABLE solarnet.sn_loc_req (
	id			BIGINT GENERATED BY DEFAULT AS IDENTITY,
	created		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id		BIGINT NOT NULL,
	status 		CHARACTER(1) NOT NULL,
	jdata		JSONB NOT NULL,
	loc_id		BIGINT,
	message		TEXT,
	fts_default tsvector,
	CONSTRAINT sn_loc_req_pk PRIMARY KEY (id)
);

CREATE INDEX sn_loc_req_user_idx ON solarnet.sn_loc_req (user_id, status);

CREATE INDEX sn_loc_req_fts_default_idx ON solarnet.sn_loc_req USING gin(fts_default);

CREATE OR REPLACE FUNCTION solarnet.sn_loc_req_maintain_fts()
	RETURNS "trigger"  LANGUAGE 'plpgsql' VOLATILE AS $$
BEGIN
	NEW.fts_default :=
		   to_tsvector('pg_catalog.english', COALESCE(NEW.jdata ->> 'sourceId',''))
		|| to_tsvector('pg_catalog.english', COALESCE(NEW.jdata #>> '{location,name}',''))
		|| to_tsvector('pg_catalog.english', COALESCE(NEW.jdata #>> '{location,country}',''))
		|| to_tsvector('pg_catalog.english', COALESCE(NEW.jdata #>> '{location,region}',''))
		|| to_tsvector('pg_catalog.english', COALESCE(NEW.jdata #>> '{location,stateOrProvince}',''))
		|| to_tsvector('pg_catalog.english', COALESCE(NEW.jdata #>> '{location,locality}',''))
		|| to_tsvector('pg_catalog.english', COALESCE(NEW.jdata #>> '{location,postalCode}',''));
	RETURN NEW;
END
$$;

CREATE TRIGGER sn_loc_req_maintain_fts
  BEFORE INSERT OR UPDATE
  ON solarnet.sn_loc_req
  FOR EACH ROW
  EXECUTE PROCEDURE solarnet.sn_loc_req_maintain_fts();
