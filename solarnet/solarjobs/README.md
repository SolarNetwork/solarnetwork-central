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
 
# Profiles

The following Spring profiles are available:

| Profile | Description |
|:--------|:------------|
| `datum-import-s3-resource-storage` | Store datum import resources in S3. See [S3ResourceStorageConfig][S3ResourceStorageConfig]. |
| `mqtt` | Enables publishing aggregates to SolarFlux. See [SolarFluxPublishingConfig][SolarFluxPublishingConfig]. |
| `snf-billing` | Enable SNF billing. See [SnfBillingConfig][SnfBillingConfig]. |
| `user-event-sqs` | Enable the SQS user event service. See [UserEventServiceSqsConfig][UserEventServiceSqsConfig]. |

For example, in a production deployment the `SPRING_PROFILES_ACTIVE` environment variable can be
configured as

```
SPRING_PROFILES_ACTIVE="production,datum-import-s3-resource-storage,mqtt,snf-billing,user-event-sqs"
```

[solarnet-common]: ../common/
[solarnet-datum]: ../datum/
[solarnet-instructor]: ../instructor/
[solarnet-ocpp]: ../ocpp/
[solarnet-user]: ../user/
[solarnet-user-billing]: ../user-billing/
[solarnet-user-datum]: ../user-datum/
[solarnet-user-ocpp]: ../user-ocpp/
[S3ResourceStorageConfig]: ../datum/src/main/java/net/solarnetwork/central/datum/imp/config/S3ResourceStorageConfig.java
[SnfBillingConfig]: src/main/java/net/solarnetwork/central/jobs/config/SnfBillingConfig.java
[SolarFluxPublishingConfig]: src/main/java/net/solarnetwork/central/jobs/config/SolarFluxPublishingConfig.java
[UserEventServiceSqsConfig]: ../user-datum/src/main/java/net/solarnetwork/central/user/event/config/UserEventServiceSqsConfig.java
