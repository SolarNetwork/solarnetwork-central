DROP VIEW solaruser.user_auth_token_login;

ALTER TABLE solaruser.user_auth_token ALTER jpolicy TYPE jsonb USING jpolicy::jsonb;

CREATE OR REPLACE VIEW solaruser.user_auth_token_login AS
	SELECT t.auth_token AS username,
		t.auth_secret AS password,
		u.enabled,
		u.id AS user_id,
		u.disp_name AS display_name,
		t.token_type::character varying AS token_type,
		t.jpolicy
	 FROM solaruser.user_auth_token t
		 JOIN solaruser.user_user u ON u.id = t.user_id
	WHERE t.status = 'Active'::solaruser.user_auth_token_status;
