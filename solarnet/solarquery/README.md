# SolarQuery

This app provides read-only web API access to SolarNetwork data.

# Dependencies

This app depends on the [solarnet-common][solarnet-common] and [solarnet-datum][solarnet-datum]
projects.

# Runtime profiles

The following Spring runtime profiles are available:

| Profile | Description |
|:--------|:------------|
| `query-auditor` | Enables auditing datum query access using a second write-access JDBC connection. See [DatumQueryBizConfig][DatumQueryBizConfig], [JdbcQueryAuditorConfig][JdbcQueryAuditorConfig].|


# Runtime configuration

See the [application.yml][app-config] file for the available runtime configuration properties, and
their default values. You can override any property by creating an `application.yml` file in the
working directory of the application, or via profile-specific files like 
`application-production.yml` when the `production` profile is active.


# Building

The build is managed by Gradle, and requires a Java Development Kit version 17+ to build (and run).

```sh
# Unix-like OS:
../gradlew build

# Build without running tests:
../gradlew build -x test

# Windows:
../gradlew.bat build
```

The build produces an executable JAR at `build/libs/solarquery-x.y.z.jar`.


[app-config]: src/main/resources/application.yml
[solarnet-common]: ../common/
[solarnet-datum]: ../datum/
[DatumQueryBizConfig]: src/main/java/net/solarnetwork/central/query/config/DatumQueryBizConfig.java
[JdbcQueryAuditorConfig]: ../datum/src/main/java/net/solarnetwork/central/datum/config/JdbcQueryAuditorConfig.java
