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
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import javax.cache.Cache;
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
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.http.OAuth2RestOperationsHelper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Enphase implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.0
 */
public class EnphaseCloudDatumStreamService extends BaseOAuth2ClientCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.enphase";

	/** The data value filter key for a system ID. */
	public static final String SYSTEM_ID_FILTER = "systemId";

	/** The data value filter key for a device ID. */
	public static final String DEVICE_ID_FILTER = "deviceId";

	/** Constant device ID value for site-level data. */
	public static final String SITE_DEVICE_ID = "site";

	/**
	 * The URI path to view a given system.
	 *
	 * <p>
	 * Accepts a single {@code {systemId}} parameter.
	 * </p>
	 */
	public static final String SYSTEM_VIEW_URL_TEMPLATE = "/api/v4/systems/{systemId}";

	/**
	 * The URI path to list the devices for a given system.
	 *
	 * <p>
	 * Accepts a single {@code {systemId}} parameter.
	 * </p>
	 */
	public static final String SYSTEM_DEVICES_URL_TEMPLATE = "/api/v4/systems/{systemId}/devices";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		SETTINGS = List.of();
	}

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SYSTEM_ID_FILTER,
			DEVICE_ID_FILTER);

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
						userEventAppenderBiz, restOps, HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> EnphaseCloudIntegrationService.SECURE_SETTINGS,
						oauthClientManager, clock, integrationLocksCache),
				oauthClientManager);
	}

	@Override
	protected Iterable<String> supportedPlaceholders() {
		return SUPPORTED_PLACEHOLDERS;
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
			// list available sites
			result = systems(integration);
		}
		return result;
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");

		final Instant endDate = null;
		final Instant startDate = null;

		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);

		final var result = datum(datumStream, filter);
		if ( result == null ) {
			return Collections.emptyList();
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

			return null;
		});
	}

	private List<CloudDataValue> systems(CloudIntegrationConfiguration integration) {
		final var decryp = integration.copyWithId(integration.getId());
		decryp.unmaskSensitiveInformation(id -> SECURE_SETTINGS, encryptor);
		List<CloudDataValue> result = null;

		final var pagination = new Pagination();

		while ( pagination.hasMore() ) {
			List<CloudDataValue> pageResults = restOpsHelper.httpGet("List systems", integration,
					JsonNode.class,
					(req) -> fromUri(resolveBaseUrl(integration, BASE_URI))
							.path(EnphaseCloudIntegrationService.LIST_SYSTEMS_URL)
							.queryParam(API_KEY_PARAM,
									decryp.serviceProperty(API_KEY_SETTING, String.class))
							.queryParam(PAGE_SIZE_PARAM, MAX_PAGE_SIZE)
							.queryParam(PAGE_PARAM, Math.max(1, pagination.page))
							.buildAndExpand(integration.getServiceProperties()).toUri(),
					res -> {
						var json = res.getBody();
						pagination.parseJson(json);
						return parseSystems(json, null);
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

	private List<CloudDataValue> system(CloudIntegrationConfiguration integration,
			Map<String, ?> filters, List<CloudDataValue> children) {
		final var decryp = integration.copyWithId(integration.getId());
		decryp.unmaskSensitiveInformation(id -> SECURE_SETTINGS, encryptor);

		// first get system details, will then add site-level children
		List<CloudDataValue> result = restOpsHelper.httpGet("View system", integration, JsonNode.class,
		// @formatter:off
				(req) -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(SYSTEM_VIEW_URL_TEMPLATE)
						.queryParam(API_KEY_PARAM, decryp.serviceProperty(API_KEY_SETTING, String.class))
						.buildAndExpand(filters).toUri(),
						// @formatter:on
				res -> parseSystem(res.getBody(), filters, children));

		// add site-level children
		if ( children != null ) {
			// TODO
		}

		return result;
	}

	private List<CloudDataValue> systemDevices(final CloudIntegrationConfiguration integration,
			final String systemId, Map<String, ?> filters) {
		final var decryp = integration.copyWithId(integration.getId());
		decryp.unmaskSensitiveInformation(id -> SECURE_SETTINGS, encryptor);

		return restOpsHelper.httpGet("List system devices", integration, JsonNode.class,
		// @formatter:off
				(req) -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(SYSTEM_DEVICES_URL_TEMPLATE)
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

	private static List<CloudDataValue> parseSystems(JsonNode json, Map<String, ?> filters) {
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
			result.addAll(parseSystem(sysNode, filters, null));
		}
		return result;
	}

	private static List<CloudDataValue> parseSystem(JsonNode json, Map<String, ?> filters,
			Collection<CloudDataValue> children) {
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
			result.addAll(siteInverterDataValues(systemId));
			// TODO: add device-level values
		}

		final JsonNode meters = devices.path("meters");
		if ( !(meters.isMissingNode() || meters.isEmpty()) ) {
			result.addAll(siteMeterDataValues(systemId));
			// TODO: add device-level values
		}

		return result;
	}

	private static List<CloudDataValue> siteInverterDataValues(final String systemId) {
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
				// site-level
				dataValue(List.of(systemId, Inverter.getKey(), SITE_DEVICE_ID, "W"), "Active power"),
				dataValue(List.of(systemId, Inverter.getKey(), SITE_DEVICE_ID, "Wh"), "Active energy")
				);
		// @formatter:on
	}

	private static List<CloudDataValue> siteMeterDataValues(final String systemId) {
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
				dataValue(List.of(systemId, Meter.getKey(), SITE_DEVICE_ID, "W"), "Active power"),
				dataValue(List.of(systemId, Meter.getKey(), SITE_DEVICE_ID, "WhExp"), "Active energy exported")
				);
		// @formatter:on
	}

}
