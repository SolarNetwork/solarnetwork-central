CREATE SCHEMA IF NOT EXISTS solarev;

ALTER DEFAULT PRIVILEGES IN SCHEMA solarev REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA solarev REVOKE ALL ON SEQUENCES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA solarev REVOKE ALL ON FUNCTIONS FROM PUBLIC;

CREATE SEQUENCE solarev.ocpp_system_user_seq;

CREATE TABLE solarev.ocpp_system_user (
	id					BIGINT NOT NULL DEFAULT nextval('solarev.ocpp_system_user_seq'),
	created				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id				BIGINT NOT NULL,
	username			VARCHAR(64),
	password			VARCHAR(128),
	allowed_cp			VARCHAR(255)[],
	CONSTRAINT ocpp_system_user_pk PRIMARY KEY (id),
	CONSTRAINT ocpp_system_user_unq UNIQUE (username),
	CONSTRAINT ocpp_system_user_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE SEQUENCE solarev.ocpp_authorization_seq;

CREATE TABLE solarev.ocpp_authorization (
	id					BIGINT NOT NULL DEFAULT nextval('solarev.ocpp_authorization_seq'),
	created				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id				BIGINT NOT NULL,
	token				VARCHAR(20) NOT NULL,
	enabled				BOOLEAN NOT NULL DEFAULT true,
	expires				TIMESTAMP,
	parent_id			VARCHAR(20),
	CONSTRAINT ocpp_authorization_pk PRIMARY KEY (id),
	CONSTRAINT ocpp_authorization_unq UNIQUE (user_id, token),
	CONSTRAINT ocpp_authorization_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE SEQUENCE solarev.ocpp_charge_point_seq;

CREATE TABLE solarev.ocpp_charge_point (
	id					BIGINT NOT NULL DEFAULT nextval('solarev.ocpp_charge_point_seq'),
	created				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id				BIGINT NOT NULL,
	enabled				BOOLEAN NOT NULL DEFAULT true,
	reg_status			SMALLINT NOT NULL DEFAULT 0,
	ident				VARCHAR(48) NOT NULL,
	vendor				VARCHAR(20) NOT NULL,
	model				VARCHAR(20) NOT NULL,
	serial_num			VARCHAR(25),
	box_serial_num		VARCHAR(25),
	fw_vers				VARCHAR(50),
	iccid				VARCHAR(20),
	imsi				VARCHAR(20),
	meter_type			VARCHAR(25),
	meter_serial_num	VARCHAR(25),
	conn_count			SMALLINT NOT NULL DEFAULT 0,
	CONSTRAINT ocpp_charge_point_pk PRIMARY KEY (id),
	CONSTRAINT ocpp_charge_point_unq UNIQUE (user_id, ident),
	CONSTRAINT ocpp_charge_point_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE solarev.ocpp_charge_point_conn (
	cp_id				BIGINT NOT NULL,
	conn_id				INTEGER NOT NULL,
	created				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	status				SMALLINT NOT NULL DEFAULT 0,
	error_code			SMALLINT NOT NULL DEFAULT 0,
	ts					TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	info				VARCHAR(50),
	vendor_id			VARCHAR(255),
	vendor_error		VARCHAR(50),
	CONSTRAINT ocpp_charge_point_conn_pk PRIMARY KEY (cp_id, conn_id),
	CONSTRAINT ocpp_charge_point_conn_charge_point_fk FOREIGN KEY (cp_id)
		REFERENCES solarev.ocpp_charge_point (id)
		ON DELETE CASCADE
);

CREATE SEQUENCE solarev.ocpp_charge_tx_seq
MINVALUE 1 MAXVALUE 2147483647 INCREMENT BY 1 CYCLE;

CREATE TABLE solarev.ocpp_charge_sess (
	id					uuid NOT NULL,
	created				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	auth_id				VARCHAR(20) NOT NULL,
	cp_id				BIGINT NOT NULL,
	conn_id				INTEGER NOT NULL,
	tx_id				INTEGER NOT NULL DEFAULT nextval('solarev.ocpp_charge_tx_seq'),
	ended				TIMESTAMP WITH TIME ZONE,
	end_reason			SMALLINT NOT NULL DEFAULT 0,
	end_auth_id			VARCHAR(20),
	posted				TIMESTAMP WITH TIME ZONE,
	CONSTRAINT ocpp_charge_sess_pk PRIMARY KEY (id),
	CONSTRAINT ocpp_charge_sess_charge_point_fk FOREIGN KEY (cp_id)
		REFERENCES solarev.ocpp_charge_point (id)
		ON DELETE CASCADE
);

CREATE TABLE solarev.ocpp_charge_sess_reading (
	sess_id				uuid NOT NULL,
	ts					TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	location			SMALLINT NOT NULL DEFAULT 0,
	unit 				SMALLINT NOT NULL DEFAULT 0,
	context 			SMALLINT NOT NULL DEFAULT 0,
	measurand			SMALLINT NOT NULL DEFAULT 0,
	phase				SMALLINT,
	reading 			VARCHAR(64) NOT NULL,
	CONSTRAINT ocpp_charge_sess_reading_charge_sess_fk FOREIGN KEY (sess_id)
		REFERENCES solarev.ocpp_charge_sess (id)
		ON DELETE CASCADE
);

CREATE INDEX ocpp_charge_sess_reading_sess_id_idx ON solarev.ocpp_charge_sess_reading (sess_id);
