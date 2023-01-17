ALTER TABLE solaruser.user_auth_token
	ADD COLUMN disp_name CHARACTER VARYING(128),
	ADD COLUMN description CHARACTER VARYING(512);
