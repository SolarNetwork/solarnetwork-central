/* ==================================================================
 * SigenergyCloudDatumStreamService.java - 7/12/2025 6:10:05 am
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

package net.solarnetwork.central.c2c.biz.sigen;

import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.BASE_URI_TEMPLATE;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.RESPONSE_DATA_FIELD;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.jsonObjectOrArray;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.requireSuccessResponse;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_FIRMWARE_VERSION_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.LOCALITY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STREET_ADDRESS_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.TIME_ZONE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.cache.Cache;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.util.UriComponentsBuilder;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseRestOperationsCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.CloudIntegrationsUtils;
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
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Sigenergy implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.0
 */
public class SigenergyCloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.sigen";

	/** The data value filter key for a system ID. */
	public static final String SYSTEM_ID_FILTER = "systemId";

	/** The data value filter key for a filter ID. */
	public static final String DEVICE_ID_FILTER = "deviceId";

	/** The metadata key for a Sigenergy device type. */
	public static final String DEVICE_TYPE_METADATA = "deviceType";

	/** The data value identifier levels source ID range. */
	public static final IntRange DATA_VALUE_IDENTIFIER_LEVELS_SOURCE_ID_RANGE = IntRange.rangeOf(0, 1);

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// @formatter:off
		SETTINGS = List.of(
				UPPER_CASE_SOURCE_ID_SETTING_SPECIFIER,
				SOURCE_ID_MAP_SETTING_SPECIFIER,
				VIRTUAL_SOURCE_IDS_SETTING_SPECIFIER,
				MULTI_STREAM_MAXIMUM_LAG_SETTING_SPECIFIER
				);
		// @formatter:on
	}

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SYSTEM_ID_FILTER,
			DEVICE_ID_FILTER);

	/**
	 * The device ID data value filter value that represents system-level data.
	 *
	 * <p>
	 * Note the {@code $} prefix is used to sort this before "normal" device
	 * serial number values.
	 * </p>
	 */
	public static final String SYSTEM_DEVICE_ID = "$SYS";

	/** The device data value name for system-level data. */
	public static final String SYSTEM_DEVICE_NAME = "System";

	/** The data resolution duration. */
	public static final Duration DATA_RESOLUTION = Duration.ofMinutes(5L);

	/** The maximum length of time to query for data. */
	public static final Duration MAX_QUERY_TIME_RANGE = Duration.ofDays(5);

	private final ObjectMapper mapper;
	private final SigenergyFields fields;
	private Cache<String, CloudDataValue[]> systemDeviceCache;

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
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
	 * @param restOpsHelper
	 *        the REST operations helper
	 * @param mapper
	 *        the object mapper
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SigenergyCloudDatumStreamService(Clock clock, UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
			RestOperationsHelper restOpsHelper, ObjectMapper mapper) {
		super(SERVICE_IDENTIFIER, "Sigenergy Datum Stream Service", clock, userEventAppenderBiz,
				encryptor, expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS, restOpsHelper);
		this.mapper = requireNonNullArgument(mapper, "mapper");
		// @formatter:off
		this.fields = new SigenergyFields(Map.of(
				SigenergyFields.AIO_FIELD_SET_NAME, new ClassPathResource("sigenergy-aio-fields.csv", getClass()),
				SigenergyFields.GATEWAY_FIELD_SET_NAME, new ClassPathResource("sigenergy-gateway-fields.csv", getClass()),
				SigenergyFields.METER_FIELD_SET_NAME, new ClassPathResource("sigenergy-meter-fields.csv", getClass()),
				SigenergyFields.SYS_SUMMERY_FIELD_SET_NAME, new ClassPathResource("sigenergy-sys_summary-fields.csv", getClass()),
				SigenergyFields.SYS_ENERGY_FLOW_FIELD_SET_NAME, new ClassPathResource("sigenergy-sys_energyflow-fields.csv", getClass())
				));
		// @formatter:on
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
			result = deviceFields(integration, systemId, deviceId);
		} else if ( filters != null && filters.get(SYSTEM_ID_FILTER) != null ) {
			String systemId = filters.get(SYSTEM_ID_FILTER).toString();
			result = systemDevices(integration, systemId);
		} else {
			// list available systems
			result = systems(integration);
		}
		Collections.sort(result);
		return result;
	}

	private List<CloudDataValue> systems(CloudIntegrationConfiguration integration) {
		final SigenergyRegion region = SigenergyRestOperationsHelper.resolveRegion(integration);
		return restOpsHelper.httpGet("List systems", integration, JsonNode.class,
				(_) -> UriComponentsBuilder.fromUriString(resolveBaseUrl(integration, BASE_URI_TEMPLATE))
						.path(SigenergyRestOperationsHelper.SYSTEM_LIST_PATH)
						.buildAndExpand(region.getKey()).toUri(),
				(res) -> parseSystems(res.getBody()));
	}

	private List<CloudDataValue> parseSystems(JsonNode json) {
		/*- EXAMPLE JSON:
		{
		  "code": 0,
		  "msg": "success",
		  "timestamp": 1764870200,
		  "data": "[{\"systemId\":\"ABC123\",\"systemName\":\"Test House\",\"addr\":\"123 Main Street, Anytown\"
		  	,\"status\":\"Normal\",\"isActivate\":true,\"onOffGridStatus\":\"onGrid\",\"timeZone\":\"Pacific/Auckland\"
		  	,\"gridConnectedTime\":1746419521000,\"pvCapacity\":9.2,\"batteryCapacity\":9.2}]"
		}
		*/
		if ( json == null ) {
			return List.of();
		}
		final JsonNode data;
		try {
			data = jsonObjectOrArray(mapper, json, RESPONSE_DATA_FIELD);
		} catch ( IllegalArgumentException e ) {
			return List.of();
		}
		final var result = new ArrayList<CloudDataValue>(4);
		for ( JsonNode sysNode : data ) {
			CloudDataValue sys = parseSystem(sysNode);
			if ( sys != null ) {
				result.add(sys);
			}
		}
		return result;
	}

	private static CloudDataValue parseSystem(JsonNode sysNode) {
		if ( sysNode == null ) {
			return null;
		}
		final String id = sysNode.path("systemId").asString().trim();
		final String name = nonEmptyString(sysNode.path("systemName").asString().trim());
		final String addr = nonEmptyString(sysNode.path("addr").asString().trim());
		final var meta = new LinkedHashMap<String, Object>(4);
		final JsonNode addrNode = sysNode.path("address");
		if ( addrNode != null ) {
			List<String> addrComponents = StringUtils.commaDelimitedStringToList(addr);
			if ( addrComponents != null ) {
				if ( addrComponents.size() > 0 ) {
					meta.put(STREET_ADDRESS_METADATA, addrComponents.get(0).trim());
				}
				if ( addrComponents.size() > 1 ) {
					meta.put(LOCALITY_METADATA, addrComponents.get(1).trim());
				}
			}
		}
		populateNonEmptyValue(sysNode, "timeZone", TIME_ZONE_METADATA, meta);
		populateIsoTimestampValue(sysNode, "gridConnectedTime", "gridConnectedTime", meta);
		populateIsoTimestampValue(sysNode, "status", "status", meta);
		populateNumberValue(sysNode, "onOffGridStatus", "onOffGridStatus", meta);
		populateNumberValue(sysNode, "pvCapacity", "pvCapacity", meta);
		populateNumberValue(sysNode, "batteryCapacity", "batteryCapacity", meta);
		return intermediateDataValue(List.of(id), name, meta.isEmpty() ? null : meta);
	}

	private List<CloudDataValue> systemDevices(final CloudIntegrationConfiguration integration,
			final String systemId) {
		final Cache<String, CloudDataValue[]> cache = getSystemDeviceCache();

		final CloudDataValue[] cachedResult = (cache != null ? cache.get(systemId) : null);
		if ( cachedResult != null ) {
			return Arrays.asList(cachedResult);
		}

		final SigenergyRegion region = SigenergyRestOperationsHelper.resolveRegion(integration);
		List<CloudDataValue> result = restOpsHelper.httpGet("List system devices", integration,
				JsonNode.class,
				(_) -> UriComponentsBuilder.fromUriString(resolveBaseUrl(integration, BASE_URI_TEMPLATE))
						.path(SigenergyRestOperationsHelper.SYSTEM_DEVICE_LIST_PATH)
						.buildAndExpand(region.getKey(), systemId).toUri(),
				(res) -> parseSystemDevices(res.getBody(), systemId));
		if ( result != null && !result.isEmpty() && cache != null ) {
			cache.put(systemId, result.toArray(CloudDataValue[]::new));
		}
		return result;
	}

	private List<CloudDataValue> parseSystemDevices(final JsonNode json, final String systemId) {
		/*- EXAMPLE JSON:
		{
		  "code": 0,
		  "msg": "success",
		  "timestamp": 1764869251,
		  "data": [
		    "{\"systemId\":\"ABC123\",\"serialNumber\":\"110A123\",\"deviceType\":\"Inverter\",\"status\":\"Normal\"
		    ,\"pn\":\"1104004100\",\"firmwareVersion\":\"V100R001C22SPC111B064L\"
		    ,\"attrMap\":\"{\\\"ratedFrequency\\\":50.000,\\\"ratedVoltage\\\":230.000,\\\"maxAbsorbedPower\\\":11.000
		    ,\\\"ratedActivePower\\\":10.000,\\\"pvStringNumber\\\":4,\\\"maxActivePower\\\":11.000}\"}"
		  ]
		}
		*/
		if ( json == null ) {
			return List.of();
		}
		final JsonNode data;
		try {
			data = jsonObjectOrArray(mapper, json, RESPONSE_DATA_FIELD);
		} catch ( IllegalArgumentException e ) {
			return List.of();
		}

		final List<CloudDataValue> result = new ArrayList<>(8);

		// include sys "device"
		result.add(intermediateDataValue(List.of(systemId, SYSTEM_DEVICE_ID), "System", null));

		for ( JsonNode devNodeJson : data ) {
			final JsonNode devNode;
			try {
				devNode = jsonObjectOrArray(mapper, devNodeJson);
			} catch ( IllegalArgumentException e ) {
				continue;
			}
			final var meta = new LinkedHashMap<String, Object>(4);
			populateNonEmptyValue(devNode, "serialNumber", DEVICE_SERIAL_NUMBER_METADATA, meta);
			populateNonEmptyValue(devNode, "deviceType", DEVICE_TYPE_METADATA, meta);
			populateNonEmptyValue(devNode, "firmwareVersion", DEVICE_FIRMWARE_VERSION_METADATA, meta);
			populateNonEmptyValue(devNode, "status", "status", meta);
			populateNonEmptyValue(devNode, "pn", "pn", meta);

			try {
				final JsonNode attrMapNode = jsonObjectOrArray(mapper, devNode, "attrMap");
				for ( String attr : attrMapNode.propertyNames() ) {
					if ( !meta.containsKey(attr) ) {
						JsonNode attrVal = attrMapNode.get(attr);
						if ( attrVal.isNumber() ) {
							populateNumberValue(attrMapNode, attr, attr, meta);
						} else if ( attrVal.isBigDecimal() ) {
							populateBooleanValue(attrMapNode, attr, attr, meta);
						} else {
							populateNonEmptyValue(attrMapNode, attr, attr, meta);
						}
					}
				}
			} catch ( IllegalArgumentException e ) {
				continue;
			}

			final Object id = meta.get(DEVICE_SERIAL_NUMBER_METADATA);
			if ( id == null ) {
				continue;
			}

			result.add(intermediateDataValue(List.of(systemId, id.toString()), id.toString(),
					meta.isEmpty() ? null : meta));
		}

		return result;
	}

	private List<CloudDataValue> deviceFields(CloudIntegrationConfiguration integration, String systemId,
			String deviceId) {
		if ( SYSTEM_DEVICE_ID.equals(deviceId) ) {
			return systemDeviceFields(integration, systemId);
		}

		// look up the device type to know what fields are supported
		final List<CloudDataValue> deviceInfos = systemDevices(integration, systemId);
		final CloudDataValue deviceInfo = (deviceInfos != null
				? deviceInfos.stream().filter(e -> deviceId.equals(e.getIdentifiers().getLast()))
						.findAny().orElse(null)
				: null);

		SigenergyDeviceType deviceType = null;
		if ( deviceInfo != null && deviceInfo.getMetadata() != null
				&& deviceInfo.getMetadata().containsKey(DEVICE_TYPE_METADATA) ) {
			try {
				deviceType = SigenergyDeviceType
						.valueOf(deviceInfo.getMetadata().get(DEVICE_TYPE_METADATA).toString());
			} catch ( Exception e ) {
				// continue
			}
		}
		return switch (deviceType) {
			case AcCharger -> deviceFields_Aio(systemId, deviceId);
			case Battery -> deviceFields_Aio(systemId, deviceId);
			case DcCharger -> deviceFields_Aio(systemId, deviceId);
			case Gateway -> deviceFields_Gateway(systemId, deviceId);
			case Inverter -> deviceFields_Aio(systemId, deviceId);
			case Meter -> deviceFields_Meter(systemId, deviceId);
			case null -> List.of();
		};
	}

	public static final String FIELD_NAME_METADATA = "field";

	private List<CloudDataValue> deviceFields_Aio(String systemId, String deviceId) {
		return fields.cloudDataValues(SigenergyFields.AIO_FIELD_SET_NAME, systemId, deviceId);
	}

	private List<CloudDataValue> deviceFields_Gateway(String systemId, String deviceId) {
		return fields.cloudDataValues(SigenergyFields.GATEWAY_FIELD_SET_NAME, systemId, deviceId);
	}

	private List<CloudDataValue> deviceFields_Meter(String systemId, String deviceId) {
		return fields.cloudDataValues(SigenergyFields.METER_FIELD_SET_NAME, systemId, deviceId);
	}

	/*- example JSON from /summary
	 {
	 	"lifetimeCo2":2.28,
	 	"lifetimeCoal":1.92,
	 	"lifetimeTreeEquivalent":3.11,
	 	"dailyPowerGeneration":0.07,
	 	"monthlyPowerGeneration":115.53,
	 	"annualPowerGeneration":4791.59,
	 	"lifetimePowerGeneration":4791.6
	 }
	
	    example JSON from /energyFlow
	 {
	 	"pvPower":0.3,
	 	"gridPower":3.71,
	 	"evPower":0.0,
	 	"loadPower":0.89,
	 	"heatPumpPower":0.0,
	 	"batteryPower":-4.3,
	 	"batterySoc":88.3
	 }
	 */

	private List<CloudDataValue> systemDeviceFields(CloudIntegrationConfiguration integration,
			String systemId) {
		return fields.mergedCloudDataValues(systemId, SYSTEM_DEVICE_ID,
				SigenergyFields.SYS_SUMMERY_FIELD_SET_NAME,
				SigenergyFields.SYS_ENERGY_FLOW_FIELD_SET_NAME);
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
	 * <li>field</li>
	 * </ol>
	 */
	private static final Pattern VALUE_REF_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/(.+)");

	private static record ValueRef(String systemId, String deviceId, String fieldName,
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
	 * which Sigenergy APIs are necessary to satisfy those configurations.
	 * </p>
	 */
	private static class SystemQueryPlan {

		/** The system ID. */
		private final String systemId;

		private final List<ValueRef> systemSummaryDeviceRefs = new ArrayList<>(8);

		private final List<ValueRef> systemEnergyFlowDeviceRefs = new ArrayList<>(8);

		private final Map<String, List<ValueRef>> deviceRefs = new LinkedHashMap<>(8);

		private SystemQueryPlan(String systemId) {
			super();
			this.systemId = requireNonNullArgument(systemId, "systemId");
		}

	}

	private Map<String, SystemQueryPlan> resolveSystemQueryPlans(
			CloudIntegrationConfiguration integration, CloudDatumStreamConfiguration datumStream,
			Map<String, String> sourceIdMap, List<CloudDatumStreamPropertyConfiguration> propConfigs) {
		final var result = new LinkedHashMap<String, SystemQueryPlan>(2);

		@SuppressWarnings("unchecked")
		final List<Map<String, ?>> placeholderSets = resolvePlaceholderSets(
				datumStream.serviceProperty(PLACEHOLDERS_SERVICE_PROPERTY, Map.class),
				(sourceIdMap != null ? sourceIdMap.keySet() : null));

		for ( CloudDatumStreamPropertyConfiguration config : propConfigs ) {
			for ( Map<String, ?> ph : placeholderSets ) {
				final String ref = StringUtils.expandTemplateString(config.getValueReference(), ph);
				final Matcher m = VALUE_REF_PATTERN.matcher(ref);
				if ( !m.matches() ) {
					continue;
				}
				// groups: 1 = systemId, 2 = deviceId, 3 = field
				final String systemId = m.group(1);
				final String deviceId = m.group(2);
				final String fieldName = m.group(3);

				final SystemQueryPlan plan = result.computeIfAbsent(systemId, _ -> {
					return new SystemQueryPlan(systemId);
				});

				final ValueRef valueRef = new ValueRef(systemId, deviceId, fieldName, config);

				// sort into API-specific groups
				if ( SYSTEM_DEVICE_ID.equals(deviceId) ) {
					if ( fields.fieldIsMemberOfSet(SigenergyFields.SYS_ENERGY_FLOW_FIELD_SET_NAME,
							fieldName) ) {
						plan.systemEnergyFlowDeviceRefs.add(valueRef);
					} else {
						// assume summary field
						plan.systemSummaryDeviceRefs.add(valueRef);
					}
				} else {
					plan.deviceRefs.computeIfAbsent(deviceId, _ -> new ArrayList<>(8)).add(valueRef);
				}
			}
		}

		return result;
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

			final SigenergyRegion region = SigenergyRestOperationsHelper.resolveRegion(integration);

			// mapping of /systemId/deviceId -> sourceId
			final Map<String, String> sourceIdMap = servicePropertyStringMap(ds, SOURCE_ID_MAP_SETTING);

			final List<GeneralDatum> resultDatum = new ArrayList<>(16);
			final Map<String, SystemQueryPlan> queryPlans = resolveSystemQueryPlans(integration, ds,
					sourceIdMap, valueProps);

			for ( SystemQueryPlan queryPlan : queryPlans.values() ) {
				Map<String, GeneralDatum> systemSourceIdMapping = new LinkedHashMap<>(4);
				// system summary data
				if ( !queryPlan.systemSummaryDeviceRefs.isEmpty() ) {
					final URI viewSysSummaryUri = UriComponentsBuilder
							.fromUriString(resolveBaseUrl(integration, BASE_URI_TEMPLATE))
							.path(SigenergyRestOperationsHelper.SYSTEM_SUMMARY_VIEW_PATH)
							.buildAndExpand(region.getKey()).toUri();
					restOpsHelper.httpGet("View system summary", integration, JsonNode.class,
							(_) -> viewSysSummaryUri, (res) -> parseSystemSummaryDatum(viewSysSummaryUri,
									res.getBody(), queryPlan, ds, sourceIdMap, systemSourceIdMapping));
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

			return finalResult;
		});
	}

	private Void parseSystemSummaryDatum(URI uri, JsonNode body, SystemQueryPlan queryPlan,
			CloudDatumStreamConfiguration datumStream, Map<String, String> sourceIdMap,
			Map<String, GeneralDatum> systemSourceIdMapping) {
		/*- Example JSON:
			{
			    "code": 0,
			    "msg": "success",
			    "timestamp": 1757581478,
			    "data": {
			        "dailyPowerGeneration": 0.0,
			        "monthlyPowerGeneration": 0.0,
			        "annualPowerGeneration": 1394.37,
			        "lifetimePowerGeneration": 1394.38,
			        "lifetimeCo2": 0.66,
			        "lifetimeCoal": 0.56,
			        "lifetimeTreeEquivalent": 0.9
			    }
			}
		 */
		requireSuccessResponse("View system summary", uri, body);

		final JsonNode data;
		try {
			data = jsonObjectOrArray(mapper, body, RESPONSE_DATA_FIELD);
		} catch ( IllegalArgumentException e ) {
			return null;
		}

		final String sourceId = resolveSourceId(datumStream, queryPlan, SYSTEM_DEVICE_ID, sourceIdMap);
		if ( sourceId == null ) {
			return null;
		}
		final List<ValueRef> summaryRefs = queryPlan.systemSummaryDeviceRefs;
		// TODO
		return null;
	}

	private static String resolveSourceId(CloudDatumStreamConfiguration datumStream,
			SystemQueryPlan sitePlan, String deviceId, Map<String, String> sourceIdMap) {
		if ( sourceIdMap != null ) {
			String key = "/%s/%s".formatted(sitePlan.systemId, deviceId);
			return sourceIdMap.get(key);
		}

		Boolean ucSourceId = datumStream.serviceProperty(UPPER_CASE_SOURCE_ID_SETTING, Boolean.class);

		String result = "/%s/%s".formatted(datumStream.getSourceId(), deviceId);
		return (ucSourceId != null && ucSourceId ? result.toUpperCase(Locale.ENGLISH) : result);
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

			final Map<String, String> sourceIdMap = servicePropertyStringMap(ds, SOURCE_ID_MAP_SETTING);

			final List<GeneralDatum> resultDatum = new ArrayList<>(16);
			final Map<String, SystemQueryPlan> queryPlans = resolveSystemQueryPlans(integration, ds,
					sourceIdMap, valueProps);

			BasicQueryFilter nextQueryFilter = null;

			Instant startDate = CloudIntegrationsUtils.truncateDate(filterStartDate, DATA_RESOLUTION,
					UTC);
			Instant endDate = CloudIntegrationsUtils.truncateDate(filterEndDate, DATA_RESOLUTION, UTC);
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
			for ( SystemQueryPlan queryPlan : queryPlans.values() ) {
				// TODO

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
					nextQueryFilter.setStartDate(CloudIntegrationsUtils
							.truncateDate(leastGreatestTimestampPerStream, DATA_RESOLUTION, UTC));
				}
			}

			return new BasicCloudDatumStreamQueryResult(usedQueryFilter, nextQueryFilter, finalResult);
		});
	}

	/**
	 * Get the system device cache.
	 *
	 * @return the cache
	 */
	public Cache<String, CloudDataValue[]> getSystemDeviceCache() {
		return systemDeviceCache;
	}

	/**
	 * Set the system device cache.
	 *
	 * @param systemDeviceCache
	 *        the cache to set
	 */
	public void setSystemDeviceCache(Cache<String, CloudDataValue[]> systemDeviceCache) {
		this.systemDeviceCache = systemDeviceCache;
	}

}
