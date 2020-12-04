-- this view called into solaragg.agg_datum_daily table; replaced by datum metadata query
DROP VIEW IF EXISTS solaruser.user_auth_token_sources;

-- tweak to add STRICT and ROWS
CREATE OR REPLACE FUNCTION solarcommon.reduce_dim(anyarray)
	RETURNS SETOF anyarray LANGUAGE plpgsql IMMUTABLE STRICT ROWS 20 AS
$$
DECLARE
	s $1%TYPE;
BEGIN
	FOREACH s SLICE 1 IN ARRAY $1 LOOP
		RETURN NEXT s;
	END LOOP;
	RETURN;
END
$$;
