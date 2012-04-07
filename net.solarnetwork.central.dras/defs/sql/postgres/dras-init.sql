DROP SCHEMA IF EXISTS solardras CASCADE;
CREATE SCHEMA solardras;

CREATE SEQUENCE solardras.solardras_seq;

CREATE DOMAIN solardras.pk_i
  AS bigint
  DEFAULT nextval('solardras.solardras_seq'::regclass)
  NOT NULL;

CREATE DOMAIN solardras.pk_i_ref
  AS bigint;

CREATE DOMAIN solardras.ts
  AS timestamp with time zone
  DEFAULT CURRENT_TIMESTAMP
  NOT NULL;

/* ========================================================================
   Location
   ======================================================================== */

CREATE TABLE solardras.loc
(
  id solardras.pk_i,
  created solardras.ts,
  loc_name character varying(128),
  country character(2),
  time_zone character varying(64),
  region character varying(128),
  state_prov character varying(128),
  locality character varying(128),
  postal_code character varying(32),
  gxp character varying(128),
  icp character varying(128),
  address character varying(256),
  latitude double precision,
  longitude double precision,
  fts_default tsvector,
  CONSTRAINT loc_pkey PRIMARY KEY (id)
);

/* ========================================================================
   User
   ======================================================================== */

CREATE TABLE solardras.dras_user
(
  id solardras.pk_i,
  created solardras.ts,
  username character varying(128) NOT NULL,
  passwd character varying(64),
  disp_name character varying(128) NOT NULL,
  address character varying(128) ARRAY,
  vendor character varying(128),
  enabled boolean NOT NULL DEFAULT FALSE,
  fts_default tsvector,
  CONSTRAINT dras_user_pkey PRIMARY KEY (id),
  CONSTRAINT dras_user_username_unq UNIQUE (username)
);

CREATE INDEX dras_user_fts_default_idx ON solardras.dras_user
USING gin(fts_default);

CREATE TYPE solardras.contact_kind AS ENUM ('EMAIL', 'MOBILE', 'VOICE', 'PAGER', 'FAX');

CREATE TABLE solardras.dras_user_contact
(
  usr_id solardras.pk_i_ref NOT NULL,
  idx integer NOT NULL,
  kind solardras.contact_kind NOT NULL,
  contact character varying(128) NOT NULL,
  priority smallint,
  CONSTRAINT dras_user_contact_pkey PRIMARY KEY (usr_id, idx),
  CONSTRAINT dras_user_contact_dras_user_fk FOREIGN KEY (usr_id)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT dras_user_contact_priority_unq UNIQUE (usr_id, priority)
      DEFERRABLE INITIALLY DEFERRED,
  CONSTRAINT dras_user_contact_priority_chk CHECK
      (priority IS NULL OR priority > 0)
);

CREATE TABLE solardras.dras_role
(
  rolename character varying(64) NOT NULL,
  description text,
  CONSTRAINT dras_role_pkey PRIMARY KEY (rolename)
);

CREATE TABLE solardras.dras_user_role
(
  usr_id solardras.pk_i_ref NOT NULL,
  rolename character varying(64) NOT NULL,
  CONSTRAINT dras_user_role_pkey PRIMARY KEY (usr_id, rolename)
      DEFERRABLE INITIALLY DEFERRED,
  CONSTRAINT dras_user_contact_dras_user_fk FOREIGN KEY (usr_id)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT dras_user_role_dras_role_fk FOREIGN KEY (rolename)
      REFERENCES solardras.dras_role (rolename) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE OR REPLACE VIEW solardras.dras_auth_roles AS 
	SELECT u.username, r.rolename
	FROM solardras.dras_user_role r
	LEFT OUTER JOIN solardras.dras_user u ON u.id = r.usr_id
	UNION
	SELECT u.username, rolename
	FROM solardras.dras_user u
	CROSS JOIN unnest(ARRAY['AUTHENTICATED_USER']) AS rolename;

/* ========================================================================
   Effective
   ======================================================================== */

CREATE TABLE solardras.effective
(
  id solardras.pk_i,
  created solardras.ts,
  creator solardras.pk_i_ref NOT NULL,
  effective solardras.ts,
  CONSTRAINT effective_pkey PRIMARY KEY (id),
  CONSTRAINT effective_dras_user_fk FOREIGN KEY (creator)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX effective_effective_idx ON solardras.effective (effective);

/* ========================================================================
   User Groups
   ======================================================================== */

CREATE TABLE solardras.dras_user_group
(
  id solardras.pk_i,
  created solardras.ts,
  groupname character varying(128) NOT NULL,
  loc_id solardras.pk_i_ref,
  enabled boolean NOT NULL DEFAULT FALSE,
  fts_default tsvector,
  CONSTRAINT dras_user_group_group_pkey PRIMARY KEY (id),
  CONSTRAINT dras_user_group_groupname_unq UNIQUE (groupname),
  CONSTRAINT dras_user_group_loc_fk FOREIGN KEY (loc_id)
      REFERENCES solardras.loc (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX dras_user_group_fts_default_idx ON solardras.dras_user_group
USING gin(fts_default);

/* Defines members of a given user group. */
CREATE TABLE solardras.dras_user_group_member
(
  ugr_id solardras.pk_i_ref NOT NULL,
  usr_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT dras_user_group_member_pkey PRIMARY KEY (ugr_id, usr_id, eff_id),
  CONSTRAINT dras_user_group_member_dras_user_group_fk FOREIGN KEY (ugr_id)
      REFERENCES solardras.dras_user_group (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT dras_user_group_member_dras_user_fk FOREIGN KEY (usr_id)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT dras_user_group_member_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* ========================================================================
   Program
   ======================================================================== */

CREATE TABLE solardras.program
(
  id solardras.pk_i,
  created solardras.ts,
  creator solardras.pk_i_ref NOT NULL,
  pro_name character varying(128) NOT NULL,
  priority integer NOT NULL DEFAULT 0,
  enabled boolean NOT NULL DEFAULT FALSE,
  fts_default tsvector,
  CONSTRAINT program_pkey PRIMARY KEY (id),
  CONSTRAINT program_pro_name_unq UNIQUE (pro_name),
  CONSTRAINT program_dras_user_fk FOREIGN KEY (creator)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX program_fts_default_idx ON solardras.program
USING gin(fts_default);

/* Defines members of a given program. */
CREATE TABLE solardras.program_user
(
  pro_id solardras.pk_i_ref NOT NULL,
  usr_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_user_pkey PRIMARY KEY (pro_id, usr_id, eff_id),
  CONSTRAINT program_user_program_fk FOREIGN KEY (pro_id)
      REFERENCES solardras.program (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_user_dras_user_fk FOREIGN KEY (usr_id)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_user_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* ========================================================================
   Constraints
   ======================================================================== */

CREATE TYPE solardras.dras_constraint_filter AS ENUM ('ACCEPT', 'REJECT', 'FORCE', 'RESTRICT');

CREATE TABLE solardras.dras_constraint
(
  id solardras.pk_i,
  event_window_start time without time zone,
  event_window_end time without time zone,
  event_window_filter solardras.dras_constraint_filter NOT NULL DEFAULT 'REJECT'::solardras.dras_constraint_filter,
  max_event_dur interval,
  max_event_dur_filter solardras.dras_constraint_filter NOT NULL DEFAULT 'REJECT'::solardras.dras_constraint_filter,
  notif_window_max interval,
  notif_window_min interval,
  notif_window_filter solardras.dras_constraint_filter NOT NULL DEFAULT 'REJECT'::solardras.dras_constraint_filter,
  max_consec_days integer,
  max_consec_days_filter solardras.dras_constraint_filter NOT NULL DEFAULT 'REJECT'::solardras.dras_constraint_filter,
  blackout_filter solardras.dras_constraint_filter NOT NULL DEFAULT 'REJECT'::solardras.dras_constraint_filter,
  valid_filter solardras.dras_constraint_filter NOT NULL DEFAULT 'REJECT'::solardras.dras_constraint_filter,
  
  CONSTRAINT dras_constraint_pkey PRIMARY KEY (id),
  CONSTRAINT dras_constraint_event_window_chk CHECK 
      (event_window_start < event_window_end),
  CONSTRAINT dras_constraint_notif_window_chk CHECK 
      (notif_window_max > notif_window_min)
);

CREATE TYPE solardras.dtwindow_kind AS ENUM ('VALID', 'BLACKOUT');

CREATE TABLE solardras.dras_constraint_dtwindow
(
  con_id solardras.pk_i_ref NOT NULL,
  kind solardras.dtwindow_kind NOT NULL,
  idx integer NOT NULL,
  start_date timestamp with time zone NOT NULL,
  end_date timestamp with time zone NOT NULL,
  CONSTRAINT dras_constraint_dtwindow_pkey PRIMARY KEY (con_id, kind, idx),
  CONSTRAINT dras_constraint_dtwindow_dras_constraint_fk FOREIGN KEY (con_id)
      REFERENCES solardras.dras_constraint (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT dras_constraint_window_chk CHECK 
      (start_date < end_date)
);

/* A constraint applied at the program level, as a default for all users within the program. */
CREATE TABLE solardras.program_constraint
(
  pro_id solardras.pk_i_ref NOT NULL,
  con_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_constraint_pkey PRIMARY KEY (pro_id, con_id, eff_id),
  CONSTRAINT program_constraint_program_fk FOREIGN KEY (pro_id)
      REFERENCES solardras.program (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_constraint_dras_constraint_fk FOREIGN KEY (con_id)
      REFERENCES solardras.dras_constraint (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_constraint_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* A constraint for a user, as a default to apply to all programs the user participates in. */
CREATE TABLE solardras.user_constraint
(
  usr_id solardras.pk_i_ref NOT NULL,
  con_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT user_constraint_pkey PRIMARY KEY (usr_id, con_id, eff_id),
  CONSTRAINT user_constraint_dras_user_fk FOREIGN KEY (usr_id)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT user_constraint_dras_constraint_fk FOREIGN KEY (con_id)
      REFERENCES solardras.dras_constraint (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_constraint_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* A constraint for a specific user in a specific program. */
CREATE TABLE solardras.user_program_constraint
(
  usr_id solardras.pk_i_ref NOT NULL,
  pro_id solardras.pk_i_ref NOT NULL,
  con_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT user_program_constraint_pkey PRIMARY KEY (usr_id, pro_id, con_id, eff_id),
  CONSTRAINT user_program_constraint_dras_user_fk FOREIGN KEY (usr_id)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT user_program_constraint_program_fk FOREIGN KEY (pro_id)
      REFERENCES solardras.program (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT user_program_constraint_dras_constraint_fk FOREIGN KEY (con_id)
      REFERENCES solardras.dras_constraint (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT user_program_constraint_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* A constraint applied at the participant level as a default for all programs. */
CREATE TABLE solardras.participant_constraint
(
  par_id solardras.pk_i_ref NOT NULL,
  con_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT participant_constraint_pkey PRIMARY KEY (par_id, con_id, eff_id),
  CONSTRAINT participant_constraint_participant_fk FOREIGN KEY (par_id)
      REFERENCES solardras.participant (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_constraint_dras_constraint_fk FOREIGN KEY (con_id)
      REFERENCES solardras.dras_constraint (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_constraint_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* A constraint applied for a specific participant within a specific program. */
CREATE TABLE solardras.participant_program_constraint
(
  par_id solardras.pk_i_ref NOT NULL,
  pro_id solardras.pk_i_ref NOT NULL,
  con_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT participant_program_constraint_pkey PRIMARY KEY (par_id, pro_id, con_id, eff_id),
  CONSTRAINT participant_program_constraint_participant_fk FOREIGN KEY (par_id)
      REFERENCES solardras.participant (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_program_constraint_program_fk FOREIGN KEY (pro_id)
      REFERENCES solardras.program (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_constraint_dras_constraint_fk FOREIGN KEY (con_id)
      REFERENCES solardras.dras_constraint (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_constraint_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* ========================================================================
   Demand Response Capabilities
   ======================================================================== */

CREATE TABLE solardras.capability
(
  id solardras.pk_i,
  created solardras.ts,
  /* TODO: what is "demand response type"; turn into ENUM? */
  dr_kind text,
  max_power bigint,
  max_energy bigint,
  max_var bigint,
  contracted_power bigint,
  CONSTRAINT capability_pkey PRIMARY KEY (id)
);

/* ========================================================================
   Verification meth
   ======================================================================== */

CREATE TABLE solardras.verification_method
(
  id solardras.pk_i,
  created solardras.ts,
  method_name character varying(128),
  CONSTRAINT verification_method_pkey PRIMARY KEY (id)
);

/* ========================================================================
   Participant
   ======================================================================== */

CREATE TYPE solardras.client_kind AS ENUM ('SMART', 'SIMPLE');

/* A DRAS client, e.g. SolarNode. */
CREATE TABLE solardras.participant
(
  id solardras.pk_i,
  created solardras.ts,
  creator solardras.pk_i_ref NOT NULL,
  usr_id solardras.pk_i_ref NOT NULL,
  kind solardras.client_kind NOT NULL DEFAULT 'SMART',
  loc_id solardras.pk_i_ref NOT NULL,
  cap_id solardras.pk_i_ref,
  confirmed boolean NOT NULL DEFAULT FALSE,
  ver_id solardras.pk_i_ref,
  enabled boolean NOT NULL DEFAULT FALSE,
  CONSTRAINT participant_pkey PRIMARY KEY (id),
  CONSTRAINT participant_loc_unq UNIQUE (loc_id),
  CONSTRAINT participant_capability_unq UNIQUE (cap_id),
  CONSTRAINT participant_creator_dras_user_fk FOREIGN KEY (creator)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_user_id_dras_user_fk FOREIGN KEY (usr_id)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_loc_fk FOREIGN KEY (loc_id)
      REFERENCES solardras.loc (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_capability_fk FOREIGN KEY (cap_id)
      REFERENCES solardras.capability (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_verification_method_fk FOREIGN KEY (ver_id)
      REFERENCES solardras.verification_method (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* Effective set of participants of a program. */
CREATE TABLE solardras.program_participant
(
  pro_id solardras.pk_i_ref NOT NULL,
  par_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_participant_pkey PRIMARY KEY (pro_id, par_id, eff_id),
  CONSTRAINT program_participant_program_fk FOREIGN KEY (pro_id)
      REFERENCES solardras.program (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_participant_participant_fk FOREIGN KEY (par_id)
      REFERENCES solardras.participant (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_participant_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* ========================================================================
   Participant Group
   ======================================================================== */

CREATE TABLE solardras.participant_group
(
  id solardras.pk_i,
  created solardras.ts,
  creator solardras.pk_i_ref NOT NULL,
  loc_id solardras.pk_i_ref NOT NULL,
  cap_id solardras.pk_i_ref,
  ver_id solardras.pk_i_ref,
  confirmed boolean NOT NULL DEFAULT FALSE,
  enabled boolean NOT NULL DEFAULT FALSE,
  CONSTRAINT participant_group_pkey PRIMARY KEY (id),
  CONSTRAINT participant_group_loc_unq UNIQUE (loc_id),
  CONSTRAINT participant_group_capability_unq UNIQUE (cap_id),
  CONSTRAINT participant_group_dras_user_fk FOREIGN KEY (creator)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_group_loc_fk FOREIGN KEY (loc_id)
      REFERENCES solardras.loc (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_group_capability_fk FOREIGN KEY (cap_id)
      REFERENCES solardras.capability (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_group_capability_verification_method_fk FOREIGN KEY (ver_id)
      REFERENCES solardras.verification_method (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* Effective set of participants of a participant group. */
CREATE TABLE solardras.participant_group_member
(
  pgr_id solardras.pk_i_ref NOT NULL,
  par_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT participant_group_member_pkey PRIMARY KEY (pgr_id, par_id, eff_id),
  CONSTRAINT participant_group_member_participant_group_fk FOREIGN KEY (pgr_id)
      REFERENCES solardras.participant_group (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_group_member_participant_fk FOREIGN KEY (par_id)
      REFERENCES solardras.participant (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_group_member_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* ========================================================================
   Event Rules
   ======================================================================== */

CREATE TYPE solardras.event_rule_kind AS ENUM (
	'PRICE_ABSOLUTE', 
	'PRICE_RELATIVE',
	'PRICE_MULTIPLE',
	'LOAD_LEVEL',
	'LOAD_AMOUNT',
	'LOAD_PERCENTAGE',
	'GRID_RELIABILITY'
);

CREATE TYPE solardras.schedule_kind AS ENUM ('NONE', 'DYNAMIC', 'STATIC');

CREATE TABLE solardras.event_rule
(
  id solardras.pk_i,
  created solardras.ts,
  creator solardras.pk_i_ref NOT NULL,
  kind solardras.event_rule_kind NOT NULL DEFAULT 'LOAD_AMOUNT',
  rule_name character varying(128) NOT NULL,
  min_value double precision,
  max_value double precision,
  schedule_kind solardras.schedule_kind NOT NULL DEFAULT 'NONE',
  CONSTRAINT event_rule_pkey PRIMARY KEY (id),
  CONSTRAINT participant_dras_user_fk FOREIGN KEY (creator)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE solardras.event_rule_schedule
(
  evr_id solardras.pk_i_ref NOT NULL,
  event_offset interval NOT NULL,
  CONSTRAINT event_rule_schedule_pkey PRIMARY KEY (evr_id, event_offset),
  CONSTRAINT event_rule_schedule_event_rule_fk FOREIGN KEY (evr_id)
      REFERENCES solardras.event_rule (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE solardras.event_rule_enum
(
  evr_id solardras.pk_i_ref NOT NULL,
  target_value double precision NOT NULL,
  CONSTRAINT event_rule_enum_pkey PRIMARY KEY (evr_id, target_value),
  CONSTRAINT event_rule_enum_event_rule_fk FOREIGN KEY (evr_id)
      REFERENCES solardras.event_rule (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Event rules defined at the program level: define what rules can be applied to
   events created for that program. */
CREATE TABLE solardras.program_event_rule
(
  pro_id solardras.pk_i_ref NOT NULL,
  evr_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_event_rule_pkey PRIMARY KEY (pro_id, evr_id, eff_id),
  CONSTRAINT program_event_rule_program_fk FOREIGN KEY (pro_id)
      REFERENCES solardras.program (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_rule_event_rule_fk FOREIGN KEY (evr_id)
      REFERENCES solardras.event_rule (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_rule_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* ========================================================================
   Event Target
   ======================================================================== */

CREATE TABLE solardras.event_target
(
  id solardras.pk_i,
  created solardras.ts,
  evr_id solardras.pk_i_ref NOT NULL,
  end_offset interval,
  CONSTRAINT event_target_pkey PRIMARY KEY (id),
  CONSTRAINT event_target_event_rule_fk FOREIGN KEY (evr_id)
      REFERENCES solardras.event_rule (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE solardras.event_target_value
(
  eta_id solardras.pk_i_ref NOT NULL,
  event_offset interval NOT NULL,
  target_value double precision NOT NULL,
  CONSTRAINT event_target_value_pkey PRIMARY KEY (eta_id, event_offset),
  CONSTRAINT event_target_value_event_target_fk FOREIGN KEY (eta_id)
      REFERENCES solardras.event_target (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
	
);

/* TODO: event_target_users, event_target_groups? */

/* ========================================================================
   Program Event
   ======================================================================== */

CREATE TABLE solardras.program_event
(
  id solardras.pk_i,
  created solardras.ts,
  creator solardras.pk_i_ref NOT NULL,
  pro_id solardras.pk_i_ref NOT NULL,
  event_name character varying(128),
  initiator character varying(128),
  notif_date timestamp with time zone NOT NULL,
  start_date timestamp with time zone NOT NULL,
  end_date timestamp with time zone NOT NULL,
  /* TODO: bidding info */
  enabled boolean NOT NULL DEFAULT FALSE,
  test boolean NOT NULL DEFAULT FALSE,
  fts_default tsvector,
  CONSTRAINT program_event_pkey PRIMARY KEY (id),
  CONSTRAINT program_event_dras_user_fk FOREIGN KEY (creator)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_program_fk FOREIGN KEY (pro_id)
      REFERENCES solardras.program (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_time_chk CHECK
      (notif_date < start_date AND start_date < end_date)
);

CREATE INDEX program_event_fts_default_idx ON solardras.program_event
USING gin(fts_default);

/* Unordered set of users within the event */
CREATE TABLE solardras.program_event_user
(
  evt_id solardras.pk_i_ref NOT NULL,
  usr_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_event_user_pkey PRIMARY KEY (evt_id, usr_id, eff_id),
  CONSTRAINT program_event_user_program_event_fk FOREIGN KEY (evt_id)
      REFERENCES solardras.program_event (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_user_dras_user_fk FOREIGN KEY (usr_id)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_user_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* Unordered set of user groups within the event */
CREATE TABLE solardras.program_event_user_group
(
  evt_id solardras.pk_i_ref NOT NULL,
  ugr_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_event_user_group_pkey PRIMARY KEY (evt_id, ugr_id, eff_id),
  CONSTRAINT program_event_user_group_program_event_fk FOREIGN KEY (evt_id)
      REFERENCES solardras.program_event (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_user_group_dras_user_group_fk FOREIGN KEY (ugr_id)
      REFERENCES solardras.dras_user_group (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_user_group_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* Unordered set of users within the event */
CREATE TABLE solardras.program_event_participant
(
  evt_id solardras.pk_i_ref NOT NULL,
  par_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_event_participant_pkey PRIMARY KEY (evt_id, par_id, eff_id),
  CONSTRAINT program_event_participant_program_event_fk FOREIGN KEY (evt_id)
      REFERENCES solardras.program_event (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_participant_participant_fk FOREIGN KEY (par_id)
      REFERENCES solardras.participant (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_participant_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* Unordered set of participant groups within the event */
CREATE TABLE solardras.program_event_participant_group
(
  evt_id solardras.pk_i_ref NOT NULL,
  pgr_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_event_participant_group_pkey PRIMARY KEY (evt_id, pgr_id, eff_id),
  CONSTRAINT program_event_participant_group_program_event_fk FOREIGN KEY (evt_id)
      REFERENCES solardras.program_event (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_participant_group_participant_group_fk FOREIGN KEY (pgr_id)
      REFERENCES solardras.participant_group (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_participant_group_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* Unordered set of locations within the event */
CREATE TABLE solardras.program_event_loc
(
  evt_id solardras.pk_i_ref NOT NULL,
  loc_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_event_loc_pkey PRIMARY KEY (evt_id, loc_id, eff_id),
  CONSTRAINT program_event_loc_program_event_fk FOREIGN KEY (evt_id)
      REFERENCES solardras.program_event (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_loc_loc_fk FOREIGN KEY (loc_id)
      REFERENCES solardras.loc (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_loc_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* Unordered set of targets within the event */
CREATE TABLE solardras.program_event_target
(
  evt_id solardras.pk_i_ref NOT NULL,
  eta_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_event_target_pkey PRIMARY KEY (evt_id, eta_id, eff_id),
  CONSTRAINT program_event_target_program_event_fk FOREIGN KEY (evt_id)
      REFERENCES solardras.program_event (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_target_event_target_fk FOREIGN KEY (eta_id)
      REFERENCES solardras.event_target (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_target_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE solardras.program_event_execution
(
  id solardras.pk_i,
  created solardras.ts,
  evt_id solardras.pk_i_ref NOT NULL,
  execution_key character varying(256),
  execution_date timestamp with time zone,
  CONSTRAINT program_event_execution_pkey PRIMARY KEY (id),
  CONSTRAINT program_event_target_program_event_fk FOREIGN KEY (evt_id)
      REFERENCES solardras.program_event (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


/* ========================================================================
   Feedback
   ======================================================================== */

CREATE TABLE solardras.feedback
(
  id solardras.pk_i,
  created solardras.ts,
  creator solardras.pk_i_ref NOT NULL,
  fname character varying(64) NOT NULL,
  fvalue text NOT NULL,
  CONSTRAINT feedback_pkey PRIMARY KEY (id)
);

CREATE TABLE solardras.program_feedback
(
  pro_id solardras.pk_i_ref NOT NULL,
  fbk_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_feedback_pkey PRIMARY KEY (pro_id, fbk_id),
  CONSTRAINT program_feedback_program_fk FOREIGN KEY (pro_id)
      REFERENCES solardras.program (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_feedback_feedback_fk FOREIGN KEY (fbk_id)
      REFERENCES solardras.feedback (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE solardras.user_feedback
(
  usr_id solardras.pk_i_ref NOT NULL,
  fbk_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT user_feedback_pkey PRIMARY KEY (usr_id, fbk_id),
  CONSTRAINT user_feedback_dras_user_fk FOREIGN KEY (usr_id)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT user_feedback_feedback_fk FOREIGN KEY (fbk_id)
      REFERENCES solardras.feedback (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE solardras.participant_feedback
(
  par_id solardras.pk_i_ref NOT NULL,
  fbk_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT participant_feedback_pkey PRIMARY KEY (par_id, fbk_id),
  CONSTRAINT participant_feedback_participant_fk FOREIGN KEY (par_id)
      REFERENCES solardras.participant (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_feedback_feedback_fk FOREIGN KEY (fbk_id)
      REFERENCES solardras.feedback (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE solardras.event_feedback
(
  evt_id solardras.pk_i_ref NOT NULL,
  fbk_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT event_feedback_pkey PRIMARY KEY (evt_id, fbk_id),
  CONSTRAINT event_feedback_event_fk FOREIGN KEY (evt_id)
      REFERENCES solardras.program_event (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT event_feedback_feedback_fk FOREIGN KEY (fbk_id)
      REFERENCES solardras.feedback (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* ========================================================================
   Opt Out
   ======================================================================== */

CREATE TABLE solardras.user_opt_out
(
  id solardras.pk_i,
  usr_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT user_opt_out_pkey PRIMARY KEY (id),
  CONSTRAINT user_opt_out_dras_user_fk FOREIGN KEY (usr_id)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_event_version_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE solardras.user_opt_out_schedule
(
  uoo_id solardras.pk_i_ref NOT NULL,
  start_date timestamp with time zone NOT NULL,
  end_date timestamp with time zone NOT NULL,
  CONSTRAINT user_opt_out_schedule_pkey PRIMARY KEY (uoo_id, start_date, end_date),
  CONSTRAINT user_opt_out_schedule_user_opt_out_fk FOREIGN KEY (uoo_id)
      REFERENCES solardras.user_opt_out (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT user_opt_out_schedule_date_chk CHECK
      (start_date < end_date)
);

CREATE TABLE solardras.program_user_opt_out
(
  pro_id solardras.pk_i_ref NOT NULL,
  uoo_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT program_user_opt_out_pkey PRIMARY KEY (pro_id, uoo_id),
  CONSTRAINT program_user_opt_out_program_fk FOREIGN KEY (pro_id)
      REFERENCES solardras.program (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT program_user_opt_out_user_opt_out_fk FOREIGN KEY (uoo_id)
      REFERENCES solardras.user_opt_out (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE solardras.event_user_opt_out
(
  evt_id solardras.pk_i_ref NOT NULL,
  uoo_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT event_user_opt_out_pkey PRIMARY KEY (evt_id, uoo_id),
  CONSTRAINT event_user_opt_out_event_fk FOREIGN KEY (evt_id)
      REFERENCES solardras.program_event (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT event_user_opt_out_user_opt_out_fk FOREIGN KEY (uoo_id)
      REFERENCES solardras.user_opt_out (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* ========================================================================
   Meter read datum
   ======================================================================== */

CREATE TABLE solardras.participant_meter_datum
(
  id solardras.pk_i,
  created solardras.ts,
  par_id solardras.pk_i_ref NOT NULL,
  meter_date timestamp with time zone NOT NULL,
  meter_energy bigint NOT NULL, /* watt hours */
  previous solardras.pk_i_ref,
  CONSTRAINT participant_meter_datum_pkey PRIMARY KEY (id),
  CONSTRAINT participant_meter_datum_unq UNIQUE (meter_date, par_id),
  CONSTRAINT participant_meter_datum_participant_fk FOREIGN KEY (par_id)
      REFERENCES solardras.participant (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
	OIDS = FALSE,
	FILLFACTOR = 100
);

/* ========================================================================
   Pricing
   ======================================================================== */

CREATE TABLE solardras.fee
(
  id solardras.pk_i,
  created solardras.ts,
  currency varchar(10) NOT NULL,
  establish bigint NOT NULL, /* one off */
  available bigint NOT NULL, /* per availability_period */
  available_period interval NOT NULL,
  event bigint NOT NULL, /* per event */
  delivery bigint NOT NULL, /* per watt hour */
  cancel bigint NOT NULL, /* per event */
  CONSTRAINT fee_pkey PRIMARY KEY (id)
);

CREATE TABLE solardras.user_fee
(
  usr_id solardras.pk_i_ref NOT NULL,
  fee_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT user_fee_pkey PRIMARY KEY (usr_id, eff_id),
  CONSTRAINT user_fee_dras_user_fk FOREIGN KEY (usr_id)
      REFERENCES solardras.dras_user (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT user_fee_fee_fk FOREIGN KEY (fee_id)
      REFERENCES solardras.fee (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT user_fee_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE solardras.participant_fee
(
  par_id solardras.pk_i_ref NOT NULL,
  fee_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT participant_fee_pkey PRIMARY KEY (par_id, eff_id),
  CONSTRAINT participant_fee_participant_fk FOREIGN KEY (par_id)
      REFERENCES solardras.participant (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_fee_fee_fk FOREIGN KEY (fee_id)
      REFERENCES solardras.fee (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_fee_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE solardras.participant_group_fee
(
  pgr_id solardras.pk_i_ref NOT NULL,
  fee_id solardras.pk_i_ref NOT NULL,
  eff_id solardras.pk_i_ref NOT NULL,
  CONSTRAINT participant_group_fee_pkey PRIMARY KEY (pgr_id, eff_id),
  CONSTRAINT participant_group_fee_participant_group_fk FOREIGN KEY (pgr_id)
      REFERENCES solardras.participant_group (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_group_fee_fee_fk FOREIGN KEY (fee_id)
      REFERENCES solardras.fee (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT participant_group_fee_effective_fk FOREIGN KEY (eff_id)
      REFERENCES solardras.effective (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* ========================================================================
   Outbound Mail
   ======================================================================== */

create table solardras.outbound_mail
(
  id solardras.pk_i,
  created solardras.ts,
  creator solardras.pk_i_ref NOT NULL,
  to_address character varying(256) ARRAY NOT NULL,
  message_id character varying(256), 
  subject character varying(256),
  message text,
  fts_default tsvector,
  CONSTRAINT outbound_mail_pkey PRIMARY KEY (id)
);

CREATE INDEX outbound_mail_fts_default_idx ON solardras.outbound_mail
USING gin(fts_default);
