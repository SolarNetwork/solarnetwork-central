# SolarNet PostgreSQL Database Setup

SolarNet requires a [PostgreSQL][pgsql] database, version 9.6 or higher, to
operate. Please note that only version 9.6 has been extensively tested.

## Requirements

 * PostgreSQL 9.6 - http://www.postgresql.org/
 * citext extension (from PostgreSQL contrib)
 * pgcrypto extension (from PostgreSQL contrib)
 * uuid-ossp extension (from PostgreSQL contrib)

## Create database

The `bin/setup-db.sh` script is the easiest way to setup or re-create the database.
To create a database **solarnetwork** and database user **solarnet** for development:

```sh
./bin/setup-db.sh -mrv -u solarnet -d solarnetwork
```

To create a database **solarnet_unittest** and database user **solarnet_test** for testing,
the defaults can be used:

```sh
./bin/setup-db.sh -mrv
```

If Postgres is running on a non-standard port, or on a remote host, pass `psql` connection
arguments via the `-c` switch:

```sh
./bin/setup-db.sh -mrv -c '-p 5496 -h postgres96.example.com'
```

### SQL only database creation

The `postgres-create.sql` script can be used to

 1. Create a **solarnet** database user.
 2. Create a **solarnetwork** database, owned by the **solarnet** user.
 3. Install the **citext** and **pgcrypto** and **uuid-ossp** extensions.

This script should be run as a database superuser, for example **postgres**.
Assuming Postgres is available on the same machine you are are, the script can
be executed similarly to this:

```shell
$ psql -U postgres -d tempalte0 -f postgres-create.sql
```

Alternatively you can execute commands manually like the following:

```shell
$ createuser -U postgres -EP solarnet
$ createdb -U postgres -E UTF8 -O solarnet -T template0 solarnetwork
$ psql -U postgres -d solarnetwork \
	-c 'CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;'
$ psql -U postgres -d solarnetwork \
	-c 'CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;'
$ psql -U postgres -d solarnetwork \
	-c 'CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;'
$ psql -U postgres -d solarnetwork \
	-c 'CREATE EXTENSION IF NOT EXISTS timescaledb WITH SCHEMA public;'
$ psql -U postgres -d solarnetwork \
	-c 'CREATE EXTENSION IF NOT EXISTS aggs_for_vecs WITH SCHEMA public;'
```

## Setup database

Now the SolarNet database schemas and tables can be created, this time as the
normal database user:

```shell
$ psql -U solarnet -d solarnetwork -f postgres-init.sql
```

## Done

The database setup is now complete.

  [pgsql]: http://www.postgresql.org/
