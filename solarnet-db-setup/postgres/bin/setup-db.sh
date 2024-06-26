#!/usr/bin/env bash

# Script for creating or re-creating the SolarNetwork database in its default form.
# The resulting database is suitable for development and unit testing.
#
# To create a database 'solarnetwork_unittest' and database user 'solartest' for testing:
#
#     ./bin/setup-db.sh -mrv
#
# To create a database 'solarnetwork' and database user 'solarnet' for development:
#
#     ./bin/setup-db.sh -mrv -u solarnet -d solarnetwork
#
# If Postgres is running on a non-standard port, or on a remote host, pass `psql` connection
# arguments via the -c switch:
#
#     ./bin/setup-db.sh -mrv -c '-p 5496 -h postgres96.example.com'

PSQL_CONN_ARGS=""
PG_USER="solartest"
PG_DB="solarnetwork_unittest"
PG_ADMIN_USER="postgres"
PG_ADMIN_DB="postgres"
PG_TEMPLATE_DB="template0"
RECREATE_DB=""
CREATE_USER=""
DRY_RUN=""
VERBOSE=""

do_help () {
	cat 1>&2 <<EOF
Usage: $0 <arguments>

 -c <args>        - extra arguments to pass to psql
 -d <db name>     - the database name; defaults to 'solarnetwork_unittest'
 -D <admin db>    - the Postgres admin database name; defaults to 'postgres'
 -m               - create the Postgres database user
 -r               - recreate the database (drop and create again)
 -t               - dry run
 -T <db name>     - the Postgres template database name to use; defaults to
                    'template0'
 -u <db user>     - the Postgres database user to use; defaults to 'solartest'
 -U <admin user>  - the Postgres admin user to use; defaults to 'postgres'
 -v               - increase verbosity

Example that creates the test database and user for the first time:

  ./bin/setup-db.sh -m

To connect to a different Postgres port:

  ./bin/setup-db.sh -c '-p 5412'

To recreate the database (drop and create):

  ./bin/setup-db.sh -r

EOF
}

while getopts ":c:d:D:hmrtT:u:U:v" opt; do
	case $opt in
		c) PSQL_CONN_ARGS="${OPTARG}";;
		d) PG_DB="${OPTARG}";;
		D) PG_ADMIN_DB="${OPTARG}";;
		h) do_help
			exit 0
			;;
		m) CREATE_USER='TRUE';;
		r) RECREATE_DB='TRUE';;
		t) DRY_RUN='TRUE';;
		T) PG_TEMPLATE_DB="${OPTARG}";;
		u) PG_USER="${OPTARG}";;
		U) PG_ADMIN_USER="${OPTARG}";;
		v) VERBOSE='TRUE';;
		*) echo "Unknown argument ${OPTARG}"
			do_help
			exit 1
	esac
done
shift $(($OPTIND - 1))

# Check that PostgreSQL is available
type psql &>/dev/null || { echo "psql command not found."; exit 1; }

# Check we appear to be in correct directory
if [ ! -e "postgres-init.sql" ]; then
	echo "postgres-init.sql DDL not found; please run this script in the same directory as that file.";
	exit 2;
fi

if [ -n "$RECREATE_DB" ]; then
	if [ -n "$VERBOSE" ]; then
		echo "Dropping database [$PG_DB]"
	fi
	if [ -n "$DRY_RUN" ]; then
		echo "psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_ADMIN_DB -c 'DROP DATABASE IF EXISTS $PG_DB'"
	else
		psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_ADMIN_DB -c "DROP DATABASE IF EXISTS $PG_DB" || exit 3
	fi
fi

if [ -n "$CREATE_USER" ]; then
	if [ -n "$VERBOSE" ]; then
		echo "Creating database user [$PG_USER]"
	fi
	if [ -n "$DRY_RUN" ]; then
		echo "psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_ADMIN_DB -c 'CREATE USER $PG_USER ENCRYPTED PASSWORD '$PG_USER''"
	else
		psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_ADMIN_DB -c "CREATE USER $PG_USER ENCRYPTED PASSWORD '$PG_USER'" || exit 3
	fi
fi

if [ -n "$VERBOSE" ]; then
	echo "Creating database [$PG_DB] with owner [$PG_USER]"
fi
if [ -n "$DRY_RUN" ]; then
	echo "psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_ADMIN_DB -c 'CREATE DATABASE $PG_DB WITH ENCODING='UTF8' OWNER=$PG_USER TEMPLATE=$PG_TEMPLATE_DB LC_COLLATE='C' LC_CTYPE='C''"
	echo "psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_DB -c 'CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public'"
	echo "psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_DB -c 'CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public'"
	echo "psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_DB -c 'CREATE EXTENSION IF NOT EXISTS "'"'"uuid-ossp"'"'" WITH SCHEMA public'"
	echo "psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_DB -c 'CREATE EXTENSION IF NOT EXISTS timescaledb WITH SCHEMA public'"
	echo "psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_DB -c 'CREATE EXTENSION IF NOT EXISTS aggs_for_vecs WITH SCHEMA public'"
else
	psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_ADMIN_DB -c "CREATE DATABASE $PG_DB WITH ENCODING='UTF8' OWNER=$PG_USER TEMPLATE=$PG_TEMPLATE_DB LC_COLLATE='C' LC_CTYPE='C'" || exit 3
	psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_DB -c "CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public" || exit 3
	psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_DB -c "CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public" || exit 3
	psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_DB -c 'CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public' || exit 3
	psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_DB -c "CREATE EXTENSION IF NOT EXISTS timescaledb WITH SCHEMA public" || exit 3
	psql $PSQL_CONN_ARGS -U $PG_ADMIN_USER -d $PG_DB -c "CREATE EXTENSION IF NOT EXISTS aggs_for_vecs WITH SCHEMA public" || exit 3
fi

if [ -n "$VERBOSE" ]; then
	echo "Creating SolarNetwork database tables and functions"
fi
if [ -n "$DRY_RUN" ]; then
	echo "psql $PSQL_CONN_ARGS -U $PG_USER -d $PG_DB -f postgres-init.sql"
else
	psql $PSQL_CONN_ARGS -U $PG_USER -d $PG_DB -f postgres-init.sql || exit 3
fi

