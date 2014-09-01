CREATE TRIGGER mig_consum_datum
  BEFORE INSERT OR UPDATE
  ON solarnet.sn_consum_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.mig_consum_datum();

CREATE TRIGGER mig_power_datum
  BEFORE INSERT OR UPDATE
  ON solarnet.sn_power_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.mig_power_datum();

CREATE TRIGGER mig_hardware_control_datum
  BEFORE INSERT OR UPDATE
  ON solarnet.sn_hardware_control_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.mig_hardware_control_datum();
