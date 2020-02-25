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
