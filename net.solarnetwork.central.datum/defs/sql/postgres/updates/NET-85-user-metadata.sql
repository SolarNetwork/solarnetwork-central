/******************************************************************************
 * TABLE solaruser.user_meta
 * 
 * JSON metadata specific to a user.
 */
CREATE TABLE solaruser.user_meta (
  user_id 			BIGINT NOT NULL,
  created 			solarcommon.ts NOT NULL,
  updated 			solarcommon.ts NOT NULL,
  jdata 			json NOT NULL,
  CONSTRAINT user_meta_pkey PRIMARY KEY (user_id),
  CONSTRAINT user_meta_user_fk FOREIGN KEY (user_id)
        REFERENCES solaruser.user_user (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE
);

/******************************************************************************
 * FUNCTION solaruser.store_meta(timestamptz, bigint, text)
 * 
 * Add or update user metadata.
 * 
 * @param cdate the creation date to use
 * @param userid the user ID
 * @param jdata the metadata to store
 */
CREATE OR REPLACE FUNCTION solaruser.store_user_meta(
	cdate solarcommon.ts, 
	userid BIGINT, 
	jdata text)
  RETURNS void AS
$BODY$
DECLARE
	udate solarcommon.ts := now();
	jdata_json json := jdata::json;
BEGIN
	-- We mostly expect updates, so try that first, then insert
	-- In 9.5 we can do upsert with ON CONFLICT.
	LOOP
		-- first try to update
		UPDATE solaruser.user_meta SET 
			jdata = jdata_json, 
			updated = udate
		WHERE
			user_id = userid;

		-- check if the row is found
		IF FOUND THEN
			RETURN;
		END IF;
		
		-- not found so insert the row
		BEGIN
			INSERT INTO solaruser.user_meta(user_id, created, updated, jdata)
			VALUES (userid, cdate, udate, jdata_json);
			RETURN;
		EXCEPTION WHEN unique_violation THEN
			-- do nothing and loop
		END;
	END LOOP;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE;
