CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

CREATE OR REPLACE FUNCTION solaruser.snws2_signing_key(sign_date date, secret text)
RETURNS bytea AS $$
	SELECT hmac('snws2_request', hmac(to_char(sign_date, 'YYYYMMDD'), 'SNWS2' || secret, 'sha256'), 'sha256');
$$ LANGUAGE SQL STRICT IMMUTABLE;

CREATE OR REPLACE FUNCTION solaruser.snws2_signing_key_hex(sign_date date, secret text)
RETURNS text AS $$
	SELECT encode(solaruser.snws2_signing_key(sign_date, secret), 'hex');
$$ LANGUAGE SQL STRICT IMMUTABLE;
