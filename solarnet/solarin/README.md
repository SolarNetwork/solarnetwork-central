# SolarIn

This app provides APIs for the ingestion of data from SolarNode devices, both via HTTP and MQTT.

# Dependencies

This app depends on the following projects:

 * [solarnet-common][solarnet-common]
 * [solarnet-datum][solarnet-datum]
 * [solarnet-instructor][solarnet-instructor]
 * [solarnet-ocpp][solarnet-ocpp]
 * [solarnet-user][solarnet-user]
 
# Profiles

The following Spring profiles are available:

| Profile | Description |
|:--------|:------------|
| `dogtag` | Node PKI support via Dogtag. See [PkiDogtagConfig][PkiDogtagConfig]. |
| `mqtt` | Enables integration with SolarIn/MQTT. See [MqttDataCollectorConfig][MqttDataCollectorConfig]. |
| `ocpp-charge-session` | Enable OCPP charge session support. See [OcppChargeSessionManagerConfig][OcppChargeSessionManagerConfig]. |
| `ocpp-v16` | Enables OCPP v1.6 integration. See references to [SolarNetOcppConfiguration][SolarNetOcppConfiguration]. |

For example, in a production deployment the `SPRING_PROFILES_ACTIVE` environment variable can be
configured as

```
SPRING_PROFILES_ACTIVE="production,dogtag,mqtt,ocpp-charge-session,ocpp-v16"
```

[solarnet-common]: ../common/
[solarnet-datum]: ../datum/
[solarnet-instructor]: ../instructor/
[solarnet-ocpp]: ../ocpp/
[solarnet-user]: ../user/
[MqttDataCollectorConfig]: src/main/java/net/solarnetwork/central/in/config/MqttDataCollectorConfig.java
[OcppChargeSessionManagerConfig]: ../ocpp/src/main/java/net/solarnetwork/central/ocpp/config/OcppChargeSessionManagerConfig.java
[PkiDogtagConfig]: ../user/src/main/java/net/solarnetwork/central/user/config/PkiDogtagConfig.java
[SolarNetOcppConfiguration]: ../ocpp/src/main/java/net/solarnetwork/central/ocpp/config/SolarNetOcppConfiguration.java
[UserEventServiceSqsConfig]: ../user-datum/src/main/java/net/solarnetwork/central/user/event/config/UserEventServiceSqsConfig.java