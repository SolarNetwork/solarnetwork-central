/* ==================================================================
 * EnphaseCloudDatumStreamService.java - 4/03/2025 5:23:17 am
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

import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.API_KEY_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudIntegrationService.API_KEY_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudIntegrationService.MAX_PAGE_SIZE;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudIntegrationService.PAGE_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudIntegrationService.PAGE_SIZE_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudIntegrationService.SECURE_SETTINGS;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseDeviceType.Inverter;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseDeviceType.Meter;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseGranularity.FifteenMinute;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.cache.Cache;
import org.apache.commons.lang3.mutable.MutableLong;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
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
import net.solarnetwork.central.c2c.http.OAuth2RestOperationsHelper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.TextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.StringUtils;

/**
 * Enphase implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.8
 */
public class EnphaseCloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.enphase";

	/** The data value filter key for a system ID. */
	public static final String SYSTEM_ID_FILTER = "systemId";

	/** The data value filter key for a device ID. */
	public static final String DEVICE_ID_FILTER = "deviceId";

	/** Constant device ID value for system-level data. */
	public static final String SYSTEM_DEVICE_ID = "sys";

	/** The setting to upper-case source ID values. */
	public static final String UPPER_CASE_SOURCE_ID_SETTING = "upperCaseSourceId";

	/**
	 * The URI path to view a given system.
	 *
	 * <p>
	 * Accepts a single {@code {systemId}} parameter.
	 * </p>
	 */
	public static final String SYSTEM_VIEW_PATH_TEMPLATE = "/api/v4/systems/{systemId}";

	/**
	 * The URI path to list the devices for a given system.
	 *
	 * <p>
	 * Accepts a single {@code {systemId}} parameter.
	 * </p>
	 */
	public static final String SYSTEM_DEVICES_PATH_TEMPLATE = "/api/v4/systems/{systemId}/devices";

	/**
	 * The URI path to list inverter telemetry a given system.
	 *
	 * <p>
	 * Accepts a single {@code {systemId}} parameter.
	 * </p>
	 */
	public static final String INVERTER_TELEMETRY_PATH_TEMPLATE = "/api/v4/systems/{systemId}/telemetry/production_micro";

	/**
	 * The URI path to list revenue grade meter telemetry a given system.
	 *
	 * <p>
	 * Accepts a single {@code {systemId}} parameter.
	 * </p>
	 */
	public static final String RGM_TELEMETRY_PATH_TEMPLATE = "/api/v4/systems/{systemId}/rgm_stats";

	/** The epoch end date query parameter name. */
	public static final String END_AT_PARAM = "end_at";

	/** The epoch start date query parameter name. */
	public static final String START_AT_PARAM = "start_at";

	/** The epoch end date query parameter name. */
	public static final String GRANULARITY_PARAM = "granularity";

	/**
	 * The default duration used if the
	 * {@link #DEVICE_REPORTING_MAXIMUM_LAG_SETTING} is not defined.
	 *
	 * @since 1.7
	 */
	public static final Duration DEFAULT_DEVICE_REPORTING_MAXIMUM_LAG = Duration.ofHours(3);

	/**
	 * The setting for a "devices reporting" maximum lag, when less than the
	 * "total devices" available.
	 *
	 * <p>
	 * The value can be an ISO duration like {@code PT2H} for "2 hours" or an
	 * integer number of seconds.
	 * </p>
	 *
	 * @since 1.7
	 */
	public static final String DEVICE_REPORTING_MAXIMUM_LAG_SETTING = "deviceReportingMaximumLag";

	/**
	 * A setting specifier for the {@code DEVICE_REPORTING_MAXIMUM_LAG_SETTING}.
	 *
	 * @since 1.7
	 */
	public static final TextFieldSettingSpecifier DEVICE_REPORTING_MAXIMUM_LAG_SETTING_SPECIFIER = new BasicTextFieldSettingSpecifier(
			DEVICE_REPORTING_MAXIMUM_LAG_SETTING, DEFAULT_DEVICE_REPORTING_MAXIMUM_LAG.toString());

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// @formatter:off
		SETTINGS = List.of(
				DEVICE_REPORTING_MAXIMUM_LAG_SETTING_SPECIFIER,
				UPPER_CASE_SOURCE_ID_SETTING_SPECIFIER,
				SOURCE_ID_MAP_SETTING_SPECIFIER,
				VIRTUAL_SOURCE_IDS_SETTING_SPECIFIER
				);
		// @formatter:on
	}

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SYSTEM_ID_FILTER,
			DEVICE_ID_FILTER);

	/** The data value identifier levels source ID range. */
	public static final IntRange DATA_VALUE_IDENTIFIER_LEVELS_SOURCE_ID_RANGE = IntRange.rangeOf(0, 2);

	/** The maximum period of time to request data for in one request. */
	private static final Duration MAX_QUERY_TIME_RANGE = Duration.ofDays(7);

	/**
	 * An internal datum property to hold the "devices reporting" integer value.
	 *
	 * @since 1.7
	 */
	public static final String INTERNAL_DEVICES_REPORTING_PROPERTY = "__DevicesReporting";

	/**
	 * An internal datum property to hold the "total devices" integer value.
	 *
	 * @since 1.7
	 */
	public static final String INTERNAL_TOTAL_DEVICES_PROPERTY = "__TotalDevices";

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
	public EnphaseCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager, Clock clock,
			Cache<UserLongCompositePK, Lock> integrationLocksCache) {
		super(SERVICE_IDENTIFIER, "Enphase Datum Stream Service", clock, userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new OAuth2RestOperationsHelper(
						LoggerFactory.getLogger(EnphaseCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						_ -> EnphaseCloudIntegrationService.SECURE_SETTINGS, oauthClientManager, clock,
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
	public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
		MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");
		List<LocalizedServiceInfo> result = new ArrayList<>(2);
		for ( String key : new String[] { SYSTEM_ID_FILTER } ) {
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
		if ( filters != null && filters.get(SYSTEM_ID_FILTER) != null ) {
			String systemId = filters.get(SYSTEM_ID_FILTER).toString();
			result = systemDevices(integration, systemId, filters);
		} else {
			// list available systems
			result = systems(integration);
		}
		return result;
	}

	private List<CloudDataValue> systems(CloudIntegrationConfiguration integration) {
		final var decryp = integration.copyWithId(integration.getId());
		decryp.unmaskSensitiveInformation(_ -> SECURE_SETTINGS, encryptor);
		List<CloudDataValue> result = null;

		final var pagination = new Pagination();

		while ( pagination.hasMore() ) {
			List<CloudDataValue> pageResults = restOpsHelper.httpGet("List systems", integration,
					JsonNode.class,
					_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
							.path(EnphaseCloudIntegrationService.LIST_SYSTEMS_PATH)
							.queryParam(API_KEY_PARAM,
									decryp.serviceProperty(API_KEY_SETTING, String.class))
							.queryParam(PAGE_SIZE_PARAM, MAX_PAGE_SIZE)
							.queryParam(PAGE_PARAM, Math.max(1, pagination.page))
							.buildAndExpand(integration.getServiceProperties()).toUri(),
					res -> {
						var json = res.getBody();
						pagination.parseJson(json);
						return parseSystems(json);
					});
			if ( pageResults != null ) {
				if ( result == null ) {
					result = pageResults;
				} else {
					result.addAll(pageResults);
				}
			}
		}

		return result;
	}

	private List<CloudDataValue> systemDevices(final CloudIntegrationConfiguration integration,
			final String systemId, Map<String, ?> filters) {
		final var decryp = integration.copyWithId(integration.getId());
		decryp.unmaskSensitiveInformation(_ -> SECURE_SETTINGS, encryptor);

		return restOpsHelper.httpGet("List system devices", integration, JsonNode.class,
		// @formatter:off
				_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(SYSTEM_DEVICES_PATH_TEMPLATE)
						.queryParam(API_KEY_PARAM, decryp.serviceProperty(API_KEY_SETTING, String.class))
						.buildAndExpand(filters).toUri(),
						// @formatter:on
				res -> parseSystemDevices(res.getBody(), systemId));
	}

	private static class Pagination {

		private long page;
		private long total;
		private int pageSize;
		private int count;

		private void parseJson(JsonNode json) {
			if ( json == null ) {
				return;
			}
			if ( json.has("total") ) {
				total = json.path("total").longValue();
			}
			if ( json.has("current_page") ) {
				page = json.path("current_page").longValue();
			}
			if ( json.has("size") ) {
				pageSize = json.path("size").intValue();
			}
			if ( json.has("count") ) {
				count = json.path("count").intValue();
			}
		}

		private boolean hasMore() {
			return page == 0 || (total > 0 && (((page - 1) * pageSize) + count) < total);
		}
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private static List<CloudDataValue> parseSystems(JsonNode json) {
		if ( json == null ) {
			return Collections.emptyList();
		}
		/*- EXAMPLE JSON:
		{
		  "total": 2,
		  "current_page": 1,
		  "size": 10,
		  "count": 2,
		  "items": "systems",
		  "systems": [
		    {
		      "system_id": 2875,
		      "name": "Site 1",
		      "public_name": "Residential System",
		      "timezone": "US/Eastern",
		      "address": {
		        "city": "Anytown",
		        "state": "MD",
		        "country": "US",
		        "postal_code": "20906"
		      },
		      "connection_type": "cellular",
		      "status": "normal",
		      "last_report_at": 1740976229,
		      "last_energy_at": 1740969000,
		      "operational_at": 1657080000,
		      "attachment_type": "rack_mount",
		      "interconnect_date": 1657080000,
		      "energy_lifetime": -1,
		      "energy_today": -1,
		      "system_size": -1
		    },
		*/
		final var result = new ArrayList<CloudDataValue>(4);
		for ( JsonNode sysNode : json.path("systems") ) {
			result.addAll(parseSystem(sysNode, null));
		}
		return result;
	}

	private static List<CloudDataValue> parseSystem(JsonNode json, Collection<CloudDataValue> children) {
		/*- EXAMPLE JSON:
		    {
		      "system_id": 2875,
		      "name": "Site 1",
		      "public_name": "Residential System",
		      "timezone": "US/Eastern",
		      "address": {
		        "city": "Anytown",
		        "state": "MD",
		        "country": "US",
		        "postal_code": "20906"
		      },
		      "connection_type": "cellular",
		      "status": "normal",
		      "last_report_at": 1740976229,
		      "last_energy_at": 1740969000,
		      "operational_at": 1657080000,
		      "attachment_type": "rack_mount",
		      "interconnect_date": 1657080000,
		      "energy_lifetime": -1,
		      "energy_today": -1,
		      "system_size": -1
		    },
		*/
		if ( json == null ) {
			return List.of();
		}
		final String id = json.path("system_id").asText();
		final String name = json.path("name").asText().trim();

		final var meta = new LinkedHashMap<String, Object>(4);
		populateNonEmptyValue(json, "timezone", CloudDataValue.TIME_ZONE_METADATA, meta);

		JsonNode addrNode = json.path("address");
		populateNonEmptyValue(addrNode, "city", CloudDataValue.LOCALITY_METADATA, meta);
		populateNonEmptyValue(addrNode, "state", CloudDataValue.STATE_PROVINCE_METADATA, meta);
		populateNonEmptyValue(addrNode, "country", CloudDataValue.COUNTRY_METADATA, meta);
		populateNonEmptyValue(addrNode, "postal_code", CloudDataValue.POSTAL_CODE_METADATA, meta);

		long lastSeen = json.path("last_report_at").longValue();
		if ( lastSeen > 0 ) {
			meta.put("lastSeenAt", Instant.ofEpochSecond(lastSeen));
		}

		return List.of(intermediateDataValue(List.of(id), name, meta, children));
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private static List<CloudDataValue> parseSystemDevices(final JsonNode json, final String systemId) {
		/*- EXAMPLE JSON:
		{
		  "system_id": 2875,
		  "total_devices": 3,
		  "items": "devices",
		  "devices": {
		    "micros": [
		      {
		        "id": 57389303,
		        "last_report_at": 1741011543,
		        "name": "Microinverter 000000000699",
		        "serial_number": "000000000699",
		        "part_number": "800-01731-r02",
		        "sku": "IQ7PLUS-72-M-US",
		        "model": "IQ7+",
		        "status": "normal",
		        "active": true,
		        "product_name": "IQ7+"
		      }
		    ],
		    "meters": [
		      {
		        "id": 57387732,
		        "last_report_at": 1741012200,
		        "name": "production",
		        "serial_number": "000000058195EIM1",
		        "part_number": "800-00655-r09",
		        "sku": null,
		        "model": "Envoy S",
		        "status": "normal",
		        "active": true,
		        "state": "enabled",
		        "config_type": "Production",
		        "product_name": "IQ Gateway"
		      }
		    ]
		  }
		}
		*/
		if ( json == null ) {
			return List.of();
		}

		final List<CloudDataValue> result = new ArrayList<>(8);

		final JsonNode devices = json.path("devices");

		final JsonNode inverters = devices.path("micros");
		if ( !(inverters.isMissingNode() || inverters.isEmpty()) ) {
			result.addAll(systemInverterDataValues(systemId));
			// TODO: add device-level values
		}

		final JsonNode meters = devices.path("meters");
		if ( !(meters.isMissingNode() || meters.isEmpty()) ) {
			result.addAll(systemMeterDataValues(systemId));
			// TODO: add device-level values
		}

		return result;
	}

	private static List<CloudDataValue> systemInverterDataValues(final String systemId) {
		/*- EXAMPLE JSON /systems/{systemId}/telemetry/production_micro
		{
		  "end_at": 1738321500,
		  "devices_reporting": 16,
		  "powr": 0,
		  "enwh": 0
		},
		 */
		// @formatter:off
		return Arrays.asList(
				dataValue(List.of(systemId, Inverter.getKey(), SYSTEM_DEVICE_ID, "DevicesReporting"), "Devices reporting"),

				dataValue(List.of(systemId, Inverter.getKey(), SYSTEM_DEVICE_ID, "W"), "Active power"),
				dataValue(List.of(systemId, Inverter.getKey(), SYSTEM_DEVICE_ID, "Wh"), "Active energy")
				);
		// @formatter:on
	}

	private static List<CloudDataValue> systemMeterDataValues(final String systemId) {
		/*- EXAMPLE JSON /systems/{systemId}/rgm_stats
		{
		  "system_id": 2875299,
		  "total_devices": 1,
		  "intervals": [
		    {
		      "end_at": 1741065300,
		      "devices_reporting": 1,
		      "wh_del": 0
		    }
		  ],
		  "meta": {
		    "status": "normal",
		    "last_report_at": 1741108110,
		    "last_energy_at": 1741104000,
		    "operational_at": 1657080000
		  },
		  "meter_intervals": [
		    {
		      "meter_serial_number": "202125058195EIM1",
		      "envoy_serial_number": "202125058195",
		      "intervals": [
		        {
		          "channel": 1,
		          "wh_del": 0.0,
		          "curr_w": 0,
		          "end_at": 1741065300
		        },
		 */
		// @formatter:off
		return Arrays.asList(
				dataValue(List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "DevicesReporting"), "Devices reporting"),

				dataValue(List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "W"), "Active power"),
				dataValue(List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "WhExp"), "Active energy exported"),

				dataValue(List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWA"), "Phase active power - A"),
				dataValue(List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWB"), "Phase active power - B"),
				dataValue(List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWC"), "Phase active power - C"),
				dataValue(List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWhExpA"), "Phase active energy exported - A"),
				dataValue(List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWhExpB"), "Phase active energy exported - B"),
				dataValue(List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWhExpC"), "Phase active energy exported - C")
				);
		// @formatter:on
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
	 * <li>deviceType</li>
	 * <li>deviceId</li>
	 * <li>field</li>
	 * </ol>
	 */
	private static final Pattern VALUE_REF_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)/(.+)");

	private static record ValueRef(Long systemId, EnphaseDeviceType deviceType, String deviceId,
			String fieldName, CloudDatumStreamPropertyConfiguration property, String sourceId) {

		private ValueRef(Long systemId, EnphaseDeviceType deviceType, String deviceId, String fieldName,
				CloudDatumStreamPropertyConfiguration property) {
			this(systemId, deviceType, deviceId, fieldName, property,
					"/%d/%s/%s".formatted(systemId, deviceType.getKey(), deviceId));
		}

		private boolean isSystemDevice() {
			return SYSTEM_DEVICE_ID.equals(deviceId);
		}

		private boolean isMeterPhaseField() {
			return deviceType == Meter && ("W".equals(fieldName) || fieldName.startsWith("PW"));
		}

	}

	/**
	 * A system-specific query plan.
	 *
	 * <p>
	 * This plan is constructed from a set of
	 * {@link CloudDatumStreamPropertyConfiguration}, and used to determine
	 * which Enphase APIs are necessary to satisfy those configurations.
	 * </p>
	 */
	private static class SystemQueryPlan {

		/** The Enhpase system ID. */
		private final Long systemId;

		private final Map<EnphaseDeviceType, List<ValueRef>> systemValueRefs = new LinkedHashMap<>(2);;
		private final Map<EnphaseDeviceType, Map<String, List<ValueRef>>> deviceValueRefs = new LinkedHashMap<>(
				2);

		private SystemQueryPlan(Long systemId) {
			super();
			this.systemId = requireNonNullArgument(systemId, "systemId");
		}

		private void addValueRef(ValueRef ref) {
			if ( ref.isSystemDevice() ) {
				systemValueRefs.computeIfAbsent(ref.deviceType, _ -> new ArrayList<>(4)).add(ref);
			} else {
				deviceValueRefs.computeIfAbsent(ref.deviceType, _ -> new LinkedHashMap<>(2))
						.computeIfAbsent(ref.deviceId, _ -> new ArrayList<>(4)).add(ref);
			}
		}

		private List<ValueRef> systemValueRefs(EnphaseDeviceType type) {
			return systemValueRefs.get(type);
		}

	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");

		final Instant endDate = FifteenMinute.tickStart(clock.instant(), UTC);
		final Instant startDate = endDate.minus(FifteenMinute.getTickAmount());

		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);

		final var result = datum(datumStream, filter);
		if ( result == null ) {
			return Collections.emptyList();
		}
		return result.getResults();
	}

	private static void updateLastReportDate(Duration maxLag, MutableLong date, JsonNode json,
			Instant now, List<GeneralDatum> datum) {
		// track the minimum "last report date" value in a response, to adjust "next start" query value
		long jsonLastReportAt = json.path("meta").path("last_report_at").longValue();
		if ( jsonLastReportAt > 0 ) {
			if ( jsonLastReportAt < date.longValue() ) {
				date.setValue(jsonLastReportAt);
			}
		}
		long jsonLastEnergyAt = json.path("meta").path("last_energy_at").longValue();
		if ( jsonLastEnergyAt > 0 ) {
			if ( jsonLastEnergyAt < date.longValue() ) {
				date.setValue(jsonLastEnergyAt);
			}
		}

		for ( GeneralDatum d : datum ) {
			DatumSamples s = d.getSamples();
			Map<String, Object> status = s.getStatus();
			if ( status == null ) {
				continue;
			}

			// get (and remove) the internal device count properties
			Integer totalCount = (Integer) status.remove(INTERNAL_TOTAL_DEVICES_PROPERTY);
			Integer reportingCount = (Integer) status.remove(INTERNAL_DEVICES_REPORTING_PROPERTY);
			if ( status.isEmpty() ) {
				s.setStatus(null);
			}

			if ( totalCount != null && reportingCount != null && reportingCount < totalCount ) {
				Duration lag = Duration.between(d.getTimestamp(), now);
				if ( lag.compareTo(maxLag) <= 0 ) {
					// reporting count is less than total count, and datum is within "max lag" setting,
					// so adjust date to this datum's time
					long datumEpoch = d.getTimestamp().getEpochSecond();
					if ( datumEpoch < date.longValue() ) {
						date.setValue(datumEpoch);
					}
				}
			}
		}
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

			final var decryptedIntegration = integration.copyWithId(integration.getId());
			decryptedIntegration.unmaskSensitiveInformation(_ -> SECURE_SETTINGS, encryptor);

			final Duration deviceReportingMaxLag = servicePropertyDuration(datumStream,
					DEVICE_REPORTING_MAXIMUM_LAG_SETTING, DEFAULT_DEVICE_REPORTING_MAXIMUM_LAG);

			final Instant filterStartDate = requireNonNullArgument(filter.getStartDate(),
					"filter.startDate");
			final Instant filterEndDate = requireNonNullArgument(filter.getEndDate(),
					"filter.startDate");

			BasicQueryFilter nextQueryFilter = null;

			Instant startDate = FifteenMinute.tickStart(filterStartDate, UTC);
			Instant endDate = FifteenMinute.tickStart(filterEndDate, UTC);
			if ( Duration.between(startDate, endDate).compareTo(MAX_QUERY_TIME_RANGE) > 0 ) {
				Instant nextEndDate = FifteenMinute
						.tickStart(startDate.plus(MAX_QUERY_TIME_RANGE.multipliedBy(2)), UTC);
				if ( nextEndDate.isAfter(endDate) ) {
					nextEndDate = endDate;
				}

				endDate = FifteenMinute.tickStart(startDate.plus(MAX_QUERY_TIME_RANGE), UTC);

				nextQueryFilter = new BasicQueryFilter();
				nextQueryFilter.setStartDate(endDate);
				nextQueryFilter.setEndDate(nextEndDate);
			}

			final BasicQueryFilter usedQueryFilter = new BasicQueryFilter();
			usedQueryFilter.setStartDate(startDate);
			usedQueryFilter.setEndDate(endDate);

			final Map<String, String> sourceIdMap = servicePropertyStringMap(ds, SOURCE_ID_MAP_SETTING);

			final List<GeneralDatum> resultDatum = new ArrayList<>(16);
			final Map<Long, SystemQueryPlan> queryPlans = resolveSystemQueryPlans(ds, sourceIdMap,
					valueProps);

			// track the earliest reported "data valid as of" date, as an epoch second
			final var lastReportDate = new MutableLong(endDate.getEpochSecond());

			for ( SystemQueryPlan queryPlan : queryPlans.values() ) {
				// system inverter data
				List<ValueRef> systemInvRefs = queryPlan.systemValueRefs(Inverter);
				if ( systemInvRefs != null && !systemInvRefs.isEmpty() ) {
					List<GeneralDatum> datum = restOpsHelper.httpGet("List system inverter data",
							integration, JsonNode.class,
							_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
									.path(INVERTER_TELEMETRY_PATH_TEMPLATE)
									.queryParam(API_KEY_PARAM,
											decryptedIntegration.serviceProperty(API_KEY_SETTING,
													String.class))
									.queryParam(START_AT_PARAM, startDate.getEpochSecond())
									.queryParam(GRANULARITY_PARAM,
											EnphaseGranularity.forQueryDateRange(filter.getStartDate(),
													filter.getEndDate()).getKey())
									.buildAndExpand(queryPlan.systemId).toUri(),
							res -> {
								var result = parseSiteInverterDatum(res.getBody(), systemInvRefs, ds,
										sourceIdMap, usedQueryFilter);
								updateLastReportDate(deviceReportingMaxLag, lastReportDate,
										res.getBody(), clock.instant(), result);
								return result;
							});
					if ( datum != null ) {
						resultDatum.addAll(datum);
					}
				}

				// system meter data

				List<ValueRef> systemMetRefs = queryPlan.systemValueRefs(Meter);
				if ( systemMetRefs != null && !systemMetRefs.isEmpty() ) {
					List<GeneralDatum> datum = restOpsHelper.httpGet("List system meter data",
							integration, JsonNode.class,
							_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
									.path(RGM_TELEMETRY_PATH_TEMPLATE)
									.queryParam(API_KEY_PARAM,
											decryptedIntegration.serviceProperty(API_KEY_SETTING,
													String.class))
									.queryParam(START_AT_PARAM, startDate.getEpochSecond())
									.queryParam(END_AT_PARAM,
											usedQueryFilter.getEndDate().getEpochSecond())
									.buildAndExpand(queryPlan.systemId).toUri(),
							res -> {
								var result = parseSiteMeterDatum(res.getBody(), systemMetRefs, ds,
										sourceIdMap);
								updateLastReportDate(deviceReportingMaxLag, lastReportDate,
										res.getBody(), clock.instant(), result);
								return result;
							});
					if ( datum != null ) {
						resultDatum.addAll(datum);
					}
				}
			}

			// tick-align lastReportDate value
			lastReportDate.setValue(FifteenMinute
					.tickStart(Instant.ofEpochSecond(lastReportDate.longValue()), UTC).getEpochSecond());

			if ( lastReportDate.getValue() < endDate.getEpochSecond() ) {
				// data drop out? adjust next start date
				if ( nextQueryFilter == null ) {
					nextQueryFilter = new BasicQueryFilter();
				}
				nextQueryFilter.setStartDate(Instant.ofEpochSecond(lastReportDate.longValue()));
			}

			// evaluate expressions on merged datum
			var r = evaluateExpressions(datumStream, exprProps, resultDatum, mapping.getConfigId(),
					integration.getConfigId());

			return new BasicCloudDatumStreamQueryResult(usedQueryFilter, nextQueryFilter,
					r.stream().map(Datum.class::cast).toList());
		});
	}

	private Map<Long, SystemQueryPlan> resolveSystemQueryPlans(CloudDatumStreamConfiguration datumStream,
			Map<String, String> sourceIdMap, List<CloudDatumStreamPropertyConfiguration> propConfigs) {
		final var result = new LinkedHashMap<Long, SystemQueryPlan>(2);
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
				// groups: 1 = systemId, 2 = deviceType, 3 = deviceId, 4 = field
				Long systemId = Long.valueOf(m.group(1));
				String deviceTypeKey = m.group(2);
				String deviceId = m.group(3);
				String fieldName = m.group(4);

				EnphaseDeviceType deviceType;
				try {
					deviceType = EnphaseDeviceType.fromValue(deviceTypeKey);
				} catch ( IllegalArgumentException e ) {
					// ignore and continue
					continue;
				}

				SystemQueryPlan plan = result.computeIfAbsent(systemId, _ -> {
					return new SystemQueryPlan(systemId);
				});

				ValueRef valueRef = new ValueRef(systemId, deviceType, deviceId, fieldName, config);
				plan.addValueRef(valueRef);
			}
		}

		// TODO: resolve wildcard inverter component IDs

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

	private List<GeneralDatum> parseSiteInverterDatum(JsonNode json, List<ValueRef> refs,
			CloudDatumStreamConfiguration ds, Map<String, String> sourceIdMap,
			CloudDatumStreamQueryFilter filter) {
		/*- EXAMPLE JSON:
			{
			  "system_id": 2875,
			  "granularity": "day",
			  "total_devices": 16,
			  "start_at": 1738321200,
			  "end_at": 1738407600,
			  "items": "intervals",
			  "intervals": [
			    {
			      "end_at": 1738321500,
			      "devices_reporting": 16,
			      "powr": 0,
			      "enwh": 0
			    }
			  ],
			  "meta": {
			    "status": "normal",
			    "last_report_at": 1741059029,
			    "last_energy_at": 1741055400,
			    "operational_at": 1657080000
			  }
			}
		 */
		if ( json == null ) {
			return List.of();
		}

		final Integer totalDeviceCount = json.path("total_devices").intValue();

		final List<GeneralDatum> result = new ArrayList<>(16);

		// only need to compute the source ID once, as the same for all site inverter data
		String sourceId = null;

		for ( JsonNode telem : json.path("intervals") ) {
			long ts = telem.path(END_AT_PARAM).longValue();
			if ( ts < 1 ) {
				continue;
			} else if ( ts > filter.getEndDate().getEpochSecond() ) {
				// inverter query does not use end date, so abort once get to filter end date
				break;
			}

			DatumSamples s = new DatumSamples();
			for ( ValueRef ref : refs ) {
				if ( sourceId == null ) {
					sourceId = nonEmptyString(resolveSourceId(ds, ref, sourceIdMap));
					if ( sourceId == null ) {
						return List.of();
					}
				}
				JsonNode fieldNode = switch (ref.fieldName) {
					case "DevicesReporting" -> telem.path("devices_reporting");
					case "W" -> telem.path("powr");
					case "Wh" -> telem.path("enwh");
					default -> null;
				};
				if ( fieldNode == null || fieldNode.isNull() || fieldNode.isMissingNode() ) {
					continue;
				}

				Object propVal = parseJsonDatumPropertyValue(fieldNode, ref.property.getPropertyType());
				propVal = ref.property.applyValueTransforms(propVal);
				if ( propVal != null ) {
					s.putSampleValue(ref.property.getPropertyType(), ref.property.getPropertyName(),
							propVal);
				}
			}

			if ( s.isEmpty() ) {
				continue;
			}

			// add internal device count props
			int reportingDeviceCount = telem.path("devices_reporting").intValue();
			s.putStatusSampleValue(INTERNAL_TOTAL_DEVICES_PROPERTY, totalDeviceCount);
			s.putStatusSampleValue(INTERNAL_DEVICES_REPORTING_PROPERTY, reportingDeviceCount);

			result.add(new GeneralDatum(
					new DatumId(ds.getKind(), ds.getObjectId(), sourceId, ofEpochSecond(ts)), s));
		}

		return result;
	}

	private static boolean hasPhaseRef(List<ValueRef> refs) {
		if ( refs == null ) {
			return false;
		}
		for ( ValueRef ref : refs ) {
			if ( ref.isMeterPhaseField() ) {
				return true;
			}
		}
		return false;
	}

	private List<GeneralDatum> parseSiteMeterDatum(JsonNode json, List<ValueRef> refs,
			CloudDatumStreamConfiguration ds, Map<String, String> sourceIdMap) {
		/*- EXAMPLE JSON:
			{
			  "system_id": 2875,
			  "total_devices": 1,
			  "intervals": [
			    {
			      "end_at": 1741088700,
			      "devices_reporting": 1,
			      "wh_del": 0
			    }
			  ],
			  "meta": {
			    "status": "normal",
			    "last_report_at": 1741118911,
			    "last_energy_at": 1741118400,
			    "operational_at": 1657080000
			  },
			  "meter_intervals": [
			    {
			      "meter_serial_number": "000005058195EIM1",
			      "envoy_serial_number": "000005058195",
			      "intervals": [
			        {
			          "channel": 1,
			          "wh_del": 0.0,
			          "curr_w": 0,
			          "end_at": 1741088700
			        },
			        {
			          "channel": 2,
			          "wh_del": 0.0,
			          "curr_w": 0,
			          "end_at": 1741088700
			        },
			        {
			          "channel": 3,
			          "wh_del": null,
			          "curr_w": null,
			          "end_at": 1741088700
			        }
			      ]
			    }
			  ]
			}
		 */
		if ( json == null ) {
			return List.of();
		}

		final Integer totalDeviceCount = json.path("total_devices").intValue();

		final List<GeneralDatum> result = new ArrayList<>(16);

		// only need to compute the source ID once, as the same for all site meter data
		String sourceId = null;

		// first gather up phase-level readings
		final Map<Instant, List<JsonNode>> phaseReadings = hasPhaseRef(refs) ? new HashMap<>(8) : null;
		if ( phaseReadings != null ) {
			for ( JsonNode meter : json.path("meter_intervals") ) {
				for ( JsonNode telem : meter.path("intervals") ) {
					long ts = telem.path(END_AT_PARAM).longValue();
					if ( ts < 1 ) {
						continue;
					}
					phaseReadings.computeIfAbsent(ofEpochSecond(ts), _ -> new ArrayList<>(3)).add(telem);
				}
			}
		}

		for ( JsonNode telem : json.path("intervals") ) {
			long ts = telem.path(END_AT_PARAM).longValue();
			if ( ts < 1 ) {
				continue;
			}

			Instant date = ofEpochSecond(ts);
			DatumSamples s = new DatumSamples();
			for ( ValueRef ref : refs ) {
				if ( sourceId == null ) {
					sourceId = nonEmptyString(resolveSourceId(ds, ref, sourceIdMap));
					if ( sourceId == null ) {
						return List.of();
					}
				}
				Object propVal = null;
				if ( "DevicesReporting".equals(ref.fieldName) ) {
					propVal = telem.path("devices_reporting").intValue();
				} else if ( "WhExp".equals(ref.fieldName) ) {
					JsonNode fieldNode = telem.path("wh_del");
					if ( fieldNode == null || fieldNode.isNull() || fieldNode.isMissingNode() ) {
						continue;
					}
					propVal = parseJsonDatumPropertyValue(fieldNode, ref.property.getPropertyType());
				} else if ( phaseReadings != null ) {
					// phase data required
					List<JsonNode> phaseNodes = phaseReadings.get(date);
					if ( phaseNodes == null ) {
						continue;
					}
					if ( "W".equals(ref.fieldName) ) {
						// average of available phases
						BigDecimal totPower = BigDecimal.ZERO;
						int count = 0;
						for ( JsonNode phaseNode : phaseNodes ) {
							JsonNode powerNode = phaseNode.path("curr_w");
							if ( powerNode.isNumber() ) {
								totPower = totPower.add(powerNode.decimalValue());
								count++;
							}
						}
						if ( count > 0 ) {
							propVal = totPower.divide(new BigDecimal(count), RoundingMode.DOWN);
						}
					} else {
						String fieldName = ref.fieldName.substring(0, ref.fieldName.length() - 1);
						int desiredChannel = switch (ref.fieldName.charAt(fieldName.length())) {
							case 'A' -> 1;
							case 'B' -> 2;
							case 'C' -> 3;
							default -> -1;
						};
						JsonNode phaseTelem = phaseNodes.stream()
								.filter(n -> desiredChannel == n.path("channel").intValue()).findFirst()
								.orElse(null);
						JsonNode fieldNode = switch (fieldName) {
							case "PW" -> phaseTelem.path("curr_w");
							case "PWhExp" -> phaseTelem.path("wh_del");
							default -> null;
						};
						if ( fieldNode == null || fieldNode.isNull() || fieldNode.isMissingNode() ) {
							continue;
						}

						propVal = parseJsonDatumPropertyValue(fieldNode, ref.property.getPropertyType());
					}
				}
				propVal = ref.property.applyValueTransforms(propVal);
				if ( propVal != null ) {
					s.putSampleValue(ref.property.getPropertyType(), ref.property.getPropertyName(),
							propVal);
				}
			}

			if ( s.isEmpty() ) {
				continue;
			}

			// add internal device count props
			int reportingDeviceCount = telem.path("devices_reporting").intValue();
			s.putStatusSampleValue(INTERNAL_TOTAL_DEVICES_PROPERTY, totalDeviceCount);
			s.putStatusSampleValue(INTERNAL_DEVICES_REPORTING_PROPERTY, reportingDeviceCount);

			result.add(new GeneralDatum(new DatumId(ds.getKind(), ds.getObjectId(), sourceId, date), s));
		}

		return result;
	}

}
