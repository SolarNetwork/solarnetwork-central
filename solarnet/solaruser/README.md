# SolarUser

This app provides both an administrative web-based UI for users to manage their SolarNetwork
account and a REST API for user-level administration functions.

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
 
# Profiles

The following Spring profiles are available:

| Profile | Description |
|:--------|:------------|
| `datum-import-s3-resource-storage` | Store datum import resources in S3. See [S3ResourceStorageConfig][S3ResourceStorageConfig]. |
| `dogtag` | Node PKI support via Dogtag. See [PkiDogtagConfig][PkiDogtagConfig]. |
| `mqtt` | Enables publishing aggregates to SolarFlux. See [SolarFluxPublishingConfig][SolarFluxPublishingConfig]. |
| `ocpp-v16` | Enables OCPP v1.6 integration. See references to [SolarNetOcppConfiguration][SolarNetOcppConfiguration]. |
| `snf-billing` | Enable SNF billing. See [SnfBillingConfig][SnfBillingConfig]. |
| `user-event-sqs` | Enable the SQS user event service. See [UserEventServiceSqsConfig][UserEventServiceSqsConfig]. |

For example, in a production deployment the `SPRING_PROFILES_ACTIVE` environment variable can be
configured as

```
SPRING_PROFILES_ACTIVE="production,datum-import-s3-resource-storage,dogtag,mqtt,ocpp-v16,snf-billing,user-event-sqs"
```

[solarnet-common]: ../common/
[solarnet-datum]: ../datum/
[solarnet-instructor]: ../instructor/
[solarnet-ocpp]: ../ocpp/
[solarnet-user]: ../user/
[solarnet-user-billing]: ../user-billing/
[solarnet-user-datum]: ../user-datum/
[solarnet-user-ocpp]: ../user-ocpp/
[PkiDogtagConfig]: ../user/src/main/java/net/solarnetwork/central/user/config/PkiDogtagConfig.java
[S3ResourceStorageConfig]: ../datum/src/main/java/net/solarnetwork/central/datum/imp/config/S3ResourceStorageConfig.java
[SnfBillingConfig]: src/main/java/net/solarnetwork/central/jobs/config/SnfBillingConfig.java
[SolarFluxPublishingConfig]: src/main/java/net/solarnetwork/central/jobs/config/SolarFluxPublishingConfig.java
[SolarNetOcppConfiguration]: ../ocpp/src/main/java/net/solarnetwork/central/ocpp/config/SolarNetOcppConfiguration.java
[UserEventServiceSqsConfig]: ../user-datum/src/main/java/net/solarnetwork/central/user/event/config/UserEventServiceSqsConfig.java
