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
import static net.solarnetwork.central.c2c.domain.CloudDataValue.AZIMUTH_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.RELATED_IDENTIFIER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.TILT_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY;
import static net.solarnetwork.central.datum.domain.DatumValidationType.TimeGap;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.cache.Cache;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.client.RestOperations;
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
import net.solarnetwork.central.datum.domain.DatumValidationType;
import net.solarnetwork.central.datum.support.OrderedDatumSamplesBuffer;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.support.DateTimeUtils;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumStreamId;
import net.solarnetwork.domain.datum.DatumStreamIdentity;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.CollectionUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.StringUtils;
import tools.jackson.databind.JsonNode;

/**
 * AlsoEnergy implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 2.2
 */
public class AlsoEnergyCloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

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
	 * The setting for a "granularity periods" mapping.
	 *
	 * <p>
	 * Granularity periods are a mapping of time offsets to associated
	 * granularity values. The purpose of this is affect the granularity
	 * requested for historic time periods, because Also Energy does not retain
	 * raw data indefinitely.
	 * </p>
	 *
	 * <p>
	 * The format of this property is either a {@code Map<String, String>} or a
	 * comma-delimited {@code key=value} listing, where the keys are ISO 8601
	 * period strings and the values are {@link AlsoEnergyGranularity} enum
	 * names. Each key represents the date <b>after which</b> the associated
	 * granularity should be applied. Larger periods override smaller periods.
	 * </p>
	 *
	 * <p>
	 * For example, the {@link #DEFAULT_GRANULARITY_PERIODS} mapping would be
	 * configured like this, expressed as JSON:
	 * </p>
	 *
	 * <pre>
	 * {@code{
	 *   "granularityPeriods": {
	 *     "P6M": "FiveMinute",
	 *     "P2Y": "FifteenMinute"
	 *   }
	 * }}
	 * </pre>
	 *
	 * <p>
	 * Alternatively, the same mapping could be expressed as a delimited string
	 * like this:
	 * </p>
	 *
	 * <pre>
	 * {@code{
	 *   "granularityPeriods": "P6M=FiveMinute, P2Y=FifteenMinute"
	 * }}
	 * </pre>
	 *
	 * <p>
	 * This mapping means that raw data will be requested for time periods
	 * within 6 months of the current time, 5-minute data for time periods
	 * between 6 months and 2 years, and then 15-minute data for anything older
	 * than 2 years.
	 * </p>
	 *
	 * @since 2.2
	 */
	public static final String GRANULARITY_PERIODS_SETTING = "granularityPeriods";

	/**
	 * The {@code granularityPeriods} setting default value.
	 *
	 * @since 2.2
	 */
	public static final SortedMap<Period, AlsoEnergyGranularity> DEFAULT_GRANULARITY_PERIODS;
	static {
		var periodMap = new TreeMap<Period, AlsoEnergyGranularity>(DateTimeUtils::comparePeriods);
		periodMap.put(Period.ofMonths(6), AlsoEnergyGranularity.FiveMinute);
		periodMap.put(Period.ofYears(2), AlsoEnergyGranularity.FifteenMinute);
		DEFAULT_GRANULARITY_PERIODS = Collections.unmodifiableSortedMap(periodMap);
	}

	/**
	 * The URI path to list the hardware for a given site.
	 *
	 * <p>
	 * Accepts a single {@code {siteId}} parameter.
	 * </p>
	 */
	public static final String SITE_HARDWARE_URL_TEMPLATE = "/sites/{siteId}/hardware";

	/**
	 * Site hardware URI parameter name to include disabled hardware.
	 *
	 * @since 2.2
	 */
	public static final String INCLUDE_DISABLED_HARDWARE_PARAM = "includeDisabledHardware";

	/**
	 * Site hardware URI parameter name to include device configuration.
	 *
	 * @since 2.2
	 */
	public static final String INCLUDE_DEVICE_CONFIG_PARAM = "includeDeviceConfig";

	/**
	 * Site hardware URI parameter name to include archived fields.
	 *
	 * @since 2.2
	 */
	public static final String INCLUDE_ARCHIVED_FIELDS_PARAM = "includeArchivedFields";

	/**
	 * A site hardware flag value for "in enabled"
	 *
	 * @since 2.2
	 */
	public static final String IS_ENABLED_FLAG = "IsEnabled";

	/** The URI path to query for data. */
	public static final String BIN_DATA_URL = "/v2/data/bindata";

	/**
	 * The data value identifier levels source ID range.
	 *
	 * @since 1.3
	 */
	public static final IntRange DATA_VALUE_IDENTIFIER_LEVELS_SOURCE_ID_RANGE = IntRange.rangeOf(0, 2);

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;

	static {
		// menu for granularity
		var granularitySpec = new BasicMultiValueSettingSpecifier(GRANULARITY_SETTING,
				AlsoEnergyGranularity.Raw.name());
		var granularityTitles = unmodifiableMap(Arrays.stream(AlsoEnergyGranularity.values())
				.collect(Collectors.toMap(AlsoEnergyGranularity::name, AlsoEnergyGranularity::name,
						(_, r) -> r,
						() -> new LinkedHashMap<>(LocusEnergyGranularity.values().length))));
		granularitySpec.setValueTitles(granularityTitles);

		// @formatter:off
		SETTINGS = List.of(
				  granularitySpec
				, new BasicTextFieldSettingSpecifier(TIME_ZONE_SETTING, null)
				, SOURCE_ID_MAP_SETTING_SPECIFIER
				, VIRTUAL_SOURCE_IDS_SETTING_SPECIFIER
				, MULTI_STREAM_MAXIMUM_LAG_SETTING_SPECIFIER
				, VALIDATION_IGNORE_SETTING_SPECIFIER
				, TIME_GAP_VALIDATION_THRESHOLD_SETTING_SPECIFIER
			);
		// @formatter:on
	}

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SITE_ID_FILTER,
			HARDWARE_ID_FILTER);

	/** The maximum period of time to request data for in one request. */
	private static final Duration MAX_QUERY_TIME_RANGE = Duration.ofDays(7);

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
	 *         if any argument except {@code integrationLocksCache} is
	 *         {@code null}
	 */
	public AlsoEnergyCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager, Clock clock,
			@Nullable Cache<UserLongCompositePK, Lock> integrationLocksCache) {
		super(SERVICE_IDENTIFIER, "AlsoEnergy Datum Stream Service", clock, userEventAppenderBiz,
				encryptor, expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new OAuth2RestOperationsHelper(
						LoggerFactory.getLogger(AlsoEnergyCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						_ -> AlsoEnergyCloudIntegrationService.SECURE_SETTINGS, oauthClientManager,
						clock, integrationLocksCache));
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
		for ( String key : new String[] { SITE_ID_FILTER } ) {
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
		if ( filters != null && filters.get(SITE_ID_FILTER) != null ) {
			result = siteHardware(integration, Long.valueOf(filters.get(SITE_ID_FILTER).toString()),
					filters);
		} else {
			// list available sites
			result = sites(integration);
		}
		result.sort(null);
		return result;
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");
		final ZoneId zone = resolveTimeZone(datumStream, null);
		final AlsoEnergyGranularity granularity = resolveGranularity(datumStream, null, null, null,
				null);

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

			final ZoneId zone = resolveTimeZone(datumStream, filter.getParameters());

			final SortedMap<Period, AlsoEnergyGranularity> granularityPeriods = resolveGranularityPeriods(
					datumStream);

			final AlsoEnergyGranularity resolution = resolveGranularity(ds, filter.getParameters(),
					granularityPeriods, filterStartDate, zone);

			final Map<String, String> sourceIdMap = ds.servicePropertyStringMap(SOURCE_ID_MAP_SETTING);

			// validation support
			final Set<String> ignoredValidations = ds
					.servicePropertyStringSet(VALIDATION_IGNORE_SETTING);

			// construct (siteId, hardwareId) to ValueRef[] mapping
			final Map<UserLongCompositePK, List<ValueRef>> hardwareGroups = resolveHardwareGroups(
					integration, ds, sourceIdMap != null ? sourceIdMap.keySet() : null, valueProps);

			BasicQueryFilter nextQueryFilter = null;

			Instant startDate = resolution.tickStart(filterStartDate, zone);
			Instant endDate = resolution.tickStart(filterEndDate, zone);
			if ( endDate.isBefore(filterEndDate) ) {
				endDate = resolution.nextTickStart(endDate, zone);
			}

			endDate = adjustEndDateForGranularityPeriods(granularityPeriods, startDate, endDate, zone);

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

			if ( !endDate.isAfter(startDate) ) {
				return new BasicCloudDatumStreamQueryResult(usedQueryFilter, nextQueryFilter, List.of());
			}

			final OrderedDatumSamplesBuffer streamBuffer = new OrderedDatumSamplesBuffer();

			for ( Entry<UserLongCompositePK, List<ValueRef>> e : hardwareGroups.entrySet() ) {
				final ZonedDateTime siteStartDate = startDate.atZone(zone);
				final ZonedDateTime siteEndDate = endDate.atZone(zone);

				final List<Map<String, Object>> reqBody = new ArrayList<>(e.getValue().size());
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
						}, (req, res) -> parseDatum(req, res.getBody(), e.getValue(), ds, sourceIdMap,
								streamBuffer, ignoredValidations));
			}

			List<GeneralDatum> resultDatum = streamBuffer.datum(GeneralDatum::new);

			nextQueryFilter = resolveNextQueryFilterForMultiStreamLag(ds, streamBuffer, nextQueryFilter,
					resolution.getTickAmount(), zone, filterEndDate, endDate);

			// evaluate expressions on merged datum
			var r = evaluateExpressions(datumStream, exprProps, resultDatum, mapping.getConfigId(),
					integration.getConfigId());

			return new BasicCloudDatumStreamQueryResult(usedQueryFilter, nextQueryFilter,
					r.stream().map(Datum.class::cast).toList(), streamBuffer.auxiliaryOrNull());
		});
	}

	private List<CloudDataValue> sites(CloudIntegrationConfiguration integration) {
		var sprops = integration.getServiceProperties();
		return restOpsHelper.httpGet("List sites", integration, JsonNode.class,
				_ -> fromUri(resolveBaseUrl(integration, AlsoEnergyCloudIntegrationService.BASE_URI))
						.path(AlsoEnergyCloudIntegrationService.LIST_SITES_URL)
						.buildAndExpand(sprops != null ? sprops : Map.of()).toUri(),
				(_, res) -> parseSites(res.getBody()));
	}

	private List<CloudDataValue> siteHardware(CloudIntegrationConfiguration integration,
			final Long siteId, final Map<String, ?> filters) {
		return restOpsHelper.httpGet("List site hardware", integration, JsonNode.class,
		// @formatter:off
				_ -> fromUri(resolveBaseUrl(integration, AlsoEnergyCloudIntegrationService.BASE_URI))
						.path(SITE_HARDWARE_URL_TEMPLATE)
						.queryParam(INCLUDE_ARCHIVED_FIELDS_PARAM, true)
						.queryParam(INCLUDE_DEVICE_CONFIG_PARAM, true)
						.queryParam(INCLUDE_DISABLED_HARDWARE_PARAM, true)
						.buildAndExpand(filters).toUri(),
						// @formatter:on
				(_, res) -> parseSiteHardware(siteId, res.getBody()));
	}

	private static List<CloudDataValue> parseSites(@Nullable JsonNode json) {
		if ( json == null ) {
			return new ArrayList<>(0);
		}
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
			final String id = siteNode.path("siteId").asString();
			final String name = siteNode.path("siteName").asString().trim();
			result.add(intermediateDataValue(List.of(id), name, null));
		}
		return result;
	}

	/**
	 * Parse a site hardware JSON response.
	 *
	 * @param siteId
	 *        the site ID
	 * @param json
	 *        the JSON to parse
	 * @return the parsed inventory
	 */
	public static List<CloudDataValue> parseSiteHardware(final Long siteId,
			final @Nullable JsonNode json) {
		if ( json == null ) {
			return new ArrayList<>(0);
		}
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
		final String siteIdent = siteId.toString();
		final var result = new ArrayList<CloudDataValue>(4);
		for ( JsonNode deviceNode : json.path("hardware") ) {
			final JsonNode fieldsNode = deviceNode.path("fieldsArchived");
			if ( !fieldsNode.isArray() || fieldsNode.isEmpty() ) {
				continue;
			}
			final String id = deviceNode.path("id").asString().trim();
			if ( id.isEmpty() ) {
				continue;
			}
			final String name = deviceNode.path("name").asString().trim();
			final var meta = new LinkedHashMap<String, Object>(4);
			populateNonEmptyValue(deviceNode, "functionCode", "functionCode", meta);

			final JsonNode configNode = deviceNode.path("config");
			populateNonEmptyValue(configNode, "serialNumber", DEVICE_SERIAL_NUMBER_METADATA, meta);
			populateNonEmptyValue(configNode, "deviceType", "deviceType", meta);

			final long outputHardwareId = configNode.path("outputHardwareId").asLong(0L);
			if ( outputHardwareId > 0 ) {
				meta.put(RELATED_IDENTIFIER_METADATA, outputHardwareId);
			}

			final JsonNode flagsNode = deviceNode.path("flags");
			final Set<String> flags = new LinkedHashSet<>(4);
			if ( flagsNode.isArray() && !flagsNode.isEmpty() ) {
				for ( JsonNode flagNode : flagsNode ) {
					if ( flagNode.isString() ) {
						String flag = flagNode.stringValue();
						if ( flag != null && !flag.isEmpty() ) {
							flags.add(flag);
						}
					}
				}
			}
			meta.put(CloudDataValue.ACTIVE_METADATA, flags.contains(IS_ENABLED_FLAG));
			if ( !flags.isEmpty() ) {
				meta.put("flags", flags);
			}

			final JsonNode inverterConfigsNode = configNode.path("inverterConfig");
			if ( inverterConfigsNode.isArray() && inverterConfigsNode.size() == 1 ) {
				final JsonNode inverterConfigNode = inverterConfigsNode.path(0);
				double ratedAcPower = inverterConfigNode.path("ratedAcPower").asDouble(0.0);
				if ( ratedAcPower > 0.0 ) {
					meta.put(CloudDataValue.RATED_POWER_METADATA, ratedAcPower * 1000.0);
				}
				populateNumberValue(inverterConfigNode, "azimuth", AZIMUTH_METADATA, meta);
				populateNumberValue(inverterConfigNode, "tilt", TILT_METADATA, meta);
			}

			List<CloudDataValue> fields = new ArrayList<>(fieldsNode.size());
			for ( JsonNode fieldNode : fieldsNode ) {
				final String fieldName = fieldNode.asString();

				List<CloudDataValue> aggs = new ArrayList<>(AlsoEnergyFieldFunction.values().length);
				for ( AlsoEnergyFieldFunction fn : AlsoEnergyFieldFunction.values() ) {
					aggs.add(dataValue(List.of(siteIdent, id, fieldName, fn.name()),
							fieldName + " " + fn.name()));
				}

				fields.add(
						intermediateDataValue(List.of(siteIdent, id, fieldName), fieldName, null, aggs));
			}

			result.add(intermediateDataValue(List.of(siteIdent, id), name, meta, fields));
		}
		return result;
	}

	/**
	 * Resolve the appropriate granularity to use.
	 *
	 * @param datumStream
	 *        the datum stream
	 * @param parameters
	 *        optional parameters to override the datum stream settings
	 * @param granularityPeriods
	 *        the granularity periods to constrain the result to
	 * @param timestamp
	 *        the timestamp to constrain the result to
	 * @param zone
	 *        the site zone to use for granularity period evaluation
	 * @return the granularity to use
	 */
	public AlsoEnergyGranularity resolveGranularity(CloudDatumStreamConfiguration datumStream,
			@Nullable Map<String, ?> parameters,
			@Nullable SortedMap<Period, AlsoEnergyGranularity> granularityPeriods,
			@Nullable Instant timestamp, @Nullable ZoneId zone) {
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
		if ( result == null ) {
			result = AlsoEnergyGranularity.Raw;
		}
		if ( granularityPeriods != null && timestamp != null && zone != null ) {
			// constrain based on granularity periods
			final ZonedDateTime date = result.tickStart(timestamp, zone).atZone(zone);
			final ZonedDateTime now = clock.instant().atZone(zone);
			AlsoEnergyGranularity periodGranularity = null;
			for ( Entry<Period, AlsoEnergyGranularity> entry : granularityPeriods.reversed()
					.entrySet() ) {
				if ( now.minus(entry.getKey()).isAfter(date)
						&& entry.getValue().compareTo(result) > 0 ) {
					periodGranularity = entry.getValue();
					break;
				}
			}
			if ( periodGranularity != null ) {
				result = periodGranularity;
			}
		}
		return (result != null ? result : AlsoEnergyGranularity.Raw);
	}

	private ZoneId resolveTimeZone(CloudDatumStreamConfiguration datumStream,
			@Nullable Map<String, ?> parameters) {
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

	private record ValueRef(Long siteId, Long hardwareId, String fieldName, AlsoEnergyFieldFunction fn,
			String hardwareRef, CloudDatumStreamPropertyConfiguration property) {

		private ValueRef(Long siteId, Long hardwareId, String fieldName, AlsoEnergyFieldFunction fn,
				CloudDatumStreamPropertyConfiguration property) {
			this(siteId, hardwareId, fieldName, fn, "/%s/%s".formatted(siteId, hardwareId), property);
		}

	}

	private Map<UserLongCompositePK, List<ValueRef>> resolveHardwareGroups(
			CloudIntegrationConfiguration integration, CloudDatumStreamConfiguration datumStream,
			@Nullable Collection<String> sourceValueRefs,
			List<CloudDatumStreamPropertyConfiguration> propConfigs) {
		@SuppressWarnings("unchecked")
		List<Map<String, ?>> placeholderSets = resolvePlaceholderSets(
				datumStream.serviceProperty(PLACEHOLDERS_SERVICE_PROPERTY, Map.class), sourceValueRefs);
		final var result = new LinkedHashMap<UserLongCompositePK, List<ValueRef>>(16);
		for ( CloudDatumStreamPropertyConfiguration config : propConfigs ) {
			for ( Map<String, ?> ph : placeholderSets ) {
				String ref = StringUtils.expandTemplateString(config.getValueReference(), ph);
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

				// verify against site inventory
				final CloudDataValue[] siteInventory = resolveSiteInventory(integration, siteId);
				if ( siteInventory != null ) {
					CloudDataValue match = CloudDataValue.findFirst(siteInventory,
							List.of(siteId.toString(), hardwareId.toString(), fieldName, fn.name()));
					if ( match == null ) {
						// not available so skip
						continue;
					}
				}

				final var valueRef = new ValueRef(siteId, hardwareId, fieldName, fn, config);
				final List<ValueRef> valueRefs = result.computeIfAbsent(
						new UserLongCompositePK(siteId, hardwareId), _ -> new ArrayList<>(8));
				if ( !valueRefs.contains(valueRef) ) {
					valueRefs.add(valueRef);
				}
			}
		}
		return result;
	}

	private Void parseDatum(RequestEntity<List<Map<String, Object>>> request, @Nullable JsonNode body,
			List<ValueRef> refs, CloudDatumStreamConfiguration datumStream,
			@Nullable Map<String, String> sourceIdMap, OrderedDatumSamplesBuffer streamBuffer,
			Set<String> ignoredValidations) {
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
		if ( body == null ) {
			// API might return 204 NoContent, and then we get here
			return null;
		}

		final Duration timeGapThreshold = (!ignoredValidations.contains(TimeGap.getKey())
				? resolveTimeGapValidationThreshold(datumStream)
				: null);

		final JsonNode items = body.path("items");
		final int refCount = refs.size();
		final var datumIsNew = new MutableBoolean(false);

		for ( JsonNode item : items ) {
			JsonNode tsNode = item.path("timestamp");
			if ( !tsNode.isString() ) {
				continue;
			}
			Instant ts = Instant.parse(tsNode.asString());
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
					DatumStreamIdentity streamId = new DatumStreamId(datumStream.getKind(),
							datumStream.getObjectId(), sourceId).toIdentity();
					datumIsNew.setFalse();
					final DatumSamples samples = streamBuffer.getOrCreate(streamId, ts, datumIsNew);
					samples.putSampleValue(ref.property.getPropertyType(),
							ref.property.getPropertyName(), propVal);
					if ( samples.isEmpty() ) {
						streamBuffer.removeTimestamp(streamId, ts, samples);
						continue;
					}
					if ( datumIsNew.booleanValue() && timeGapThreshold != null ) {
						// time gap validation for new datum
						Instant prevTs = streamBuffer.previousTimestamp(streamId, ts);
						if ( prevTs == null ) {
							final var prevDatum = lookupPreviousDatum(datumStream, sourceId, ts);
							if ( prevDatum != null ) {
								prevTs = prevDatum.getTimestamp();
							}
						}
						if ( prevTs != null ) {
							streamBuffer.addAuxiliary(streamId,
									validateTimeGap(datumStream, request, ref.hardwareRef, null,
											timeGapThreshold, prevTs, streamId.datumIdentity(ts)));
						}
					}
				}
			}
		}
		return null;
	}

	private @Nullable String resolveSourceId(CloudDatumStreamConfiguration datumStream, ValueRef ref,
			@Nullable Map<String, String> sourceIdMap) {
		if ( sourceIdMap != null ) {
			return sourceIdMap.get(ref.hardwareRef);
		}
		return datumStream.getSourceId() + ref.hardwareRef;
	}

	private CloudDataValue @Nullable [] resolveSiteInventory(CloudIntegrationConfiguration integration,
			Long siteId) {
		assert integration != null && siteId != null;
		final var cache = getSiteInventoryCache();

		CloudDataValue[] result = (cache != null ? cache.get(siteId) : null);
		if ( result != null ) {
			return result;
		}

		List<CloudDataValue> response = siteHardware(integration, siteId,
				Map.of(SITE_ID_FILTER, siteId));
		if ( response != null ) {
			result = response.toArray(CloudDataValue[]::new);
			if ( cache != null ) {
				cache.put(siteId, result);
			}
		}

		return result;
	}

	/**
	 * Resolve the granularity periods to use on a given datum stream.
	 *
	 * @param datumStream
	 *        the configuration to extract the operational range mapping from
	 * @return the mapping, falling back to {@link #DEFAULT_GRANULARITY_PERIODS}
	 *         if not configured on the datum stream
	 * @since 2.2
	 * @see #GRANULARITY_PERIODS_SETTING
	 */
	public SortedMap<Period, AlsoEnergyGranularity> resolveGranularityPeriods(
			CloudDatumStreamConfiguration datumStream) {
		final Map<String, String> periodMapping = datumStream
				.servicePropertyStringMap(GRANULARITY_PERIODS_SETTING);
		SortedMap<Period, AlsoEnergyGranularity> result = null;
		if ( periodMapping != null && !periodMapping.isEmpty() ) {
			Map<Period, AlsoEnergyGranularity> mapped = CollectionUtils.transformMap(periodMapping, null,
					(k, _) -> {
						return Period.parse(k).normalized();
					}, (_, v) -> {
						return AlsoEnergyGranularity.fromValue(v);
					});
			if ( mapped != null ) {
				result = new TreeMap<>(DateTimeUtils::comparePeriods);
				result.putAll(mapped);
			}
		}
		return (result != null && !result.isEmpty() ? result : DEFAULT_GRANULARITY_PERIODS);
	}

	/**
	 * Adjust a query end date using granularity periods.
	 *
	 * @param granularityPeriods
	 *        the granularity periods
	 * @param startDate
	 *        the query start date
	 * @param endDate
	 *        the query end date
	 * @param zone
	 *        the time zone
	 * @return the end date to use
	 */
	public Instant adjustEndDateForGranularityPeriods(
			SortedMap<Period, AlsoEnergyGranularity> granularityPeriods, Instant startDate,
			Instant endDate, ZoneId zone) {
		final ZonedDateTime start = startDate.atZone(zone);
		final ZonedDateTime end = endDate.atZone(zone);
		final ZonedDateTime now = clock.instant().atZone(zone);
		for ( Entry<Period, AlsoEnergyGranularity> entry : granularityPeriods.entrySet() ) {
			ZonedDateTime periodEndDate = now.minus(entry.getKey());
			if ( start.compareTo(periodEndDate) != end.compareTo(periodEndDate) ) {
				// not within same period, so clamp end to period end
				return entry.getValue().tickStart(periodEndDate.toInstant(), zone);
			}
		}
		return endDate;
	}

	/**
	 * Get the site inventory cache.
	 *
	 * @return the cache
	 * @since 2.1
	 */
	public final @Nullable Cache<Long, CloudDataValue[]> getSiteInventoryCache() {
		return siteInventoryCache;
	}

	/**
	 * Set the site inventory cache.
	 *
	 * <p>
	 * This cache can be provided to help with device lookup by site. ID.
	 * </p>
	 *
	 * @param siteInventoryCache
	 *        the cache to set
	 * @since 2.1
	 */
	public final void setSiteInventoryCache(@Nullable Cache<Long, CloudDataValue[]> siteInventoryCache) {
		this.siteInventoryCache = siteInventoryCache;
	}
}
