/**
 * Account-wide DNP3 trusted X.509 certificate roots.
 *
 * @column user_id 		the ID of the account owner
 * @column subject_dn 	the subject DN extracted from the certificate, normalized by application
 * @column created		the creation date
 * @column modified		the modification date
 * @column enabled		a flag to mark the certificate as enabled for use by application or not
 * @column expires		the expiration date extracted from the certificate
 * @column cert			the certificate data, DER encoded
 */
CREATE TABLE solardnp3.dnp3_ca_cert (
	user_id			BIGINT NOT NULL,
	subject_dn		TEXT NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	enabled			BOOLEAN NOT NULL DEFAULT FALSE,
	expires			TIMESTAMP WITH TIME ZONE NOT NULL,
	cert			bytea NOT NULL,
	CONSTRAINT dnp3_ca_cert_pk PRIMARY KEY (user_id, subject_dn),
	CONSTRAINT dnp3_ca_cert_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/**
 * DNP3 overall server configuration.
 *
 * @column user_id 		the ID of the account owner
 * @column id 			generated ID for the configuration
 * @column created		the creation date
 * @column modified		the modification date
 * @column enabled		a flag to mark the configuration as enabled for use by application or not
 * @column cname		friendly name
 */
CREATE TABLE solardnp3.dnp3_server (
	user_id			BIGINT NOT NULL,
	id				BIGINT GENERATED BY DEFAULT AS IDENTITY,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	enabled			BOOLEAN NOT NULL DEFAULT FALSE,
	cname			CHARACTER VARYING(64) NOT NULL,
	CONSTRAINT dnp3_server_pk PRIMARY KEY (user_id, id),
	CONSTRAINT dnp3_server_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/**
 * DNP3 server authorization configuration.
 *
 * @column user_id 		the ID of the account owner
 * @column server_id 	the ID of the solardnp3.dnp3_server record
 * @column ident 		the client identity (e.g. subject DN)
 * 						note this column is GLOBALLY unique to facilitate a 1-to-1 mapping of
 *						identity to SN user
 * @column created		the creation date
 * @column modified		the modification date
 * @column enabled		a flag to mark the configuration as enabled for use by application or not
 * @column cname		friendly name
 */
CREATE TABLE solardnp3.dnp3_server_auth (
	user_id			BIGINT NOT NULL,
	server_id		BIGINT NOT NULL,
	ident			TEXT NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	enabled			BOOLEAN NOT NULL DEFAULT FALSE,
	cname			CHARACTER VARYING(64) NOT NULL,
	CONSTRAINT dnp3_server_auth_pk PRIMARY KEY (user_id, server_id, ident),
	CONSTRAINT dnp3_server_auth_ident_unq UNIQUE (ident),
	CONSTRAINT dnp3_server_auth_server_fk FOREIGN KEY (user_id, server_id)
		REFERENCES solardnp3.dnp3_server (user_id, id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/**
 * DNP3 server measurement configuration.
 *
 * @column user_id 		the ID of the account owner
 * @column server_id 	the ID of the solardnp3.dnp3_server record
 * @column idx 			sort index
 * @column created		the creation date
 * @column modified		the modification date
 * @column enabled		a flag to mark the configuration as enabled for use by application or not
 * @column node_id		node ID
 * @column source_id	source ID
 * @column pname		property name
 * @column mtype		measurement type (code)
 * @column dmult		optional decimal multiplier
 * @column doffset		optional decimal addition
 * @column dscale		optional decimal scale
 */
CREATE TABLE solardnp3.dnp3_server_meas (
	user_id			BIGINT NOT NULL,
	server_id		BIGINT NOT NULL,
	idx				INTEGER NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	enabled			BOOLEAN NOT NULL DEFAULT FALSE,
	node_id			BIGINT NOT NULL,
	source_id		CHARACTER VARYING(64) NOT NULL,
	pname			CHARACTER VARYING(255) NOT NULL,
	mtype			CHARACTER NOT NULL,
	dmult			NUMERIC,
	doffset			NUMERIC,
	dscale			INTEGER,
	CONSTRAINT dnp3_server_meas_pk PRIMARY KEY (user_id, server_id, idx),
	CONSTRAINT dnp3_server_meas_server_fk FOREIGN KEY (user_id, server_id)
		REFERENCES solardnp3.dnp3_server (user_id, id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/**
 * DNP3 server control configuration.
 *
 * @column user_id 		the ID of the account owner
 * @column server_id 	the ID of the solardnp3.dnp3_server record
 * @column idx 			sort index
 * @column created		the creation date
 * @column modified		the modification date
 * @column enabled		a flag to mark the configuration as enabled for use by application or not
 * @column node_id		node ID
 * @column control_id	control ID
 * @column pname		optional property name
 * @column ctype		control type (code)
 * @column dmult		optional decimal multiplier
 * @column doffset		optional decimal addition
 * @column dscale		optional decimal scale
 */
CREATE TABLE solardnp3.dnp3_server_ctrl (
	user_id			BIGINT NOT NULL,
	server_id		BIGINT NOT NULL,
	idx				INTEGER NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	enabled			BOOLEAN NOT NULL DEFAULT FALSE,
	node_id			BIGINT NOT NULL,
	control_id		CHARACTER VARYING(64) NOT NULL,
	pname			CHARACTER VARYING(255),
	ctype			CHARACTER NOT NULL,
	dmult			NUMERIC,
	doffset			NUMERIC,
	dscale			INTEGER,
	CONSTRAINT dnp3_server_ctrl_pk PRIMARY KEY (user_id, server_id, idx),
	CONSTRAINT dnp3_server_ctrl_server_fk FOREIGN KEY (user_id, server_id)
		REFERENCES solardnp3.dnp3_server (user_id, id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);