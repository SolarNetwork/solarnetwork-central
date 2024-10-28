/**
 * Cloud integration user (account) configuration.
 *
 * @column user_id 		the ID of the account owner
 * @column id 			the ID of the configuration
 * @column created		the creation date
 * @column modified		the modification date
 * @column pub_in		a flag to publish datum streams to SolarIn
 * @column pub_flux		a flag to publish datum streams to SolarFlux
 */
CREATE TABLE solardin.cin_user_settings (
	user_id			BIGINT NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	pub_in			BOOLEAN NOT NULL DEFAULT TRUE,
	pub_flux		BOOLEAN NOT NULL DEFAULT FALSE,
	CONSTRAINT cin_user_settings_pk PRIMARY KEY (user_id),
	CONSTRAINT cin_user_settings_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/**
 * Cloud datum stream settings, to override cin_user_settings.
 *
 * @column user_id 		the ID of the account owner
 * @column ds_id 		the ID of the datum stream associated with this configuration
 * @column created		the creation date
 * @column modified		the modification date
 * @column pub_in		a flag to publish datum streams to SolarIn
 * @column pub_flux		a flag to publish datum streams to SolarFlux
 */
CREATE TABLE solardin.cin_datum_stream_settings (
	user_id			BIGINT NOT NULL,
	ds_id 			BIGINT NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	pub_in			BOOLEAN NOT NULL DEFAULT TRUE,
	pub_flux		BOOLEAN NOT NULL DEFAULT TRUE,
	CONSTRAINT cin_datum_stream_settings_pk PRIMARY KEY (user_id, ds_id),
	CONSTRAINT cin_datum_stream_settings_ds_fk FOREIGN KEY (user_id, ds_id)
		REFERENCES solardin.cin_datum_stream (user_id, id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);
