# SolarNet Postgres Database Setup

SolarNet requires a [Postgres][pgsql] database, version 12 or higher, to operate. Please note that
only version 12 has been extensively tested, but newer versions may work fine.

## Requirements

 * Postgres 12 - http://www.postgresql.org/
 * **citext** extension (from contrib)
 * **pgcrypto** extension (from contrib)
 * **uuid-ossp** extension (from contrib)
 * [timescaledb](https://docs.timescale.com/) extension
 * [aggs_for_vecs](https://github.com/pjungwir/aggs_for_vecs) extension
 
# macOS setup

On macOS, Postgres can be easily installed via the [Postgres.app](https://postgresapp.com/) or
[Homebrew](https://brew.sh/). Either of these approaches will come with the standard contrib
extensions. Homebrew also supports the `timescaledb` extension. Either way you'll have to compile
the `aggs_for_vecs` extension from source after Postgres is installed and for that you'll need the
command line developer tools. They can be installed in a variety of ways. One easy way is to run the
following:

```sh
xcode-select --install
```

# Install TimescaleDB extension from source

See the [Timescale docs](https://docs.timescale.com/install/latest/self-hosted/installation-source/)
for details. You'll also need `cmake` to build, which is provided by most Linux distributions and
can be installed via Homebrew/MacPorts on macOS. First clone the repository:

```sh
# clone source
git clone https://github.com/timescale/timescaledb.git

# checkout version tag
cd timescaledb
git checkout 2.5.1   # or some other release
```

Then, build and install according to the TimescaleDB documentation. On macOS, for example:

```sh
# make sure Postgres is on your PATH, e.g. for macOS Postgres.app:
export PATH=/Applications/Postgres.app/Contents/Versions/12/bin:$PATH

# in theory: ./bootstrap but on macOS may require OpenSSL from Homebrew:
OPENSSL_ROOT_DIR=/usr/local/opt/openssl ./bootstrap -DREGRESS_CHECKS=OFF

cd build && make
make install
```


# Install aggs_for_vecs extension from source

Clone the [aggs_for_vecs](https://github.com/pjungwir/aggs_for_vecs) repo. To build:

```sh
# Clone repo
git clone https://github.com/pjungwir/aggs_for_vecs.git

# Change into project directory
cd aggs_for_vecs

# Install for Linux
make && sudo make install

# Install for macOS Postgres.app
make PG_CONFIG=/Applications/Postgres.app/Contents/Versions/12/bin/pg_config
make install PG_CONFIG=/Applications/Postgres.app/Contents/Versions/12/bin/pg_config
```
 

# Create database

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
 3. Install the **citext**, **pgcrypto**, **uuid-ossp**, **[timescaledb][timescaledb]**, and 
    **[aggs_for_vecs][aggs_for_vecs]** extensions.

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

[aggs_for_vecs]: https://github.com/pjungwir/aggs_for_vecs
[pgsql]: http://www.postgresql.org/
[timescaledb]: https://docs.timescale.com/

