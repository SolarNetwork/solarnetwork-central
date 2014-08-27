CREATE OR REPLACE FUNCTION solardatum.mig_consum_datum(datum solarnet.sn_consum_datum)
  RETURNS void AS
$BODY$
DECLARE
	jtext TEXT;
	src TEXT;
BEGIN
	src := datum.source_id;
	IF src = '' THEN
		src := 'Consumption';
	END IF;

	jtext :=  '{';
	IF datum.watts IS NOT NULL THEN
		jtext := jtext || '"i":{"watts":' || datum.watts || '}';
	END IF;
	IF datum.watt_hour IS NOT NULL THEN
		IF length(jtext) > 1 THEN
			jtext := jtext || ',';
		END IF;
		jtext := jtext || '"a":{"watt_hours":' || datum.watt_hour || '}';
	END IF;
	jtext := jtext || '}';

	BEGIN
		INSERT INTO solardatum.da_datum(
			ts, node_id, source_id, posted, jdata)
		VALUES (
			datum.created, 
			datum.node_id,
			src, 
			datum.posted,
			jtext::json
		);
	EXCEPTION WHEN unique_violation THEN
		UPDATE solardatum.da_datum SET
			jdata = jtext::json
		WHERE 
			node_id = datum.node_id
			AND source_id = src
			AND ts = datum.created;
	END;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solardatum.mig_consum_datum()
  RETURNS trigger AS
$BODY$BEGIN
	PERFORM solardatum.mig_consum_datum(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE TRIGGER mig_consum_datum
  BEFORE INSERT OR UPDATE
  ON solarnet.sn_consum_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.mig_consum_datum();

CREATE OR REPLACE FUNCTION solardatum.mig_power_datum(datum solarnet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	jtext TEXT;
	src TEXT;
BEGIN
	src := datum.source_id;
	IF src = '' THEN
		src := 'Power';
	END IF;
	
	jtext :=  '{';
	IF datum.watts IS NOT NULL THEN
		jtext := jtext || '"i":{"watts":' || datum.watts || '}';
	END IF;
	IF datum.watt_hour IS NOT NULL THEN
		IF length(jtext) > 1 THEN
			jtext := jtext || ',';
		END IF;
		jtext := jtext || '"a":{"watt_hours":' || datum.watt_hour || '}';
	END IF;
	jtext := jtext || '}';

	BEGIN
		INSERT INTO solardatum.da_datum(
			ts, node_id, source_id, posted, jdata)
		VALUES (
			datum.created, 
			datum.node_id,
			src, 
			datum.posted,
			jtext::json
		);
	EXCEPTION WHEN unique_violation THEN
		UPDATE solardatum.da_datum SET
			jdata = jtext::json
		WHERE 
			node_id = datum.node_id
			AND source_id = src
			AND ts = datum.created;
	END;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solardatum.mig_power_datum()
  RETURNS trigger AS
$BODY$BEGIN
	PERFORM solardatum.mig_power_datum(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE TRIGGER mig_power_datum
  BEFORE INSERT OR UPDATE
  ON solarnet.sn_power_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.mig_power_datum();

CREATE OR REPLACE FUNCTION solardatum.mig_hardware_control_datum(datum solarnet.sn_hardware_control_datum)
  RETURNS void AS
$BODY$
DECLARE
	jtext TEXT;
	src TEXT;
	prop TEXT;
BEGIN
	-- The source_id value could be in form source;prop where 'prop' is a property name.
	-- If this is the case, we store only 'source' as the source_id, and name the JSON key the value of 'prop'.
	-- If 'prop' is not defined, we name the JSON key 'val'.
	src := split_part(datum.source_id, ';', 1);
	prop := substring(datum.source_id from E'[^;"]*$');
	IF src = '' THEN
		src := 'HardwareControl';
	END IF;
	
	jtext :=  '{';
	IF datum.int_val IS NOT NULL OR datum.float_val IS NOT NULL THEN
		jtext := jtext || '"s":{"';
		IF length(prop) > 0 THEN
			jtext := jtext || prop;
		ELSE
			jtext := jtext || 'val';
		END IF;
		jtext := jtext || '":';
		IF datum.int_val IS NOT NULL THEN
			jtext := jtext || datum.int_val::text;
		ELSE
			jtext := jtext || datum.float_val::text;
		END IF;
		jtext := jtext || '}';
	END IF;
	jtext := jtext || '}';

	BEGIN
		INSERT INTO solardatum.da_datum(
			ts, node_id, source_id, posted, jdata)
		VALUES (
			datum.created, 
			datum.node_id,
			src, 
			datum.created,
			jtext::json
		);
	EXCEPTION WHEN unique_violation THEN
		UPDATE solardatum.da_datum SET
			jdata = jtext::json
		WHERE 
			node_id = datum.node_id
			AND source_id = src
			AND ts = datum.created;
	END;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solardatum.mig_hardware_control_datum()
  RETURNS trigger AS
$BODY$BEGIN
	PERFORM solardatum.mig_hardware_control_datum(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE TRIGGER mig_hardware_control_datum
  BEFORE INSERT OR UPDATE
  ON solarnet.sn_hardware_control_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.mig_hardware_control_datum();
