/* ==================================================================
 * AlsoEnergyCloudDatumStreamService.java - 22/11/2024 9:04:46 am
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
import static net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.resolvePlaceholders;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.ObjectUtils;

/**
 * AlsoEnergy implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.0
 */
public class AlsoEnergyCloudDatumStreamService extends BaseOAuth2ClientCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.also";

	/** The data value filter key for a site ID. */
	public static final String SITE_ID_FILTER = "siteId";

	/** The data value filter key for a hardware ID. */
	public static final String HARDWARE_ID_FILTER = "hardwareId";

	/** The setting for granularity. */
	public static final String GRANULARITY_SETTING = "granularity";

	/** The setting for time zone identifier. */
	public static final String TIME_ZONE_SETTING = "tz";

	/**
	 * The URI path to list the hardware for a given site.
	 *
	 * <p>
	 * Accepts a single {@code {siteId}} parameter.
	 * </p>
	 */
	public static final String SITE_HARDWARE_URL_TEMPLATE = "/sites/{siteId}/hardware";

	/** The URI path to query for data. */
	public static final String BIN_DATA_URL = "/v2/data/bindata";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// menu for granularity
		var granularitySpec = new BasicMultiValueSettingSpecifier(GRANULARITY_SETTING,
				AlsoEnergyGranularity.Raw.name());
		var granularityTitles = unmodifiableMap(Arrays.stream(AlsoEnergyGranularity.values())
				.collect(Collectors.toMap(AlsoEnergyGranularity::name, AlsoEnergyGranularity::name,
						(l, r) -> r,
						() -> new LinkedHashMap<>(LocusEnergyGranularity.values().length))));
		granularitySpec.setValueTitles(granularityTitles);

		var tzSpec = new BasicTextFieldSettingSpecifier(TIME_ZONE_SETTING, null);
		SETTINGS = List.of(granularitySpec, tzSpec);
	}

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SITE_ID_FILTER,
			HARDWARE_ID_FILTER);

	/** The maximum period of time to request data for in one request. */
	private static final Duration MAX_QUERY_TIME_RANGE = Duration.ofDays(7);

	private final InstantSource clock;

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
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public AlsoEnergyCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager, InstantSource clock) {
		super(SERVICE_IDENTIFIER, "AlsoEnergy Datum Stream Service", userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new OAuth2RestOperationsHelper(
						LoggerFactory.getLogger(AlsoEnergyCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> AlsoEnergyCloudIntegrationService.SECURE_SETTINGS,
						oauthClientManager),
				oauthClientManager);
		this.clock = ObjectUtils.requireNonNullArgument(clock, "clock");
	}

	@Override
	protected Iterable<String> supportedPlaceholders() {
		return SUPPORTED_PLACEHOLDERS;
	}

	@Override
	public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
		MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");
		List<LocalizedServiceInfo> result = new ArrayList<>(2);
		for ( String key : new String[] { SITE_ID_FILTER } ) {
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
		if ( filters != null && filters.get(SITE_ID_FILTER) != null ) {
			result = siteHardware(integration, filters);
		} else {
			// list available sites
			result = sites(integration);
		}
		Collections.sort(result);
		return result;
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");
		final ZoneId zone = resolveTimeZone(datumStream, null);
		final AlsoEnergyGranularity granularity = resolveGranularity(datumStream, null);

		final Instant endDate;
		final Instant startDate;
		if ( granularity == AlsoEnergyGranularity.Raw ) {
			endDate = clock.instant().truncatedTo(ChronoUnit.SECONDS);
			startDate = endDate.minus(Duration.ofMinutes(10));
		} else {
			endDate = granularity.tickStart(clock.instant().truncatedTo(ChronoUnit.SECONDS), zone);
			startDate = granularity.prevTickStart(endDate, zone);
		}

		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);

		final var result = datum(datumStream, filter);
		if ( result == null || result.isEmpty() ) {
			return null;
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

			final ZoneId zone = resolveTimeZone(datumStream, null);

			final AlsoEnergyGranularity resolution = resolveGranularity(ds, null);

			final Map<String, String> sourceIdMap = servicePropertyStringMap(ds, SOURCE_ID_MAP_SETTING);

			// construct (siteId, hardwareId) to ValueRef[] mapping
			final Map<UserLongCompositePK, List<ValueRef>> hardwareGroups = resolveHardwareGroups(
					integration, ds, valueProps);

			BasicQueryFilter nextQueryFilter = null;

			Instant startDate = resolution.tickStart(filterStartDate, zone);
			Instant endDate = resolution.tickStart(filterEndDate, zone);
			if ( Duration.between(startDate, endDate).compareTo(MAX_QUERY_TIME_RANGE) > 0 ) {
				Instant nextEndDate = resolution
						.tickStart(startDate.plus(MAX_QUERY_TIME_RANGE.multipliedBy(2)), zone);
				if ( nextEndDate.isAfter(endDate) ) {
					nextEndDate = endDate;
				}

				endDate = resolution.tickStart(startDate.plus(MAX_QUERY_TIME_RANGE), zone);

				nextQueryFilter = new BasicQueryFilter();
				nextQueryFilter.setStartDate(endDate);
				nextQueryFilter.setEndDate(nextEndDate);
			}

			final BasicQueryFilter usedQueryFilter = new BasicQueryFilter();
			usedQueryFilter.setStartDate(startDate);
			usedQueryFilter.setEndDate(endDate);

			final Map<DatumId, DatumSamples> dataMap = new TreeMap<>();

			for ( Entry<UserLongCompositePK, List<ValueRef>> e : hardwareGroups.entrySet() ) {
				final ZonedDateTime siteStartDate = startDate.atZone(zone);
				final ZonedDateTime siteEndDate = endDate.atZone(zone);

				final var reqBody = new ArrayList<Map<String, Object>>(e.getValue().size());
				for ( ValueRef ref : e.getValue() ) {
					var reqField = new LinkedHashMap<String, Object>(4);
					reqField.put("siteId", ref.siteId);
					reqField.put("hardwareId", ref.hardwareId);
					reqField.put("function", ref.fn.name());
					reqField.put("fieldName", ref.fieldName);
					reqBody.add(reqField);
				}

				String startDateParam = ISO_DATE_OPT_TIME_OPT_MILLIS_UTC
						.format(siteStartDate.toLocalDateTime());
				String endDateParam = ISO_DATE_OPT_TIME_OPT_MILLIS_UTC
						.format(siteEndDate.toLocalDateTime());

				restOpsHelper.http("List data for hardware", HttpMethod.POST, reqBody, integration,
						JsonNode.class, req -> {
							req.setContentType(MediaType.APPLICATION_JSON);
						// @formatter:off
							return fromUri(resolveBaseUrl(integration, BASE_URI)).path(BIN_DATA_URL)
									.queryParam("from", startDateParam)
									.queryParam("to", endDateParam)
									.queryParam("binSizes", resolution.getQueryKey())
									.queryParam("tz", zone.getId())
									.buildAndExpand().toUri();
							// @formatter:on
						}, res -> parseDatum(res.getBody(), e.getValue(), ds, sourceIdMap, dataMap));
			}

			List<GeneralDatum> resultDatum = dataMap.entrySet().stream()
					.filter(e -> !e.getValue().isEmpty())
					.map(e -> new GeneralDatum(e.getKey(), e.getValue())).toList();

			// evaluate expressions on merged datum
			evaluateExpressions(exprProps, resultDatum, mapping.getConfigId(),
					integration.getConfigId());

			return new BasicCloudDatumStreamQueryResult(usedQueryFilter, nextQueryFilter,
					resultDatum.stream().map(Datum.class::cast).toList());
		});
	}

	private List<CloudDataValue> sites(CloudIntegrationConfiguration integration) {
		return restOpsHelper.httpGet("List sites", integration, JsonNode.class,
				(req) -> fromUri(resolveBaseUrl(integration, AlsoEnergyCloudIntegrationService.BASE_URI))
						.path(AlsoEnergyCloudIntegrationService.LIST_SITES_URL)
						.buildAndExpand(integration.getServiceProperties()).toUri(),
				res -> parseSites(res.getBody()));
	}

	private List<CloudDataValue> siteHardware(CloudIntegrationConfiguration integration,
			Map<String, ?> filters) {
		return restOpsHelper.httpGet("List sites", integration, JsonNode.class,
		// @formatter:off
				(req) -> fromUri(resolveBaseUrl(integration, AlsoEnergyCloudIntegrationService.BASE_URI))
						.path(SITE_HARDWARE_URL_TEMPLATE)
						.queryParam("includeArchivedFields", true)
						.queryParam("includeDeviceConfig", true)
						.buildAndExpand(filters).toUri(),
						// @formatter:on
				res -> parseSiteHardware(res.getBody(), filters));
	}

	private static List<CloudDataValue> parseSites(JsonNode json) {
		assert json != null;
		/*- EXAMPLE JSON:
		{
		  "items": [
		    {
		      "siteId": 12345,
		      "siteName": "Site One"
		    },
		    {
		      "siteId": 23456,
		      "siteName": "Site Two"
		    },
		*/
		final var result = new ArrayList<CloudDataValue>(4);
		for ( JsonNode siteNode : json.path("items") ) {
			final String id = siteNode.path("siteId").asText();
			final String name = siteNode.path("siteName").asText().trim();
			result.add(intermediateDataValue(List.of(id), name, null));
		}
		return result;
	}

	private static List<CloudDataValue> parseSiteHardware(JsonNode json, Map<String, ?> filters) {
		assert json != null;
		/*- EXAMPLE JSON:
		{
		  "hardware": [
		    {
		      "id": 12345,
		      "stringId": "C12345_S12345_PM0",
		      "functionCode": "PM",
		      "flags": [
		        "IsEnabled"
		      ],
		      "fieldsArchived": [
		        "KWHnet",
		        "KW",
		        "KVAR",
		        "PowerFactor",
		        "KWHrec",
		        "KWHdel",
		        "Frequency",
		        "VacA",
		        "VacB",
		        "VacC",
		        "VacAB",
		        "VacBC",
		        "VacCA",
		        "IacA",
		        "IacB",
		        "IacC"
		      ],
		      "name": "Elkor Production Meter - A",
		      "lastUpdate": "2024-11-21T17:19:02.4069164-05:00",
		      "lastUpload": "2024-11-21T17:17:00-05:00",
		      "iconUrl": "https://alsoenergy.com/Pub/Images/device/7855.png",
		      "config": {
		        "deviceType": "ProductionPowerMeter",
		        "hardwareStrId": "C12345_S12345_PM0",
		        "hardwareId": 12345,
		        "address": 1,
		        "portNumber": 1,
		        "baudRate": 9600,
		        "comType": "Rs485_2Wire",
		        "serialNumber": "12345",
		        "name": "Elkor Production Meter - A",
		        "outputHardwareId": 0,
		        "weatherStationId": 0,
		        "meterConfig": {
		          "scaleFactor": 0.0,
		          "isReversed": false,
		          "grossEnergy": "None",
		          "maxPowerKw": 58444.96,
		          "maxVoltage": 480.0,
		          "maxAmperage": 12200.0,
		          "acPhase": "Wye"
		        }
		      }
		    },
		*/
		final String siteId = filters.get(SITE_ID_FILTER).toString();
		final var result = new ArrayList<CloudDataValue>(4);
		for ( JsonNode deviceNode : json.path("hardware") ) {
			final JsonNode fieldsNode = deviceNode.path("fieldsArchived");
			if ( !(fieldsNode.isArray() && fieldsNode.size() > 0) ) {
				continue;
			}
			final String id = deviceNode.path("id").asText().trim();
			if ( id.isEmpty() ) {
				continue;
			}
			final String name = deviceNode.path("name").asText().trim();
			final var meta = new LinkedHashMap<String, Object>(4);
			populateNonEmptyValue(deviceNode, "functionCode", "functionCode", meta);
			JsonNode configNode = deviceNode.path("config");
			populateNonEmptyValue(configNode, "serialNumber", DEVICE_SERIAL_NUMBER_METADATA, meta);
			populateNonEmptyValue(configNode, "deviceType", "deviceType", meta);

			List<CloudDataValue> fields = new ArrayList<>(fieldsNode.size());
			for ( JsonNode fieldNode : fieldsNode ) {
				final String fieldName = fieldNode.asText();

				List<CloudDataValue> aggs = new ArrayList<>(AlsoEnergyFieldFunction.values().length);
				for ( AlsoEnergyFieldFunction fn : AlsoEnergyFieldFunction.values() ) {
					aggs.add(dataValue(List.of(siteId, id, fieldName, fn.name()),
							fieldName + " " + fn.name()));
				}

				fields.add(intermediateDataValue(List.of(siteId, id, fieldName), fieldName, null, aggs));
			}

			result.add(intermediateDataValue(List.of(siteId, id), name, meta, fields));
		}
		return result;
	}

	private AlsoEnergyGranularity resolveGranularity(CloudDatumStreamConfiguration datumStream,
			Map<String, ?> parameters) {
		AlsoEnergyGranularity result = null;
		try {
			String settingVal = null;
			if ( parameters != null && parameters.get(GRANULARITY_SETTING) instanceof String s ) {
				settingVal = s;
			} else if ( datumStream != null ) {
				settingVal = datumStream.serviceProperty(GRANULARITY_SETTING, String.class);
			}
			if ( settingVal != null && !settingVal.isEmpty() ) {
				result = AlsoEnergyGranularity.fromValue(settingVal);
			}
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		return (result != null ? result : AlsoEnergyGranularity.Raw);
	}

	private ZoneId resolveTimeZone(CloudDatumStreamConfiguration datumStream,
			Map<String, ?> parameters) {
		ZoneId result = null;
		try {
			String settingVal = null;
			if ( parameters != null && parameters.get(TIME_ZONE_SETTING) instanceof String s ) {
				settingVal = s;
			} else if ( datumStream != null ) {
				settingVal = datumStream.serviceProperty(TIME_ZONE_SETTING, String.class);
			}
			if ( settingVal != null && !settingVal.isEmpty() ) {
				result = ZoneId.of(settingVal);
			}
		} catch ( Exception e ) {
			// ignore
		}
		return (result != null ? result : ZoneOffset.UTC);
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
	 * <li>hardwareId</li>
	 * <li>field</li>
	 * <li>function</li>
	 * </ol>
	 */
	private static final Pattern VALUE_REF_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)/(.+)");

	private static record ValueRef(Long siteId, Long hardwareId, String fieldName,
			AlsoEnergyFieldFunction fn, String sourceId,
			CloudDatumStreamPropertyConfiguration property) {

		public ValueRef(Long siteId, Long hardwareId, String fieldName, AlsoEnergyFieldFunction fn,
				CloudDatumStreamPropertyConfiguration property) {
			this(siteId, hardwareId, fieldName, fn, "/%s/%s".formatted(siteId, hardwareId), property);
		}
	}

	private Map<UserLongCompositePK, List<ValueRef>> resolveHardwareGroups(
			CloudIntegrationConfiguration integration, CloudDatumStreamConfiguration datumStream,
			List<CloudDatumStreamPropertyConfiguration> propConfigs) {
		var result = new LinkedHashMap<UserLongCompositePK, List<ValueRef>>(16);
		for ( CloudDatumStreamPropertyConfiguration config : propConfigs ) {
			String ref = resolvePlaceholders(config.getValueReference(), datumStream);
			Matcher m = VALUE_REF_PATTERN.matcher(ref);
			if ( !m.matches() ) {
				continue;
			}
			// groups: 1 = siteId, 2 = hardwareId, 3 = field, 4 = function
			Long siteId = Long.valueOf(m.group(1));
			Long hardwareId = Long.valueOf(m.group(2));
			String fieldName = m.group(3);
			AlsoEnergyFieldFunction fn;
			try {
				fn = AlsoEnergyFieldFunction.valueOf(m.group(4));
			} catch ( IllegalArgumentException e ) {
				fn = AlsoEnergyFieldFunction.Last;
			}

			result.computeIfAbsent(new UserLongCompositePK(siteId, hardwareId), k -> new ArrayList<>(8))
					.add(new ValueRef(siteId, hardwareId, fieldName, fn, config));
		}
		return result;
	}

	private Void parseDatum(JsonNode body, List<ValueRef> refs,
			CloudDatumStreamConfiguration datumStream, Map<String, String> sourceIdMap,
			Map<DatumId, DatumSamples> dataMap) {
		/*- EXAMPLE JSON:
		{
		  "info": [
		    {
		      "hardwareId": 12345,
		      "dataIndex": 0,
		      "name": "KwAC",
		      "units": "Kilowatts"
		    },
		    {
		      "hardwareId": 12345,
		      "dataIndex": 1,
		      "name": "KwhAC",
		      "units": "KilowattHours"
		    }
		  ],
		  "items": [
		    {
		      "timestamp": "2024-11-21T10:00:00-05:00",
		      "data": [
		        0.0,
		        438.201
		      ]
		    },
		 */
		final JsonNode items = body.path("items");
		final int refCount = refs.size();
		for ( JsonNode item : items ) {
			JsonNode tsNode = item.path("timestamp");
			if ( !tsNode.isTextual() ) {
				continue;
			}
			Instant ts = Instant.parse(tsNode.asText());
			JsonNode dataNode = item.path("data");
			if ( dataNode.size() != refCount ) {
				continue;
			}
			for ( int i = 0; i < refCount; i++ ) {
				ValueRef ref = refs.get(i);
				final String sourceId = nonEmptyString(resolveSourceId(datumStream, ref, sourceIdMap));
				if ( sourceId == null ) {
					continue;
				}
				JsonNode valNode = dataNode.get(i);
				Object propVal = parseJsonDatumPropertyValue(valNode, ref.property.getPropertyType());
				propVal = ref.property.applyValueTransforms(propVal);
				if ( propVal != null ) {
					dataMap.computeIfAbsent(
							new DatumId(datumStream.getKind(), datumStream.getObjectId(), sourceId, ts),
							k -> new DatumSamples()).putSampleValue(ref.property.getPropertyType(),
									ref.property.getPropertyName(), propVal);
				}

			}
		}
		return null;
	}

	private String resolveSourceId(CloudDatumStreamConfiguration datumStream, ValueRef ref,
			Map<String, String> sourceIdMap) {
		if ( sourceIdMap != null ) {
			return sourceIdMap.get(ref.sourceId);
		}
		return datumStream.getSourceId() + ref.sourceId;
	}

}
