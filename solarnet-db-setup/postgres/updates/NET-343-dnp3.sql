/**
 * Account-wide DNP3 trusted X.509 certificate roots.
 *
 * @column user_id 		the ID of the account owner
 * @column subject_dn 	the subject DN extracted from the certificate, normalized by application
 * 						note this column is GLOBALLY unique to facilitate a 1-to-1 mapping of
 *						subject DN to SN user
 * @column created		the creation date
 * @column modified		the modification date
 * @column expires		the expiration date extracted from the certificate
 * @column enable		a flag to mark the certificate as enabled for use by application or not
 * @column cert			the certificate data, DER encoded
 */
CREATE TABLE solardnp3.dnp3_ca_cert (
	user_id			BIGINT NOT NULL,
	subject_dn		TEXT NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	expires			TIMESTAMP WITH TIME ZONE NOT NULL,
	enabled			BOOLEAN NOT NULL DEFAULT FALSE,
	cert			bytea NOT NULL,
	CONSTRAINT dnp3_ca_cert_pk PRIMARY KEY (user_id, subject_dn),
	CONSTRAINT dnp3_ca_cert_subject_dn_unq UNIQUE (subject_dn),
	CONSTRAINT dnp3_ca_cert_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);
