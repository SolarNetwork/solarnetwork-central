# SolarQuery

This app provides read-only web API access to SolarNetwork data.

# Dependencies

This app depends on the [solarnet-common][solarnet-common] and [solarnet-datum][solarnet-datum]
projects.

# Profiles

The following Spring profiles are available:

| Profile | Description |
|:--------|:------------|
| `query-auditor` | Enables auditing datum query access using a second write-access JDBC connection. See [DatumQueryBizConfig][DatumQueryBizConfig], [JdbcQueryAuditorConfig][JdbcQueryAuditorConfig].|

[solarnet-common]: ../common/
[solarnet-datum]: ../datum/
[DatumQueryBizConfig]: src/main/java/net/solarnetwork/central/query/config/DatumQueryBizConfig.java
[JdbcQueryAuditorConfig]: ../datum/src/main/java/net/solarnetwork/central/datum/config/JdbcQueryAuditorConfig.java
