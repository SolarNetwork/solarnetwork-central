/* psql -d solarnetwork_tmp -U postgres -f postgres-disable-triggers.sql */
DO $$DECLARE r record;
BEGIN
    FOR r IN SELECT table_schema, table_name FROM information_schema.tables 
    	WHERE table_schema IN ('solarnet','solarrep','solaruser') AND table_type <> 'VIEW' ORDER BY table_schema, table_name
    LOOP
    	RAISE NOTICE 'Disabling triggers on table %.%', quote_ident(r.table_schema), quote_ident(r.table_name);
        EXECUTE 'alter table '|| quote_ident(r.table_schema) || '.' || quote_ident(r.table_name) || ' disable trigger all';
    END LOOP;
END$$;
