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

import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.BASE_URI_TEMPLATE;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.RESPONSE_DATA_FIELD;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.jsonObjectOrArray;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_FIRMWARE_VERSION_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.LOCALITY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STREET_ADDRESS_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.TIME_ZONE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.cache.Cache;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseRestOperationsCloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
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
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.IntRange;

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
				(req) -> UriComponentsBuilder
						.fromUriString(resolveBaseUrl(integration, BASE_URI_TEMPLATE))
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
		final String id = sysNode.path("systemId").asText().trim();
		final String name = nonEmptyString(sysNode.path("systemName").asText().trim());
		final String addr = nonEmptyString(sysNode.path("addr").asText().trim());
		final var meta = new LinkedHashMap<String, Object>(4);
		final JsonNode addrNode = sysNode.path("address");
		if ( addrNode != null ) {
			String[] addrComponents = StringUtils.commaDelimitedListToStringArray(addr);
			if ( addrComponents != null ) {
				if ( addrComponents.length > 0 ) {
					meta.put(STREET_ADDRESS_METADATA, addrComponents[0].trim());
				}
				if ( addrComponents.length > 1 ) {
					meta.put(LOCALITY_METADATA, addrComponents[1].trim());
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
				(req) -> UriComponentsBuilder
						.fromUriString(resolveBaseUrl(integration, BASE_URI_TEMPLATE))
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
				for ( Iterator<String> itr = attrMapNode.fieldNames(); itr.hasNext(); ) {
					String attr = itr.next();
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

		private boolean isSystemRef() {
			return SYSTEM_DEVICE_ID.equals(deviceId);
		}

	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter) {
		// TODO Auto-generated method stub
		return null;
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
