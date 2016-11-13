/**************************************************************************************************
 * TABLE solarnet.sn_node_meta
 * 
 * Stores JSON metadata specific to a node.
 */
CREATE TABLE solarnet.sn_node_meta (
  node_id 			solarcommon.node_id NOT NULL,
  created 			solarcommon.ts NOT NULL,
  updated 			solarcommon.ts NOT NULL,
  jdata 			json NOT NULL,
  CONSTRAINT sn_node_meta_pkey PRIMARY KEY (node_id)  DEFERRABLE INITIALLY IMMEDIATE,
  CONSTRAINT sn_node_meta_node_fk FOREIGN KEY (node_id)
        REFERENCES solarnet.sn_node (node_id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE
);

/**************************************************************************************************
 * FUNCTION solarnet.store_node_meta(timestamptz, bigint, text)
 * 
 * Add or update node metadata.
 * 
 * @param cdate the creation date to use
 * @param node the node ID
 * @param jdata the metadata to store
 */
CREATE OR REPLACE FUNCTION solarnet.store_node_meta(
	cdate solarcommon.ts, 
	node solarcommon.node_id, 
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
		UPDATE solarnet.sn_node_meta SET 
			jdata = jdata_json, 
			updated = udate
		WHERE
			node_id = node;

		-- check if the row is found
		IF FOUND THEN
			RETURN;
		END IF;
		
		-- not found so insert the row
		BEGIN
			INSERT INTO solarnet.sn_node_meta(node_id, created, updated, jdata)
			VALUES (node, cdate, udate, jdata_json);
			RETURN;
		EXCEPTION WHEN unique_violation THEN
			-- do nothing and loop
		END;
	END LOOP;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE;
