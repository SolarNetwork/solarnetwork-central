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

import static java.time.temporal.ChronoUnit.DAYS;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.impl.SmaCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
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
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.StringUtils;

/**
 * SMA implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.2
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
		SETTINGS = List.of(UPPER_CASE_SOURCE_ID_SETTING_SPECIFIER, SOURCE_ID_MAP_SETTING_SPECIFIER,
				VIRTUAL_SOURCE_IDS_SETTING_SPECIFIER);
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
	 * The defaut maximum period of time to request data for in one call to
	 * {@link #datum(CloudDatumStreamConfiguration, CloudDatumStreamQueryFilter)}.
	 */
	private static final Duration DEFAULT_MAX_FILTER_TIME_RANGE = Duration.ofDays(7);

	private static final Logger log = LoggerFactory.getLogger(SmaCloudDatumStreamService.class);

	/**
	 * A cache of SMA system IDs to associated time zones. This is used because
	 * the timestamps returned from the API are all in site-local time.
	 */
	private Cache<String, ZoneId> systemTimeZoneCache;
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
	 *         if any argument is {@literal null}
	 */
	public SmaCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager, Clock clock,
			Cache<UserLongCompositePK, Lock> integrationLocksCache) {
		super(SERVICE_IDENTIFIER, "SMA Datum Stream Service", clock, userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new OAuth2RestOperationsHelper(LoggerFactory.getLogger(SmaCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> SmaCloudIntegrationService.SECURE_SETTINGS,
						oauthClientManager, clock, integrationLocksCache));
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
			Map<String, ?> filters) {
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
		List<CloudDataValue> result = restOpsHelper.httpGet("List systems", integration, JsonNode.class,
				req -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(SmaCloudIntegrationService.LIST_SYSTEMS_PATH)
						.buildAndExpand(integration.getServiceProperties()).toUri(),
				res -> parseSystems(res.getBody(), null));

		return result;
	}

	private static List<CloudDataValue> parseSystems(JsonNode json, Map<String, ?> filters) {
		if ( json == null ) {
			return Collections.emptyList();
		}
		/*- EXAMPLE JSON:
		{
		  "plants": [
		    {...},
		*/
		final var result = new ArrayList<CloudDataValue>(4);
		for ( JsonNode sysNode : json.path("plants") ) {
			result.addAll(parseSystem(sysNode, filters, null));
		}
		return result;
	}

	private static List<CloudDataValue> parseSystem(JsonNode json, Map<String, ?> filters,
			Collection<CloudDataValue> children) {
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
		final String id = json.path("plantId").asText();
		final String name = json.path("name").asText().trim();

		final var meta = new LinkedHashMap<String, Object>(4);
		populateNonEmptyValue(json, "timezone", CloudDataValue.TIME_ZONE_METADATA, meta);

		JsonNode addrNode = json.path("location").path("address");

		if ( addrNode.has("street") && addrNode.has("streetNo") ) {
			meta.put(CloudDataValue.STREET_ADDRESS_METADATA, "%s %s".formatted(
					addrNode.get("streetNo").textValue(), addrNode.get("street").textValue()));
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
		populateNumberValue(instNode, "peakPower", "peakPower", meta);
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
				req -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(SYSTEM_DEVICES_PATH_TEMPLATE)
						.buildAndExpand(filters).toUri(),
						// @formatter:on
				res -> parseSystemDevices(res.getBody(), systemId));
	}

	private static List<CloudDataValue> parseSystemDevices(final JsonNode json, final String systemId) {
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
		      "isActive": true
		    },
		*/
		if ( json == null ) {
			return List.of();
		}

		final List<CloudDataValue> result = new ArrayList<>(8);

		for ( JsonNode devNode : json.path("devices") ) {
			final String id = devNode.path("deviceId").asText();
			final String name = devNode.path("name").asText().trim();

			final var meta = new LinkedHashMap<String, Object>(4);
			populateNonEmptyValue(devNode, "product", CloudDataValue.DEVICE_MODEL_METADATA, meta);
			populateNonEmptyValue(devNode, "vendor", CloudDataValue.MANUFACTURER_METADATA, meta);
			populateNonEmptyValue(devNode, "serial", CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA, meta);
			populateBooleanValue(devNode, "isActive", CloudDataValue.ACTIVE_METADATA, meta);

			populateNonEmptyValue(devNode, "type", "type", meta);
			populateNumberValue(devNode, "productId", "productId", meta);
			populateNumberValue(devNode, "generatorPower", "generatorPower", meta);
			populateNumberValue(devNode, "generatorPowerDc", "generatorPowerDc", meta);

			result.add(intermediateDataValue(List.of(systemId, id), name, meta, null));
		}

		return result;
	}

	private List<CloudDataValue> deviceMeasurements(final CloudIntegrationConfiguration integration,
			final String systemId, final String deviceId, Map<String, ?> filters) {
		return restOpsHelper.httpGet("List device measurement sets", integration, JsonNode.class,
		// @formatter:off
				req -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(DEVICE_MEASUREMENT_SETS_PATH_TEMPLATE)
						.buildAndExpand(filters).toUri(),
						// @formatter:on
				res -> parseDeviceMeasurements(res.getBody(), systemId, deviceId));
	}

	private static List<CloudDataValue> parseDeviceMeasurements(final JsonNode json,
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
			final String measSetName = measSetNode.textValue();
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

	private BasicCloudDatumStreamQueryResult query(CloudDatumStreamQueryFilter filter, MessageSource ms,
			CloudDatumStreamConfiguration ds, CloudDatumStreamMappingConfiguration mapping,
			CloudIntegrationConfiguration integration,
			List<CloudDatumStreamPropertyConfiguration> valueProps,
			List<CloudDatumStreamPropertyConfiguration> exprProps, SmaPeriod queryPeriod) {
		if ( valueProps.isEmpty() ) {
			String msg = "Datum stream has no properties.";
			Errors errors = new BindException(ds, "datumStream");
			errors.reject("error.datumStream.noProperties", null, msg);
			throw new ValidationException(msg, errors, ms);
		}

		final Map<String, String> sourceIdMap = servicePropertyStringMap(ds, SOURCE_ID_MAP_SETTING);

		final QueryPlan plan = resolveQueryPlan(integration, ds, sourceIdMap, valueProps);

		final Instant filterStartDate = SmaResolution.FiveMinute
				.tickStart(queryPeriod != SmaPeriod.Recent
						? requireNonNullArgument(filter.getStartDate(), "filter.startDate")
						: Instant.now(), ZoneOffset.UTC);
		final Instant filterEndDate = SmaResolution.FiveMinute.tickStart(queryPeriod != SmaPeriod.Recent
				? requireNonNullArgument(filter.getEndDate(), "filter.endDate")
				: filterStartDate.plus(1, DAYS), ZoneOffset.UTC);

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

		// have to combine measurement set queries into datun instances by source ID, date
		Map<String, SortedMap<Instant, GeneralDatum>> resultDatum = new LinkedHashMap<>(128);

		// for each zone, interate over days and devices
		for ( Entry<ZoneId, Map<String, DeviceQueryPlan>> zoneEntry : plan.zoneDevicePlans.entrySet() ) {
			ZoneId zone = zoneEntry.getKey();
			for ( var ts = usedQueryFilter.getStartDate(); ts.atZone(zone).toLocalDate()
					.atStartOfDay(zone).toInstant()
					.isBefore(usedQueryFilter.getEndDate()); ts = ts.plus(1, DAYS) ) {
				var day = ts.atZone(zone).toLocalDate();
				final String queryDay = day.toString();
				for ( DeviceQueryPlan devPlan : zoneEntry.getValue().values() ) {
					String sourceId = nonEmptyString(resolveSourceId(ds, sourceIdMap, devPlan));
					if ( sourceId == null ) {
						continue;
					}
					for ( Entry<SmaMeasurementSetType, List<ValueRef>> measurementSetEntry : devPlan.measurementSetRefs
							.entrySet() ) {
						restOpsHelper.httpGet("List device measurement set data", integration,
								JsonNode.class, req -> {
									UriComponentsBuilder b = fromUri(
											resolveBaseUrl(integration, BASE_URI))
													.path(DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE);
									if ( queryPeriod != SmaPeriod.Recent ) {
										b.queryParam(DATE_PARAM, queryDay);
									}
									return b.queryParam(RETURN_ENERGY_VALUES_PARAM,
											measurementSetEntry.getKey().shouldReturnEnergyValues())
											.buildAndExpand(devPlan.deviceId,
													measurementSetEntry.getKey(), queryPeriod.getKey())
											.toUri();
								},
								res -> parseDeviceDatum(res.getBody(), usedQueryFilter, devPlan.zone,
										devPlan.systemId, devPlan.deviceId,
										measurementSetEntry.getValue(), ds, sourceId, resultDatum));
					}
				}
			}
		}

		List<GeneralDatum> allDatum = resultDatum.entrySet().stream()
				.flatMap(e -> e.getValue().values().stream()).toList();

		// evaluate expressions on merged datum
		var r = evaluateExpressions(ds, exprProps, allDatum, mapping.getConfigId(),
				integration.getConfigId());

		return new BasicCloudDatumStreamQueryResult(
				queryPeriod != SmaPeriod.Recent ? usedQueryFilter : null, nextQueryFilter,
				r.stream().map(Datum.class::cast).toList());
	}

	private static String resolveSourceId(CloudDatumStreamConfiguration datumStream,
			Map<String, String> sourceIdMap, DeviceQueryPlan devPlan) {
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

	private List<GeneralDatum> parseDeviceDatum(JsonNode json, CloudDatumStreamQueryFilter filter,
			ZoneId zone, String systemId, String deviceId, List<ValueRef> valueRefs,
			CloudDatumStreamConfiguration ds, String sourceId,
			Map<String, SortedMap<Instant, GeneralDatum>> resultDatum) {
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

		for ( JsonNode dataNode : json.path("set") ) {
			if ( !dataNode.has("time") ) {
				continue;
			}
			Instant ts = LocalDateTime.parse(dataNode.get("time").textValue()).atZone(zone).toInstant();
			if ( ts.isBefore(filter.getStartDate()) ) {
				continue;
			} else if ( !ts.isBefore(filter.getEndDate()) ) {
				break;
			}
			GeneralDatum datum = resultDatum.computeIfAbsent(sourceId, k -> new TreeMap<>())
					.computeIfAbsent(ts,
							k -> new GeneralDatum(
									new DatumId(ds.getKind(), ds.getObjectId(), sourceId, k),
									new DatumSamples()));
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
						populateSampleProp(datum, ref, propVal, "_" + key.toLowerCase(Locale.ENGLISH));
					}
				} else {
					populateSampleProp(datum, ref, propVal, null);
				}
			}
		}

		return null;
	}

	private void populateSampleProp(GeneralDatum datum, ValueRef ref, Object propVal,
			String propNameSuffix) {
		propVal = ref.property.applyValueTransforms(propVal);
		if ( propVal != null ) {
			String propName = ref.property.getPropertyName();
			if ( propNameSuffix != null ) {
				propName = propName + propNameSuffix;
			}
			datum.getSamples().putSampleValue(ref.property.getPropertyType(), propName, propVal);
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

	private static record ValueRef(String systemId, String deviceId,
			SmaMeasurementSetType measurementSet, SmaMeasurementType<?> measurement,
			CloudDatumStreamPropertyConfiguration property) {

	}

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
			measurementSetRefs.computeIfAbsent(ref.measurementSet, k -> new ArrayList<>(4)).add(ref);
		}
	}

	private static final class QueryPlan {

		private final Map<ZoneId, Map<String, DeviceQueryPlan>> zoneDevicePlans = new LinkedHashMap<>(2);

		private void addValueRef(ZoneId zone, ValueRef ref) {
			assert ref != null;
			zoneDevicePlans.computeIfAbsent(zone, k -> new LinkedHashMap<>(4))
					.computeIfAbsent(ref.deviceId,
							k -> new DeviceQueryPlan(ref.systemId, ref.deviceId, zone))
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
			CloudDatumStreamConfiguration datumStream, Map<String, String> sourceIdMap,
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

				SmaMeasurementType<?> measurement = measurementSet.getMeasurements()
						.get(measurementName);
				if ( measurement == null ) {
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
				headers -> fromUri(resolveBaseUrl(integration, BASE_URI))
							.path(SYSTEM_VIEW_PATH_TEMPLATE)
							.buildAndExpand(systemId)
							.toUri()
					// @formatter:on
				, res -> {
					ZoneId zone = ZoneOffset.UTC;
					var json = res.getBody();
					String zoneId = json != null
							? StringUtils.nonEmptyString(json.findValue("timezone").asText())
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

	/**
	 * Get the system time zone cache.
	 *
	 * @return the cache
	 */
	public final Cache<String, ZoneId> getSystemTimeZoneCache() {
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
	public final void setSystemTimeZoneCache(Cache<String, ZoneId> systemTimeZoneCache) {
		this.systemTimeZoneCache = systemTimeZoneCache;
	}

	/**
	 * Get the maximum filter time range.
	 *
	 * @return the range; defaults to
	 *         {@link SmaCloudDatumStreamService#DEFAULT_MAX_FILTER_TIME_RANGE}
	 */
	public Duration getMaxFilterTimeRange() {
		return maxFilterTimeRange;
	}

	/**
	 * Set the maximum filter time range.
	 *
	 * @param maxFilterTimeRange
	 *        the range to set; if {@code null} then
	 *        {@link #DEFAULT_MAX_FILTER_TIME_RANGE} will be set instead
	 */
	public void setMaxFilterTimeRange(Duration maxFilterTimeRange) {
		this.maxFilterTimeRange = (maxFilterTimeRange != null ? maxFilterTimeRange
				: DEFAULT_MAX_FILTER_TIME_RANGE);
	}

}
