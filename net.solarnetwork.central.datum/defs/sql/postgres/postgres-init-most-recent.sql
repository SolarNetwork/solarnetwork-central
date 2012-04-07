/**************************************************************************************************
 * FUNCTION solarnet.maintain_datum_most_recent(text, bigint, text, bigint, timestamp with time zone)
 * 
 * Dynamically update a "most recent" datum table based on node and source IDs. This is used for
 * datum that is node-specific and a source ID is used to distinguish values for the same node. 
 * If the passed in d_created date is newer than the creation date for the current "most recent" 
 * datum the d_id datum ID will be assigned as the "most recent" reference. If no "most recent" 
 * row exists, a new row will be inserted using the d_id datum ID.
 * 
 * @param d_name text the datum name, which will be used to determine the table names to use
 * @param d_node_id bigint the node ID
 * @param d_source_id text the node source ID
 * @param d_id bigint the datum ID
 * @param d_created the creation date of the datum
 */
CREATE OR REPLACE FUNCTION solarnet.maintain_datum_most_recent(
		d_name TEXT, 
		d_node_id BIGINT, 
		d_source_id TEXT, 
		d_id BIGINT, 
		d_created TIMESTAMP WITH TIME ZONE)
  RETURNS void AS
$BODY$
DECLARE
	recent timestamptz := NULL;
BEGIN
	EXECUTE 'SELECT d.created FROM solarnet.sn_' || d_name || '_datum_most_recent r '
		|| 'INNER JOIN solarnet.sn_' || d_name || '_datum d ON d.id = r.datum_id '
		|| 'WHERE r.node_id = $1 AND d.source_id = $2'
	INTO recent
	USING d_node_id, d_source_id;

	IF recent IS NULL THEN
		EXECUTE 'INSERT INTO solarnet.sn_' || d_name || '_datum_most_recent (node_id, source_id, datum_id) VALUES '
			|| '($1, $2, $3)'
		USING d_node_id, d_source_id, d_id;
	ELSIF recent < d_created THEN
		EXECUTE 'UPDATE solarnet.sn_' || d_name ||'_datum_most_recent SET datum_id = $1 '
			|| 'WHERE node_id = $2 AND source_id = $3'
		USING d_id, d_node_id, d_source_id;
	END IF;

END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

/**************************************************************************************************
 * FUNCTION solarnet.maintain_loc_datum_most_recent(text, bigint, bigint, timestamp with time zone)
 * 
 * Dynamically update a "most recent" datum table based on location ID. This is used for
 * datum that is not node-specific. If the passed in d_created date is newer than the creation
 * date for the current "most recent" datum the d_id datum ID will be assigned as the "most
 * recent" reference. If no "most recent" row exists, a new row will be inserted using the d_id
 * datum ID.
 * 
 * @param d_name text the datum name, which will be used to determine the table names to use
 * @param d_loc_id bigint the location ID
 * @param d_id bigint the datum ID
 * @param d_created the creation date of the datum
 */
CREATE OR REPLACE FUNCTION solarnet.maintain_loc_datum_most_recent(
		d_name TEXT, 
		d_loc_id BIGINT, 
		d_id BIGINT, 
		d_created TIMESTAMP WITH TIME ZONE)
  RETURNS void AS
$BODY$
DECLARE
	recent timestamptz := NULL;
BEGIN
	EXECUTE 'SELECT d.created FROM solarnet.sn_' || d_name || '_datum_most_recent r '
		|| 'INNER JOIN solarnet.sn_' || d_name || '_datum d ON d.id = r.datum_id '
		|| 'WHERE r.loc_id = $1'
	INTO recent
	USING d_loc_id;

	IF recent IS NULL THEN
		EXECUTE 'INSERT INTO solarnet.sn_' || d_name || '_datum_most_recent (loc_id, datum_id) VALUES '
			|| '($1, $2)'
		USING d_loc_id, d_id;
	ELSIF recent < d_created THEN
		EXECUTE 'UPDATE solarnet.sn_' || d_name ||'_datum_most_recent SET datum_id = $1 '
			|| 'WHERE loc_id = $2'
		USING d_id, d_loc_id;
	END IF;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

/* =========================================================================
   =========================================================================
   CONSUMPTION
   =========================================================================
   ========================================================================= */

CREATE TABLE solarnet.sn_consum_datum_most_recent
(
  node_id bigint NOT NULL,
  source_id character varying(255) NOT NULL,
  datum_id bigint NOT NULL,
  CONSTRAINT sn_consum_datum_most_recent_pkey PRIMARY KEY (node_id, source_id),
  CONSTRAINT sn_consum_datum_most_recent_datum_fk FOREIGN KEY (datum_id)
      REFERENCES solarnet.sn_consum_datum (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE OR REPLACE FUNCTION solarnet.populate_consum_datum_most_recent()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM solarnet.maintain_datum_most_recent('consum', 
		NEW.node_id, NEW.source_id, NEW.id, NEW.created);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_consum_datum_most_recent
  AFTER INSERT OR UPDATE
  ON solarnet.sn_consum_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarnet.populate_consum_datum_most_recent();

/* =========================================================================
   =========================================================================
   POWER
   =========================================================================
   ========================================================================= */

CREATE TABLE solarnet.sn_power_datum_most_recent
(
  node_id bigint NOT NULL,
  source_id character varying(255) NOT NULL,
  datum_id bigint NOT NULL,
  CONSTRAINT sn_power_datum_most_recent_pkey PRIMARY KEY (node_id, source_id),
  CONSTRAINT sn_power_datum_most_recent_datum_fk FOREIGN KEY (datum_id)
      REFERENCES solarnet.sn_power_datum (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE OR REPLACE FUNCTION solarnet.populate_power_datum_most_recent()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM solarnet.maintain_datum_most_recent('power', 
		NEW.node_id, NEW.source_id, NEW.id, NEW.created);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_power_datum_most_recent
  AFTER INSERT OR UPDATE
  ON solarnet.sn_power_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarnet.populate_power_datum_most_recent();

/* =========================================================================
   =========================================================================
   HARDWARE CONTROL
   =========================================================================
   ========================================================================= */

CREATE TABLE solarnet.sn_hardware_control_datum_most_recent
(
  node_id bigint NOT NULL,
  source_id character varying(255) NOT NULL,
  datum_id bigint NOT NULL,
  CONSTRAINT sn_hardware_control_datum_most_recent_pkey PRIMARY KEY (node_id, source_id),
  CONSTRAINT sn_hardware_control_datum_most_recent_datum_fk FOREIGN KEY (datum_id)
      REFERENCES solarnet.sn_hardware_control_datum (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE OR REPLACE FUNCTION solarnet.populate_hardware_control_datum_most_recent()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM solarnet.maintain_datum_most_recent('hardware_control', 
		NEW.node_id, NEW.source_id, NEW.id, NEW.created);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_hardware_control_datum_most_recent
  AFTER INSERT OR UPDATE
  ON solarnet.sn_hardware_control_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarnet.populate_hardware_control_datum_most_recent();

/* =========================================================================
   =========================================================================
   PRICE
   =========================================================================
   ========================================================================= */

CREATE TABLE solarnet.sn_price_datum_most_recent
(
  loc_id BIGINT NOT NULL,
  datum_id BIGINT NOT NULL,
  CONSTRAINT sn_price_datum_most_recent_pkey PRIMARY KEY (loc_id),
  CONSTRAINT sn_price_datum_most_recent_datum_fk FOREIGN KEY (datum_id)
      REFERENCES solarnet.sn_price_datum (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE OR REPLACE FUNCTION solarnet.populate_price_datum_most_recent()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM solarnet.maintain_loc_datum_most_recent('price', 
		NEW.loc_id, NEW.id, NEW.created);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_price_datum_most_recent
  AFTER INSERT OR UPDATE
  ON solarnet.sn_price_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solarnet.populate_price_datum_most_recent();
