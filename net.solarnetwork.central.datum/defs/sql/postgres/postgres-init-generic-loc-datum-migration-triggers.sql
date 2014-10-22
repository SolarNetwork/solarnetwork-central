CREATE TRIGGER mig_day_datum
  BEFORE INSERT OR UPDATE
  ON solarnet.sn_day_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.mig_day_datum();

CREATE TRIGGER mig_price_datum
  BEFORE INSERT OR UPDATE
  ON solarnet.sn_price_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.mig_price_datum();

CREATE TRIGGER mig_weather_datum
  BEFORE INSERT OR UPDATE
  ON solarnet.sn_weather_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardatum.mig_weather_datum();
