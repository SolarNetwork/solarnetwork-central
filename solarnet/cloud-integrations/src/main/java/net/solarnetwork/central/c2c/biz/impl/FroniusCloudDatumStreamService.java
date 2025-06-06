/* ==================================================================
 * FroniusCloudDatumStreamService.java - 3/12/2024 12:19:01 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.ACTIVE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.COUNTRY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.LOCALITY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.MANUFACTURER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.POSTAL_CODE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STATE_PROVINCE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STREET_ADDRESS_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.TIME_ZONE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.UNIT_OF_MEASURE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.cache.Cache;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.client.RestOperations;
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
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
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
 * Fronius implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.2
 */
public class FroniusCloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.fronius";

	/** The data value filter key for a system ID. */
	public static final String SYSTEM_ID_FILTER = "systemId";

	/** The data value filter key for a filter ID. */
	public static final String DEVICE_ID_FILTER = "deviceId";

	/**
	 * The URI path to view information for a given system.
	 *
	 * <p>
	 * Accepts a single {@code {systemId}} parameter.
	 * </p>
	 */
	public static final String SYSTEM_URL_TEMPLATE = "/swqapi/pvsystems/{systemId}";

	/**
	 * The URI path to list the devices for a given system.
	 *
	 * <p>
	 * Accepts a single {@code {systemId}} parameter.
	 * </p>
	 */
	public static final String SYSTEM_DEVICES_URL_TEMPLATE = "/swqapi/pvsystems/{systemId}/devices";

	/**
	 * The URI path to list the history for a given device.
	 *
	 * <p>
	 * Accepts {@code {systemId}} and {@code {deviceId}} parameters.
	 * </p>
	 */
	public static final String DEVICE_HISTORY_URL_TEMPLATE = "/swqapi/pvsystems/{systemId}/devices/{deviceId}/histdata";

	/** The data value identifier levels source ID range. */
	public static final IntRange DATA_VALUE_IDENTIFIER_LEVELS_SOURCE_ID_RANGE = IntRange.rangeOf(0, 1);

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		SETTINGS = List.of(UPPER_CASE_SOURCE_ID_SETTING_SPECIFIER, SOURCE_ID_MAP_SETTING_SPECIFIER,
				VIRTUAL_SOURCE_IDS_SETTING_SPECIFIER);
	}

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SYSTEM_ID_FILTER,
			DEVICE_ID_FILTER);

	/** Metadata key for a "last import" {@code Instant}. */
	public static final String LAST_IMPORT_METADATA = "lastImport";

	/** The epoch end date query parameter name. */
	public static final String END_AT_PARAM = "to";

	/** The epoch start date query parameter name. */
	public static final String START_AT_PARAM = "from";

	/** The limit query parameter name. */
	public static final String LIMIT_PARAM = "limit";

	/** The offset query parameter name. */
	public static final String OFFSET_PARAM = "offset";

	/** The default query limit parameter value. */
	public static final int DEFAULT_QUERY_LIMIT = 500;

	/**
	 * The maximum period of time to request data for in one call to
	 * {@link #datum(CloudDatumStreamConfiguration, CloudDatumStreamQueryFilter)}.
	 */
	private static final Duration MAX_FILTER_TIME_RANGE = Duration.ofDays(7);

	/** The maximum period of time to request data for in one API request. */
	private static final Duration MAX_QUERY_TIME_RANGE = Duration.ofHours(24);

	/** The supported device types. */
	private static final Set<String> SUPPORTED_DEVICE_TYPES = Set.of("Battery", "EVCharger", "Inverter",
			"Ohmpilot", "Sensor", "SmartMeter");

	/**
	 * A cache of system IDs to associated metadata.
	 */
	private Cache<String, CloudDataValue> systemCache;
	private int queryLimit = DEFAULT_QUERY_LIMIT;

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
	 * @param clock
	 *        the clock to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public FroniusCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			Clock clock) {
		super(SERVICE_IDENTIFIER, "Fronius Datum Stream Service", clock, userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new FroniusRestOperationsHelper(
						LoggerFactory.getLogger(FroniusCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> FroniusCloudIntegrationService.SECURE_SETTINGS));
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
		List<CloudDataValue> result = Collections.emptyList();
		if ( filters != null && filters.get(SYSTEM_ID_FILTER) != null
				&& filters.get(DEVICE_ID_FILTER) != null ) {
			String systemId = filters.get(SYSTEM_ID_FILTER).toString();
			String deviceId = filters.get(DEVICE_ID_FILTER).toString();
			result = deviceChannels(integration, systemId, deviceId, filters);
		} else if ( filters != null && filters.get(SYSTEM_ID_FILTER) != null ) {
			String systemId = filters.get(SYSTEM_ID_FILTER).toString();
			result = systemDevices(integration, systemId, filters);
		} else {
			// list available systems
			result = systems(integration);
		}
		Collections.sort(result);
		return result;
	}

	private List<CloudDataValue> systems(CloudIntegrationConfiguration integration) {
		return restOpsHelper.httpGet("List systems", integration, JsonNode.class,
				(req) -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(FroniusCloudIntegrationService.LIST_SYSTEMS_URL)
						.buildAndExpand(integration.getServiceProperties()).toUri(),
				res -> parseSystems(res.getBody()));
	}

	private List<CloudDataValue> systemDevices(final CloudIntegrationConfiguration integration,
			final String systemId, Map<String, ?> filters) {
		return restOpsHelper.httpGet("List system devices", integration, JsonNode.class,
		// @formatter:off
				(req) -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(SYSTEM_DEVICES_URL_TEMPLATE)
						.buildAndExpand(filters).toUri(),
						// @formatter:on
				res -> parseSystemDevices(res.getBody(), systemId));
	}

	private List<CloudDataValue> deviceChannels(final CloudIntegrationConfiguration integration,
			final String systemId, String deviceId, Map<String, ?> filters) {
		// query history for previous 24 hours, limiting to just 1
		Instant endDate = clock.instant().truncatedTo(ChronoUnit.HOURS);
		Instant startDate = endDate.minus(1, ChronoUnit.DAYS);
		return restOpsHelper.httpGet("List devices channels", integration, JsonNode.class,
		// @formatter:off
				(req) -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(DEVICE_HISTORY_URL_TEMPLATE)
						.queryParam(START_AT_PARAM, startDate.toString())
						.queryParam(END_AT_PARAM, endDate.toString())
						.queryParam(LIMIT_PARAM, 1)
						.buildAndExpand(filters).toUri(),
						// @formatter:on
				res -> parseDeviceChannels(res.getBody(), systemId, deviceId));
	}

	private static List<CloudDataValue> parseSystems(JsonNode json) {
		/*- EXAMPLE JSON:
			{
			  "pvSystems": [
			    {
			      "pvSystemId": "3e210d7d-2b9e-43d6-830a-000000000000",
			      "name": "Site 1",
			      "address": {
			        "country": null,
			        "zipCode": null,
			        "street": null,
			        "city": null,
			        "state": null
			      },
			      "pictureURL": "https://froniussestimagesweprod/pvsystem-images/3e210d7d-2b9e-43d6-830a-000000000000",
			      "peakPower": 98340.0,
			      "installationDate": "2009-06-15T00:00:00Z",
			      "lastImport": "2025-03-24T00:35:07Z",
			      "meteoData": "pro",
			      "timeZone": "Europe/Berlin"
			    },
			    {
			      "pvSystemId": "ced6f980-8907-4128-87ea-000000000000",
			      "name": "Site 2",
			      "address": {
			        "country": null,
			        "zipCode": null,
			        "street": null,
			        "city": null,
			        "state": null
			      },
			      "pictureURL": "https://froniussestimagesweprod/pvsystem-images/ced6f980-8907-4128-87ea-000000000000",
			      "peakPower": 7600.0,
			      "installationDate": "2000-01-01T00:00:00Z",
			      "lastImport": "2025-03-23T23:15:08Z",
			      "meteoData": "light",
			      "timeZone": "Europe/London"
			    }
			  ],
			  "links": {
			    "first": "https://api.solarweb.com/swqapi/pvsystems?offset=0&limit=2",
			    "prev": "https://api.solarweb.com/swqapi/pvsystems?offset=0&limit=2",
			    "self": "https://api.solarweb.com/swqapi/pvsystems?offset=2&limit=2",
			    "next": "https://api.solarweb.com/swqapi/pvsystems?offset=4&limit=2",
			    "last": "https://api.solarweb.com/swqapi/pvsystems?offset=8&limit=2",
			    "totalItemsCount": 9
			  }
		*/
		if ( json == null ) {
			return List.of();
		}
		final var result = new ArrayList<CloudDataValue>(4);
		for ( JsonNode sysNode : json.path("pvSystems") ) {
			final String id = sysNode.path("pvSystemId").asText();
			result.add(parseSystem(sysNode, id));
		}
		return result;
	}

	private static CloudDataValue parseSystem(final JsonNode sysNode, final String systemId) {
		if ( sysNode == null ) {
			return null;
		}
		final String name = sysNode.path("name").asText().trim();
		final var meta = new LinkedHashMap<String, Object>(4);
		final JsonNode addrNode = sysNode.path("address");
		if ( addrNode.isObject() ) {
			populateNonEmptyValue(addrNode, "street", STREET_ADDRESS_METADATA, meta);
			populateNonEmptyValue(addrNode, "city", LOCALITY_METADATA, meta);
			populateNonEmptyValue(addrNode, "state", STATE_PROVINCE_METADATA, meta);
			populateNonEmptyValue(addrNode, "zipCode", POSTAL_CODE_METADATA, meta);
			populateNonEmptyValue(addrNode, "country", COUNTRY_METADATA, meta);
		}
		populateNonEmptyValue(sysNode, "timeZone", TIME_ZONE_METADATA, meta);
		populateIsoTimestampValue(sysNode, "installationDate", "installationDate", meta);
		populateIsoTimestampValue(sysNode, "lastImport", LAST_IMPORT_METADATA, meta);
		populateNumberValue(sysNode, "peakPower", "peakPower", meta);
		return intermediateDataValue(List.of(systemId), name, meta.isEmpty() ? null : meta);
	}

	private static List<CloudDataValue> parseSystemDevices(final JsonNode json, final String systemId) {
		/*- EXAMPLE JSON:
		{
		  "devices": [
		    {
		      "deviceType": "Inverter",
		      "deviceId": "7796944e-b09e-424e-a77f-000000000000",
		      "deviceName": "Primo GEN24 3.6 Plus",
		      "deviceManufacturer": "Fronius",
		      "serialNumber": "32555894",
		      "deviceTypeDetails": "Primo GEN24 3.6 Plus",
		      "dataloggerId": "pilot-0.5e-580975161653392885_0000000000",
		      "nodeType": 254,
		      "numberMPPTrackers": 2,
		      "numberPhases": 1,
		      "peakPower": {
		        "dc1": 3600.0,
		        "dc2": 0.0
		      },
		      "nominalAcPower": 3600.0,
		      "firmware": {
		        "updateAvailable": false,
		        "installedVersion": null,
		        "availableVersion": null
		      },
		      "isActive": false,
		      "activationDate": "2022-03-23T18:15:23Z",
		      "deactivationDate": "2023-07-28T14:24:06Z"
		    },
		*/
		if ( json == null ) {
			return List.of();
		}

		final List<CloudDataValue> result = new ArrayList<>(8);

		for ( JsonNode devNode : json.path("devices") ) {
			final String id = devNode.path("deviceId").asText();
			final String name = devNode.path("deviceName").asText().trim();

			final var meta = new LinkedHashMap<String, Object>(4);
			populateNonEmptyValue(devNode, "deviceManufacturer", MANUFACTURER_METADATA, meta);
			populateNonEmptyValue(devNode, "serialNumber", DEVICE_SERIAL_NUMBER_METADATA, meta);
			populateBooleanValue(devNode, "isActive", ACTIVE_METADATA, meta);
			populateNonEmptyValue(devNode, "deviceType", "deviceType", meta);

			if ( !(meta.get("deviceType") instanceof String devType
					&& SUPPORTED_DEVICE_TYPES.contains(devType)) ) {
				continue;
			}

			// Inverter
			populateNumberValue(devNode, "numberMPPTrackers", "numberMPPTrackers", meta);
			populateNumberValue(devNode, "numberPhases", "numberPhases", meta);
			populateIsoTimestampValue(devNode, "activationDate", "activationDate", meta);
			populateIsoTimestampValue(devNode, "deactivationDate", "deactivationDate", meta);

			// Battery
			populateNumberValue(devNode, "capacity", "capacity", meta);
			populateNumberValue(devNode, "maxChargePower", "maxChargePower", meta);
			populateNumberValue(devNode, "maxDischargePower", "maxDischargePower", meta);
			populateNumberValue(devNode, "maxSOC", "maxSOC", meta);
			populateNumberValue(devNode, "minSOC", "minSOC", meta);

			// SmartMeter
			populateNonEmptyValue(devNode, "deviceCategory", "deviceCategory", meta);
			populateNonEmptyValue(devNode, "deviceLocation", "deviceLocation", meta);

			// EVCharger
			populateBooleanValue(devNode, "isOnline", "isOnline", meta);

			result.add(intermediateDataValue(List.of(systemId, id), name, meta.isEmpty() ? null : meta));
		}

		return result;
	}

	private static List<CloudDataValue> parseDeviceChannels(final JsonNode json, final String systemId,
			final String deviceId) {
		/*- EXAMPLE JSON:
		{
		  "pvSystemId": "ced6f980-8907-4128-87ea-000000000000",
		  "deviceId": "3b482b62-1754-48f1-a0ca-000000000000",
		  "data": [
		    {
		      "logDateTime": "2025-03-23T01:25:00Z",
		      "logDuration": 300,
		      "channels": [
		        {
		          "channelName": "EnergyExported",
		          "channelType": "Energy",
		          "unit": "Wh",
		          "value": 0.0
		        },
		*/
		if ( json == null ) {
			return List.of();
		}

		final List<CloudDataValue> result = new ArrayList<>(8);
		for ( JsonNode dataNode : json.path("data") ) {
			for ( JsonNode channelNode : dataNode.path("channels") ) {
				final String id = channelNode.path("channelName").asText();

				final var meta = new LinkedHashMap<String, Object>(4);
				populateNonEmptyValue(channelNode, "channelType", "channelType", meta);
				populateNonEmptyValue(channelNode, "unit", UNIT_OF_MEASURE_METADATA, meta);

				result.add(dataValue(List.of(systemId, deviceId, id), id, meta.isEmpty() ? null : meta));
			}
		}
		return result;
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
	 * <li>channel</li>
	 * </ol>
	 */
	private static final Pattern VALUE_REF_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/(.+)");

	private static record ValueRef(String systemId, String deviceId, String channelName,
			CloudDatumStreamPropertyConfiguration property, String sourceId) {

		private ValueRef(String systemId, String deviceId, String channelName,
				CloudDatumStreamPropertyConfiguration property) {
			this(systemId, deviceId, channelName, property, "/%s/%s".formatted(systemId, deviceId));
		}

	}

	/**
	 * A system-specific query plan.
	 *
	 * <p>
	 * This plan is constructed from a set of
	 * {@link CloudDatumStreamPropertyConfiguration}, and used to determine
	 * which device APIs are necessary to satisfy those configurations.
	 * </p>
	 */
	private static class SystemQueryPlan {

		/** The system ID. */
		private final String systemId;

		private final Map<String, List<ValueRef>> deviceValueRefs = new LinkedHashMap<>(2);

		private SystemQueryPlan(String systemId) {
			super();
			this.systemId = requireNonNullArgument(systemId, "systemId");
		}

		private void addValueRef(ValueRef ref) {
			deviceValueRefs.computeIfAbsent(ref.deviceId, k -> new ArrayList<>(4)).add(ref);
		}

	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");
		return performAction(datumStream, (ms, ds, mapping, integration, valueProps, exprProps) -> {

			if ( valueProps.isEmpty() ) {
				String msg = "Datum stream has no properties.";
				Errors errors = new BindException(ds, "datumStream");
				errors.reject("error.datumStream.noProperties", null, msg);
				throw new ValidationException(msg, errors, ms);
			}

			final Map<String, String> sourceIdMap = servicePropertyStringMap(ds, SOURCE_ID_MAP_SETTING);

			var resultDatum = new ArrayList<GeneralDatum>();
			final Map<String, SystemQueryPlan> queryPlans = resolveSystemQueryPlans(ds, sourceIdMap,
					valueProps);

			// for each system, find last import date and query for latest
			for ( Entry<String, SystemQueryPlan> planEntry : queryPlans.entrySet() ) {
				var systemId = planEntry.getKey();
				Instant lastImportDate = lastImportDate(systemId, () -> integration);
				var filter = new BasicQueryFilter();
				filter.setStartDate(lastImportDate != null ? lastImportDate.truncatedTo(HOURS)
						: clock.instant().truncatedTo(HOURS).minus(1, HOURS));
				filter.setEndDate(filter.getStartDate().plus(1, HOURS));
				filter.setParameters(Map.of(SYSTEM_ID_FILTER, systemId));
				filter.setMax(1);

				fetchDatumForSystem(filter, datumStream, integration, sourceIdMap, planEntry.getValue(),
						resultDatum);
			}

			// evaluate expressions on merged datum
			var r = evaluateExpressions(datumStream, exprProps, resultDatum, mapping.getConfigId(),
					integration.getConfigId());

			return r.stream().map(Datum.class::cast).toList();
		});
	}

	@Override
	public CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter) {
		requireNonNullArgument(datumStream, "datumStream");
		requireNonNullArgument(filter, "filter");
		return performAction(datumStream, (ms, ds, mapping, integration, valueProps, exprProps) -> {

			if ( valueProps.isEmpty() ) {
				String msg = "Datum stream has no properties.";
				Errors errors = new BindException(ds, "datumStream");
				errors.reject("error.datumStream.noProperties", null, msg);
				throw new ValidationException(msg, errors, ms);
			}

			final Instant filterStartDate = requireNonNullArgument(filter.getStartDate(),
					"filter.startDate");
			final Instant filterEndDate = requireNonNullArgument(filter.getEndDate(),
					"filter.startDate");

			BasicQueryFilter nextQueryFilter = null;

			Instant startDate = filterStartDate.truncatedTo(HOURS);
			Instant endDate = filterEndDate.truncatedTo(HOURS);
			if ( Duration.between(startDate, endDate).compareTo(MAX_FILTER_TIME_RANGE) > 0 ) {
				Instant nextEndDate = startDate.plus(MAX_FILTER_TIME_RANGE.multipliedBy(2));
				if ( nextEndDate.isAfter(endDate) ) {
					nextEndDate = endDate;
				}

				endDate = startDate.plus(MAX_FILTER_TIME_RANGE);

				nextQueryFilter = new BasicQueryFilter();
				nextQueryFilter.setStartDate(endDate);
				nextQueryFilter.setEndDate(nextEndDate);
			}

			final Map<String, String> sourceIdMap = servicePropertyStringMap(ds, SOURCE_ID_MAP_SETTING);

			final var resultDatum = new ArrayList<GeneralDatum>();
			final Map<String, SystemQueryPlan> queryPlans = resolveSystemQueryPlans(ds, sourceIdMap,
					valueProps);

			// we can only query for at most MAX_QUERY_TIME_RANGE per request, so have to iterate over time
			final var queryFilter = new BasicQueryFilter();
			queryFilter.setStartDate(startDate);
			queryFilter.setEndDate(startDate.plus(MAX_QUERY_TIME_RANGE));

			while ( queryFilter.getStartDate().isBefore(endDate) ) {
				if ( queryFilter.getEndDate().isAfter(endDate) ) {
					queryFilter.setEndDate(endDate);
				}
				for ( Entry<String, SystemQueryPlan> planEntry : queryPlans.entrySet() ) {
					var systemId = planEntry.getKey();
					queryFilter.setParameters(Map.of(SYSTEM_ID_FILTER, systemId));

					fetchDatumForSystem(queryFilter, datumStream, integration, sourceIdMap,
							planEntry.getValue(), resultDatum);
				}
				queryFilter.setStartDate(queryFilter.getEndDate());
				queryFilter.setEndDate(queryFilter.getStartDate().plus(MAX_QUERY_TIME_RANGE));
			}

			// evaluate expressions on merged datum
			var r = evaluateExpressions(datumStream, exprProps, resultDatum, mapping.getConfigId(),
					integration.getConfigId());

			var usedFilter = new BasicQueryFilter();
			usedFilter.setStartDate(startDate);
			usedFilter.setEndDate(endDate);

			return new BasicCloudDatumStreamQueryResult(usedFilter, nextQueryFilter,
					r.stream().map(Datum.class::cast).toList());
		});
	}

	private void fetchDatumForSystem(BasicQueryFilter filter, CloudDatumStreamConfiguration datumStream,
			CloudIntegrationConfiguration integration, Map<String, String> sourceIdMap,
			SystemQueryPlan queryPlan, List<GeneralDatum> resultDatum) {
		if ( queryPlan.deviceValueRefs == null ) {
			return;
		}
		for ( Entry<String, List<ValueRef>> deviceEntry : queryPlan.deviceValueRefs.entrySet() ) {
			final String deviceId = deviceEntry.getKey();
			final int limit = filter.getMax() != null ? filter.getMax() : queryLimit;
			final var pageFilter = BasicQueryFilter.copyOf(filter);
			pageFilter.setOffset(0L);
			final var links = new Links();
			while ( links.hasMore(pageFilter.getOffset()) ) {
				List<GeneralDatum> datum = restOpsHelper.httpGet("List device data", integration,
						JsonNode.class, req -> {
							var b = fromUri(resolveBaseUrl(integration, BASE_URI))
									.path(DEVICE_HISTORY_URL_TEMPLATE)
									.queryParam(START_AT_PARAM, pageFilter.getStartDate())
									.queryParam(END_AT_PARAM, pageFilter.getEndDate())
									.queryParam(OFFSET_PARAM, pageFilter.getOffset())
									.queryParam(LIMIT_PARAM, limit);
							return b.buildAndExpand(queryPlan.systemId, deviceId).toUri();
						}, res -> {
							JsonNode json = res.getBody();
							links.parseJson(json);
							return parseDeviceDatum(json, deviceEntry.getValue(), datumStream,
									sourceIdMap, pageFilter);
						});
				if ( datum != null ) {
					resultDatum.addAll(datum);
				}
				if ( filter.getMax() != null && datum.size() >= filter.getMax() ) {
					break;
				}
				pageFilter.setOffset(pageFilter.getOffset() + limit);
			}
		}
	}

	/*-
	  "links": {
	    "first": "https://api.solarweb.com/swqapi/pvsystems?offset=0&limit=2",
	    "prev": "https://api.solarweb.com/swqapi/pvsystems?offset=0&limit=2",
	    "self": "https://api.solarweb.com/swqapi/pvsystems?offset=2&limit=2",
	    "next": "https://api.solarweb.com/swqapi/pvsystems?offset=4&limit=2",
	    "last": "https://api.solarweb.com/swqapi/pvsystems?offset=8&limit=2",
	    "totalItemsCount": 9
	  }
	 */
	private static class Links {

		private String next;
		private int count = -1;

		private void parseJson(JsonNode json) {
			if ( json == null || !json.has("links") ) {
				return;
			}
			var links = json.path("links");
			next = links.path("next").textValue();
			if ( links.hasNonNull("totalItemsCount") ) {
				count = links.path("totalItemsCount").intValue();
			}
		}

		private boolean hasMore(long offset) {
			return count < 0 || (offset < count && next != null);
		}
	}

	private List<GeneralDatum> parseDeviceDatum(JsonNode json, List<ValueRef> refs,
			CloudDatumStreamConfiguration ds, Map<String, String> sourceIdMap,
			CloudDatumStreamQueryFilter filter) {
		/*- EXAMPLE JSON:
			{
			  "pvSystemId": "ced6f980-8907-4128-87ea-000000000000",
			  "deviceId": "3b482b62-1754-48f1-a0ca-000000000000",
			  "data": [
			    {
			      "logDateTime": "2025-03-24T16:00:00Z",
			      "logDuration": 300,
			      "channels": [
			        {
			          "channelName": "EnergyExported",
			          "channelType": "Energy",
			          "unit": "Wh",
			          "value": 94.46
			        },
		 */
		if ( json == null ) {
			return List.of();
		}

		final List<GeneralDatum> result = new ArrayList<>(16);

		// only need to compute the source ID once, as the same for all device data
		String sourceId = null;

		Set<String> channelNames = refs.stream().map(ValueRef::channelName).collect(toUnmodifiableSet());

		for ( JsonNode dataNode : json.path("data") ) {
			final Instant ts;
			try {
				ts = Instant.parse(dataNode.path("logDateTime").textValue());
			} catch ( DateTimeParseException e ) {
				// ignore and continue
				continue;
			}
			if ( !ts.isBefore(filter.getEndDate()) ) {
				// query can return data past desired end date (inclusive end) so bail now
				break;
			}
			DatumSamples s = new DatumSamples();
			for ( JsonNode channelNode : dataNode.path("channels") ) {
				String channelName = channelNode.path("channelName").textValue();
				if ( !channelNames.contains(channelName) ) {
					continue;
				}

				for ( ValueRef ref : refs ) {
					if ( !channelName.equals(ref.channelName) ) {
						continue;
					}
					if ( sourceId == null ) {
						sourceId = nonEmptyString(resolveSourceId(ds, ref, sourceIdMap));
						if ( sourceId == null ) {
							return List.of();
						}
					}

					Object propVal = parseJsonDatumPropertyValue(channelNode.path("value"),
							ref.property.getPropertyType());
					propVal = ref.property.applyValueTransforms(propVal);
					if ( propVal != null ) {
						s.putSampleValue(ref.property.getPropertyType(), ref.property.getPropertyName(),
								propVal);
					}
				}
			}
			if ( s.isEmpty() ) {
				continue;
			}

			result.add(new GeneralDatum(new DatumId(ds.getKind(), ds.getObjectId(), sourceId, ts), s));
		}

		return result;
	}

	private static String resolveSourceId(CloudDatumStreamConfiguration datumStream, ValueRef ref,
			Map<String, String> sourceIdMap) {
		if ( sourceIdMap != null ) {
			return sourceIdMap.get(ref.sourceId);
		}

		String result = datumStream.getSourceId() + ref.sourceId;

		Boolean ucSourceId = datumStream.serviceProperty(UPPER_CASE_SOURCE_ID_SETTING, Boolean.class);
		if ( ucSourceId != null && ucSourceId ) {
			result = result.toUpperCase(Locale.ENGLISH);
		}

		return result;
	}

	private Map<String, SystemQueryPlan> resolveSystemQueryPlans(
			CloudDatumStreamConfiguration datumStream, Map<String, String> sourceIdMap,
			List<CloudDatumStreamPropertyConfiguration> propConfigs) {
		final var result = new LinkedHashMap<String, SystemQueryPlan>(2);
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
				// groups: 1 = systemId, 2 = deviceId, 3 = channel
				String systemId = m.group(1);
				String deviceId = m.group(2);
				String channelName = m.group(3);

				SystemQueryPlan plan = result.computeIfAbsent(systemId, id -> {
					return new SystemQueryPlan(systemId);
				});

				ValueRef valueRef = new ValueRef(systemId, deviceId, channelName, config);
				plan.addValueRef(valueRef);
			}
		}

		// TODO: resolve wildcard inverter component IDs

		return result;
	}

	private CloudDataValue systemInfo(String systemId,
			Supplier<CloudIntegrationConfiguration> integrationProvider) {
		final var cache = getSystemCache();
		CloudDataValue result = null;
		if ( cache != null ) {
			result = cache.get(systemId);
		}
		if ( result == null ) {
			var integration = integrationProvider.get();
			result = restOpsHelper.httpGet("View system information", integration, JsonNode.class,
			// @formatter:off
							(req) -> fromUri(resolveBaseUrl(integration, BASE_URI))
									.path(SYSTEM_URL_TEMPLATE)
									.buildAndExpand(systemId).toUri(),
									// @formatter:on
					res -> parseSystem(res.getBody(), systemId));
			if ( result != null && cache != null ) {
				cache.put(systemId, result);
			}
		}
		return result;
	}

	private Instant lastImportDate(String systemId,
			Supplier<CloudIntegrationConfiguration> integrationProvider) {
		CloudDataValue sys = systemInfo(systemId, integrationProvider);
		if ( sys != null && sys.getMetadata() != null
				&& sys.getMetadata().get(LAST_IMPORT_METADATA) instanceof Instant date ) {
			return date;
		}
		return null;
	}

	/**
	 * Get the system cache.
	 *
	 * @return the cache
	 */
	public Cache<String, CloudDataValue> getSystemCache() {
		return systemCache;
	}

	/**
	 * Set the system cache.
	 *
	 * @param systemCache
	 *        the cache to set
	 */
	public void setSystemCache(Cache<String, CloudDataValue> systemCache) {
		this.systemCache = systemCache;
	}

	/**
	 * Get the query limit.
	 *
	 * @return the limit; defaults to {@link #DEFAULT_QUERY_LIMIT}
	 */
	public int getQueryLimit() {
		return queryLimit;
	}

	/**
	 * Set the query limit.
	 *
	 * @param queryLimit
	 *        the limit to set
	 */
	public void setQueryLimit(int queryLimit) {
		this.queryLimit = queryLimit;
	}

}
