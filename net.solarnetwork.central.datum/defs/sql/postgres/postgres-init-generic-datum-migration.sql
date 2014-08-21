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

	<<insert_update>>
	LOOP
		UPDATE solardatum.sn_datum SET
			jdata = jtext::json
		WHERE 
			node_id = datum.node_id
			AND source_id = src
			AND ts = datum.created;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solardatum.sn_datum (
			ts, node_id, source_id, posted, jdata)
		VALUES (
			datum.created, 
			datum.node_id,
			src, 
			datum.posted,
			jtext::json
		);
		EXIT insert_update;
	END LOOP insert_update;
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

	<<insert_update>>
	LOOP
		UPDATE solardatum.sn_datum SET
			jdata = jtext::json
		WHERE 
			node_id = datum.node_id
			AND source_id = src
			AND ts = datum.created;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO solardatum.sn_datum(
			ts, node_id, source_id, posted, jdata)
		VALUES (
			datum.created, 
			datum.node_id,
			src, 
			datum.posted,
			jtext::json
		);
		EXIT insert_update;
	END LOOP insert_update;
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

