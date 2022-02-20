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
| `mqtt` | Enables publishing aggregates to SolarFlux. See [SolarFluxPublishingConfig][SolarFluxPublishingConfig]. |

[solarnet-common]: ../common/
[solarnet-datum]: ../datum/
[solarnet-instructor]: ../instructor/
[solarnet-ocpp]: ../ocpp/
[solarnet-user]: ../user/
[solarnet-user-billing]: ../user-billing/
[solarnet-user-datum]: ../user-datum/
[solarnet-user-ocpp]: ../user-ocpp/
[SolarFluxPublishingConfig]: src/main/java/net/solarnetwork/central/jobs/config/SolarFluxPublishingConfig.java
