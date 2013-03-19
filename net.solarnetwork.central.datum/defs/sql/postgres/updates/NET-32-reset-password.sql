DROP VIEW solaruser.user_login;
CREATE OR REPLACE VIEW solaruser.user_login AS
	SELECT
		email AS username, 
		password AS password, 
		enabled AS enabled,
		id AS user_id,
		disp_name AS display_name
	FROM solaruser.user_user;
