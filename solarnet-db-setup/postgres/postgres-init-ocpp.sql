CREATE SEQUENCE solarev.ocpp_system_user_seq;

CREATE TABLE solarev.ocpp_system_user (
	id					BIGINT NOT NULL DEFAULT nextval('solarev.ocpp_system_user_seq'),
	created				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id				BIGINT NOT NULL,
	username			VARCHAR(64),
	password			VARCHAR(128),
	allowed_cp			VARCHAR(48)[],
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
	node_id				BIGINT NOT NULL,
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
	CONSTRAINT ocpp_charge_point_user_node_fk FOREIGN KEY (user_id, node_id)
		REFERENCES solaruser.user_node (user_id, node_id) MATCH SIMPLE
		ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE solarev.ocpp_charge_point_conn (
	cp_id				BIGINT NOT NULL,
	conn_id				INTEGER NOT NULL,
	created				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	status				SMALLINT NOT NULL DEFAULT 0,
	error_code			SMALLINT,
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

CREATE TABLE solarev.ocpp_user_settings (
	user_id				BIGINT NOT NULL,
	created				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	pub_in				BOOLEAN NOT NULL DEFAULT TRUE,
	pub_flux			BOOLEAN NOT NULL DEFAULT TRUE,
	source_id_tmpl		VARCHAR(255),
	CONSTRAINT ocpp_user_settings_pk PRIMARY KEY (user_id),
	CONSTRAINT ocpp_user_settings_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE solarev.ocpp_charge_point_settings (
	cp_id				BIGINT NOT NULL,
	created				TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	pub_in				BOOLEAN NOT NULL DEFAULT TRUE,
	pub_flux			BOOLEAN NOT NULL DEFAULT TRUE,
	source_id_tmpl		VARCHAR(255),
	CONSTRAINT ocpp_charge_point_settings_pk PRIMARY KEY (cp_id),
	CONSTRAINT ocpp_charge_point_settings_charge_point_fk FOREIGN KEY (cp_id)
		REFERENCES solarev.ocpp_charge_point (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/**
 * Table for OCPP charger-wide status info.
 *
 * created 			the row creation date
 * user_id 			the account owner
 * cp_id			the charge point ID
 * connected_to		the name of the SolarIn instance connected to, NULL if not connected
 * connected_ts		the date the charger last connected
 */
CREATE TABLE solarev.ocpp_charge_point_status (
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cp_id			BIGINT NOT NULL,
	connected_to	TEXT,
	session_id		TEXT,
	connected_date	TIMESTAMP WITH TIME ZONE,
	CONSTRAINT ocpp_charge_point_status_pk PRIMARY KEY (user_id, cp_id),
	CONSTRAINT ocpp_charge_point_status_charge_point_fk FOREIGN KEY (cp_id)
		REFERENCES solarev.ocpp_charge_point (id)
		ON DELETE CASCADE
);

/**
 * Table for OCPP "last seen" timestamp for each action of a charger.
 *
 * created 			the row creation date
 * user_id 			the account owner
 * cp_id			the charge point ID
 * conn_id			the connector ID (>= 0) related to the action, or 0 for the charger
 * action			the name of the OCPP action
 * msg_id			the action message ID
 * ts				the  date the action occurred at
 */
CREATE TABLE solarev.ocpp_charge_point_action_status (
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cp_id			BIGINT NOT NULL,
	conn_id			INTEGER NOT NULL,
	action			TEXT NOT NULL,
	msg_id			TEXT NOT NULL,
	ts				TIMESTAMP WITH TIME ZONE NOT NULL,
	CONSTRAINT ocpp_charge_point_action_status_pk PRIMARY KEY (user_id, cp_id, conn_id, action),
	CONSTRAINT ocpp_charge_point_action_status_charge_point_fk FOREIGN KEY (cp_id)
		REFERENCES solarev.ocpp_charge_point (id)
		ON DELETE CASCADE
);
