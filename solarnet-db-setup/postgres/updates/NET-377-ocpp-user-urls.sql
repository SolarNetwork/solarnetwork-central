/**
 * Generate a random string between a given length, using only basic ASCII characters.
 *
 * The generated value will contain only ASCII alphanumeric values, excluding some
 * common homographs like 0 and O.
 *
 * @param min_len the minimum character length to generate (inclusive)
 * @param max_len the maximum character length to generate (inclusive)
 * @returns the random string value
 */
CREATE OR REPLACE FUNCTION solarcommon.short_id(min_len INTEGER, max_len INTEGER)
RETURNS text LANGUAGE plpgsql AS
$$
DECLARE
	sid TEXT := '';
	len INTEGER := floor(random() * (max_len - min_len + 1) + min_len);
BEGIN
	LOOP
		-- generate random string, then strip out non-letter and common homographs
		sid := sid || translate(encode(gen_random_bytes(len - char_length(sid)), 'base64'), '/+0OI1l=', '');

    	IF char_length(sid) > max_len THEN
    		sid :=  SUBSTRING(sid FROM 1 FOR min_len);
    	END IF;


    	EXIT WHEN char_length(sid) >= min_len;
	END LOOP;

	RETURN sid;
END
$$;


/**
 * Assign an `hid` text column a short unique value.
 *
 * The `solarcommon.short_id()` function will be used to generate a short text ID of at least
 * 8 characters. After every 100 unsuccesful attempts to generate a unique value, the length will
 * be increased by 1.
 *
 * NOTE it is still possible for the generated value to collide, if two INSERTs at the same time
 * generate the same value. The application will have to deal with that possibility, i.e. retry
 * the INSERT that fails from a duplicate value constraint violation.
 *
 * If the row to insert already has a non-NULL `hid` value, that will be preserved and no random
 * value will be generated.
 */
CREATE OR REPLACE FUNCTION solarcommon.assign_human_id()
RETURNS TRIGGER LANGUAGE plpgsql AS
$$
DECLARE
	qry TEXT := format('SELECT TRUE FROM %I.%I WHERE hid = $1', TG_TABLE_SCHEMA, TG_TABLE_NAME);
	hid TEXT := '';
	len INTEGER := 8;
	itr INTEGER := 0;
	found BOOL;
BEGIN
	IF NEW.hid IS NULL THEN
		LOOP
			-- if we keep failing to find unique key, increase key length
			itr := itr + 1;
			IF itr % 100 = 0 THEN
				len := len + 1;
			END IF;

			-- generate short ID
			hid := solarcommon.short_id(len, len);

			-- lookup existing key
			EXECUTE qry INTO found USING hid;

			EXIT WHEN found IS NULL;
		END LOOP;

		NEW.hid = hid;
	END IF;

	RETURN NEW;
END
$$;

ALTER TABLE solarev.ocpp_user_settings ADD COLUMN hid TEXT;

UPDATE solarev.ocpp_user_settings SET hid = solarcommon.short_id(8,8);

ALTER TABLE solarev.ocpp_user_settings ADD CONSTRAINT ocpp_user_settings_hid_unq UNIQUE(hid);

CREATE TRIGGER ocpp_user_settings_genhid BEFORE INSERT ON solarev.ocpp_user_settings
FOR EACH ROW EXECUTE PROCEDURE solarcommon.assign_human_id();

ALTER TABLE solarev.ocpp_user_settings ALTER COLUMN hid SET NOT NULL;
