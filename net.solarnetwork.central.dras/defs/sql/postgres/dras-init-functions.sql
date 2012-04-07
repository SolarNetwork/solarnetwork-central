/* ========================================================================
   General
   ======================================================================== */

/*****************************************************************************
 * FUNCTION solardras.populate_previous(table name, interval string)
 * 
 * Dynamically call the 'table name' function, passing NEW as the first
 * parameter and 'interval string' cast to an INTERVAL as the second.
 */
CREATE OR REPLACE FUNCTION solardras.populate_previous()
  RETURNS "trigger" AS
$BODY$BEGIN
	EXECUTE 'SELECT '||TG_ARGV[0]||'($1, $2::interval)' INTO NEW.previous 
		USING NEW, TG_ARGV[1];
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

/* ========================================================================
   User
   ======================================================================== */

/*****************************************************************************
 * FUNCTION solardras.dras_user_fts
 * 
 * Maintain the FTS data on the solardras.dras_user table. We don't use
 * the built-in tsvector_update_trigger because address is an ARRAY.
 */
CREATE OR REPLACE FUNCTION solardras.dras_user_maintain_fts()
  RETURNS "trigger" AS
$BODY$BEGIN
	NEW.fts_default := to_tsvector('english', 
		coalesce(NEW.username, '')
		|| ' ' ||
		coalesce(NEW.disp_name, '')
		|| ' ' ||
		coalesce(array_to_string(NEW.address, ' '), '')
		|| ' ' ||
		coalesce(NEW.vendor, '')
		);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

DROP TRIGGER IF EXISTS maintain_fts ON solardras.dras_user;
CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solardras.dras_user
  FOR EACH ROW EXECUTE PROCEDURE solardras.dras_user_maintain_fts();

/* ========================================================================
   User Group
   ======================================================================== */

DROP TRIGGER IF EXISTS maintain_fts ON solardras.dras_user_group;
CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solardras.dras_user_group
  FOR EACH ROW EXECUTE PROCEDURE 
  tsvector_update_trigger(fts_default, 'pg_catalog.english', groupname);

/* ========================================================================
   Program
   ======================================================================== */

DROP TRIGGER IF EXISTS maintain_fts ON solardras.program;
CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solardras.program
  FOR EACH ROW EXECUTE PROCEDURE 
  tsvector_update_trigger(fts_default, 'pg_catalog.english', pro_name);

/* ========================================================================
   Location
   ======================================================================== */

DROP TRIGGER IF EXISTS maintain_fts ON solardras.loc;
CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solardras.loc
  FOR EACH ROW EXECUTE PROCEDURE 
  tsvector_update_trigger(fts_default, 'pg_catalog.english', 
  	loc_name, country, region, state_prov, locality, postal_code, gxp, icp, address);

/* ========================================================================
   Event
   ======================================================================== */

DROP TRIGGER IF EXISTS maintain_fts ON solardras.program_event;
CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solardras.program_event
  FOR EACH ROW EXECUTE PROCEDURE 
  tsvector_update_trigger(fts_default, 'pg_catalog.english', event_name);

/* ========================================================================
   Meter read datum
   ======================================================================== */

CREATE OR REPLACE FUNCTION solardras.find_prev_participant_meter_datum(
	datum solardras.participant_meter_datum, 
	period interval DEFAULT interval '1 hour') 
	RETURNS bigint AS
$BODY$
	SELECT d.id
	FROM solardras.participant_meter_datum d
	WHERE d.meter_date < $1.meter_date
		AND d.meter_date >= ($1.meter_date - $2)
		AND d.par_id = $1.par_id
	ORDER BY d.meter_date DESC
	LIMIT 1;
$BODY$
  LANGUAGE 'sql' STABLE;

DROP TRIGGER IF EXISTS populate_prev_participant_meter_datum 
  ON solardras.participant_meter_datum;
CREATE TRIGGER populate_prev_participant_meter_datum
  BEFORE INSERT
  ON solardras.participant_meter_datum
  FOR EACH ROW
  EXECUTE PROCEDURE solardras.populate_previous(
  	'solardras.find_prev_participant_meter_datum', '1 hour');

/* ========================================================================
   Outbound Mail
   ======================================================================== */

/*****************************************************************************
 * FUNCTION solardras.outbound_mail_maintain_fts
 * 
 * Maintain the FTS data on the solardras.outbound_mail table. We don't use
 * the built-in tsvector_update_trigger because to_address is an ARRAY.
 */
CREATE OR REPLACE FUNCTION solardras.outbound_mail_maintain_fts()
  RETURNS "trigger" AS
$BODY$BEGIN
	NEW.fts_default := to_tsvector('english', 
		coalesce(NEW.message_id, '')
		|| ' ' ||
		coalesce(NEW.subject, '')
		|| ' ' ||
		coalesce(NEW.message, '')
		|| ' ' ||
		coalesce(array_to_string(NEW.to_address, ' '), '')
		);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

DROP TRIGGER IF EXISTS maintain_fts ON solardras.outbound_mail;
CREATE TRIGGER maintain_fts
  BEFORE INSERT OR UPDATE ON solardras.outbound_mail
  FOR EACH ROW EXECUTE PROCEDURE solardras.outbound_mail_maintain_fts();

