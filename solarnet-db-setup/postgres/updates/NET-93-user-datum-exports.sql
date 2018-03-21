CREATE SEQUENCE solaruser.user_export_seq;

CREATE TABLE solaruser.user_export_data_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_export_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	sprops			jsonb,
	filter			jsonb,
	CONSTRAINT user_export_data_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_export_data_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_export_data_conf_user_idx ON solaruser.user_export_data_conf (user_id);

CREATE TABLE solaruser.user_export_dest_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_export_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	sprops			jsonb,
	CONSTRAINT user_export_dest_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_export_dest_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_export_dest_conf_user_idx ON solaruser.user_export_dest_conf (user_id);

CREATE TABLE solaruser.user_export_outp_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_export_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	sprops			jsonb,
	compression		CHARACTER(1) NOT NULL,
	CONSTRAINT user_export_outp_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_export_outp_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_export_outp_conf_user_idx ON solaruser.user_export_outp_conf (user_id);

CREATE TABLE solaruser.user_export_datum_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_export_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	delay_mins		INTEGER NOT NULL,
	schedule		CHARACTER(1) NOT NULL,
	data_conf_id	BIGINT,
	dest_conf_id	BIGINT,
	outp_conf_id	BIGINT,
	CONSTRAINT user_export_datum_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_export_datum_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE,
	CONSTRAINT user_export_datum_conf_data_fk FOREIGN KEY (data_conf_id)
		REFERENCES solaruser.user_export_data_conf (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT user_export_datum_conf_dest_fk FOREIGN KEY (dest_conf_id)
		REFERENCES solaruser.user_export_dest_conf (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT user_export_datum_conf_outp_fk FOREIGN KEY (outp_conf_id)
		REFERENCES solaruser.user_export_outp_conf (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_export_datum_conf_user_idx ON solaruser.user_export_datum_conf (user_id);
