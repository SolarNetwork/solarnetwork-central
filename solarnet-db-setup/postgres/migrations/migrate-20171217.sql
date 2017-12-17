-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20171217.sql

\cd updates
\i NET-111-remove-domains.sql
\i NET-112-jsonb.sql
