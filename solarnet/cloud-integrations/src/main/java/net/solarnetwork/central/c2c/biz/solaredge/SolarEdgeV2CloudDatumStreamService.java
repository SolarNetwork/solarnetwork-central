/* ==================================================================
 * SolarEdgeV2CloudDatumStreamService.java - 13 Aug 2026 4:29:16 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.solaredge;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.comparing;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.solaredge.SolarEdgeV2CloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY;
import static net.solarnetwork.central.datum.domain.DatumValidationType.TimeGap;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SequencedSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.cache.Cache;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.RequestEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.client.RestOperations;
import org.threeten.extra.Interval;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseRestOperationsCloudDatumStreamService;
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
import net.solarnetwork.central.datum.domain.DatumValidationType;
import net.solarnetwork.central.datum.support.OrderedDatumSamplesBuffer;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumStreamId.DatumStreamIdent;
import net.solarnetwork.domain.datum.DatumStreamIdentity;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.StringUtils;
import tools.jackson.databind.JsonNode;

/**
 * SolarEdge implementation of {@link CloudDatumStreamService} using the V2 API.
 *
 * @author matt
 * @version 1.0
 */
public class SolarEdgeV2CloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.solaredge.v2";

	/** The data value filter key for a site ID. */
	public static final String SITE_ID_FILTER = "siteId";

	/**
	 * The data value filter key for a {@link SolarEdgeDeviceType} device type.
	 */
	public static final String DEVICE_TYPE_FILTER = "deviceType";

	/** The data value filter key for a component ID. */
	public static final String COMPONENT_ID_FILTER = "componentId";

	/** The setting for resolution. */
	public static final String RESOLUTION_SETTING = "resolution";

	/** The setting to upper-case source ID values. */
	public static final String UPPER_CASE_SOURCE_ID_SETTING = "upperCaseSourceId";

	/** The setting to device index based source ID values. */
	public static final String INDEX_BASED_SOURCE_ID_SETTING = "indexBasedSourceId";

	/** The query parameter for the maximum number of sites per response. */
	public static final String SITES_MAX_RESULTS_PARAM = "sites-in-page";

	/** The query parameter for a comma-delimited list of device types. */
	public static final String TYPES_PARAM = "types";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// menu for granularity
		var resolutionSpec = new BasicMultiValueSettingSpecifier(RESOLUTION_SETTING,
				SolarEdgeResolution.FifteenMinute.getKey());
		// @formatter:off
		var resolutionTitles = unmodifiableMap(Arrays.stream(SolarEdgeResolution.values())
				.filter(e -> e != SolarEdgeResolution.Total)
				.collect(Collectors.toMap(SolarEdgeResolution::getKey, SolarEdgeResolution::getKey,
						(_, r) -> r, () -> new LinkedHashMap<>(SolarEdgeResolution.values().length - 1))));
		// @formatter:on
		resolutionSpec.setValueTitles(resolutionTitles);

		// @formatter:off
		SETTINGS = List.of(
				  UPPER_CASE_SOURCE_ID_SETTING_SPECIFIER
				, new BasicToggleSettingSpecifier(INDEX_BASED_SOURCE_ID_SETTING, false)
				, resolutionSpec
				, SOURCE_ID_MAP_SETTING_SPECIFIER
				, MULTI_STREAM_MAXIMUM_LAG_SETTING_SPECIFIER
				, VIRTUAL_SOURCE_IDS_SETTING_SPECIFIER
				, VALIDATION_IGNORE_SETTING_SPECIFIER
				, TIME_GAP_VALIDATION_THRESHOLD_SETTING_SPECIFIER
				);
		// @formatter:on
	}

	/**
	 * The URI path to list the inventory for a given site.
	 *
	 * <p>
	 * Accepts a single {@code {siteId}} parameter.
	 * </p>
	 */
	public static final String SITE_INVENTORY_URL_TEMPLATE = "/sites/{siteId}/devices";

	/**
	 * The URI path to view device information.
	 *
	 * <p>
	 * Accepts two parameters: {@code {siteId}} and {@code {componentId}}.
	 * </p>
	 */
	public static final String DEVICE_INFO_URL_TEMPLATE = "/sites/{siteId}/devices/{componentId}";

	/**
	 * The URI path to view the details for a given site.
	 *
	 * <p>
	 * Accepts a single {@code {siteId}} parameter.
	 * </p>
	 */
	public static final String SITE_DETAILS_URL_TEMPLATE = "/sites/{siteId}";

	/**
	 * The URI path to list device telemetry data.
	 *
	 * <p>
	 * Accepts three parameters: {@code {siteId}}, {@code {deviceType}}, and
	 * {@code {componentId}} (comma-delimited list supported).
	 * </p>
	 */
	public static final String DEVICE_TELEMETRY_URL_TEMPLATE = "/sites/{siteId}/{deviceType}/{componentId}/telemetry";

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SITE_ID_FILTER, DEVICE_TYPE_FILTER,
			COMPONENT_ID_FILTER);

	/** The supported data value wildcard levels. */
	public static final List<Integer> SUPPORTED_DATA_VALUE_WILDCARD_LEVELS = List.of(2);

	/** The data value identifier levels source ID range. */
	public static final IntRange DATA_VALUE_IDENTIFIER_LEVELS_SOURCE_ID_RANGE = IntRange.rangeOf(0, 2);

	/** The maximum length of time to query for data. */
	public static final Duration MAX_QUERY_TIME_RANGE = Duration.ofDays(5);

	/**
	 * A cache of SolarEdge site IDs to associated time zones. This is used
	 * because the timestamps returned from the API are all in site-local time.
	 */
	private @Nullable Cache<Long, ZoneId> siteTimeZoneCache;

	/**
	 * A cache of SolarEdge site IDs to associated inventory information. This
	 * is used to resolve the available device identifiers for a given site.
	 */
	private @Nullable Cache<Long, CloudDataValue[]> siteInventoryCache;

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
	 *         if any argument is {@code null}
	 */
	public SolarEdgeV2CloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			Clock clock) {
		super(SERVICE_IDENTIFIER, "SolarEdge V2 Datum Stream Service", clock, userEventAppenderBiz,
				encryptor, expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new SolarEdgeV2RestOperationsHelper(clock,
						LoggerFactory.getLogger(SolarEdgeV2CloudDatumStreamService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						_ -> SolarEdgeV2CloudIntegrationService.SECURE_SETTINGS));
	}

	@Override
	protected Iterable<String> supportedPlaceholders() {
		return SUPPORTED_PLACEHOLDERS;
	}

	@Override
	protected Iterable<Integer> supportedDataValueWildcardIdentifierLevels() {
		return SUPPORTED_DATA_VALUE_WILDCARD_LEVELS;
	}

	@Override
	protected IntRange dataValueIdentifierLevelsSourceIdRange() {
		return DATA_VALUE_IDENTIFIER_LEVELS_SOURCE_ID_RANGE;
	}

	@Override
	public Iterable<LocalizedServiceInfo> supportedValidations(Locale locale) {
		MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");
		List<LocalizedServiceInfo> result = new ArrayList<>(2);
		for ( String key : new String[] { DatumValidationType.TimeGap.getKey() } ) {
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
		for ( String key : new String[] { SITE_ID_FILTER, DEVICE_TYPE_FILTER, COMPONENT_ID_FILTER } ) {
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
		List<CloudDataValue> result;
		if ( filters != null && filters.get(SITE_ID_FILTER) != null
				&& filters.get(DEVICE_TYPE_FILTER) != null
				&& filters.get(COMPONENT_ID_FILTER) != null ) {
			result = component(filters.get(SITE_ID_FILTER).toString(),
					SolarEdgeDeviceType.fromValue(filters.get(DEVICE_TYPE_FILTER).toString()),
					filters.get(COMPONENT_ID_FILTER).toString());
		} else if ( filters != null && filters.get(SITE_ID_FILTER) != null ) {
			final CloudDataValue[] inventory = resolveSiteInventory(integration,
					Long.valueOf(filters.get(SITE_ID_FILTER).toString()));
			result = Arrays.asList(inventory);
		} else {
			// list available sites
			result = sites(integration);
		}
		result.sort(null);
		return result;
	}

	private CloudDataValue @Nullable [] resolveSiteInventory(CloudIntegrationConfiguration integration,
			Long siteId) {
		assert integration != null && siteId != null;
		final var cache = getSiteInventoryCache();

		CloudDataValue[] result = (cache != null ? cache.get(siteId) : null);
		if ( result != null ) {
			return result;
		}

		List<CloudDataValue> response = siteInventory(integration, siteId,
				Map.of(SITE_ID_FILTER, siteId));
		if ( response != null ) {
			result = response.toArray(CloudDataValue[]::new);
			if ( cache != null ) {
				cache.put(siteId, result);
			}
		}

		return result;
	}

	private List<CloudDataValue> siteInventory(final CloudIntegrationConfiguration integration,
			final Long siteId, final Map<String, ?> filters) {
		return restOpsHelper.httpGet("List site inventory", integration, JsonNode.class,
				_ -> fromUri(resolveBaseUrl(integration, BASE_URI)).path(SITE_INVENTORY_URL_TEMPLATE)
						.queryParam(TYPES_PARAM, SolarEdgeDeviceType.ALL_API_TYPES)
						.buildAndExpand(filters).toUri(),
				(_, res) -> parseSiteInventory(siteId.toString(), res.getBody()));
	}

	private List<CloudDataValue> sites(CloudIntegrationConfiguration integration) {
		var sprops = integration.getServiceProperties();
		return restOpsHelper.httpGet("List sites", integration, JsonNode.class,
				_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(SolarEdgeV2CloudIntegrationService.SITES_LIST_URL)
						.queryParam(SITES_MAX_RESULTS_PARAM, 1000)
						.buildAndExpand(sprops != null ? sprops : Map.of()).toUri(),
				(_, res) -> parseSites(res.getBody()));
	}

	private static List<CloudDataValue> parseSites(@Nullable JsonNode json) {
		if ( json == null ) {
			return new ArrayList<>(0);
		}
		/*- EXAMPLE JSON:
		{
		  "sites": {
		    "count": -2147483648,
		    "site": [
		      {
		        "siteId": 93082,
		        "name": "Smith, John CRM1234",
		        "peakPower": 6.14,
		        "installationDate": "2022-11-10",
		        "location": {
		          "address": "2888 Main St",
		          "city": "Green Bay",
		          "state": "Wisconsin",
		          "zip": "54311",
		          "country": "United States"
		        },
		        "activationStatus": "ACTIVE",
		        "note": "Created via API, triggered from CRM"
		      },
		      ...
		*/
		final var result = new ArrayList<CloudDataValue>(4);
		for ( JsonNode siteNode : json.path("sites").path("site") ) {
			CloudDataValue site = parseSite(siteNode);
			if ( site != null ) {
				result.add(site);
			}
		}
		return result;
	}

	private static @Nullable CloudDataValue parseSite(@Nullable JsonNode siteNode) {
		/*- EXAMPLE JSON:
		{
		  "siteId": 93082,
		  "name": "Smith, John CRM1234",
		  "accountId": 123456,
		  "peakPower": 6.14,
		  "installationDate": "2022-11-10",
		  "lastUpdateTime": "2023-02-08T13:32:57.597429300+02:00",
		  "location": {
		    "address": "2888 Main St",
			"address2": "P.O. Box 123",
			"latitude": -1.7976931348623157e+308,
			"longitude": -1.7976931348623157e+308,
		    "city": "Green Bay",
		    "state": "Wisconsin",
		    "zip": "54311",
		    "country": "United States",
			"countryCode": "US",
			"timezone": "America/Chicago"
		  },
		  "activationStatus": "ACTIVE",
		  "note": "Created via API, triggered from CRM"
		}
		*/
		if ( siteNode == null ) {
			return null;
		}
		final String id = siteNode.path("siteId").asString();
		final String name = siteNode.path("name").asString().trim();

		final var meta = new LinkedHashMap<String, Object>(4);
		populateNumberValue(siteNode, "peakPower", CloudDataValue.RATED_POWER_METADATA, meta, 3, 2);
		populateTimestampValue(siteNode, "installationDate", CloudDataValue.START_DATE_METADATA, meta,
				s -> {
					var ts = DateUtils.parseIsoTimestamp(s, ZoneOffset.UTC);
					return (ts != null ? ts.toInstant() : null);
				});

		final JsonNode locNode = siteNode.path("location");
		if ( locNode.isObject() ) {
			populateNumberValue(locNode, "latitude", CloudDataValue.LATITUDE_METADATA, meta);
			populateNumberValue(locNode, "longitude", CloudDataValue.LONGITUDE_METADATA, meta);
			populateNonEmptyValue(locNode, "address", CloudDataValue.STREET_ADDRESS_METADATA, meta);
			populateNonEmptyValue(locNode, "city", CloudDataValue.LOCALITY_METADATA, meta);
			populateNonEmptyValue(locNode, "state", CloudDataValue.STATE_PROVINCE_METADATA, meta);
			populateNonEmptyValue(locNode, "zip", CloudDataValue.POSTAL_CODE_METADATA, meta);
			if ( locNode.has("countryCode") ) {
				populateNonEmptyValue(locNode, "countryCode", CloudDataValue.COUNTRY_METADATA, meta);
			} else {
				populateNonEmptyValue(locNode, "country", CloudDataValue.COUNTRY_METADATA, meta);
			}
			populateNonEmptyValue(locNode, "timezone", CloudDataValue.TIME_ZONE_METADATA, meta);
		}
		populateNonEmptyValue(siteNode, "activationStatus", "activationStatus", meta);
		meta.put(CloudDataValue.ACTIVE_METADATA,
				meta.get("activationStatus") != null && "ACTIVE".equals(meta.get("activationStatus")));
		populateNonEmptyValue(siteNode, "note", "notes", meta);

		return intermediateDataValue(List.of(id), name, meta.isEmpty() ? null : meta);
	}

	/**
	 * Parse a site inventory JSON response into a data values list.
	 *
	 * @param siteId
	 *        the site ID
	 * @param json
	 *        the JSON to parse
	 * @return the data values, never {@code null}
	 */
	public static List<CloudDataValue> parseSiteInventory(final String siteId,
			final @Nullable JsonNode json) {
		if ( json == null ) {
			return new ArrayList<>(0);
		}
		/*- EXAMPLE JSON:
		[
		  {
		    "type": "INVERTER",
		    "serialNumber": "AAAAD9D0-31",
		    "manufacturer": "SolarEdge",
		    "partNumber": "SE33.3K-USR8IBNZ4",
		    "powerModel": "------",
		    "createdAt": "2025-08-02T08:36:28-04:00",
		    "firmwareVersion": "4.24.518",
		    "connectedTo": "AAAA86EB-06",
		    "active": true,
		    "nameplate": 33300.0,
		    "name": "Inverter 10",
		    "form": null,
		    "communicationType": "RS485",
		    "firmware": "4.24.518.1.20.24115.2.20.24016",
		    "connectedOptimizers": 58
		  },
		*/
		final Map<SolarEdgeDeviceType, List<CloudDataValue>> values = new TreeMap<>(); // TreeMap for consistent group order
		for ( final JsonNode inventoryNode : json ) {
			final SolarEdgeDeviceType type;
			try {
				type = SolarEdgeDeviceType.fromValue(inventoryNode.path("type").asString(null));
			} catch ( IllegalArgumentException e ) {
				// unsupported type
				continue;
			}
			final String componentId = inventoryNode.path("serialNumber").asString(null);
			if ( componentId == null ) {
				continue;
			}

			final String name = inventoryNode.path("name").asString(null);
			if ( name == null ) {
				continue;
			}

			final var meta = new LinkedHashMap<String, Object>(4);
			meta.put(CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA, componentId);
			populateNonEmptyValue(inventoryNode, "manufacturer", CloudDataValue.MANUFACTURER_METADATA,
					meta);

			populateNonEmptyValue(inventoryNode, "model", CloudDataValue.DEVICE_MODEL_METADATA, meta);
			populateNonEmptyValue(inventoryNode, "partNumber", "partNumber", meta);
			if ( !meta.containsKey(CloudDataValue.DEVICE_MODEL_METADATA)
					&& meta.containsKey("partNumber") ) {
				// treat part number as model
				meta.put(CloudDataValue.DEVICE_MODEL_METADATA, meta.remove("partNumber"));
			}

			populateNonEmptyValue(inventoryNode, "firmwareVersion",
					CloudDataValue.DEVICE_FIRMWARE_VERSION_METADATA, meta);
			populateBooleanValue(inventoryNode, "active", CloudDataValue.ACTIVE_METADATA, meta);
			populateNumberValue(inventoryNode, "nameplate", CloudDataValue.RATED_POWER_METADATA, meta, 0,
					2);
			populateIsoTimestampValue(inventoryNode, "createdAt", CloudDataValue.ACTIVATED_AT_METADATA,
					meta);
			populateNonEmptyValue(inventoryNode, "connectedTo",
					CloudDataValue.RELATED_IDENTIFIER_METADATA, meta);

			populateNonEmptyValue(inventoryNode, "powerModel", "powerModel", meta);
			if ( "------".equals(meta.get("powerModel")) ) {
				meta.remove("powerModel");
			}

			populateNonEmptyValue(inventoryNode, "connectedToName", "connectedToName", meta);
			populateNonEmptyValue(inventoryNode, "meterType", "meterType", meta);
			populateNonEmptyValue(inventoryNode, "form", "form", meta);
			populateNonEmptyValue(inventoryNode, "communicationType", "communicationType", meta);
			populateNumberValue(inventoryNode, "connectedOptimizers", "connectedOptimizers", meta);

			values.computeIfAbsent(type, _ -> new ArrayList<>(8))
					.add(intermediateDataValue(List.of(siteId, type.getKey(), componentId), name, meta));
		}
		final List<CloudDataValue> result = new ArrayList<>(values.size());
		for ( Entry<SolarEdgeDeviceType, List<CloudDataValue>> e : values.entrySet() ) {
			e.getValue().sort(comparing(CloudDataValue::getName, CASE_INSENSITIVE_NATURAL_SORT)
					.thenComparing(d -> d.getIdentifiers().getLast()));
			result.add(intermediateDataValue(List.of(siteId, e.getKey().getKey()),
					e.getKey().getGroupKey(), null, e.getValue()));
		}
		return result;
	}

	private static List<CloudDataValue> component(final String siteId,
			final SolarEdgeDeviceType deviceType, final String componentId) {
		final SolarEdgeTelemetryType[] types = switch (deviceType) {
			case Inverter -> SolarEdgeInverterTelemtry.values();
			case Meter -> SolarEdgeMeterTelemtry.values();
			case Battery -> SolarEdgeBatteryTelemtry.values();
		};
		final List<CloudDataValue> values = new ArrayList<>(types.length);
		for ( SolarEdgeTelemetryType type : types ) {
			values.add(dataValue(List.of(siteId, deviceType.getKey(), componentId, type.name()),
					type.description()));
		}
		return values;
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");
		final SolarEdgeResolution resolution = resolveResolution(datumStream, null);
		final Instant endDate = resolution.tickStart(clock.instant(), UTC);
		final Instant startDate = resolution.prevTickStart(endDate, UTC);

		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);

		final var result = datum(datumStream, filter);
		if ( result == null ) {
			return List.of();
		}
		return result.getResults();
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

			final SolarEdgeResolution resolution = resolveResolution(ds, filter.getParameters());

			final Map<String, String> sourceIdMap = ds.servicePropertyStringMap(SOURCE_ID_MAP_SETTING);
			final Map<String, Map<String, Map<String, Interval>>> componentOperationalRanges = resolve3LevelOperationalRanges(
					ds, COMPONENT_VALUE_REF_PATTERN);

			// validation support
			final Set<String> ignoredValidations = ds
					.servicePropertyStringSet(VALIDATION_IGNORE_SETTING);
			final var streamBuffer = new OrderedDatumSamplesBuffer();

			final Duration timeGapDuration = (!ignoredValidations.contains(TimeGap.getKey())
					? resolveTimeGapValidationThreshold(datumStream)
					: null);

			final Map<Long, SiteQueryPlan> queryPlans = resolveSiteQueryPlans(integration, ds,
					sourceIdMap, valueProps, componentOperationalRanges);

			BasicQueryFilter nextQueryFilter = null;

			Instant startDate = resolution.tickStart(filterStartDate, UTC);
			Instant endDate = resolution.tickStart(filterEndDate, UTC);
			if ( endDate.isBefore(filterEndDate) ) {
				endDate = resolution.nextTickStart(endDate, UTC);
			}
			if ( endDate.isAfter(startDate.plus(resolution.getQueryMax())) ) {
				// query range too long, so truncate
				Instant nextEndDate = startDate.plus(resolution.getQueryMax())
						.plus(resolution.getQueryMax());
				if ( nextEndDate.isAfter(endDate) ) {
					nextEndDate = endDate;
				}

				endDate = startDate.plus(resolution.getQueryMax());

				nextQueryFilter = new BasicQueryFilter();
				nextQueryFilter.setStartDate(endDate);
				nextQueryFilter.setEndDate(nextEndDate);
			}

			final BasicQueryFilter usedQueryFilter = new BasicQueryFilter();
			usedQueryFilter.setStartDate(startDate);
			usedQueryFilter.setEndDate(endDate);
			for ( SiteQueryPlan queryPlan : queryPlans.values() ) {
				final Interval queryRange = Interval.of(startDate, endDate);
				for ( SolarEdgeDeviceType deviceType : SolarEdgeDeviceType.values() ) {
					final Set<String> componentIds = queryPlan.componentIds(deviceType, queryRange);
					if ( componentIds.isEmpty() ) {
						continue;
					}
					restOpsHelper.httpGet("List %s data".formatted(deviceType), integration,
							JsonNode.class,
							// @formatter:off
							_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
								.path(DEVICE_TELEMETRY_URL_TEMPLATE)
								.queryParam("from", queryRange.getStart())
								.queryParam("to", queryRange.getEnd())
								.buildAndExpand(
										queryPlan.siteId,
										deviceType.getTelemetryType(),
										commaDelimitedStringFromCollection(componentIds))
								.toUri(),
							// @formatter:on
							(req, res) -> parseTelemetry(req, res.getBody(), queryPlan, deviceType, ds,
									sourceIdMap, timeGapDuration, streamBuffer));
				}
			}

			final List<GeneralDatum> resultDatum = streamBuffer.datum(GeneralDatum::new);

			// evaluate expressions on merged datum
			var r = evaluateExpressions(datumStream, exprProps, resultDatum, mapping.getConfigId(),
					integration.getConfigId());

			nextQueryFilter = resolveNextQueryFilterForMultiStreamLag(ds, streamBuffer, nextQueryFilter,
					resolution.getTickAmount(), UTC, filterEndDate, endDate);

			return new BasicCloudDatumStreamQueryResult(usedQueryFilter, nextQueryFilter,
					r.stream().map(Datum.class::cast).toList(), streamBuffer.auxiliaryOrNull());
		});
	}

	private SolarEdgeResolution resolveResolution(@Nullable CloudDatumStreamConfiguration datumStream,
			@Nullable Map<String, ?> parameters) {
		SolarEdgeResolution result = null;
		try {
			String settingVal = null;
			if ( parameters != null && parameters.get(RESOLUTION_SETTING) instanceof String s ) {
				settingVal = s;
			} else if ( datumStream != null ) {
				settingVal = datumStream.serviceProperty(RESOLUTION_SETTING, String.class);
			}
			if ( settingVal != null && !settingVal.isEmpty() ) {
				result = SolarEdgeResolution.fromValue(settingVal);
			}
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		return (result != null ? result : SolarEdgeResolution.FifteenMinute);
	}

	/**
	 * Value reference pattern, with component matching groups.
	 *
	 * <p>
	 * The matching groups are
	 * </p>
	 *
	 * <ol>
	 * <li>siteId</li>
	 * <li>deviceType</li>
	 * <li>componentId</li>
	 * <li>field</li>
	 * </ol>
	 */
	private static final Pattern VALUE_REF_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)/(.+)");

	/**
	 * Value reference pattern for a component, with component matching groups.
	 *
	 * <p>
	 * The matching groups are
	 * </p>
	 *
	 * <ol>
	 * <li>siteId</li>
	 * <li>deviceType</li>
	 * <li>componentId</li>
	 * </ol>
	 */
	private static final Pattern COMPONENT_VALUE_REF_PATTERN = Pattern
			.compile("/([^/]+)/([^/]+)/([^/]+)");

	private record ValueRef(Object siteId, SolarEdgeDeviceType deviceType, String componentId,
			SolarEdgeTelemetryType field, CloudDatumStreamPropertyConfiguration property) {

	}

	/**
	 * A site-specific query plan.
	 *
	 * <p>
	 * This plan is constructed from a set of
	 * {@link CloudDatumStreamPropertyConfiguration}, and used to determine
	 * which SolarEdge APIs are necessary to satisfy those configurations.
	 * </p>
	 */
	private static class SiteQueryPlan {

		/** The SolarEdge site ID. */
		private final Long siteId;

		/** The time zone used by this site. */
		private final ZoneId zone;

		/** The set of inverter IDs required. */
		private @Nullable Set<String> inverterIds;

		private Map<String, List<ValueRef>> inverterRefs = new TreeMap<>();

		private Map<String, List<ValueRef>> meterRefs = new TreeMap<>();

		private Map<String, List<ValueRef>> batteryRefs = new TreeMap<>();

		private CloudDataValue @Nullable [] inventory;

		private @Nullable Map<SolarEdgeDeviceType, Map<String, Integer>> componentIndexMap;

		private @Nullable Map<SolarEdgeDeviceType, Map<String, Interval>> componentOperationalRanges;

		private SiteQueryPlan(Long siteId, ZoneId zone,
				@Nullable Map<String, Map<String, Interval>> siteComponentOperationalRanges) {
			super();
			this.siteId = requireNonNullArgument(siteId, "siteId");
			this.zone = requireNonNullArgument(zone, "zone");
			if ( siteComponentOperationalRanges != null && !siteComponentOperationalRanges.isEmpty() ) {
				Map<SolarEdgeDeviceType, Map<String, Interval>> componentOperationalRanges = new LinkedHashMap<>(
						siteComponentOperationalRanges.size());
				for ( Entry<String, Map<String, Interval>> e : siteComponentOperationalRanges
						.entrySet() ) {
					try {
						final SolarEdgeDeviceType deviceType = SolarEdgeDeviceType.fromValue(e.getKey());
						componentOperationalRanges.put(deviceType, e.getValue());
					} catch ( IllegalArgumentException iae ) {
						// ignore and continue
					}
				}
				this.componentOperationalRanges = componentOperationalRanges;
			}
		}

		private void add(ValueRef valueRef) {
			final SolarEdgeDeviceType deviceType = valueRef.deviceType();

			Map<String, List<ValueRef>> valueRefMap = null;

			if ( deviceType == SolarEdgeDeviceType.Battery ) {
				if ( batteryRefs == null ) {
					batteryRefs = new LinkedHashMap<>(8);
				}
				valueRefMap = batteryRefs;
			} else if ( deviceType == SolarEdgeDeviceType.Meter ) {
				if ( meterRefs == null ) {
					meterRefs = new LinkedHashMap<>(8);
				}
				valueRefMap = meterRefs;
			} else if ( deviceType == SolarEdgeDeviceType.Inverter ) {
				if ( inverterIds == null ) {
					inverterIds = new TreeSet<>();
				}
				inverterIds.add(valueRef.componentId());
				if ( inverterRefs == null ) {
					inverterRefs = new LinkedHashMap<>(8);
				}
				valueRefMap = inverterRefs;
			}
			if ( valueRefMap != null ) {
				valueRefMap.computeIfAbsent(valueRef.componentId, _ -> new ArrayList<>(8)).add(valueRef);
			}
		}

		private Set<String> componentIds(SolarEdgeDeviceType deviceType, Interval queryRange) {
			return applyOperationalRangeToComponentIds(deviceType, queryRange, switch (deviceType) {
				case Inverter -> inverterIds != null ? inverterIds : Set.of();
				case Meter -> meterRefs != null ? meterRefs.keySet() : Set.of();
				case Battery -> batteryRefs != null ? batteryRefs.keySet() : Set.of();
			});
		}

		private Set<String> applyOperationalRangeToComponentIds(SolarEdgeDeviceType deviceType,
				Interval queryRange, Set<String> componentIds) {
			if ( componentOperationalRanges == null || componentIds.isEmpty() ) {
				return componentIds;
			}
			final Map<String, Interval> opRanges = componentOperationalRanges.get(deviceType);
			if ( opRanges == null || opRanges.isEmpty() ) {
				return componentIds;
			}
			Set<String> result = null;
			for ( String componentId : componentIds ) {
				final Interval compOpRange = opRanges.get(componentId);
				if ( compOpRange == null ) {
					continue;
				}
				if ( !compOpRange.overlaps(queryRange) ) {
					// no overlap, so remove from query
					if ( result == null ) {
						result = new LinkedHashSet<>(componentIds);
					}
					result.remove(componentId);
				}
			}
			return (result != null ? result : componentIds);
		}

		private Map<String, List<ValueRef>> componentRefs(SolarEdgeDeviceType deviceType) {
			return switch (deviceType) {
				case Inverter -> inverterRefs != null ? inverterRefs : Map.of();
				case Meter -> meterRefs != null ? meterRefs : Map.of();
				case Battery -> batteryRefs != null ? batteryRefs : Map.of();
			};
		}

		private static Map<SolarEdgeDeviceType, Map<String, Integer>> generateComponentIndexMap(
				final CloudDataValue[] inventory) {
			final Map<SolarEdgeDeviceType, Map<String, Integer>> map = new LinkedHashMap<>(
					SolarEdgeDeviceType.values().length);
			final int[] indexes = new int[SolarEdgeDeviceType.values().length];
			Arrays.fill(indexes, -1);
			for ( CloudDataValue v : inventory ) {
				populateComponentIndexMap(map, indexes, v);
			}
			return map;
		}

		@SuppressWarnings("EnumOrdinal")
		private static void populateComponentIndexMap(
				final Map<SolarEdgeDeviceType, Map<String, Integer>> map, final int[] indexes,
				final CloudDataValue v) {
			final List<String> identifiers = v.getIdentifiers();
			if ( identifiers != null && identifiers.size() > 2 ) {
				SolarEdgeDeviceType type = SolarEdgeDeviceType.fromValue(identifiers.get(1));
				final int indexIdx = type.ordinal();
				final int compIdx = ++indexes[indexIdx];
				map.computeIfAbsent(type, _ -> new LinkedHashMap<>(8)).put(identifiers.get(2), compIdx);
			}
			if ( v.getChildren() != null ) {
				for ( CloudDataValue child : v.getChildren() ) {
					populateComponentIndexMap(map, indexes, child);
				}
			}
		}

		private @Nullable Integer componentIndex(SolarEdgeDeviceType deviceType, String componentId) {
			if ( inventory == null ) {
				return null;
			}

			if ( componentIndexMap == null ) {
				// generate componentIndexMap now
				componentIndexMap = generateComponentIndexMap(inventory);
			}

			Map<String, Integer> typeMap = componentIndexMap.get(deviceType);
			return (typeMap != null ? typeMap.get(componentId) : null);
		}
	}

	private Map<Long, SiteQueryPlan> resolveSiteQueryPlans(CloudIntegrationConfiguration integration,
			CloudDatumStreamConfiguration datumStream, @Nullable Map<String, String> sourceIdMap,
			List<CloudDatumStreamPropertyConfiguration> propConfigs,
			@Nullable Map<String, Map<String, Map<String, Interval>>> componentOperationalRanges) {
		final var result = new LinkedHashMap<Long, SiteQueryPlan>(2);
		final boolean useIndexBasedSourceIds = useIndexBasedSourceIds(datumStream, sourceIdMap);

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
				// groups: 1 = siteId, 2 = deviceType, 3 = componentId, 4 = field
				Long siteId = Long.valueOf(m.group(1));
				String deviceTypeKey = m.group(2);
				String componentId = m.group(3);
				String fieldName = m.group(4);

				final SolarEdgeDeviceType deviceType;
				try {
					deviceType = SolarEdgeDeviceType.fromValue(deviceTypeKey);
				} catch ( IllegalArgumentException e ) {
					// ignore and continue
					continue;
				}

				final SiteQueryPlan plan = result.computeIfAbsent(siteId, id -> {
					ZoneId zone = resolveSiteTimeZone(integration, id);
					return new SiteQueryPlan(siteId, zone,
							componentOperationalRanges != null
									? componentOperationalRanges.get(m.group(1))
									: null);
				});

				final SolarEdgeTelemetryType field;
				try {
					field = switch (deviceType) {
						case Inverter -> SolarEdgeInverterTelemtry.valueOf(fieldName);
						case Meter -> SolarEdgeMeterTelemtry.valueOf(fieldName);
						case Battery -> SolarEdgeBatteryTelemtry.valueOf(fieldName);
					};
				} catch ( IllegalArgumentException e ) {
					// ignore and continue
					continue;
				}

				final var valueRef = new ValueRef(siteId, deviceType, componentId, field, config);
				plan.add(valueRef);
			}
		}

		// resolve wildcard inverter component IDs
		for ( SiteQueryPlan plan : result.values() ) {
			if ( useIndexBasedSourceIds ) {
				plan.inventory = resolveSiteInventory(integration, plan.siteId);
			}
			if ( plan.inverterIds == null
					|| !plan.inverterIds.contains(CloudDataValue.WILDCARD_IDENTIFIER) ) {
				continue;
			}

			Set<String> resolvedInverterIds = new LinkedHashSet<>(8);
			CloudDataValue[] inventory = plan.inventory;
			if ( inventory == null ) {
				inventory = resolveSiteInventory(integration, plan.siteId);
			}
			CloudDataValue inverters = Arrays.stream(inventory)
					.filter(e -> SolarEdgeDeviceType.Inverter.getGroupKey().equals(e.getName()))
					.findAny().orElse(null);
			if ( inverters != null && inverters.getChildren() != null ) {
				for ( CloudDataValue inverter : inverters.getChildren() ) {
					resolvedInverterIds.add(inverter.getIdentifiers().getLast());
				}
			}

			plan.inverterIds.remove(CloudDataValue.WILDCARD_IDENTIFIER);
			plan.inverterIds.addAll(resolvedInverterIds);
			if ( plan.inverterIds.isEmpty() ) {
				plan.inverterIds = null;
			}
		}

		return result;
	}

	private static boolean useIndexBasedSourceIds(CloudDatumStreamConfiguration datumStream,
			@Nullable Map<String, String> sourceIdMap) {
		if ( sourceIdMap != null ) {
			return false;
		}
		Boolean ibSourceId = datumStream.serviceProperty(INDEX_BASED_SOURCE_ID_SETTING, Boolean.class);
		return (ibSourceId != null && ibSourceId);
	}

	private static @Nullable String resolveSourceId(CloudDatumStreamConfiguration datumStream,
			SiteQueryPlan sitePlan, SolarEdgeDeviceType deviceType, String componentId,
			@Nullable Map<String, String> sourceIdMap) {
		if ( sourceIdMap != null ) {
			String key = "/%s/%s/%s".formatted(sitePlan.siteId, deviceType.getKey(), componentId);
			return sourceIdMap.get(key);
		}

		String devType = deviceType.getKey();
		Boolean ucSourceId = datumStream.serviceProperty(UPPER_CASE_SOURCE_ID_SETTING, Boolean.class);
		if ( ucSourceId != null && ucSourceId ) {
			devType = devType.toUpperCase(Locale.ENGLISH);
		}

		String compId = componentId;
		if ( useIndexBasedSourceIds(datumStream, null) ) {
			Integer idx = sitePlan.componentIndex(deviceType, componentId);
			if ( idx != null ) {
				compId = String.valueOf(idx + 1);
			}
		}

		return "%s/%s/%s".formatted(datumStream.getSourceId(), devType, compId);
	}

	private ZoneId resolveSiteTimeZone(CloudIntegrationConfiguration integration, Long siteId) {
		assert integration != null && siteId != null;
		final var cache = getSiteTimeZoneCache();

		ZoneId result = (cache != null ? cache.get(siteId) : null);
		if ( result != null ) {
			return result;
		}

		result = restOpsHelper.httpGet("Get site time zone", integration, JsonNode.class, _ -> {
			// @formatter:off
					return fromUri(resolveBaseUrl(integration, BASE_URI))
							.path(SITE_DETAILS_URL_TEMPLATE)
							.buildAndExpand(siteId)
							.toUri();
					// @formatter:on
		}, (_, res) -> {
			ZoneId zone = ZoneOffset.UTC;

			final CloudDataValue site = parseSite(res.getBody());
			if ( site != null && site.getMetadata() != null
					&& site.getMetadata().get(CloudDataValue.TIME_ZONE_METADATA) instanceof String s ) {
				try {
					zone = ZoneId.of(s);
				} catch ( Exception e ) {
					log.warn("Site [{}] time zone [{}] not usable, will use UTC: {}", siteId, s,
							e.toString());
				}
			}
			return zone;
		});

		if ( result != null && cache != null ) {
			cache.put(siteId, result);
		}

		return result;
	}

	private Void parseTelemetry(RequestEntity<Void> req, @Nullable JsonNode json,
			SiteQueryPlan queryPlan, SolarEdgeDeviceType deviceType,
			CloudDatumStreamConfiguration datumStream, @Nullable Map<String, String> sourceIdMap,
			@Nullable Duration timeGapThreshold, OrderedDatumSamplesBuffer streamBuffer) {
		/*- EXAMPLE JSON:
		{
		  "AAAA1DFE-E9": {
		      "power": {
		        "period": {
		          "from": "2026-08-10T05:00:00-04:00",
		          "to": "2026-08-10T08:00:00-04:00"
		        },
		        "unit": "W",
		        "resolution": "QUARTER_HOUR",
		        "values": [
		          {
		            "timestamp": "2026-08-10T05:00:00-04:00",
		            "value": null
		          },
		 */
		if ( json == null ) {
			return null;
		}

		final Map<String, List<ValueRef>> componentRefs = queryPlan.componentRefs(deviceType);
		if ( componentRefs.isEmpty() ) {
			return null;
		}

		final Map<DatumStreamIdentity, SequencedSet<String>> streamIdents = new LinkedHashMap<>(4);

		for ( Entry<String, JsonNode> componentEntry : json.path(deviceType.getTelemetryType())
				.properties() ) {
			final String componentId = componentEntry.getKey();
			final String sourceId = resolveSourceId(datumStream, queryPlan, deviceType, componentId,
					sourceIdMap);
			if ( sourceId == null ) {
				continue;
			}
			final DatumStreamIdentity streamId = new DatumStreamIdent(datumStream.getKind(),
					datumStream.getObjectId(), sourceId);
			final String deviceRef = "/%d/%s/%s".formatted(queryPlan.siteId, deviceType.getKey(),
					componentId);

			for ( String componentRefId : new String[] { CloudDataValue.WILDCARD_IDENTIFIER,
					componentId } ) {
				if ( !componentRefs.containsKey(componentRefId) ) {
					continue;
				}
				for ( ValueRef ref : componentRefs.get(componentRefId) ) {
					final JsonNode fieldNode = componentEntry.getValue().get(ref.field.key());
					if ( fieldNode == null ) {
						continue;
					}

					final SolarEdgeMeasurementUnit unit = SolarEdgeMeasurementUnit
							.fromValue(fieldNode.path("unit").asString(null));

					final SolarEdgeResolution res;
					try {
						res = SolarEdgeResolution.fromValue(fieldNode.path("resolution").asString(null));
					} catch ( IllegalArgumentException e ) {
						// ignore and continue
						continue;
					}

					for ( JsonNode valuesNode : fieldNode.path("values") ) {
						final JsonNode valueNode = valuesNode.path("value");
						if ( valueNode.isMissingNode() || valueNode.isNull() ) {
							continue;
						}
						final Instant ts = ref.field.resolveWindowTimestamp(res, queryPlan.zone,
								valuesNode.path("timestamp").asString());
						if ( ts == null ) {
							continue;
						}
						final DatumSamples s = streamBuffer.getOrCreate(streamId, ts);
						Object propVal = parseJsonDatumPropertyValue(valueNode,
								ref.property.getPropertyType());
						if ( propVal instanceof Number n ) {
							propVal = unit.scaled(n);
						}
						propVal = ref.property.applyValueTransforms(propVal);
						if ( propVal != null ) {
							s.putSampleValue(ref.property.getPropertyType(),
									ref.property.getPropertyName(), propVal);
						} else if ( s.isEmpty() ) {
							streamBuffer.removeTimestamp(streamId, ts, s);
						}
						streamIdents.computeIfAbsent(streamId, _ -> new LinkedHashSet<>(4))
								.add(deviceRef);
					}
				}
			}
		}

		if ( timeGapThreshold != null ) {
			for ( Entry<DatumStreamIdentity, SequencedSet<String>> streamEntry : streamIdents
					.entrySet() ) {
				final DatumStreamIdentity streamId = streamEntry.getKey();
				SortedMap<Instant, DatumSamples> streamData = streamBuffer.streamBuffer(streamId);
				if ( streamData == null ) {
					continue;
				}

				final String deviceRef = streamEntry.getValue().getFirst();

				for ( Instant ts : streamData.keySet() ) {
					Instant prevTs = streamBuffer.previousTimestamp(streamId, ts);
					if ( prevTs == null ) {
						final var prevDatum = lookupPreviousDatum(datumStream, streamId.getSourceId(),
								ts);
						if ( prevDatum != null ) {
							prevTs = prevDatum.getTimestamp();
						}
					}
					if ( prevTs != null ) {
						streamBuffer.addAuxiliary(streamId, validateTimeGap(datumStream, req, deviceRef,
								null, timeGapThreshold, prevTs, streamId.datumIdentity(ts)));
					}
				}
			}
		}

		return null;
	}

	/**
	 * Get the site time zone cache.
	 *
	 * @return the cache
	 */
	public final @Nullable Cache<Long, ZoneId> getSiteTimeZoneCache() {
		return siteTimeZoneCache;
	}

	/**
	 * Set the site time zone cache.
	 *
	 * <p>
	 * This cache can be provided to help with time zone lookup by SolarEdge
	 * site ID.
	 * </p>
	 *
	 * @param siteTimeZoneCache
	 *        the cache to set
	 */
	public final void setSiteTimeZoneCache(@Nullable Cache<Long, ZoneId> siteTimeZoneCache) {
		this.siteTimeZoneCache = siteTimeZoneCache;
	}

	/**
	 * Get the site inventory cache.
	 *
	 * @return the cache
	 */
	public final @Nullable Cache<Long, CloudDataValue[]> getSiteInventoryCache() {
		return siteInventoryCache;
	}

	/**
	 * Set the site inventory cache.
	 *
	 * <p>
	 * This cache can be provided to help with device lookup by SolarEdge site
	 * ID.
	 * </p>
	 *
	 * @param siteInventoryCache
	 *        the cache to set
	 */
	public final void setSiteInventoryCache(@Nullable Cache<Long, CloudDataValue[]> siteInventoryCache) {
		this.siteInventoryCache = siteInventoryCache;
	}

}
