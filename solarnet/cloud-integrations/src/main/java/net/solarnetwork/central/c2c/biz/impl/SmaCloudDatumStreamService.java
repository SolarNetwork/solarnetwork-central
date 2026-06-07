/* ==================================================================
 * SmaCloudDatumStreamService.java - 29/03/2025 7:32:45 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.c2c.biz.impl;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.impl.SmaCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.biz.impl.SmaResolution.FiveMinute;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY;
import static net.solarnetwork.central.datum.domain.DatumValidationType.TimeGap;
import static net.solarnetwork.central.datum.support.OrderedDatumSamplesBuffer.greatestTimestamp;
import static net.solarnetwork.central.datum.support.OrderedDatumSamplesBuffer.leastTimestamp;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.cache.Cache;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.RequestEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.Interval;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.http.OAuth2RestOperationsHelper;
import net.solarnetwork.central.datum.domain.DatumValidationType;
import net.solarnetwork.central.datum.support.OrderedDatumSamplesBuffer;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumStreamId;
import net.solarnetwork.domain.datum.DatumStreamIdentity;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.CollectionUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.StringUtils;
import tools.jackson.databind.JsonNode;

/**
 * SMA implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 2.3
 */
public class SmaCloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.sma";

	/** The data value filter key for a system ID. */
	public static final String SYSTEM_ID_FILTER = "systemId";

	/** The data value filter key for a device ID. */
	public static final String DEVICE_ID_FILTER = "deviceId";

	/** The setting to upper-case source ID values. */
	public static final String UPPER_CASE_SOURCE_ID_SETTING = "upperCaseSourceId";

	/**
	 * The URI path to view a given system.
	 *
	 * <p>
	 * Accepts a single {@code {systemId}} parameter.
	 * </p>
	 */
	public static final String SYSTEM_VIEW_PATH_TEMPLATE = "/v1/plants/{systemId}";

	/**
	 * The URI path to list the devices for a given system.
	 *
	 * <p>
	 * Accepts a single {@code {systemId}} parameter.
	 * </p>
	 */
	public static final String SYSTEM_DEVICES_PATH_TEMPLATE = "/v1/plants/{systemId}/devices/lean";

	/**
	 * The URI path to list the measurement sets for a given device.
	 *
	 * <p>
	 * Accepts a single {@code deviceId} parameter.
	 * </p>
	 */
	public static final String DEVICE_MEASUREMENT_SETS_PATH_TEMPLATE = "/v1/devices/{deviceId}/measurements/sets";

	/**
	 * The URI path to list the measurement sets for a given device.
	 *
	 * <p>
	 * Accepts {@code deviceId}, {@code measurementSet}, and {@code period}
	 * parameters.
	 * </p>
	 */
	public static final String DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE = "/v1/devices/{deviceId}/measurements/sets/{measurementSet}/{period}";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;

	static {
		// @formatter:off
		SETTINGS = List.of(
				UPPER_CASE_SOURCE_ID_SETTING_SPECIFIER,
				SOURCE_ID_MAP_SETTING_SPECIFIER,
				MULTI_STREAM_MAXIMUM_LAG_SETTING_SPECIFIER,
				OPERATIONAL_DATE_RANGES_SETTING_SPECIFIER,
				VIRTUAL_SOURCE_IDS_SETTING_SPECIFIER,
				VALIDATION_IGNORE_SETTING_SPECIFIER,
				ENERGY_VALIDATION_THRESHOLD_SETTING_SPECIFIER,
				TIME_GAP_VALIDATION_THRESHOLD_SETTING_SPECIFIER);
		// @formatter:on
	}

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SYSTEM_ID_FILTER,
			DEVICE_ID_FILTER);

	/** The data value identifier levels source ID range. */
	public static final IntRange DATA_VALUE_IDENTIFIER_LEVELS_SOURCE_ID_RANGE = IntRange.rangeOf(0, 2);

	/** The date query parameter name. */
	public static final String DATE_PARAM = "Date";

	/** The "return energy values" parameter name. */
	public static final String RETURN_ENERGY_VALUES_PARAM = "ReturnEnergyValues";

	/**
	 * A measurement set key for PV generation, to use during validation.
	 *
	 * @since 2.2
	 */
	public static final String PV_GENERATION_MEASUREMENT_KEY = "pvGeneration";

	/**
	 * The default maximum period of time to request data for in one call to
	 * {@link #datum(CloudDatumStreamConfiguration, CloudDatumStreamQueryFilter)}.
	 */
	private static final Duration DEFAULT_MAX_FILTER_TIME_RANGE = Duration.ofDays(7);

	private static final Logger log = LoggerFactory.getLogger(SmaCloudDatumStreamService.class);

	/**
	 * A cache of SMA system IDs to associated time zones. This is used because
	 * the timestamps returned from the API are all in site-local time.
	 */
	private @Nullable Cache<String, ZoneId> systemTimeZoneCache;

	/**
	 * A cache of SMA system IDs to associated inventory information. This is
	 * used to resolve system characteristics for use in data validation.
	 */
	private @Nullable Cache<String, CloudDataValue[]> systemInventoryCache;

	private Duration maxFilterTimeRange = DEFAULT_MAX_FILTER_TIME_RANGE;

	/**
	 * Constructor.
	 *
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param expressionService
	 *        the expression service
	 * @param integrationDao
	 *        the integration DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param datumStreamMappingDao
	 *        the datum stream mapping DAO
	 * @param datumStreamPropertyDao
	 *        the datum stream property DAO
	 * @param restOps
	 *        the REST operations
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @param clock
	 *        the instant source to use
	 * @param integrationLocksCache
	 *        an optional cache that, when provided, will be used to obtain a
	 *        lock before acquiring an access token; this can be used in prevent
	 *        concurrent requests using the same {@code config} from making
	 *        multiple token requests; not the cache is assumed to have
	 *        read-through semantics that always returns a new lock for missing
	 *        keys
	 * @throws IllegalArgumentException
	 *         if any argument except {@code integrationLocksCache} is
	 *         {@code null}
	 */
	public SmaCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager, Clock clock,
			@Nullable Cache<UserLongCompositePK, Lock> integrationLocksCache) {
		super(SERVICE_IDENTIFIER, "SMA Datum Stream Service", clock, userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new OAuth2RestOperationsHelper(LoggerFactory.getLogger(SmaCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						_ -> SmaCloudIntegrationService.SECURE_SETTINGS, oauthClientManager, clock,
						integrationLocksCache));
	}

	@Override
	protected Iterable<String> supportedPlaceholders() {
		return SUPPORTED_PLACEHOLDERS;
	}

	@Override
	protected IntRange dataValueIdentifierLevelsSourceIdRange() {
		return DATA_VALUE_IDENTIFIER_LEVELS_SOURCE_ID_RANGE;
	}

	@Override
	public Iterable<LocalizedServiceInfo> supportedValidations(Locale locale) {
		MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");
		List<LocalizedServiceInfo> result = new ArrayList<>(2);
		for ( String key : new String[] { DatumValidationType.EnergySpike.getKey(),
				DatumValidationType.TimeGap.getKey() } ) {
			result.add(new BasicLocalizedServiceInfo(key, locale,
					ms.getMessage("validationType.%s.key".formatted(key), null, key, locale),
					ms.getMessage("validationType.%s.desc".formatted(key), null, null, locale), null));
		}
		return result;
	}

	@Override
	public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
		MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");
		List<LocalizedServiceInfo> result = new ArrayList<>(2);
		for ( String key : new String[] { SYSTEM_ID_FILTER, DEVICE_ID_FILTER } ) {
			result.add(new BasicLocalizedServiceInfo(key, locale,
					ms.getMessage("dataValueFilter.%s.key".formatted(key), null, key, locale),
					ms.getMessage("dataValueFilter.%s.desc".formatted(key), null, null, locale), null));
		}
		return result;
	}

	@Override
	public Iterable<CloudDataValue> dataValues(UserLongCompositePK integrationId,
			@Nullable Map<String, ?> filters) {
		final CloudIntegrationConfiguration integration = requireNonNullObject(
				integrationDao.get(requireNonNullArgument(integrationId, "integrationId")),
				"integration");
		List<CloudDataValue> result = null;
		if ( filters != null && filters.get(SYSTEM_ID_FILTER) != null
				&& filters.get(DEVICE_ID_FILTER) != null ) {
			String systemId = filters.get(SYSTEM_ID_FILTER).toString();
			String deviceId = filters.get(DEVICE_ID_FILTER).toString();
			result = deviceMeasurements(integration, systemId, deviceId, filters);
		} else if ( filters != null && filters.get(SYSTEM_ID_FILTER) != null ) {
			String systemId = filters.get(SYSTEM_ID_FILTER).toString();
			result = systemDevices(integration, systemId, filters);
		} else {
			result = systems(integration);
		}
		return result;
	}

	private List<CloudDataValue> systems(CloudIntegrationConfiguration integration) {
		final Map<String, ?> sprops = integration.getServiceProperties();
		List<CloudDataValue> result = restOpsHelper.httpGet("List systems", integration, JsonNode.class,
				_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(SmaCloudIntegrationService.LIST_SYSTEMS_PATH)
						.buildAndExpand(sprops != null ? sprops : Map.of()).toUri(),
				(_, res) -> parseSystems(res.getBody()));

		return result;
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	public static List<CloudDataValue> parseSystems(@Nullable JsonNode json) {
		if ( json == null ) {
			return List.of();
		}
		/*- EXAMPLE JSON:
		{
		  "plants": [
		    {...},
		*/
		final var result = new ArrayList<CloudDataValue>(4);
		for ( JsonNode sysNode : json.path("plants") ) {
			result.addAll(parseSystem(sysNode, null));
		}
		return result;
	}

	private static List<CloudDataValue> parseSystem(@Nullable JsonNode json,
			@Nullable Collection<CloudDataValue> children) {
		/*- EXAMPLE JSON:
		    {
		      "plantId": "7190000",
		      "name": "Site 1",
		      "timezone": "America/New_York",
		      "installation": {
		        "peakPower": 184000.0,
		        "acNominalPower": 184000.0,
		        "calcAcNominalPower": 148920.0,
		        "dcPowerInputMax": 216000.0,
		        "co2SavingsFactor": 649,
		        "startUpUtc": "2021-11-03T00:00:00",
		        "orientation": {
		          "azimuth": 28,
		          "collectorSlope": 24
		        }
		      },
		      "location": {
		        "currencyCode3": "USD",
		        "address": {
		          "country": "US",
		          "federalState": "Massachusetts",
		          "city": "Anytown",
		          "zipCode": "00001",
		          "street": "Main Street",
		          "streetNo": "123",
		          "longitude": -71.070000,
		          "latitude": 42.380000
		        }
		      }
		    }
		*/
		if ( json == null ) {
			return List.of();
		}
		final String id = json.path("plantId").asString();
		final String name = json.path("name").asString().trim();

		final var meta = new LinkedHashMap<String, Object>(4);
		populateNonEmptyValue(json, "timezone", CloudDataValue.TIME_ZONE_METADATA, meta);

		JsonNode addrNode = json.path("location").path("address");

		if ( addrNode.has("street") && addrNode.has("streetNo") ) {
			meta.put(CloudDataValue.STREET_ADDRESS_METADATA, "%s %s".formatted(
					addrNode.get("streetNo").stringValue(), addrNode.get("street").stringValue()));
		}

		populateNonEmptyValue(addrNode, "city", CloudDataValue.LOCALITY_METADATA, meta);
		populateNonEmptyValue(addrNode, "federalState", CloudDataValue.STATE_PROVINCE_METADATA, meta);
		populateNonEmptyValue(addrNode, "country", CloudDataValue.COUNTRY_METADATA, meta);
		populateNonEmptyValue(addrNode, "zipCode", CloudDataValue.POSTAL_CODE_METADATA, meta);
		populateNumberValue(addrNode, "latitude", CloudDataValue.LATITUDE_METADATA, meta);
		populateNumberValue(addrNode, "longitude", CloudDataValue.LONGITUDE_METADATA, meta);

		JsonNode instNode = json.path("installation");
		populateTimestampValue(instNode, "startUpUtc", CloudDataValue.START_DATE_METADATA, meta,
				s -> LocalDateTime.parse(s).toInstant(ZoneOffset.UTC));
		populateNumberValue(instNode, "peakPower", CloudDataValue.RATED_POWER_METADATA, meta);
		populateNumberValue(instNode, "acNominalPower", "acNominalPower", meta);
		populateNumberValue(instNode, "dcPowerInputMax", "dcPowerInputMax", meta);
		populateNumberValue(instNode, "co2SavingsFactor", "co2SavingsFactor", meta);

		JsonNode orientNode = instNode.path("orientation");
		populateNumberValue(orientNode, "azimuth", CloudDataValue.AZIMUTH_METADATA, meta);
		populateNumberValue(orientNode, "collectorSlope", CloudDataValue.TILT_METADATA, meta);

		return List.of(intermediateDataValue(List.of(id), name, meta, children));
	}

	private List<CloudDataValue> systemDevices(final CloudIntegrationConfiguration integration,
			final String systemId, Map<String, ?> filters) {
		return restOpsHelper.httpGet("List system devices", integration, JsonNode.class,
		// @formatter:off
				_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(SYSTEM_DEVICES_PATH_TEMPLATE)
						.queryParam("WithDeactivatedDevices", true)
						.buildAndExpand(filters).toUri(),
						// @formatter:on
				(_, res) -> parseSystemDevices(integration, res.getBody(), systemId));
	}

	/**
	 * Parse system device JSON.
	 *
	 * @param integration
	 *        the integration
	 * @param json
	 *        the JSON to parse
	 * @param systemId
	 *        the system ID
	 * @return the data values, never {@code null}
	 */
	public List<CloudDataValue> parseSystemDevices(final CloudIntegrationConfiguration integration,
			final @Nullable JsonNode json, final String systemId) {
		/*- EXAMPLE JSON:
		{
		  "devices": [
		    {
		      "deviceId": "16",
		      "name": "My Inverter 1",
		      "type": "Solar Inverters",
		      "product": "STP 6000TL-20",
		      "productId": 9099,
		      "serial": "3421111",
		      "vendor": "SMA Solar Technology AG",
		      "generatorPower": 6000.0,
		      "generatorPowerDc": 6000.0,
		      "isActive": true,
		      "deactivatedAt": "2019-04-07T12:30:02"
		    },
		*/
		if ( json == null ) {
			return List.of();
		}

		final List<CloudDataValue> result = new ArrayList<>(8);

		for ( JsonNode devNode : json.path("devices") ) {
			final String id = devNode.path("deviceId").asString();
			final String name = devNode.path("name").asString().trim();

			final var meta = new LinkedHashMap<String, Object>(4);
			populateNonEmptyValue(devNode, "product", CloudDataValue.DEVICE_MODEL_METADATA, meta);
			populateNonEmptyValue(devNode, "vendor", CloudDataValue.MANUFACTURER_METADATA, meta);
			populateNonEmptyValue(devNode, "serial", CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA, meta);
			populateBooleanValue(devNode, "isActive", CloudDataValue.ACTIVE_METADATA, meta);
			populateTimestampValue(devNode, "deactivatedAt", CloudDataValue.DEACTIVATED_AT_METADATA,
					meta, s -> {
						try {
							ZoneId zone = resolveSystemTimeZone(integration, systemId);
							return LocalDateTime.parse(s).atZone(zone).toInstant();
						} catch ( Exception e ) {
							return null;
						}
					});

			populateNonEmptyValue(devNode, "type", "type", meta);
			populateNumberValue(devNode, "productId", "productId", meta);
			populateNumberValue(devNode, "generatorPower", CloudDataValue.RATED_POWER_METADATA, meta);
			populateNumberValue(devNode, "generatorPowerDc", "generatorPowerDc", meta);

			result.add(intermediateDataValue(List.of(systemId, id), name, meta, null));
		}

		return result;
	}

	private List<CloudDataValue> deviceMeasurements(final CloudIntegrationConfiguration integration,
			final String systemId, final String deviceId, Map<String, ?> filters) {
		return restOpsHelper.httpGet("List device measurement sets", integration, JsonNode.class,
		// @formatter:off
				_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(DEVICE_MEASUREMENT_SETS_PATH_TEMPLATE)
						.buildAndExpand(filters).toUri(),
						// @formatter:on
				(_, res) -> parseDeviceMeasurements(res.getBody(), systemId, deviceId));
	}

	private static List<CloudDataValue> parseDeviceMeasurements(final @Nullable JsonNode json,
			final String systemId, final String deviceId) {
		/*- EXAMPLE JSON:
			{
			  "plant": {
			    "plantId": "11260000",
			    "name": "Site 2",
			    "timezone": "America/New_York"
			  },
			  "device": {
			    "deviceId": "11260000",
			    "name": "WR8KU002 SN:2001500000",
			    "timezone": "America/New_York"
			  },
			  "sets": [
			    "EnergyAndPowerPv",
			    "PowerDc",
			    "PowerAc"
			  ]
			}
		*/
		if ( json == null ) {
			return List.of();
		}

		final List<CloudDataValue> result = new ArrayList<>(8);

		for ( JsonNode measSetNode : json.path("sets") ) {
			final String measSetName = measSetNode.stringValue();
			try {
				final SmaMeasurementSetType measSet = SmaMeasurementSetType.fromValue(measSetName);
				if ( measSet != null ) {
					final List<CloudDataValue> measurementValues = new ArrayList<>(
							measSet.getMeasurements().size());
					for ( SmaMeasurementType<?> measType : measSet.getMeasurements().values() ) {
						measurementValues.add(CloudDataValue.dataValue(
								List.of(systemId, deviceId, measSet.name(), measType.name()),
								measType.name(),
								Map.of(CloudDataValue.DESCRIPTION_METADATA, measType.description())));
					}
					result.add(intermediateDataValue(List.of(systemId, deviceId, measSet.name()),
							measSet.name(),
							Map.of(CloudDataValue.DESCRIPTION_METADATA, measSet.getDescription()),
							measurementValues));
				}
			} catch ( IllegalArgumentException e ) {
				log.warn("Unsupported SMA measurement set: {}", measSetName);
			}
		}

		return result;
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");
		return performAction(datumStream, (ms, ds, mapping, integration, valueProps, exprProps) -> {
			return query(null, ms, ds, mapping, integration, valueProps, exprProps, SmaPeriod.Recent);
		}).getResults();
	}

	@Override
	public CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter) {
		requireNonNullArgument(datumStream, "datumStream");
		requireNonNullArgument(filter, "filter");
		return performAction(datumStream, (ms, ds, mapping, integration, valueProps, exprProps) -> {
			return query(filter, ms, ds, mapping, integration, valueProps, exprProps, SmaPeriod.Day);
		});
	}

	private BasicCloudDatumStreamQueryResult query(@Nullable CloudDatumStreamQueryFilter filter,
			MessageSource ms, CloudDatumStreamConfiguration ds,
			CloudDatumStreamMappingConfiguration mapping, CloudIntegrationConfiguration integration,
			List<CloudDatumStreamPropertyConfiguration> valueProps,
			List<CloudDatumStreamPropertyConfiguration> exprProps, SmaPeriod queryPeriod) {
		if ( valueProps.isEmpty() ) {
			String msg = "Datum stream has no properties.";
			Errors errors = new BindException(ds, "datumStream");
			errors.reject("error.datumStream.noProperties", null, msg);
			throw new ValidationException(msg, errors, ms);
		}

		final Map<String, String> sourceIdMap = ds.servicePropertyStringMap(SOURCE_ID_MAP_SETTING);
		final Map<String, Map<String, Interval>> systemDeviceOperationalRanges = resolveOperationalRanges(
				ds);

		final QueryPlan plan = resolveQueryPlan(integration, ds, sourceIdMap, valueProps);

		final Instant filterStartDate = FiveMinute.tickStart(
				queryPeriod != SmaPeriod.Recent
						? requireNonNullArgument(requireNonNullArgument(filter, "filter").getStartDate(),
								"filter.startDate")
						: clock.instant(),
				UTC);
		final Instant filterEndDate = FiveMinute.tickStart(
				queryPeriod != SmaPeriod.Recent
						? requireNonNullArgument(requireNonNullArgument(filter, "filter").getEndDate(),
								"filter.endDate")
						: filterStartDate.plus(1, DAYS),
				UTC);

		BasicQueryFilter nextQueryFilter = null;
		if ( queryPeriod != SmaPeriod.Recent ) {
			if ( Duration.between(filterStartDate, filterEndDate).compareTo(maxFilterTimeRange) > 0 ) {
				Instant nextStartDate = filterStartDate.plus(maxFilterTimeRange);
				Instant nextEndDate = nextStartDate.plus(maxFilterTimeRange);
				if ( nextEndDate.isAfter(filterEndDate) ) {
					nextEndDate = filterEndDate;
				}
				nextQueryFilter = new BasicQueryFilter();
				nextQueryFilter.setStartDate(nextStartDate);
				nextQueryFilter.setEndDate(nextEndDate);
			}
		}

		final BasicQueryFilter usedQueryFilter = new BasicQueryFilter();
		usedQueryFilter.setStartDate(filterStartDate);
		usedQueryFilter
				.setEndDate(nextQueryFilter != null ? nextQueryFilter.getStartDate() : filterEndDate);

		// have to combine measurement set queries into datum instances by source ID, date
		final OrderedDatumSamplesBuffer streamBuffer = new OrderedDatumSamplesBuffer();

		final Set<String> ignoredValidations = ds.servicePropertyStringSet(VALIDATION_IGNORE_SETTING);

		// query cache of device max energy per tick, for data validation
		final Map<String, Integer> deviceMaxPower = new HashMap<>(8);

		// for each zone, iterate over days and devices

		for ( Entry<ZoneId, Map<String, DeviceQueryPlan>> zoneEntry : plan.zoneDevicePlans.entrySet() ) {
			ZoneId zone = zoneEntry.getKey();
			final ZonedDateTime startTs = nonnull(usedQueryFilter.getStartDate(), "Start date")
					.atZone(zone);
			final ZonedDateTime endTs = nonnull(usedQueryFilter.getEndDate(), "End date").atZone(zone);

			// the final query day includes the endTs day, unless that is exactly at start of day
			final LocalDate endDay = endTs.toLocalDate()
					.plusDays(endTs.truncatedTo(DAYS).isBefore(endTs) ? 1 : 0);

			for ( LocalDate day = startTs.toLocalDate(); day.isBefore(endDay); day = day.plusDays(1) ) {
				final String queryDay = day.toString();
				final Interval queryDayRange = Interval.of(day.atStartOfDay(zone).toInstant(),
						day.plusDays(1).atStartOfDay(zone).toInstant());
				for ( DeviceQueryPlan devPlan : zoneEntry.getValue().values() ) {
					if ( shouldSkipQueryForDeviceData(systemDeviceOperationalRanges, devPlan,
							queryDayRange) ) {
						continue;
					}

					String sourceId = nonEmptyString(resolveSourceId(ds, sourceIdMap, devPlan));
					if ( sourceId == null ) {
						continue;
					}

					final DatumStreamIdentity streamIdent = DatumStreamId
							.datumStreamId(ds.getKind(), ds.getObjectId(), sourceId).toIdentity();

					for ( Entry<SmaMeasurementSetType, List<ValueRef>> measurementSetEntry : devPlan.measurementSetRefs
							.entrySet() ) {
						final String taskName = "Get source [%s] system %s device %s day %s measurements"
								.formatted(sourceId, devPlan.systemId, devPlan.deviceId, queryDay);
						restOpsHelper.httpGet(taskName, integration, JsonNode.class, _ -> {
							UriComponentsBuilder b = fromUri(resolveBaseUrl(integration, BASE_URI))
									.path(DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE);
							if ( queryPeriod != SmaPeriod.Recent ) {
								b.queryParam(DATE_PARAM, queryDay);
							}
							return b.queryParam(RETURN_ENERGY_VALUES_PARAM,
									measurementSetEntry.getKey().shouldReturnEnergyValues())
									.buildAndExpand(devPlan.deviceId, measurementSetEntry.getKey(),
											queryPeriod.getKey())
									.toUri();
						}, (req, res) -> parseDeviceDatum(integration, devPlan.systemId,
								devPlan.deviceId, req, res.getBody(), deviceMaxPower, ignoredValidations,
								usedQueryFilter, devPlan.zone, measurementSetEntry.getValue(), ds,
								streamIdent, streamBuffer));
					}
				}
			}
		}

		final List<GeneralDatum> allDatum = streamBuffer.datum(GeneralDatum::new);

		// evaluate expressions on merged datum
		var r = evaluateExpressions(ds, exprProps, allDatum, mapping.getConfigId(),
				integration.getConfigId());

		// latest datum might not have been reported yet; check latest datum date (per stream), and if
		// less than expected date make that the next query start date
		final Map<DatumStreamIdentity, Instant> greatestTimestampPerStream = streamBuffer
				.greatestTimestampPerStream();
		final Duration multiStreamMaximumLag = multiStreamMaximumLag(ds);
		if ( multiStreamMaximumLag.compareTo(Duration.ZERO) > 0
				&& greatestTimestampPerStream.size() > 1 ) {
			Instant leastGreatestTimestampAcrossStreams = leastTimestamp(
					greatestTimestampPerStream.values());
			Instant greatestTimestampAcrossStreams = greatestTimestamp(
					greatestTimestampPerStream.values());
			if ( leastGreatestTimestampAcrossStreams != null && greatestTimestampAcrossStreams != null
					&& leastGreatestTimestampAcrossStreams.isBefore(greatestTimestampAcrossStreams)
					&& Duration.between(leastGreatestTimestampAcrossStreams, clock.instant())
							.compareTo(multiStreamMaximumLag) < 0 ) {
				if ( nextQueryFilter == null ) {
					nextQueryFilter = new BasicQueryFilter();
				}
				nextQueryFilter.setStartDate(
						FiveMinute.nextTickStart(leastGreatestTimestampAcrossStreams, UTC));
			}
		}

		return new BasicCloudDatumStreamQueryResult(
				queryPeriod != SmaPeriod.Recent ? usedQueryFilter : null, nextQueryFilter,
				r.stream().map(Datum.class::cast).toList(), streamBuffer.auxiliaryOrNull());
	}

	/**
	 * Convert an operational range mapping into a nested system/device/interval
	 * mapping.
	 *
	 * @param ds
	 *        the configuration to extract the operational range mapping from
	 * @return the mapping, or {@code null} if not available
	 */
	private @Nullable Map<String, Map<String, Interval>> resolveOperationalRanges(
			CloudDatumStreamConfiguration ds) {
		final Map<String, Interval> rangeMapping = ds
				.servicePropertyIntervalMap(OPERATIONAL_DATE_RANGES_SETTING);
		if ( rangeMapping == null ) {
			return null;
		}
		final int sizeHint = rangeMapping.size();
		final Map<String, Map<String, Interval>> result = new LinkedHashMap<>(sizeHint);
		for ( Entry<String, Interval> e : rangeMapping.entrySet() ) {
			Matcher m = DEVICE_VALUE_REF_PATTERN.matcher(e.getKey());
			if ( m.find() ) {
				result.computeIfAbsent(m.group(1), _ -> new LinkedHashMap<>(sizeHint)).put(m.group(2),
						e.getValue());
			}
		}
		return (!result.isEmpty() ? result : null);
	}

	/**
	 * Test if a device query plan should be skipped for a given date due to an
	 * operational range constraint.
	 *
	 * @param systemDeviceOperationalRanges
	 *        the range constraints as a nested mapping of
	 *        system/device/interval
	 * @param devPlan
	 *        the device plan to test
	 * @param dayRange
	 *        the date range in question
	 * @return {@code true} if a range constraint exists for the input arguments
	 *         and no query should be performed for this device on this date
	 */
	private boolean shouldSkipQueryForDeviceData(
			final @Nullable Map<String, Map<String, Interval>> systemDeviceOperationalRanges,
			DeviceQueryPlan devPlan, Interval dayRange) {
		final Map<String, Interval> deviceRanges = (systemDeviceOperationalRanges != null
				? systemDeviceOperationalRanges.get(devPlan.systemId)
				: null);
		final Interval deviceRange = (deviceRanges != null ? deviceRanges.get(devPlan.deviceId) : null);
		if ( deviceRange == null ) {
			return false;
		}
		return !dayRange.overlaps(deviceRange);
	}

	private static @Nullable String resolveSourceId(CloudDatumStreamConfiguration datumStream,
			@Nullable Map<String, String> sourceIdMap, DeviceQueryPlan devPlan) {
		String baseSourceId = "/%s/%s".formatted(devPlan.systemId, devPlan.deviceId);
		if ( sourceIdMap != null ) {
			return sourceIdMap.get(baseSourceId);
		}

		String result = datumStream.getSourceId() + baseSourceId;

		Boolean ucSourceId = datumStream.serviceProperty(UPPER_CASE_SOURCE_ID_SETTING, Boolean.class);
		if ( ucSourceId != null && ucSourceId ) {
			result = result.toUpperCase(Locale.ENGLISH);
		}

		return result;
	}

	// note an empty list is _always_ returned as we populate resultDatum but return type required
	private List<GeneralDatum> parseDeviceDatum(CloudIntegrationConfiguration integration,
			String systemId, String deviceId, RequestEntity<Void> request, @Nullable JsonNode json,
			Map<String, Integer> deviceMaxPower, Set<String> ignoredValidations,
			CloudDatumStreamQueryFilter filter, ZoneId zone, List<ValueRef> valueRefs,
			CloudDatumStreamConfiguration ds, DatumStreamIdentity streamId,
			OrderedDatumSamplesBuffer streamBuffer) {
		/*- EXAMPLE JSON:
		{
		  "plant": {
		    "plantId": "11260000",
		    "name": "Site 1",
		    "timezone": "America/New_York"
		  },
		  "device": {
		    "deviceId": "11260000",
		    "name": "WR8KU002 SN:2001200000",
		    "timezone": "America/New_York"
		  },
		  "setType": "EnergyAndPowerPv",
		  "resolution": "FiveMinutes",
		  "set": [
		    {
		      "time": "2025-03-28T06:50:00",
		      "pvGeneration": 0.0
		    },
		 */
		if ( json == null ) {
			return List.of();
		}

		final Map<String, Object> refParameters = Map.of(SYSTEM_ID_FILTER, systemId, DEVICE_ID_FILTER,
				deviceId);

		// SMA data is queried by day, and the first data starts at 00:05 and ends at next day at 00:00;
		// if the filter end date is a whole day, that is normally exclusive but we will cut off the
		// last element if we omit it so allow that value, even though is inclusive of the end date
		final boolean endDateIsEod = nonnull(filter.getEndDate(), "End date").atZone(zone)
				.getHour() == 0;

		// use the device rated (maximum) power to validate energy readings. We see invalid energy
		// data returned from /v1/devices/{deviceId}/measurements/sets/EnergyAndPowerPv/Day?ReturnEnergyValues=true
		// sometimes, like when a new device comes online and the initial 5min reading is way too high, e.g.
		//
		//  "resolution": "FiveMinutes",
		//  "set": [
		//          {
		//            "time": "2026-04-27T20:05:00",
		//            "pvGeneration": 1024843.0
		//          },
		//
		// In this example, the device metadata "generatorPower" is 7680, so for a 5min period we'd expect
		// no more than 7680 * 5/60 = 640 Wh. Thus we can discard this data as "invalid" to work around the issue.

		Integer maxPower = null;
		if ( !ignoredValidations.contains(DatumValidationType.EnergySpike.getKey()) ) {
			maxPower = deviceMaxPower.get(deviceId);
			if ( maxPower == null ) {
				maxPower = resolveDeviceGeneratorPower(integration, systemId, deviceId);
				if ( maxPower != null ) {
					deviceMaxPower.put(deviceId, maxPower);
				}
			}
		}

		final Duration timeGapDuration = (!ignoredValidations.contains(TimeGap.getKey())
				? resolveTimeGapValidationThreshold(ds)
				: null);

		final String deviceRef = "/%s/%s".formatted(systemId, deviceId);

		// use the resolution value as the fallback time value between readings, i.e. for the first reading of the day
		long tickSeconds = 0;
		if ( maxPower != null ) {
			final SmaResolution resolution = SmaResolution
					.fromValue(json.path("resolution").stringValue(null));
			tickSeconds = resolution.getTickAmount().get(ChronoUnit.SECONDS);
		}

		final double energyValidationThreshold = resolveEnergyValidationThreshold(ds);

		final var datumIsNew = new MutableBoolean(false);

		for ( JsonNode dataNode : json.path("set") ) {
			if ( !dataNode.has("time") ) {
				continue;
			}

			Instant ts = LocalDateTime.parse(dataNode.get("time").stringValue()).atZone(zone)
					.toInstant();
			if ( ts.isBefore(filter.getStartDate()) ) {
				continue;
			} else if ( endDateIsEod ? ts.isAfter(filter.getEndDate())
					: !ts.isBefore(filter.getEndDate()) ) {
				break;
			}

			Instant prevTs = streamBuffer.previousTimestamp(streamId, ts);

			if ( (maxPower != null || timeGapDuration != null) && prevTs == null ) {
				// look up previous datum so we can perform validation
				final var prevDatum = lookupPreviousDatum(ds, streamId.getSourceId(), ts);
				if ( prevDatum != null ) {
					prevTs = prevDatum.getTimestamp();
				}
			}

			// only track time-gap validation on new datum
			datumIsNew.setFalse();
			final DatumSamples samples = streamBuffer.getOrCreate(streamId, ts, datumIsNew);
			for ( ValueRef ref : valueRefs ) {
				JsonNode measurementNode = dataNode.path(ref.measurement.name());
				if ( measurementNode == null || measurementNode.isNull()
						|| measurementNode.isMissingNode() ) {
					continue;
				}
				Object propVal = ref.measurement.parser().apply(measurementNode);

				if ( propVal instanceof Map<?, ?> m ) {
					for ( Entry<?, ?> e : m.entrySet() ) {
						String key = e.getKey().toString();
						propVal = e.getValue();
						populateSampleProp(samples, ref, propVal, "_" + key.toLowerCase(Locale.ENGLISH));
					}
				} else {
					populateSampleProp(samples, ref, propVal, null);
				}

				if ( maxPower != null && (prevTs != null || tickSeconds > 0)
						&& ref.measurementSet.name().startsWith("Energy")
						&& PV_GENERATION_MEASUREMENT_KEY.equals(ref.measurement.name())
						&& propVal instanceof Number gen ) {
					streamBuffer.addAuxiliary(streamId,
							validateEnergyDataValue(ds, request, ref.property.getValueReference(),
									refParameters, gen, maxPower, energyValidationThreshold,
									prevTs != null ? prevTs : ts.minusSeconds(tickSeconds),
									streamId.datumIdentity(ts)));
				}
			}

			if ( samples.isEmpty() ) {
				streamBuffer.removeTimestamp(streamId, ts, samples);
				continue;
			}

			if ( datumIsNew.booleanValue() && timeGapDuration != null && prevTs != null ) {
				streamBuffer.addAuxiliary(streamId, validateTimeGap(ds, request, deviceRef,
						refParameters, timeGapDuration, prevTs, streamId.datumIdentity(ts)));
			}
		}

		return List.of();
	}

	private void populateSampleProp(DatumSamples samples, ValueRef ref, Object propVal,
			@Nullable String propNameSuffix) {
		propVal = ref.property.applyValueTransforms(propVal);
		if ( propVal != null ) {
			String propName = ref.property.getPropertyName();
			if ( propNameSuffix != null ) {
				propName = propName + propNameSuffix;
			}
			samples.putSampleValue(ref.property.getPropertyType(), propName, propVal);
		}
	}

	/**
	 * Value reference pattern, with component matching groups.
	 *
	 * <p>
	 * The matching groups are
	 * </p>
	 *
	 * <ol>
	 * <li>systemId</li>
	 * <li>deviceId</li>
	 * <li>measurementSet</li>
	 * <li>measurement</li>
	 * </ol>
	 */
	private static final Pattern VALUE_REF_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)/(.+)");

	private record ValueRef(String systemId, String deviceId, SmaMeasurementSetType measurementSet,
			SmaMeasurementType<?> measurement, CloudDatumStreamPropertyConfiguration property) {

	}

	/**
	 * Value reference pattern, with component matching groups.
	 *
	 * <p>
	 * The matching groups are
	 * </p>
	 *
	 * <ol>
	 * <li>systemId</li>
	 * <li>deviceId</li>
	 * </ol>
	 */
	private static final Pattern DEVICE_VALUE_REF_PATTERN = Pattern.compile("/([^/]+)/([^/]+)");

	private static final class DeviceQueryPlan {

		/** The system ID. */
		private final String systemId;

		/** The device ID. */
		private final String deviceId;

		/** The time zone used by this system. */
		private final ZoneId zone;

		/** The measurement sets required. */
		private final Map<SmaMeasurementSetType, List<ValueRef>> measurementSetRefs = new LinkedHashMap<>(
				8);

		private DeviceQueryPlan(String systemId, String deviceId, ZoneId zone) {
			super();
			this.systemId = requireNonNullArgument(systemId, "systemId");
			this.deviceId = requireNonNullArgument(deviceId, "deviceId");
			this.zone = requireNonNullArgument(zone, "zone");
		}

		private void addValueRef(ValueRef ref) {
			assert ref != null;
			measurementSetRefs.computeIfAbsent(ref.measurementSet, _ -> new ArrayList<>(4)).add(ref);
		}
	}

	private static final class QueryPlan {

		private final Map<ZoneId, Map<String, DeviceQueryPlan>> zoneDevicePlans = new LinkedHashMap<>(2);

		private void addValueRef(ZoneId zone, ValueRef ref) {
			assert ref != null;
			zoneDevicePlans.computeIfAbsent(zone, _ -> new LinkedHashMap<>(4))
					.computeIfAbsent(ref.deviceId,
							_ -> new DeviceQueryPlan(ref.systemId, ref.deviceId, zone))
					.addValueRef(ref);
		}

	}

	/**
	 * Compute a mapping of device IDs to associated {@link DeviceQueryPlan}
	 * instances.
	 *
	 * @param integration
	 *        the integration
	 * @param datumStream
	 *        the datum stream
	 * @param sourceIdMap
	 *        the source ID mapping
	 * @param propConfigs
	 *        the property configurations
	 * @return the query plan
	 */
	private QueryPlan resolveQueryPlan(CloudIntegrationConfiguration integration,
			CloudDatumStreamConfiguration datumStream, @Nullable Map<String, String> sourceIdMap,
			List<CloudDatumStreamPropertyConfiguration> propConfigs) {
		final var plan = new QueryPlan();

		@SuppressWarnings("unchecked")
		List<Map<String, ?>> placeholderSets = resolvePlaceholderSets(
				datumStream.serviceProperty(PLACEHOLDERS_SERVICE_PROPERTY, Map.class),
				(sourceIdMap != null ? sourceIdMap.keySet() : null));

		for ( CloudDatumStreamPropertyConfiguration config : propConfigs ) {
			for ( Map<String, ?> ph : placeholderSets ) {
				String ref = StringUtils.expandTemplateString(config.getValueReference(), ph);
				Matcher m = VALUE_REF_PATTERN.matcher(ref);
				if ( !m.matches() ) {
					continue;
				}
				// groups: 1 = systemId, 2 = deviceId, 3 = measurementSet, 4 = measurement
				String systemId = m.group(1);
				String deviceId = m.group(2);
				String measurementSetName = m.group(3);
				String measurementName = m.group(4);

				SmaMeasurementSetType measurementSet;
				try {
					measurementSet = SmaMeasurementSetType.fromValue(measurementSetName);
				} catch ( IllegalArgumentException e ) {
					// ignore and continue
					continue;
				}

				var measurements = (measurementSet != null ? measurementSet.getMeasurements() : null);
				SmaMeasurementType<?> measurement = (measurements != null
						? measurements.get(measurementName)
						: null);
				if ( measurementSet == null || measurement == null ) {
					// ignore and continue
					log.warn(
							"Unsupported measurement name [{}] on CloudDatumStreamPropertyConfiguration {} value reference [{}]",
							measurementName, config.getId(), config.getValueReference());
					continue;
				}

				ZoneId zone = resolveSystemTimeZone(integration, systemId);
				ValueRef valueRef = new ValueRef(systemId, deviceId, measurementSet, measurement,
						config);
				plan.addValueRef(zone, valueRef);
			}
		}

		return plan;
	}

	private ZoneId resolveSystemTimeZone(CloudIntegrationConfiguration integration, String systemId) {
		assert integration != null && systemId != null;
		final var cache = getSystemTimeZoneCache();

		ZoneId result = (cache != null ? cache.get(systemId) : null);
		if ( result != null ) {
			return result;
		}

		/*- EXAMPLE JSON:
			{
				"plantId": "7190000",
				"name": "Site 1",
				"description": "Site 1 description",
				"timezone": "America/New_York"
			}
		 */

		result = restOpsHelper.httpGet("Query for system details", integration, JsonNode.class,
		// @formatter:off
				_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
							.path(SYSTEM_VIEW_PATH_TEMPLATE)
							.buildAndExpand(systemId)
							.toUri()
					// @formatter:on
				, (_, res) -> {
					ZoneId zone = ZoneOffset.UTC;
					var json = res.getBody();
					String zoneId = json != null
							? StringUtils.nonEmptyString(json.findValue("timezone").asString())
							: null;
					if ( zoneId != null ) {
						try {
							zone = ZoneId.of(zoneId);
						} catch ( DateTimeException e ) {
							log.warn("System [{}] time zone [{}] not usable, will use UTC: {}", systemId,
									zoneId, e.toString());
						}
					}
					return zone;
				});

		if ( result != null && cache != null ) {
			cache.put(systemId, result);
		}

		return result;
	}

	private CloudDataValue @Nullable [] resolveSystemInventory(CloudIntegrationConfiguration integration,
			String systemId) {
		final var cache = getSystemInventoryCache();

		CloudDataValue[] result = (cache != null ? cache.get(systemId) : null);
		if ( result != null ) {
			return result;
		}

		List<CloudDataValue> response = systemDevices(integration, systemId,
				Map.of(SYSTEM_ID_FILTER, systemId));
		if ( response != null ) {
			result = response.toArray(CloudDataValue[]::new);
			if ( cache != null ) {
				cache.put(systemId, result);
			}
		}

		return result;
	}

	private @Nullable Integer resolveDeviceGeneratorPower(CloudIntegrationConfiguration integration,
			String systemId, String deviceId) {
		CloudDataValue[] systemInventory = resolveSystemInventory(integration, systemId);
		if ( systemInventory == null ) {
			return null;
		}
		for ( CloudDataValue dv : systemInventory ) {
			if ( deviceId.equals(dv.getIdentifiers().getLast()) ) {
				Map<String, ?> m = dv.getMetadata();
				return CollectionUtils.getMapInteger(CloudDataValue.RATED_POWER_METADATA, m);
			}
		}
		return null;
	}

	/**
	 * Get the system time zone cache.
	 *
	 * @return the cache
	 */
	public final @Nullable Cache<String, ZoneId> getSystemTimeZoneCache() {
		return systemTimeZoneCache;
	}

	/**
	 * Set the system time zone cache.
	 *
	 * <p>
	 * This cache can be provided to help with time zone lookup by SolarEdge
	 * system ID.
	 * </p>
	 *
	 * @param systemTimeZoneCache
	 *        the cache to set
	 */
	public final void setSystemTimeZoneCache(@Nullable Cache<String, ZoneId> systemTimeZoneCache) {
		this.systemTimeZoneCache = systemTimeZoneCache;
	}

	/**
	 * Get the maximum filter time range.
	 *
	 * @return the range; defaults to
	 *         {@link SmaCloudDatumStreamService#DEFAULT_MAX_FILTER_TIME_RANGE}
	 */
	public final Duration getMaxFilterTimeRange() {
		return maxFilterTimeRange;
	}

	/**
	 * Set the maximum filter time range.
	 *
	 * @param maxFilterTimeRange
	 *        the range to set; if {@code null} then
	 *        {@link #DEFAULT_MAX_FILTER_TIME_RANGE} will be set instead
	 */
	public final void setMaxFilterTimeRange(Duration maxFilterTimeRange) {
		this.maxFilterTimeRange = (maxFilterTimeRange != null ? maxFilterTimeRange
				: DEFAULT_MAX_FILTER_TIME_RANGE);
	}

	/**
	 * Get the system inventory cache.
	 *
	 * @return the cache, or {@code null}
	 * @since 2.2
	 */
	public final @Nullable Cache<String, CloudDataValue[]> getSystemInventoryCache() {
		return systemInventoryCache;
	}

	/**
	 * Set the system inventory cache.
	 *
	 * <p>
	 * This cache can be provided to help with device lookup by SMA system ID.
	 * </p>
	 *
	 * @param systemInventoryCache
	 *        the cache to set
	 * @since 2.2
	 */
	public final void setSystemInventoryCache(
			@Nullable Cache<String, CloudDataValue[]> systemInventoryCache) {
		this.systemInventoryCache = systemInventoryCache;
	}

}
