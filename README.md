# SolarNet - the cloud SolarNetwork platform

SolarNet is the platform for the cloud component of [SolarNetwork][sn-io]. It is
mainly responsible for:

 * managing SolarNetwork user accounts
 * provisioning SolarNode accounts and certificates
 * accepting data from SolarNodes
 * providing REST APIs for accessing data accepted from SolarNodes

To get started, please see the [SolarNetwork Developer Guide][dev-guide], or follow the steps
outlined below.

# Developer Setup

To run SolarNet in your own development environment, you'll need the following:

 * Java 17, any vendor should work, e.g. 
   [Adoptium](https://adoptium.net/temurin/releases?version=17)
 * [Postgres 12](https://www.postgresql.org/download/) (later versions may work)
 * The following standard Postgres contrib extensions: **citext**, **pgcrypto**, and **uuid-ossp**.
   These come with normal Postgres distributions, possibly as part of a separate _contrib_ package.
 * The [timescaledb](https://docs.timescale.com/) Postgres extension
 * The [aggs_for_vecs](https://github.com/pjungwir/aggs_for_vecs) Postgres extension
 
See the [SolarNet Postgres Database Setup](./solardb-db-setup/postgres/) guide for information on
setting up Postgres for the first time.

See the [SolarNet Cloud Applications](./solarnet/) guide for information on building/running the
various applications that make up the SolarNet platform.

[dev-guide]: https://github.com/SolarNetwork/solarnetwork/wiki/Developer-Guide
[sn-io]: http://solarnetwork.github.io/
