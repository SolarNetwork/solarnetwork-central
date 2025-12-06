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
import static net.solarnetwork.central.c2c.domain.CloudDataValue.LOCALITY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STREET_ADDRESS_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.TIME_ZONE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseRestOperationsCloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
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
 * Sigenergy implementation of {@Link CloudDatumStreamService}.
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
	 */
	public static final String SYSTEM_DEVICE_ID = "sys";

	/**
	 * The maximum period of time to request data for in one call to
	 * {@link #datum(CloudDatumStreamConfiguration, CloudDatumStreamQueryFilter)}.
	 */
	private static final Duration MAX_FILTER_TIME_RANGE = Duration.ofDays(7);

	/** The maximum period of time to request data for in one API request. */
	private static final Duration MAX_QUERY_TIME_RANGE = Duration.ofHours(24);

	private final ObjectMapper mapper;

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
			/*- TODO
			String systemId = filters.get(SYSTEM_ID_FILTER).toString();
			String deviceId = filters.get(DEVICE_ID_FILTER).toString();
			result = deviceChannels(integration, systemId, deviceId, filters);
			*/
		} else if ( filters != null && filters.get(SYSTEM_ID_FILTER) != null ) {
			/*- TODO
			String systemId = filters.get(SYSTEM_ID_FILTER).toString();
			result = systemDevices(integration, systemId, filters);
			*/
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
				res -> parseSystems(res.getBody()));
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
					meta.put(STREET_ADDRESS_METADATA, addrComponents[0]);
				}
				if ( addrComponents.length > 1 ) {
					meta.put(LOCALITY_METADATA, addrComponents[1]);
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

	/*- TODO EXAMPLE JSON (device list)
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

}
