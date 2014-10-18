CREATE OR REPLACE FUNCTION solardatum.mig_day_datum(datum solarnet.sn_day_datum)
  RETURNS void AS
$BODY$
DECLARE
	jtext TEXT;
	src TEXT;
	loc_tz TEXT;
	loc solarcommon.loc_id;
	tstamp TIMESTAMP WITH TIME ZONE;
BEGIN
	jtext :=  '{"s":{"sunrise":"' || to_char(datum.sunrise, 'HH24:MI') 
		|| '","sunset":"' || to_char(datum.sunset, 'HH24:MI') 
		|| '"}}';

	SELECT ws.sname || ' Day', l.time_zone, l.id
	FROM solarnet.sn_weather_loc wl
	INNER JOIN solarnet.sn_weather_source ws ON ws.id = wl.source_id
	INNER JOIN solarnet.sn_loc l ON l.id = wl.loc_id
	WHERE wl.id = datum.loc_id
	INTO src, loc_tz, loc;

	tstamp := datum.day at time zone loc_tz at time zone loc_tz;

	BEGIN
		INSERT INTO solardatum.da_loc_datum(
			ts, loc_id, source_id, posted, jdata)
		VALUES (
			tstamp,
			loc,
			src, 
			datum.created,
			jtext::json
		);
	EXCEPTION WHEN unique_violation THEN
		UPDATE solardatum.da_loc_datum SET
			jdata = jtext::json
		WHERE 
			loc_id = loc
			AND source_id = src
			AND ts = tstamp;
	END;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solardatum.mig_day_datum()
  RETURNS trigger AS
$BODY$BEGIN
	PERFORM solardatum.mig_day_datum(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solardatum.mig_day_datum()
  RETURNS trigger AS
$BODY$BEGIN
	PERFORM solardatum.mig_day_datum(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solardatum.mig_weather_datum(datum solarnet.sn_weather_datum)
  RETURNS void AS
$BODY$
DECLARE
	jtext TEXT;
	src TEXT;
	loc_tz TEXT;
	loc solarcommon.loc_id;
BEGIN
	jtext :=  '{"i":{"temp":' || datum.temperature;
	
	IF datum.humidity IS NOT NULL THEN
		jtext := jtext || ',"humidity":' || datum.humidity;
	END IF;

	IF datum.uv_index IS NOT NULL THEN
		jtext := jtext || ',"uvIndex":' || datum.uv_index;
	END IF;

	IF datum.dew IS NOT NULL THEN
		jtext := jtext || ',"dew":' || datum.dew;
	END IF;

	IF datum.bar IS NOT NULL THEN
		jtext := jtext || ',"bar":' || datum.bar;
	END IF;

	IF datum.visibility IS NOT NULL AND datum.visibility::double precision < 100 THEN
		jtext := jtext || ',"visibility":' || datum.visibility::double precision;
	END IF;

	jtext := jtext || '}';
	
	IF datum.sky IS NOT NULL OR datum.bar_dir IS NOT NULL THEN
		jtext := jtext || ',"s":{';
		IF datum.sky IS NOT NULL THEN
			jtext := jtext || '"sky":"' || datum.sky || '"';
		END IF;
		IF datum.bar_dir IS NOT NULL THEN
			IF datum.sky IS NOT NULL THEN
				jtext := jtext || ',';
			END IF;
			jtext := jtext || '"barDir":"' || datum.bar_dir || '"';
		END IF;
		jtext := jtext || '}';
	END IF;
	
	jtext := jtext || '}';

	SELECT ws.sname, l.time_zone, l.id
	FROM solarnet.sn_weather_loc wl
	INNER JOIN solarnet.sn_weather_source ws ON ws.id = wl.source_id
	INNER JOIN solarnet.sn_loc l ON l.id = wl.loc_id
	WHERE wl.id = datum.loc_id
	INTO src, loc_tz, loc;

	BEGIN
		INSERT INTO solardatum.da_loc_datum(
			ts, loc_id, source_id, posted, jdata)
		VALUES (
			datum.info_date,
			loc,
			src, 
			datum.created,
			jtext::json
		);
	EXCEPTION WHEN unique_violation THEN
		UPDATE solardatum.da_loc_datum SET
			jdata = jtext::json
		WHERE 
			loc_id = loc
			AND source_id = src
			AND ts = datum.info_date;
	END;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solardatum.mig_weather_datum()
  RETURNS trigger AS
$BODY$BEGIN
	PERFORM solardatum.mig_weather_datum(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solardatum.mig_price_datum(datum solarnet.sn_price_datum)
  RETURNS void AS
$BODY$
DECLARE
	jtext TEXT;
	src TEXT;
	loc_tz TEXT;
	loc solarcommon.loc_id;
BEGIN
	IF datum.price IS NULL THEN
		RETURN;
	END IF;
	
	jtext :=  '{"i":{"price":' || datum.price || '}}';
	
	SELECT ps.sname, l.time_zone, l.id
	FROM solarnet.sn_price_loc pl
	INNER JOIN solarnet.sn_price_source ps ON ps.id = pl.source_id
	INNER JOIN solarnet.sn_loc l ON l.id = pl.loc_id
	WHERE pl.id = datum.loc_id
	INTO src, loc_tz, loc;

	BEGIN
		INSERT INTO solardatum.da_loc_datum(
			ts, loc_id, source_id, posted, jdata)
		VALUES (
			datum.created,
			loc,
			src, 
			datum.posted,
			jtext::json
		);
	EXCEPTION WHEN unique_violation THEN
		UPDATE solardatum.da_loc_datum SET
			jdata = jtext::json
		WHERE 
			loc_id = loc
			AND source_id = src
			AND ts = datum.created;
	END;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solardatum.mig_price_datum()
  RETURNS trigger AS
$BODY$BEGIN
	PERFORM solardatum.mig_price_datum(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

