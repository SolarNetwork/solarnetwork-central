# SolarUser

This app provides both an administrative web-based UI for users to manage their SolarNetwork
account and a REST API for user-level administration functions.

# Dependencies

This app depends on the following projects:

 * [solarnet-common][solarnet-common]
 * [solarnet-datum][solarnet-datum]
 * [solarnet-instructor][solarnet-instructor]
 * [solarnet-ocpp][solarnet-ocpp]
 * [solarnet-oscp][solarnet-oscp]
 * [solarnet-user][solarnet-user]
 * [solarnet-user-billing][solarnet-user-billing]
 * [solarnet-user-datum][solarnet-user-datum]
 * [solarnet-user-datum-input][solarnet-user-datum-input]
 * [solarnet-user-instructor-input][solarnet-user-instructor-input]
 * [solarnet-user-ocpp][solarnet-user-ocpp]
 * [solarnet-user-oscp][solarnet-user-oscp]

# Runtime profiles

The following Spring runtime profiles are available:

| Profile | Description |
|:--------|:------------|
| `audit-api` | Enables API auditing, if the `user-service-auditor` profile is also enabled. |
| `aws-secrets` | Enables AWS Secrets Manager persistence for some credentials, like OAuth for OSCP. |
| `datum-import-s3-resource-storage` | Store datum import resources in S3. See [S3ResourceStorageConfig][S3ResourceStorageConfig]. |
| `din` | Datum Input support. See [SolarNetUserDatumInputConfiguration][SolarNetUserDatumInputConfiguration]. |
| `dogtag` | Node PKI support via Dogtag. See [PkiDogtagConfig][PkiDogtagConfig]. |
| `inin` | Instruction Input support. See [SolarNetUserInstructionInputConfiguration][SolarNetUserInstructionInputConfiguration]. |
| `mqtt` | Enables publishing aggregates to SolarFlux. See [SolarFluxPublishingConfig][SolarFluxPublishingConfig]. |
| `node-service-auditor` | Enables auditing node service events like instruction counts. See [InstructorBizConfig][InstructorBizConfig], [JdbcNodeServiceAuditorConfig][JdbcNodeServiceAuditorConfig].|
| `no-solarflux` | Disable SolarFlux MQTT integration when `mqtt` profile is active. |
| `ocpp-v16` | Enables OCPP v1.6 integration. See references to [SolarNetOcppConfiguration][SolarNetOcppConfiguration]. |
| `oscp-v20`    | Enable OSCP v2.0 integration. See references to [SolarNetOscpConfiguration][SolarNetOscpConfiguration]. |
| `rate-limit` | Enables rate-limiting API access. See [RateLimitConfig][RateLimitConfig]. |
| `snf-billing` | Enable SNF billing. See [SnfBillingConfig][SnfBillingConfig]. |
| `user-event-sqs` | Enable the SQS user event service. See [UserEventServiceSqsConfig][UserEventServiceSqsConfig]. |
| `user-service-auditor` | Enable user service auditing. |

For example, in a production deployment the `SPRING_PROFILES_ACTIVE` environment variable can be
configured as

```
SPRING_PROFILES_ACTIVE="production,datum-import-s3-resource-storage,din,dogtag,inin,mqtt,ocpp-v16,snf-billing,user-event-sqs"
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

The build produces an executable JAR at `build/libs/solaruser-x.y.z.jar`.


[app-config]: src/main/resources/application.yml
[solarnet-common]: ../common/
[solarnet-datum]: ../datum/
[solarnet-instructor]: ../instructor/
[solarnet-ocpp]: ../ocpp/
[solarnet-oscp]: ../oscp/
[solarnet-user]: ../user/
[solarnet-user-billing]: ../user-billing/
[solarnet-user-datum]: ../user-datum/
[solarnet-user-datum-input]: ../user-datum-input/
[solarnet-user-instructor-input]: ../user-instructor-input/
[solarnet-user-ocpp]: ../user-ocpp/
[solarnet-user-oscp]: ../user-oscp/
[InstructorBizConfig]: ../instructor/src/main/java/net/solarnetwork/central/instructor/config/InstructorBizConfig.java
[JdbcNodeServiceAuditorConfig]: ../common/src/main/java/net/solarnetwork/central/common/config/JdbcNodeServiceAuditorConfig.java
[PkiDogtagConfig]: ../user/src/main/java/net/solarnetwork/central/user/config/PkiDogtagConfig.java
[RateLimitConfig]: src/main/java/net/solarnetwork/central/reg/config/RateLimitConfig.java
[S3ResourceStorageConfig]: ../datum/src/main/java/net/solarnetwork/central/datum/imp/config/S3ResourceStorageConfig.java
[SnfBillingConfig]: src/main/java/net/solarnetwork/central/jobs/config/SnfBillingConfig.java
[SolarFluxPublishingConfig]: src/main/java/net/solarnetwork/central/jobs/config/SolarFluxPublishingConfig.java
[SolarNetOcppConfiguration]: ../ocpp/src/main/java/net/solarnetwork/central/ocpp/config/SolarNetOcppConfiguration.java
[SolarNetOscpConfiguration]: ../oscp/src/main/java/net/solarnetwork/central/oscp/config/SolarNetOscpConfiguration.java
[SolarNetUserDatumInputConfiguration]: ../user-datum-input/src/main/java/net/solarnetwork/central/user/din/config/SolarNetUserDatumInputConfiguration.java
[SolarNetUserInstructionInputConfiguration]: ../user-instructor-input/src/main/java/net/solarnetwork/central/user/inin/config/SolarNetUserInstructionInputConfiguration.java
[UserEventServiceSqsConfig]: ../user-datum/src/main/java/net/solarnetwork/central/user/event/config/UserEventServiceSqsConfig.java
