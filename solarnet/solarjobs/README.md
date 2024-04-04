# SolarJobs

This app provides a batch-based job processing application SolarNetwork.

# Dependencies

This app depends on the following projects:

 * [solarnet-common][solarnet-common]
 * [solarnet-datum][solarnet-datum]
 * [solarnet-instructor][solarnet-instructor]
 * [solarnet-ocpp][solarnet-ocpp]
 * [solarnet-user][solarnet-user]
 * [solarnet-user-billing][solarnet-user-billing]
 * [solarnet-user-datum][solarnet-user-datum]
 * [solarnet-user-ocpp][solarnet-user-ocpp]

# Runtime profiles

The following Spring runtime profiles are available:

| Profile | Description |
|:--------|:------------|
| `datum-import-s3-resource-storage` | Store datum import resources in S3. See [S3ResourceStorageConfig][S3ResourceStorageConfig]. |
| `mqtt` | Enables publishing aggregates to SolarFlux. See [SolarFluxPublishingConfig][SolarFluxPublishingConfig]. |
| `no-solarflux` | Disable SolarFlux MQTT integration when `mqtt` profile is active. |
| `snf-billing` | Enable SNF billing. See [SnfBillingConfig][SnfBillingConfig]. |
| `user-event-sqs` | Enable the SQS user event service. See [UserEventServiceSqsConfig][UserEventServiceSqsConfig]. |
| `ocpp-jobs` | Enable OCPP specific jobs. See [OcppJobsConfig][OcppJobsConfig]. |

For example, in a production deployment the `SPRING_PROFILES_ACTIVE` environment variable can be
configured as

```
SPRING_PROFILES_ACTIVE="production,datum-import-s3-resource-storage,mqtt,snf-billing,user-event-sqs"
```

# Runtime configuration

See the [application.yml][app-config] file for the available runtime configuration properties, and
their default values. You can override any property by creating an `application.yml` file in the
working directory of the application, or via profile-specific files like
`application-production.yml` when the `production` profile is active.


# Building

The build is managed by Gradle, and requires a Java Development Kit version 21+ to build (and run).

```sh
# Unix-like OS:
../gradlew build

# Build without running tests:
../gradlew build -x test

# Windows:
../gradlew.bat build
```

The build produces an executable JAR at `build/libs/solarjobs-x.y.z.jar`.


[app-config]: src/main/resources/application.yml
[solarnet-common]: ../common/
[solarnet-datum]: ../datum/
[solarnet-instructor]: ../instructor/
[solarnet-ocpp]: ../ocpp/
[solarnet-user]: ../user/
[solarnet-user-billing]: ../user-billing/
[solarnet-user-datum]: ../user-datum/
[solarnet-user-ocpp]: ../user-ocpp/
[OcppJobsConfig]: src/main/java/net/solarnetwork/central/jobs/config/OcppJobsConfig.java
[S3ResourceStorageConfig]: ../datum/src/main/java/net/solarnetwork/central/datum/imp/config/S3ResourceStorageConfig.java
[SnfBillingConfig]: src/main/java/net/solarnetwork/central/jobs/config/SnfBillingConfig.java
[SolarFluxPublishingConfig]: src/main/java/net/solarnetwork/central/jobs/config/SolarFluxPublishingConfig.java
[UserEventServiceSqsConfig]: ../user-datum/src/main/java/net/solarnetwork/central/user/event/config/UserEventServiceSqsConfig.java
