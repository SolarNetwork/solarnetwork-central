-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20220131.sql

-- NOTE: the NET-289-aggs-for-vecs-admin.sql must be run as a DB superuser to install the
-- aggs_for_vecs extension

\i NET-289-aggs-for-vecs.sql
