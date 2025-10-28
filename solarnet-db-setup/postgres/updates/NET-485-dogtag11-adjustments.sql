-- increase length to work with Dogtag 11
ALTER TABLE solaruser.user_node_cert ALTER COLUMN request_id TYPE CHARACTER VARYING(128);
