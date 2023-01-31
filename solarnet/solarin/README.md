# SolarIn

This app provides APIs for the ingestion of data from SolarNode devices, both via HTTP and MQTT.

# Dependencies

This app depends on the following projects:

 * [solarnet-common][solarnet-common]
 * [solarnet-datum][solarnet-datum]
 * [solarnet-instructor][solarnet-instructor]
 * [solarnet-ocpp][solarnet-ocpp]
 * [solarnet-user][solarnet-user]

# Runtime profiles

The following Spring runtime profiles are available:

| Profile | Description |
|:--------|:------------|
| `dogtag` | Node PKI support via Dogtag. See [PkiDogtagConfig][PkiDogtagConfig]. |
| `mqtt` | Enables integration with SolarIn/MQTT. See [MqttDataCollectorConfig][MqttDataCollectorConfig]. |
| `no-solarflux` | Disable SolarFlux MQTT integration when `mqtt` profile is active. |
| `ocpp-charge-session` | Enable OCPP charge session support. See [OcppChargeSessionManagerConfig][OcppChargeSessionManagerConfig]. |
| `ocpp-v16` | Enables OCPP v1.6 integration. See references to [SolarNetOcppConfiguration][SolarNetOcppConfiguration]. |

For example, in a production deployment the `SPRING_PROFILES_ACTIVE` environment variable can be
configured as

```
SPRING_PROFILES_ACTIVE="production,dogtag,mqtt,ocpp-charge-session,ocpp-v16"
```

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

The build produces an executable JAR at `build/libs/solarin-x.y.z.jar`.


[app-config]: src/main/resources/application.yml
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
