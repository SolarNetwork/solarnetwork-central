/* ==================================================================
 * SolarEdgeV1CloudDatumStreamService.java - 7/10/2024 7:03:25 am
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

import static java.util.Collections.unmodifiableMap;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeDeviceType.Battery;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeDeviceType.Inverter;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeDeviceType.Meter;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeV1CloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.COUNTRY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_FIRMWARE_VERSION_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_MODEL_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.LOCALITY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.MANUFACTURER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.POSTAL_CODE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.REPLACED_BY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STATE_PROVINCE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STREET_ADDRESS_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.TIME_ZONE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.WILDCARD_IDENTIFIER;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.pathReferenceValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.cache.Cache;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.client.HttpClientErrorException;
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
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.StringUtils;

/**
 * SolarEdge implementation of {@link CloudDatumStreamService} using the V1 API.
 *
 * @author matt
 * @version 1.8
 */
public class SolarEdgeV1CloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.solaredge.v1";

	/** The data value filter key for a site ID. */
	public static final String SITE_ID_FILTER = "siteId";

	/**
	 * The data value filter key for a {@link SolarEdgeDeviceType} device type.
	 */
	public static final String DEVICE_TYPE_FILTER = "deviceType";

	/** The data value filter key for a component ID. */
	private static final String COMPONENT_ID_FILTER = "componentId";

	/** The setting for resolution. */
	public static final String RESOLUTION_SETTING = "resolution";

	/**
	 * The setting to upper-case source ID values.
	 *
	 * @since 1.2
	 */
	public static final String UPPER_CASE_SOURCE_ID_SETTING = "upperCaseSourceId";

	/**
	 * The setting to device index based source ID values.
	 *
	 * @since 1.2
	 */
	public static final String INDEX_BASED_SOURCE_ID_SETTING = "indexBasedSourceId";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// menu for granularity
		var resolutionSpec = new BasicMultiValueSettingSpecifier(RESOLUTION_SETTING,
				SolarEdgeResolution.FifteenMinute.getKey());
		var resolutionTitles = unmodifiableMap(Arrays.stream(SolarEdgeResolution.values())
				.collect(Collectors.toMap(SolarEdgeResolution::getKey, SolarEdgeResolution::getKey,
						(_, r) -> r, () -> new LinkedHashMap<>(SolarEdgeResolution.values().length))));
		resolutionSpec.setValueTitles(resolutionTitles);

		// @formatter:off
		SETTINGS = List.of(
				UPPER_CASE_SOURCE_ID_SETTING_SPECIFIER,
				new BasicToggleSettingSpecifier(INDEX_BASED_SOURCE_ID_SETTING, false),
				resolutionSpec,
				SOURCE_ID_MAP_SETTING_SPECIFIER,
				MULTI_STREAM_MAXIMUM_LAG_SETTING_SPECIFIER,
				VIRTUAL_SOURCE_IDS_SETTING_SPECIFIER);
		// @formatter:on
	}

	/**
	 * The URI path to list the inventory for a given site.
	 *
	 * <p>
	 * Accepts a single {@code {siteId}} parameter.
	 * </p>
	 */
	public static final String SITE_INVENTORY_URL_TEMPLATE = "/site/{siteId}/inventory";

	/**
	 * The URI path to view the details for a given site.
	 *
	 * <p>
	 * Accepts one parameter: {@code {siteId}}.
	 * </p>
	 */
	public static final String SITE_DETAILS_URL_TEMPLATE = "/site/{siteId}/details";

	/**
	 * The URI path to list the equipment data for a given site and component.
	 *
	 * <p>
	 * Accepts two parameters: {@code {siteId}} and {@code {componentId}}.
	 * </p>
	 */
	public static final String EQUIPMENT_DATA_URL_TEMPLATE = "/equipment/{siteId}/{componentId}/data";

	/**
	 * The URI path to list the equipment "change log" for a given site and
	 * component.
	 *
	 * <p>
	 * Accepts two parameters: {@code {siteId}} and {@code {componentId}}.
	 * </p>
	 *
	 * @since 1.7
	 */
	public static final String EQUIPMENT_CHANGELOG_URL_TEMPLATE = "/equipment/{siteId}/{componentId}/changeLog";

	/**
	 * The URI path to list the meter power data for a given site.
	 *
	 * <p>
	 * Accepts one parameter: {@code {siteId}}.
	 * </p>
	 */
	public static final String POWER_DETAILS_URL_TEMPLATE = "/site/{siteId}/powerDetails";

	/**
	 * The URI path to list the meter energy data for a given site.
	 *
	 * <p>
	 * Accepts one parameter: {@code {siteId}}.
	 * </p>
	 */
	public static final String METERS_URL_TEMPLATE = "/site/{siteId}/meters";

	/**
	 * The URI path to list the storage (battery) data for a given site.
	 *
	 * <p>
	 * Accepts one parameter: {@code {siteId}}.
	 * </p>
	 */
	public static final String STORAGE_DATA_URL_TEMPLATE = "/site/{siteId}/storageData";

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
	private Cache<Long, ZoneId> siteTimeZoneCache;

	/**
	 * A cache of SolarEdge site IDs to associated inventory information. This
	 * is used to resolve the available device identifiers for a given site.
	 */
	private Cache<Long, CloudDataValue[]> siteInventoryCache;

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
	public SolarEdgeV1CloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			Clock clock) {
		super(SERVICE_IDENTIFIER, "SolarEdge V1 Datum Stream Service", clock, userEventAppenderBiz,
				encryptor, expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new SolarEdgeV1RestOperationsHelper(
						LoggerFactory.getLogger(SolarEdgeV1CloudDatumStreamService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						_ -> SolarEdgeV1CloudIntegrationService.SECURE_SETTINGS));
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
			Map<String, ?> filters) {
		final CloudIntegrationConfiguration integration = requireNonNullObject(
				integrationDao.get(requireNonNullArgument(integrationId, "integrationId")),
				"integration");
		List<CloudDataValue> result;
		if ( filters != null && filters.get(SITE_ID_FILTER) != null
				&& filters.get(DEVICE_TYPE_FILTER) != null
				&& filters.get(COMPONENT_ID_FILTER) != null ) {
			result = components(filters);
		} else if ( filters != null && filters.get(SITE_ID_FILTER) != null ) {
			result = siteInventory(integration, filters);
		} else {
			// list available sites
			result = sites(integration);
		}
		Collections.sort(result);
		return result;
	}

	private List<CloudDataValue> sites(CloudIntegrationConfiguration integration) {
		return restOpsHelper.httpGet("List sites", integration, JsonNode.class,
				_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
						.path(SolarEdgeV1CloudIntegrationService.SITES_LIST_URL)
						.buildAndExpand(integration.getServiceProperties()).toUri(),
				res -> parseSites(res.getBody()));
	}

	private List<CloudDataValue> siteInventory(CloudIntegrationConfiguration integration,
			Map<String, ?> filters) {
		return restOpsHelper.httpGet("List site inventory", integration, JsonNode.class,
				_ -> fromUri(resolveBaseUrl(integration, BASE_URI)).path(SITE_INVENTORY_URL_TEMPLATE)
						.buildAndExpand(filters).toUri(),
				res -> parseSiteInventory(integration, res.getBody(), filters));
	}

	private List<CloudDataValue> equipmentChangeLog(CloudIntegrationConfiguration integration,
			Map<String, ?> filters, SolarEdgeDeviceType deviceType, String replacedByRef) {
		try {
			return restOpsHelper.httpGet("List equipment change log", integration, JsonNode.class,
					(_) -> fromUri(resolveBaseUrl(integration, BASE_URI))
							.path(EQUIPMENT_CHANGELOG_URL_TEMPLATE).buildAndExpand(filters).toUri(),
					res -> parseEquipmentChangeLog(res.getBody(), filters, deviceType, replacedByRef));
		} catch ( RemoteServiceException e ) {
			if ( e.getCause() instanceof HttpClientErrorException hce
					&& hce.getStatusCode().is4xxClientError() ) {
				// ignore as "equipment not found" and move on
				return List.of();
			}
			throw e;
		}
	}

	private List<CloudDataValue> components(Map<String, ?> filters) {
		final String siteId = filters.get(SITE_ID_FILTER).toString();
		final SolarEdgeDeviceType deviceType = SolarEdgeDeviceType
				.fromValue(filters.get(DEVICE_TYPE_FILTER).toString());
		final String componentId = filters.get(COMPONENT_ID_FILTER).toString();
		return switch (deviceType) {
			case Inverter -> inverterDataValues(siteId, deviceType, componentId);
			case Meter -> meterDataValues(siteId, deviceType, componentId);
			case Battery -> batteryDataValues(siteId, deviceType, componentId);
			default -> Collections.emptyList();
		};
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private static List<CloudDataValue> parseSites(JsonNode json) {
		if ( json == null ) {
			return Collections.emptyList();
		}
		/*- EXAMPLE JSON:
		{
		  "sites": {
		    "count": 43,
		    "site": [
		      {
		        "id": 123123,
		        "name": "Acme",
		        "accountId": 123123,
		        "status": "Active",
		        "peakPower": 244.05,
		        "lastUpdateTime": "2024-10-22",
		        "installationDate": "2022-05-02",
		        "ptoDate": null,
		        "notes": "",
		        "type": "Optimizers & Inverters",
		        "location": {
		          "country": "United States",
		          "state": "Rhode Island",
		          "city": "East Providence",
		          "address": "123 Main Street",
		          "address2": "",
		          "zip": "02916",
		          "timeZone": "America/New_York",
		          "countryCode": "US",
		          "stateCode": "RI"
		        },
		        "alertQuantity": 1,
		        "highestImpact": 2,
		        "primaryModule": {
		          "manufacturerName": "Q Cells",
		          "modelName": "Q.PEAK DUO L-G5.2 385",
		          "maximumPower": 395.0,
		          "temperatureCoef": -0.28
		        },
		        "uris": {
		          "DETAILS": "/site/123123/details",
		          "DATA_PERIOD": "/site/123123/dataPeriod",
		          "OVERVIEW": "/site/123123/overview"
		        },
		        "publicSettings": {
		          "isPublic": false
		        }
		      },
		*/
		final var result = new ArrayList<CloudDataValue>(4);
		for ( JsonNode siteNode : json.path("sites").path("site") ) {
			final String id = siteNode.path("id").asText();
			final String name = siteNode.path("name").asText().trim();
			final var meta = new LinkedHashMap<String, Object>(4);
			if ( siteNode.hasNonNull("status") ) {
				meta.put("status", siteNode.path("status").asText());
			}
			final JsonNode locNode = siteNode.path("location");
			if ( locNode.isObject() ) {
				populateNonEmptyValue(locNode, "address", STREET_ADDRESS_METADATA, meta);
				populateNonEmptyValue(locNode, "city", LOCALITY_METADATA, meta);
				populateNonEmptyValue(locNode, "state", STATE_PROVINCE_METADATA, meta);
				populateNonEmptyValue(locNode, "country", COUNTRY_METADATA, meta);
				populateNonEmptyValue(locNode, "zip", POSTAL_CODE_METADATA, meta);
				populateNonEmptyValue(locNode, "timeZone", TIME_ZONE_METADATA, meta);
			}
			populateNonEmptyValue(siteNode, "activationStatus", "activationStatus", meta);
			populateNonEmptyValue(siteNode, "notes", "notes", meta);

			result.add(intermediateDataValue(List.of(id), name, meta.isEmpty() ? null : meta));
		}
		return result;
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private List<CloudDataValue> parseSiteInventory(final CloudIntegrationConfiguration integration,
			final JsonNode json, final Map<String, ?> filters) {
		if ( json == null ) {
			return Collections.emptyList();
		}
		final String siteId = filters.get(SITE_ID_FILTER).toString();
		/*- EXAMPLE JSON:
		{
		  "Inventory": {
		    "meters": [
		      {...}
		    ],
		    "sensors": [],
		    "gateways": [],
		    "batteries": [
		      {...}
		    ],
		    "inverters": [
		      {...}
		    ],
		    "thirdPartyInverters": [
		      {...}
		    ]
		  }
		}
		*/
		final var result = new ArrayList<CloudDataValue>(4);
		final JsonNode inventoryNode = json.path("Inventory");

		// inverters
		if ( !(inventoryNode.path("inverters").isEmpty()
				&& inventoryNode.path("thirdPartyInverters").isEmpty()) ) {
			/*- EXAMPLE JSON:
			  {
			    "name": "Inverter 1",
			    "manufacturer": "SolarEdge",
			    "model": "SE7600A-USS20NHY2",
			    "communicationMethod": "SOLAREDGE_LTE",
			    "dsp1Version": "1.210.1623",
			    "dsp2Version": "2.52.615",
			    "cpuVersion": "3.2724.0",
			    "SN": "77777777-BA",
			    "connectedOptimizers": 20
			  }
			 */
			final var inverterValues = new ArrayList<CloudDataValue>(
					inventoryNode.path("inverters").size());
			for ( String nodeName : new String[] { "inverters", "thirdPartyInverters" } ) {
				for ( JsonNode inverterNode : inventoryNode.path(nodeName) ) {
					final String id = inverterNode.path("SN").asText().trim();
					if ( id.isEmpty() ) {
						continue;
					}
					final String name = inverterNode.path("name").asText().trim();
					final var meta = new LinkedHashMap<String, Object>(4);
					meta.put(DEVICE_SERIAL_NUMBER_METADATA, id);
					populateNonEmptyValue(inverterNode, "manufacturer", MANUFACTURER_METADATA, meta);
					populateNonEmptyValue(inverterNode, "model", DEVICE_MODEL_METADATA, meta);
					if ( inverterNode.hasNonNull("dsp1Version") || inverterNode.hasNonNull("dsp2Version")
							|| inverterNode.hasNonNull("cpuVersion") ) {
						StringBuilder buf = new StringBuilder();
						if ( inverterNode.hasNonNull("dsp1Version") ) {
							buf.append("DSP1: ")
									.append(inverterNode.path("dsp1Version").asText().trim());
						}
						if ( inverterNode.hasNonNull("dsp2Version") ) {
							if ( !buf.isEmpty() ) {
								buf.append(", ");
							}
							buf.append("DSP2: ")
									.append(inverterNode.path("dsp1Version").asText().trim());
						}
						if ( inverterNode.hasNonNull("cpuVersion") ) {
							if ( !buf.isEmpty() ) {
								buf.append(", ");
							}
							buf.append("CPU: ").append(inverterNode.path("cpuVersion").asText().trim());
						}
						meta.put(DEVICE_FIRMWARE_VERSION_METADATA, buf.toString());
					}

					final var dataValue = intermediateDataValue(List.of(siteId, Inverter.getKey(), id),
							name, meta);
					inverterValues.add(dataValue);

					// look for replaced equipment in changelog
					inverterValues.addAll(equipmentChangeLog(integration,
							Map.of(SITE_ID_FILTER, siteId, COMPONENT_ID_FILTER, id), Inverter,
							pathReferenceValue(dataValue.getIdentifiers())));
				}
			}
			result.add(intermediateDataValue(List.of(siteId, Inverter.getKey()), Inverter.getGroupKey(),
					null, inverterValues));
		}

		// meters
		if ( !inventoryNode.path("meters").isEmpty() ) {
			/*- EXAMPLE JSON:
			  {
			    "name": "Export Meter",
			    "manufacturer": "SolarEdge",
			    "model": "SE-MTR-3Y-240V-A",
			    "firmwareVersion": "72",
			    "connectedTo": "Inverter 1",
			    "connectedSolaredgeDeviceSN": "77777777-BA",
			    "type": "FeedIn",
			    "form": "physical",
			    "SN": "111111111"
			  }
			 */
			final var meterValues = new ArrayList<CloudDataValue>(inventoryNode.path("meters").size());
			for ( JsonNode meterNode : inventoryNode.path("meters") ) {
				final String id = meterNode.path("type").asText().trim();
				if ( id.isEmpty() ) {
					continue;
				}
				final String name = meterNode.path("name").asText().trim();
				final var meta = new LinkedHashMap<String, Object>(4);
				populateNonEmptyValue(meterNode, "SN", DEVICE_SERIAL_NUMBER_METADATA, meta);
				populateNonEmptyValue(meterNode, "manufacturer", MANUFACTURER_METADATA, meta);
				populateNonEmptyValue(meterNode, "model", DEVICE_MODEL_METADATA, meta);
				populateNonEmptyValue(meterNode, "connectedTo", "connectedTo", meta);
				populateNonEmptyValue(meterNode, "connectedSolaredgeDeviceSN", "connectedToSerial",
						meta);
				populateNonEmptyValue(meterNode, "firmwareVersion", DEVICE_FIRMWARE_VERSION_METADATA,
						meta);

				final var dataValue = intermediateDataValue(List.of(siteId, Meter.getKey(), id), name,
						meta);
				meterValues.add(dataValue);

				// look for replaced equipment in changelog
				if ( meta.containsKey(DEVICE_SERIAL_NUMBER_METADATA) ) {
					meterValues.addAll(equipmentChangeLog(integration,
							Map.of(SITE_ID_FILTER, siteId, COMPONENT_ID_FILTER,
									meta.get(DEVICE_SERIAL_NUMBER_METADATA)),
							Meter, pathReferenceValue(dataValue.getIdentifiers())));
				}
			}
			result.add(intermediateDataValue(List.of(siteId, Meter.getKey()), Meter.getGroupKey(), null,
					meterValues));
		}

		// batteries
		if ( !inventoryNode.path("batteries").isEmpty() ) {
			/*- EXAMPLE JSON:
			  {
			    "name": "Battery 1.1",
			    "manufacturer": "LG",
			    "model": "R15563P3SSEG12005081037",
			    "firmwareVersion": "DCDC 7.5.6 BMS 1.9.6.0",
			    "connectedTo": "Inverter 1",
			    "connectedInverterSn": "77777777-BA",
			    "nameplateCapacity": 9800.0,
			    "SN": "121212121212121212121212121"
			  }
			 */
			final var batteryValues = new ArrayList<CloudDataValue>(
					inventoryNode.path("batteries").size());
			for ( JsonNode batteryNode : inventoryNode.path("batteries") ) {
				final String id = batteryNode.path("SN").asText().trim();
				if ( id.isEmpty() ) {
					continue;
				}
				final String name = batteryNode.path("name").asText().trim();
				final var meta = new LinkedHashMap<String, Object>(4);
				populateNonEmptyValue(batteryNode, "SN", DEVICE_SERIAL_NUMBER_METADATA, meta);
				populateNonEmptyValue(batteryNode, "manufacturer", MANUFACTURER_METADATA, meta);
				populateNonEmptyValue(batteryNode, "model", DEVICE_MODEL_METADATA, meta);
				populateNonEmptyValue(batteryNode, "connectedTo", "connectedTo", meta);
				populateNonEmptyValue(batteryNode, "connectedInverterSn", "connectedToSerial", meta);
				populateNonEmptyValue(batteryNode, "firmwareVersion", DEVICE_FIRMWARE_VERSION_METADATA,
						meta);
				populateNonEmptyValue(batteryNode, "nameplateCapacity", "capacity", meta);

				final var dataValue = intermediateDataValue(List.of(siteId, Battery.getKey(), id), name,
						meta);
				batteryValues.add(dataValue);

				// look for replaced equipment in changelog
				batteryValues.addAll(equipmentChangeLog(integration,
						Map.of(SITE_ID_FILTER, siteId, COMPONENT_ID_FILTER, id), Battery,
						pathReferenceValue(dataValue.getIdentifiers())));

			}
			result.add(intermediateDataValue(List.of(siteId, Battery.getKey()), Battery.getGroupKey(),
					null, batteryValues));
		}

		return result;

	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private static List<CloudDataValue> parseEquipmentChangeLog(JsonNode json, Map<String, ?> filters,
			SolarEdgeDeviceType deviceType, String replacedByRef) {
		if ( json == null ) {
			return Collections.emptyList();
		}
		final String siteId = filters.get(SITE_ID_FILTER).toString();
		/*- EXAMPLE JSON:
		{
		  "ChangeLog": {
		    "count": 1,
		    "list": [
		      {
		        "serialNumber": "7E130000-01",
		        "partNumber": "SE20K-USR48NNU4",
		        "date": "2025-09-09"
		      }
		    ]
		  }
		}
		*/
		final var result = new ArrayList<CloudDataValue>(4);
		final JsonNode changeLogNode = json.path("ChangeLog");

		for ( JsonNode changedNode : changeLogNode.path("list") ) {
			final String id = changedNode.path("serialNumber").asText().trim();
			if ( id.isEmpty() ) {
				continue;
			}
			result.add(intermediateDataValue(List.of(siteId, deviceType.getKey(), id), id,
					Map.of(REPLACED_BY_METADATA, replacedByRef, DEVICE_SERIAL_NUMBER_METADATA, id)));
		}

		return result;
	}

	private static List<CloudDataValue> inverterDataValues(String siteId, SolarEdgeDeviceType deviceType,
			String componentId) {
		// battery data extracted from /equipment/{siteId}/{componentId}/data
		/*- EXAMPLE JSON:
		  {
		    "date": "2024-10-22 06:19:07",
		    "totalActivePower": 0.0,
		    "dcVoltage": null,
		    "powerLimit": 0.0,
		    "totalEnergy": 7.8223802E8,
		    "temperature": 0.0,
		    "inverterMode": "SLEEPING",
		    "operationMode": 0,
		    "vL1To2": 476.406,
		    "vL2To3": 475.594,
		    "vL3To1": 474.875,
		    "L1Data": {
		      "acCurrent": 0.0,
		      "acVoltage": 274.656,
		      "acFrequency": 59.9603,
		      "apparentPower": 0.0,
		      "activePower": 0.0,
		      "reactivePower": 0.0,
		      "cosPhi": 0.0
		    },
		    "L2Data": {
		      "acCurrent": 0.0,
		      "acVoltage": 275.406,
		      "acFrequency": 59.9614,
		      "apparentPower": 0.0,
		      "activePower": 0.0,
		      "reactivePower": 0.0,
		      "cosPhi": 0.0
		    },
		    "L3Data": {
		      "acCurrent": 0.0,
		      "acVoltage": 273.984,
		      "acFrequency": 59.9609,
		      "apparentPower": 0.0,
		      "activePower": 0.0,
		      "reactivePower": 0.0,
		      "cosPhi": 0.0
		    }
		  }
		 */
		// @formatter:off
		return Arrays.asList(
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "W"), "Total active power"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "DCV"), "DC voltage"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "GndRes"), "Ground fault resistance"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "LimitW"), "Power limit"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "TotWhExp"), "Total energy"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "Temp"), "Temperature"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "Mode"), "Mode"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "OpMode"), "Operational mode"),

				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PVphAN"), "1 phase line voltage, A"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PVphBN"), "1 phase line voltage, B"),

				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PPVphAB"), "Line-line voltage, A-B"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PPVphBC"), "Line-line voltage, B-C"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PPVphCA"), "Line-line voltage, C-A"),

				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PIA"), "Phase current - A"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PIB"), "Phase current - B"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PIC"), "Phase current - C"),

				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PVA"), "Phase voltage - A"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PVB"), "Phase voltage - B"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PVC"), "Phase voltage - C"),

				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PHzA"), "Phase frequency - A"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PHzB"), "Phase frequency - B"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PHzC"), "Phase frequency - C"),

				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PWA"), "Phase active power - A"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PWB"), "Phase active power - B"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PWC"), "Phase active power - C"),

				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PVAA"), "Phase apparent power - A"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PVAB"), "Phase apparent power - B"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PVAC"), "Phase apparent power - C"),

				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PVARA"), "Phase reactive power - A"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PVARB"), "Phase reactive power - B"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PVARC"), "Phase reactive power - C"),

				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PPFA"), "Phase power factor - A"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PPFB"), "Phase power factor - B"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "PPFC"), "Phase power factor - C")
				);
		// @formatter:on
	}

	private static List<CloudDataValue> meterDataValues(String siteId, SolarEdgeDeviceType deviceType,
			String componentId) {
		// power extracted from /site/{siteId}/powerDetails
		// lifetime energy extracted from /site/{siteId}/meters
		// @formatter:off
		return Arrays.asList(
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "W"), "Power"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "TotWh"), "Lifetime energy")
				);
		// @formatter:on
	}

	private static List<CloudDataValue> batteryDataValues(String siteId, SolarEdgeDeviceType deviceType,
			String componentId) {
		// battery data extracted from /site/{siteId}/storageData
		/*- EXAMPLE JSON:
		  {
		    "timeStamp": "2024-10-21 22:09:43",
		    "power": -273.062,
		    "batteryState": 4,
		    "lifeTimeEnergyDischarged": 5508927,
		    "lifeTimeEnergyCharged": 7468751,
		    "batteryPercentageState": 87.989944,
		    "fullPackEnergyAvailable": 8751.0,
		    "internalTemp": 20.7,
		    "ACGridCharging": 0.0
		  }
		 */
		// @formatter:off
		return Arrays.asList(
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "W"), "Power"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "State"), "Battery state"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "TotWhExp"), "Lifetime energy discharged"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "TotWhImp"), "Lifetime energy charged"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "SOC"), "State of charge"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "CapWh"), "Energy capacity"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "Temp"), "Internal temperature"),
				dataValue(List.of(siteId, deviceType.getKey(), componentId, "GridWhImp"), "AC grid charge energy")
				);
		// @formatter:on
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");
		final SolarEdgeResolution resolution = resolveResolution(datumStream, null);
		final Clock queryClock = Clock.tick(clock, resolution.getTickDuration());
		final Instant endDate = queryClock.instant();
		final Instant startDate = endDate.minus(resolution.getTickDuration());

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

			final SolarEdgeResolution resolution = resolveResolution(ds, filter.getParameters());

			final Map<String, String> sourceIdMap = servicePropertyStringMap(ds, SOURCE_ID_MAP_SETTING);

			final List<GeneralDatum> resultDatum = new ArrayList<>(16);
			final Map<Long, SiteQueryPlan> queryPlans = resolveSiteQueryPlans(integration, ds,
					sourceIdMap, valueProps);

			BasicQueryFilter nextQueryFilter = null;

			Instant startDate = resolution.truncateDate(filterStartDate);
			Instant endDate = resolution.truncateDate(filterEndDate);
			if ( Duration.between(startDate, endDate).compareTo(MAX_QUERY_TIME_RANGE) > 0 ) {
				Instant nextEndDate = startDate.plus(MAX_QUERY_TIME_RANGE.multipliedBy(2));
				if ( nextEndDate.isAfter(endDate) ) {
					nextEndDate = endDate;
				}

				endDate = startDate.plus(MAX_QUERY_TIME_RANGE);

				nextQueryFilter = new BasicQueryFilter();
				nextQueryFilter.setStartDate(endDate);
				nextQueryFilter.setEndDate(nextEndDate);
			}

			final BasicQueryFilter usedQueryFilter = new BasicQueryFilter();
			usedQueryFilter.setStartDate(startDate);
			usedQueryFilter.setEndDate(endDate);
			for ( SiteQueryPlan queryPlan : queryPlans.values() ) {
				ZonedDateTime siteStartDate = startDate.atZone(queryPlan.zone);
				ZonedDateTime siteEndDate = endDate.atZone(queryPlan.zone);

				DateTimeFormatter timestampFmt = DateUtils.ISO_DATE_OPT_TIME_ALT
						.withZone(queryPlan.zone);

				String startDateParam = timestampFmt.format(siteStartDate.toLocalDateTime());
				String endDateParam = timestampFmt.format(siteEndDate.toLocalDateTime());

				// inverter data
				if ( queryPlan.inverterIds != null && !queryPlan.inverterIds.isEmpty() ) {
					for ( String inverterId : queryPlan.inverterIds ) {
						List<GeneralDatum> datum = restOpsHelper.httpGet("List inverter data",
								integration, JsonNode.class,
								_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
										.path(EQUIPMENT_DATA_URL_TEMPLATE)
										.queryParam("startTime", startDateParam)
										.queryParam("endTime", endDateParam)
										.buildAndExpand(queryPlan.siteId, inverterId).toUri(),
								res -> parseInverterDatum(res.getBody(), queryPlan, inverterId, ds,
										sourceIdMap, timestampFmt));
						if ( datum != null ) {
							resultDatum.addAll(datum);
						}
					}
				}

				// meter data
				if ( queryPlan.includeMeters ) {
					// have to request two URLs for this
					// @formatter:off
					JsonNode meterPower = restOpsHelper.httpGet("List meter power data", integration,
							JsonNode.class,
							_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
									.path(POWER_DETAILS_URL_TEMPLATE)
									.queryParam("startTime", startDateParam)
									.queryParam("endTime", endDateParam)
									.queryParam("timeUnit", resolution.getKey())
									.buildAndExpand(queryPlan.siteId)
									.toUri(),
							HttpEntity::getBody);
					JsonNode meterEnergy = restOpsHelper.httpGet("List meter energy data", integration,
							JsonNode.class,
							_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
									.path(METERS_URL_TEMPLATE)
									.queryParam("startTime", startDateParam)
									.queryParam("endTime", endDateParam)
									.queryParam("timeUnit", resolution.getKey())
									.buildAndExpand(queryPlan.siteId)
									.toUri(),
							HttpEntity::getBody);
					// @formatter:on
					Collection<GeneralDatum> datum = parseMeterDatum(meterPower, meterEnergy, queryPlan,
							ds, sourceIdMap, timestampFmt, resolution);
					if ( datum != null ) {
						resultDatum.addAll(datum);
					}
				}

				// battery data
				if ( queryPlan.includeBatteries ) {
					List<GeneralDatum> datum = restOpsHelper.httpGet("List battery data", integration,
							JsonNode.class,
							_ -> fromUri(resolveBaseUrl(integration, BASE_URI))
									.path(STORAGE_DATA_URL_TEMPLATE)
									.queryParam("startTime", startDateParam)
									.queryParam("endTime", endDateParam).buildAndExpand(queryPlan.siteId)
									.toUri(),
							res -> parseBatteryDatum(res.getBody(), queryPlan, ds, sourceIdMap,
									timestampFmt));
					if ( datum != null ) {
						resultDatum.addAll(datum);
					}
				}

			}

			// evaluate expressions on merged datum
			var r = evaluateExpressions(datumStream, exprProps, resultDatum, mapping.getConfigId(),
					integration.getConfigId());

			Map<ObjectDatumStreamMetadataId, Instant> greatestTimestampPerStream = new HashMap<>(4);
			List<Datum> finalResult = new ArrayList<>(r.size());
			for ( GeneralDatum d : r ) {
				ObjectDatumStreamMetadataId streamPk = new ObjectDatumStreamMetadataId(d.getKind(),
						d.getObjectId(), d.getSourceId());
				Instant ts = d.getTimestamp();
				greatestTimestampPerStream.compute(streamPk,
						(_, v) -> v == null || ts.compareTo(v) > 0 ? ts : v);
				finalResult.add(d);
			}
			Collections.sort(finalResult, null);

			// latest datum might not have been reported yet; check latest datum date (per stream), and if
			// less than expected date make that the next query start date
			final Duration multiStreamMaximumLag = multiStreamMaximumLag(ds);
			if ( multiStreamMaximumLag.compareTo(Duration.ZERO) > 0
					&& greatestTimestampPerStream.size() > 1 ) {
				Instant leastGreatestTimestampPerStream = greatestTimestampPerStream.values().stream()
						.min(Instant::compareTo).get();
				Instant greatestTimestampAcrossStreams = greatestTimestampPerStream.values().stream()
						.max(Instant::compareTo).get();
				if ( leastGreatestTimestampPerStream.isBefore(greatestTimestampAcrossStreams)
						&& Duration.between(leastGreatestTimestampPerStream, clock.instant())
								.compareTo(multiStreamMaximumLag) < 0 ) {
					if ( nextQueryFilter == null ) {
						nextQueryFilter = new BasicQueryFilter();
					}
					nextQueryFilter
							.setStartDate(resolution.truncateDate(leastGreatestTimestampPerStream));
				}
			}

			return new BasicCloudDatumStreamQueryResult(usedQueryFilter, nextQueryFilter, finalResult);
		});
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private static List<GeneralDatum> parseInverterDatum(JsonNode json, SiteQueryPlan queryPlan,
			String inverterId, CloudDatumStreamConfiguration datumStream,
			Map<String, String> sourceIdMap, DateTimeFormatter timestampFmt) {
		/*- EXAMPLE JSON:
		{
		  "data": {
		    "count": 246,
		    "telemetries": [
		      {
		        "date": "2024-10-22 20:49:48",
		        "totalActivePower": 252.0,
		        "dcVoltage": 420.938,
		        "groundFaultResistance": 6000.0,
		        "powerLimit": 100.0,
		        "totalEnergy": 3.77542E7,
		        "temperature": 27.8494,
		        "inverterMode": "MPPT",
		        "operationMode": 0,
		        "vL1ToN": 123.219,
		        "vL2ToN": 123.531,
		        "L1Data": {
		          "acCurrent": 1.4375,
		          "acVoltage": 248.031,
		          "acFrequency": 60.0014,
		          "apparentPower": 354.5,
		          "activePower": 252.0,
		          "reactivePower": -249.0,
		          "cosPhi": 1.0
		        }
		      },
		 */
		final Map<String, List<ValueRef>> componentRefs = queryPlan.inverterRefs;
		final String sourceId = resolveSourceId(datumStream, queryPlan, SolarEdgeDeviceType.Inverter,
				inverterId, sourceIdMap);
		if ( sourceId == null ) {
			return Collections.emptyList();
		}
		List<GeneralDatum> result = new ArrayList<>(8);
		for ( JsonNode telem : json.findValue("telemetries") ) {
			String dateVal = nonEmptyString(telem.path("date").asText());
			if ( dateVal == null ) {
				continue;
			}
			Instant ts = timestampFmt.parse(dateVal, Instant::from);
			GeneralDatum d = new GeneralDatum(
					new DatumId(datumStream.getKind(), datumStream.getObjectId(), sourceId, ts),
					new DatumSamples());
			for ( String componentId : new String[] { WILDCARD_IDENTIFIER, inverterId } ) {
				if ( !componentRefs.containsKey(componentId) ) {
					continue;
				}
				for ( ValueRef ref : componentRefs.get(componentId) ) {
					JsonNode fieldNode = switch (ref.fieldName) {
						case "W" -> telem.path("totalActivePower");
						case "DCV" -> telem.path("dcVoltage");
						case "GndRes" -> telem.path("groundFaultResistance");
						case "LimitW" -> telem.path("powerLimit");
						case "TotWhExp" -> telem.path("totalEnergy");
						case "Temp" -> telem.path("temperature");
						case "Mode" -> telem.path("inverterMode");
						case "OpMode" -> telem.path("operationMode");

						case "PVphAN" -> telem.path("vL1ToN");
						case "PVphBN" -> telem.path("vL2ToN");

						case "PPVphAB" -> telem.path("vL1To2");
						case "PPVphBC" -> telem.path("vL2To3");
						case "PPVphCA" -> telem.path("vL3To1");

						case "PIA" -> telem.path("L1Data").path("acCurrent");
						case "PIB" -> telem.path("L2Data").path("acCurrent");
						case "PIC" -> telem.path("L3Data").path("acCurrent");

						case "PVA" -> telem.path("L1Data").path("acVoltage");
						case "PVB" -> telem.path("L2Data").path("acVoltage");
						case "PVC" -> telem.path("L3Data").path("acVoltage");

						case "PHzA" -> telem.path("L1Data").path("acFrequency");
						case "PHzB" -> telem.path("L2Data").path("acFrequency");
						case "PHzC" -> telem.path("L3Data").path("acFrequency");

						case "PWA" -> telem.path("L1Data").path("activePower");
						case "PWB" -> telem.path("L2Data").path("activePower");
						case "PWC" -> telem.path("L3Data").path("activePower");

						case "PVAA" -> telem.path("L1Data").path("apparentPower");
						case "PVAB" -> telem.path("L2Data").path("apparentPower");
						case "PVAC" -> telem.path("L3Data").path("apparentPower");

						case "PVARA" -> telem.path("L1Data").path("reactivePower");
						case "PVARB" -> telem.path("L2Data").path("reactivePower");
						case "PVARC" -> telem.path("L3Data").path("reactivePower");

						case "PPFA" -> telem.path("L1Data").path("cosPhi");
						case "PPFB" -> telem.path("L2Data").path("cosPhi");
						case "PPFC" -> telem.path("L3Data").path("cosPhi");

						default -> null;
					};
					if ( fieldNode == null || fieldNode.isNull() || fieldNode.isMissingNode() ) {
						continue;
					}

					Object propVal = parseJsonDatumPropertyValue(fieldNode,
							ref.property.getPropertyType());
					propVal = ref.property.applyValueTransforms(propVal);
					if ( propVal != null ) {
						d.getSamples().putSampleValue(ref.property.getPropertyType(),
								ref.property.getPropertyName(), propVal);
					}
				}
			}
			if ( !d.isEmpty() ) {
				result.add(d);
			}
		}
		return result;
	}

	private static Collection<GeneralDatum> parseMeterDatum(JsonNode powerJson, JsonNode energyJson,
			SiteQueryPlan queryPlan, CloudDatumStreamConfiguration datumStream,
			Map<String, String> sourceIdMap, DateTimeFormatter timestampFmt,
			SolarEdgeResolution resolution) {
		/*- EXAMPLE JSON (Power):
		{
		  "powerDetails": {
		    "timeUnit": "QUARTER_OF_AN_HOUR",
		    "unit": "W",
		    "meters": [
		      {
		        "type": "FeedIn",
		        "values": [
		          {
		            "date": "2024-10-22 20:30:00",
		            "value": 8.190719
		          },
		 */
		/*- EXAMPLE JSON (Energy):
		{
		  "meterEnergyDetails": {
		    "timeUnit": "QUARTER_OF_AN_HOUR",
		    "unit": "Wh",
		    "meters": [
		      {
		        "meterSerialNumber": "11111111",
		        "connectedSolaredgeDeviceSN": "1111111-BA",
		        "model": "SE-RGMTR-1D-240C-A",
		        "meterType": "Production",
		        "values": [
		          {
		            "date": "2024-10-22 20:44:49",
		            "value": 3.7514836E7
		          },
		 */
		final Map<String, List<ValueRef>> componentRefs = queryPlan.meterRefs;
		Map<DatumId, GeneralDatum> result = new TreeMap<>();
		for ( JsonNode json : new JsonNode[] { powerJson, energyJson } ) {
			@SuppressWarnings("ReferenceEquality")
			final boolean power = (json == powerJson);
			for ( JsonNode meterNode : json.findValue("meters") ) {
				String meterId = nonEmptyString(
						(power ? meterNode.path("type") : meterNode.path("meterType")).asText());
				if ( meterId == null ) {
					continue;
				}
				String sourceId = resolveSourceId(datumStream, queryPlan, SolarEdgeDeviceType.Meter,
						meterId, sourceIdMap);
				if ( sourceId == null ) {
					continue;
				}
				for ( JsonNode telem : meterNode.path("values") ) {
					String dateVal = nonEmptyString(telem.path("date").asText());
					if ( dateVal == null ) {
						continue;
					}
					// force align date
					Instant ts = resolution.truncateDate(timestampFmt.parse(dateVal, Instant::from));
					DatumId datumId = new DatumId(datumStream.getKind(), datumStream.getObjectId(),
							sourceId, ts);
					GeneralDatum d = result.computeIfAbsent(datumId,
							_ -> new GeneralDatum(datumId, new DatumSamples()));
					for ( String componentId : new String[] { WILDCARD_IDENTIFIER, meterId } ) {
						if ( !componentRefs.containsKey(componentId) ) {
							continue;
						}
						for ( ValueRef ref : componentRefs.get(componentId) ) {
							if ( power && !ref.fieldName.equals("W") ) {
								continue;
							} else if ( !power && !ref.fieldName.equals("TotWh") ) {
								continue;
							}
							JsonNode fieldNode = telem.path("value");
							if ( fieldNode == null || fieldNode.isNull() || fieldNode.isMissingNode() ) {
								continue;
							}

							Object propVal = parseJsonDatumPropertyValue(fieldNode,
									ref.property.getPropertyType());
							propVal = ref.property.applyValueTransforms(propVal);
							if ( propVal != null ) {
								d.getSamples().putSampleValue(ref.property.getPropertyType(),
										ref.property.getPropertyName(), propVal);
							}
						}
					}
				}
			}
		}
		return result.values().stream().filter(d -> !d.isEmpty()).toList();
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private static List<GeneralDatum> parseBatteryDatum(JsonNode json, SiteQueryPlan queryPlan,
			CloudDatumStreamConfiguration datumStream, Map<String, String> sourceIdMap,
			DateTimeFormatter timestampFmt) {
		if ( json == null ) {
			return Collections.emptyList();
		}
		/*- EXAMPLE JSON:
		{
		  "storageData": {
		    "batteryCount": 1,
		    "batteries": [
		      {
		        "nameplate": 9800.0,
		        "serialNumber": "AAAAAAAAAAAAAAAAAAAAA",
		        "modelNumber": "AAAAAAAAAAAAAAAAAAAAA",
		        "telemetryCount": 246,
		        "telemetries": [
		          {
		            "timeStamp": "2024-10-22 20:49:48",
		            "power": 0.0,
		            "batteryState": 6,
		            "lifeTimeEnergyDischarged": 5509872,
		            "lifeTimeEnergyCharged": 7471463,
		            "batteryPercentageState": 87.989944,
		            "fullPackEnergyAvailable": 8751.0,
		            "internalTemp": 22.0,
		            "ACGridCharging": 0.0
		          },
		 */
		final Map<String, List<ValueRef>> componentRefs = queryPlan.batteryRefs;
		List<GeneralDatum> result = new ArrayList<>(8);
		for ( JsonNode battery : json.findValue("batteries") ) {
			String batteryId = nonEmptyString(battery.path("serialNumber").asText());
			if ( batteryId == null ) {
				continue;
			}
			String sourceId = resolveSourceId(datumStream, queryPlan, SolarEdgeDeviceType.Battery,
					batteryId, sourceIdMap);
			if ( sourceId == null ) {
				continue;
			}
			for ( JsonNode telem : battery.path("telemetries") ) {
				String dateVal = nonEmptyString(telem.path("timeStamp").asText());
				if ( dateVal == null ) {
					continue;
				}
				Instant ts = timestampFmt.parse(dateVal, Instant::from);
				GeneralDatum d = new GeneralDatum(
						new DatumId(datumStream.getKind(), datumStream.getObjectId(), sourceId, ts),
						new DatumSamples());
				for ( String componentId : new String[] { WILDCARD_IDENTIFIER, batteryId } ) {
					if ( !componentRefs.containsKey(componentId) ) {
						continue;
					}
					for ( ValueRef ref : componentRefs.get(componentId) ) {
						JsonNode fieldNode = switch (ref.fieldName) {
							case "W" -> telem.path("power");
							case "State" -> telem.path("batteryState");
							case "TotWhExp" -> telem.path("lifeTimeEnergyDischarged");
							case "TotWhImp" -> telem.path("lifeTimeEnergyCharged");
							case "SOC" -> telem.path("batteryPercentageState");
							case "CapWh" -> telem.path("fullPackEnergyAvailable");
							case "Temp" -> telem.path("internalTemp");
							case "GridWhImp" -> telem.path("ACGridCharging");

							default -> null;
						};
						if ( fieldNode == null || fieldNode.isNull() || fieldNode.isMissingNode() ) {
							continue;
						}

						Object propVal = parseJsonDatumPropertyValue(fieldNode,
								ref.property.getPropertyType());
						propVal = ref.property.applyValueTransforms(propVal);
						if ( propVal != null ) {
							d.getSamples().putSampleValue(ref.property.getPropertyType(),
									ref.property.getPropertyName(), propVal);
						}
					}
				}
				if ( !d.isEmpty() ) {
					result.add(d);
				}
			}
		}
		return result;
	}

	private static boolean useIndexBasedSourceIds(CloudDatumStreamConfiguration datumStream,
			Map<String, String> sourceIdMap) {
		if ( sourceIdMap != null ) {
			return false;
		}
		Boolean ibSourceId = datumStream.serviceProperty(INDEX_BASED_SOURCE_ID_SETTING, Boolean.class);
		return (ibSourceId != null && ibSourceId);
	}

	private static String resolveSourceId(CloudDatumStreamConfiguration datumStream,
			SiteQueryPlan sitePlan, SolarEdgeDeviceType deviceType, String componentId,
			Map<String, String> sourceIdMap) {
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

	private SolarEdgeResolution resolveResolution(CloudDatumStreamConfiguration datumStream,
			Map<String, ?> parameters) {
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

	private ZoneId resolveSiteTimeZone(CloudIntegrationConfiguration integration, Long siteId) {
		assert integration != null && siteId != null;
		final var cache = getSiteTimeZoneCache();

		ZoneId result = (cache != null ? cache.get(siteId) : null);
		if ( result != null ) {
			return result;
		}

		/*- EXAMPLE JSON:
		{
		  "details": {
		    "id": 123123,
		    "name": "My Site",
		    "accountId": 1234,
		    "status": "Active",
		    "peakPower": 7.4,
		    "lastUpdateTime": "2024-10-22",
		    "installationDate": "2020-07-16",
		    "ptoDate": null,
		    "notes": "",
		    "type": "Optimizers & Inverters",
		    "location": {
		      "country": "United States",
		      "state": "Connecticut",
		      "city": "Anytown",
		      "address": "123 Main Street",
		      "address2": "",
		      "zip": "06830",
		      "timeZone": "America/New_York",
		      "countryCode": "US",
		      "stateCode": "CT"
		    },
		    "primaryModule": {
		      "manufacturerName": "LG",
		      "modelName": "LG335",
		      "maximumPower": 335.0
		    },
		    "uris": {
		      "SITE_IMAGE": "/site/1715515/siteImage/skyviewventures.PNG",
		      "DATA_PERIOD": "/site/1715515/dataPeriod",
		      "DETAILS": "/site/1715515/details",
		      "OVERVIEW": "/site/1715515/overview"
		    },
		    "publicSettings": {
		      "isPublic": false
		    }
		  }
		}
		 */

		result = restOpsHelper.httpGet("Query for site details", integration, JsonNode.class, _ -> {
			// @formatter:off
					return fromUri(resolveBaseUrl(integration, BASE_URI))
							.path(SITE_DETAILS_URL_TEMPLATE)
							.buildAndExpand(siteId)
							.toUri();
					// @formatter:on
		}, res -> {
			ZoneId zone = ZoneOffset.UTC;
			var json = res.getBody();
			String zoneId = json != null
					? StringUtils.nonEmptyString(json.findValue("timeZone").asText())
					: null;
			if ( zoneId != null ) {
				try {
					zone = ZoneId.of(zoneId);
				} catch ( DateTimeException e ) {
					log.warn("Site [{}] time zone [{}] not usable, will use UTC: {}", siteId, zoneId,
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

	private CloudDataValue[] resolveSiteInventory(CloudIntegrationConfiguration integration,
			Long siteId) {
		assert integration != null && siteId != null;
		final var cache = getSiteInventoryCache();

		CloudDataValue[] result = (cache != null ? cache.get(siteId) : null);
		if ( result != null ) {
			return result;
		}

		List<CloudDataValue> response = siteInventory(integration, Map.of(SITE_ID_FILTER, siteId));
		if ( response != null ) {
			result = response.toArray(CloudDataValue[]::new);
			if ( cache != null ) {
				cache.put(siteId, result);
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
	 * <li>siteId</li>
	 * <li>deviceType</li>
	 * <li>componentId</li>
	 * <li>field</li>
	 * </ol>
	 */
	private static final Pattern VALUE_REF_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)/(.+)");

	private static record ValueRef(Object siteId, SolarEdgeDeviceType deviceType, String componentId,
			String fieldName, CloudDatumStreamPropertyConfiguration property) {

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
		private Set<String> inverterIds;

		/** Flag to indicate if meter data is required. */
		private boolean includeMeters;

		/** Flag to indicate if battery data is required. */
		private boolean includeBatteries;

		private Map<String, List<ValueRef>> inverterRefs = new LinkedHashMap<>(8);

		private Map<String, List<ValueRef>> meterRefs = new LinkedHashMap<>(8);

		private Map<String, List<ValueRef>> batteryRefs = new LinkedHashMap<>(8);

		private CloudDataValue[] inventory;

		private Map<SolarEdgeDeviceType, Map<String, Integer>> componentIndexMap;

		private SiteQueryPlan(Long siteId, ZoneId zone) {
			super();
			this.siteId = requireNonNullArgument(siteId, "siteId");
			this.zone = requireNonNullArgument(zone, "zone");
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

		private Integer componentIndex(SolarEdgeDeviceType deviceType, String componentId) {
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
			CloudDatumStreamConfiguration datumStream, Map<String, String> sourceIdMap,
			List<CloudDatumStreamPropertyConfiguration> propConfigs) {
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

				SolarEdgeDeviceType deviceType;
				try {
					deviceType = SolarEdgeDeviceType.fromValue(deviceTypeKey);
				} catch ( IllegalArgumentException e ) {
					// ignore and continue
					continue;
				}

				SiteQueryPlan plan = result.computeIfAbsent(siteId, id -> {
					ZoneId zone = resolveSiteTimeZone(integration, id);
					return new SiteQueryPlan(siteId, zone);
				});

				ValueRef valueRef = new ValueRef(siteId, deviceType, componentId, fieldName, config);
				Map<String, List<ValueRef>> valueRefMap = null;

				if ( deviceType == SolarEdgeDeviceType.Battery ) {
					plan.includeBatteries = true;
					if ( plan.batteryRefs == null ) {
						plan.batteryRefs = new LinkedHashMap<>(8);
					}
					valueRefMap = plan.batteryRefs;
				} else if ( deviceType == SolarEdgeDeviceType.Meter ) {
					plan.includeMeters = true;
					if ( plan.meterRefs == null ) {
						plan.meterRefs = new LinkedHashMap<>(8);
					}
					valueRefMap = plan.meterRefs;
				} else if ( deviceType == SolarEdgeDeviceType.Inverter ) {
					if ( plan.inverterIds == null ) {
						plan.inverterIds = new LinkedHashSet<>(8);
					}
					plan.inverterIds.add(componentId);
					if ( plan.inverterRefs == null ) {
						plan.inverterRefs = new LinkedHashMap<>(8);
					}
					valueRefMap = plan.inverterRefs;
				}
				if ( valueRefMap != null ) {
					valueRefMap.computeIfAbsent(valueRef.componentId, _ -> new ArrayList<>(8))
							.add(valueRef);

				}
			}
		}

		// resolve wildcard inverter component IDs
		for ( SiteQueryPlan plan : result.values() ) {
			if ( useIndexBasedSourceIds ) {
				plan.inventory = resolveSiteInventory(integration, plan.siteId);
			}
			if ( plan.inverterIds == null || !plan.inverterIds.contains(WILDCARD_IDENTIFIER) ) {
				continue;
			}

			Set<String> resolvedInverterIds = new LinkedHashSet<>(8);
			CloudDataValue[] inventory = plan.inventory;
			if ( inventory == null ) {
				inventory = resolveSiteInventory(integration, plan.siteId);
			}
			CloudDataValue inverters = Arrays.stream(inventory)
					.filter(e -> Inverter.getGroupKey().equals(e.getName())).findAny().orElse(null);
			if ( inverters != null && inverters.getChildren() != null ) {
				for ( CloudDataValue inverter : inverters.getChildren() ) {
					resolvedInverterIds.add(inverter.getIdentifiers().getLast());
				}
			}

			plan.inverterIds.remove(WILDCARD_IDENTIFIER);
			plan.inverterIds.addAll(resolvedInverterIds);
			if ( plan.inverterIds.isEmpty() ) {
				plan.inverterIds = null;
			}
		}

		return result;
	}

	/**
	 * Get the site time zone cache.
	 *
	 * @return the cache
	 */
	public final Cache<Long, ZoneId> getSiteTimeZoneCache() {
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
	public final void setSiteTimeZoneCache(Cache<Long, ZoneId> siteTimeZoneCache) {
		this.siteTimeZoneCache = siteTimeZoneCache;
	}

	/**
	 * Get the site inventory cache.
	 *
	 * @return the cache
	 */
	public final Cache<Long, CloudDataValue[]> getSiteInventoryCache() {
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
	public final void setSiteInventoryCache(Cache<Long, CloudDataValue[]> siteInventoryCache) {
		this.siteInventoryCache = siteInventoryCache;
	}

}
