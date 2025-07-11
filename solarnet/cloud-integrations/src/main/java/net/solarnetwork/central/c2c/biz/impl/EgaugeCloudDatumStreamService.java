/* ==================================================================
 * EgaugeCloudDatumStreamService.java - 25/10/2024 11:56:42 am
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

import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.impl.CloudIntegrationsUtils.SECS_PER_HOUR;
import static net.solarnetwork.central.c2c.biz.impl.EgaugeCloudIntegrationService.BASE_URI_TEMPLATE;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;
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
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * eGauge implementation of {@link CloudDatumStreamService}.
 *
 * <p>
 * eGauge integrations actually work on the
 * {@link CloudDatumStreamConfiguration} level, as the username and password
 * required is specific to each eGauge device. The eGauge API is actually just a
 * proxy service that forwards all communication to the physical eGague device
 * for handling all queries. Because of this, each
 * {@link CloudDatumStreamConfiguration} can only include data from a single
 * eGauge device.
 * </p>
 *
 * <p>
 * The {@code deviceId}, {@code username}, and {@code password} service
 * properties must all be configured on given
 * {@link CloudDatumStreamConfiguration} for the integration to work. That means
 * a {@link CloudDatumStreamMappingConfiguration} must be created <b>before</b>
 * the {@link #dataValues(UserLongCompositePK, Map)} method can return the
 * available cloud data values. The mapping configuration need not have any
 * {@link CloudDatumStreamPropertyConfiguration} entities associated with it,
 * however.
 *
 * @author matt
 * @version 1.9
 */
public class EgaugeCloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.egauge";

	/** The data value filter key for a device ID. */
	public static final String DEVICE_ID_FILTER = "deviceId";

	/** The setting for granularity. */
	public static final String GRANULARITY_SETTING = "granularity";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// @formatter:off
		SETTINGS = List.of(
				new BasicTextFieldSettingSpecifier(DEVICE_ID_FILTER, null),
				BaseCloudIntegrationService.USERNAME_SETTING_SPECIFIER,
				BaseCloudIntegrationService.PASSWORD_SETTING_SPECIFIER,
				new BasicTextFieldSettingSpecifier(GRANULARITY_SETTING, null),
				VIRTUAL_SOURCE_IDS_SETTING_SPECIFIER
				);
		// @formatter:on
	}

	/** The service secure setting keys. */
	public static final Set<String> SECURE_SETTINGS = Collections
			.unmodifiableSet(SettingUtils.secureKeys(SETTINGS));

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(DEVICE_ID_FILTER);

	/** A default granularity duration. */
	public static final Duration DEFAULT_GRANULARITY = Duration.ofMinutes(5);

	/** The maximum length of time to query for data. */
	public static final Duration MAX_QUERY_TIME_RANGE = Duration.ofDays(7);

	/** The maximum length of time to query for data. */
	public static final Duration MAX_GRANULARITY = Duration.ofDays(1);

	/** The URL path to the register data API method. */
	public static final String REGISTER_URL_PATH = "/api/register";

	private static final String REGISTER_INDEX_METADATA = "idx";
	private static final String REGISTER_TYPE_METADATA = "type";

	/**
	 * A cache of eGauge device IDs to associated register information. This is
	 * used to resolve the register index values for a given reference register
	 * name.
	 */
	private Cache<String, CloudDataValue[]> deviceRegistersCache;

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
	 * @param rng
	 *        a random generator to use
	 * @param clientAccessTokenDao
	 *        the client access token DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public EgaugeCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			Clock clock, RandomGenerator rng, ClientAccessTokenDao clientAccessTokenDao) {
		super(SERVICE_IDENTIFIER, "eGauge Datum Stream Service", clock, userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new EgaugeRestOperationsHelper(
						LoggerFactory.getLogger(EgaugeCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						datumStreamServiceIdentifier -> SECURE_SETTINGS, clock, rng,
						clientAccessTokenDao, integrationDao));
	}

	@Override
	protected Iterable<String> supportedPlaceholders() {
		return SUPPORTED_PLACEHOLDERS;
	}

	@Override
	protected boolean dataValuesRequireDatumStream() {
		return true;
	}

	@Override
	public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
		MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");
		List<LocalizedServiceInfo> result = new ArrayList<>(2);
		for ( String key : new String[] { DEVICE_ID_FILTER } ) {
			result.add(new BasicLocalizedServiceInfo(key, locale,
					ms.getMessage("dataValueFilter.%s.key".formatted(key), null, key, locale),
					ms.getMessage("dataValueFilter.%s.desc".formatted(key), null, null, locale), null));
		}
		return result;
	}

	@SuppressWarnings("BadInstanceof")
	@Override
	public Iterable<CloudDataValue> dataValues(UserLongCompositePK integrationId,
			Map<String, ?> filters) {
		requireNonNullArgument(filters, "filters");
		final CloudIntegrationConfiguration integration = requireNonNullObject(
				integrationDao.get(requireNonNullArgument(integrationId, "integrationId")),
				"integration");
		final Long datumStreamId = requireNonNullArgument(
				filters.get(DATUM_STREAM_ID_FILTER) instanceof Object o ? Long.valueOf(o.toString())
						: null,
				"filters.datumStreamId");
		final CloudDatumStreamConfiguration datumStream = requireNonNullObject(
				datumStreamDao.get(new UserLongCompositePK(integrationId.getUserId(), datumStreamId)),
				"datumStream");
		final String deviceId = requireNonNullArgument(
				datumStream.serviceProperty(DEVICE_ID_FILTER, String.class),
				"datumStream.serviceProperties.deviceId");
		List<CloudDataValue> result = deviceRegisters(integration, datumStream, deviceId);
		Collections.sort(result);
		return result;
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");
		final Duration granularity = resolveGranularity(datumStream);
		final Clock queryClock = Clock.tick(clock, granularity);
		final Instant endDate = queryClock.instant();
		final Instant startDate = endDate.minus(granularity);

		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);

		final var result = datum(datumStream, filter);
		if ( result == null || result.isEmpty() ) {
			return null;
		}
		return result.getResults();
	}

	@SuppressWarnings("JavaDurationGetSecondsToToSeconds")
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

			final String deviceId = requireNonNullArgument(
					ds.serviceProperty(DEVICE_ID_FILTER, String.class),
					"datumStream.serviceProperties.deviceId");

			final Instant filterStartDate = requireNonNullArgument(filter.getStartDate(),
					"filter.startDate");
			final Instant filterEndDate = requireNonNullArgument(filter.getEndDate(),
					"filter.startDate");

			final Duration granularity = resolveGranularity(ds);

			BasicQueryFilter nextQueryFilter = null;

			Instant startDate = CloudIntegrationsUtils.truncateDate(filterStartDate, granularity, UTC);
			Instant endDate = CloudIntegrationsUtils.truncateDate(filterEndDate, granularity, UTC);
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

			final String queryTimeRange = "%d:%s:%d".formatted(startDate.getEpochSecond(),
					granularity.getSeconds(), endDate.getEpochSecond());

			final Map<String, List<ValueRef>> refsByRegisterName = resolveValueReferences(integration,
					ds, deviceId, valueProps);
			final String queryRegisters = registerQueryParam(refsByRegisterName.values());

			final List<GeneralDatum> resultDatum = restOpsHelper.httpGet("List register data", ds,
					JsonNode.class,
					req -> fromUriString(resolveBaseUrl(integration, BASE_URI_TEMPLATE))
							.path(REGISTER_URL_PATH).queryParam("raw").queryParam("virtual", "value")
							.queryParam("reg", queryRegisters).queryParam("time", queryTimeRange)
							.buildAndExpand(deviceId).toUri(),
					res -> parseDatum(res.getBody(), ds, refsByRegisterName));

			// evaluate expressions on final datum
			var r = evaluateExpressions(datumStream, exprProps, resultDatum, mapping.getConfigId(),
					integration.getConfigId());

			return new BasicCloudDatumStreamQueryResult(usedQueryFilter, nextQueryFilter,
					r.stream().sorted().map(Datum.class::cast).toList());
		});
	}

	private List<CloudDataValue> deviceRegisters(CloudIntegrationConfiguration integration,
			CloudDatumStreamConfiguration datumStream, String deviceId) {
		return restOpsHelper.httpGet("List registers", datumStream, JsonNode.class,
				(req) -> fromUriString(resolveBaseUrl(integration, BASE_URI_TEMPLATE))
						.path(REGISTER_URL_PATH).buildAndExpand(deviceId).toUri(),
				res -> parseDeviceRegisters(deviceId, res.getBody()));
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private static List<CloudDataValue> parseDeviceRegisters(String deviceId, JsonNode json) {
		if ( json == null ) {
			return Collections.emptyList();
		}
		/*- EXAMPLE JSON:
		{
		    "ts": "1729828293",
		    "registers": [
		        {
		            "name": "voltage_a",
		            "type": "V",
		            "idx": 2,
		            "did": 3
		        },
		*/
		final var result = new ArrayList<CloudDataValue>(16);
		for ( JsonNode regNode : json.path("registers") ) {
			String name = nonEmptyString(regNode.path("name").asText());

			final var meta = new LinkedHashMap<String, Object>(4);
			populateNumberValue(regNode, "idx", REGISTER_INDEX_METADATA, meta);
			populateNonEmptyValue(regNode, "type", REGISTER_TYPE_METADATA, meta);
			populateNumberValue(regNode, "did", "did", meta);

			result.add(dataValue(List.of(deviceId, name), name, meta));
		}
		return result;
	}

	private static Duration resolveGranularity(CloudDatumStreamConfiguration datumStream) {
		String granValue = nonEmptyString(
				datumStream.serviceProperty(GRANULARITY_SETTING, String.class));
		Duration result = DEFAULT_GRANULARITY;
		if ( granValue != null ) {
			try {
				result = Duration.ofSeconds(Long.parseLong(granValue));
			} catch ( NumberFormatException e ) {
				// ignore and fall back to default
			}
		}
		if ( result.compareTo(MAX_GRANULARITY) > 0 ) {
			result = MAX_GRANULARITY;
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
	 * <li>deviceId</li>
	 * <li>registerName</li>
	 * </ol>
	 */
	private static final Pattern VALUE_REF_PATTERN = Pattern.compile("/([^/]+)/(.+)");

	private static record ValueRef(String deviceId, String registerName, int registerIndex,
			EgaugeTypeCode registerType, CloudDatumStreamPropertyConfiguration property) {

	}

	/**
	 * Resolve a mapping of register names to associated value references.
	 *
	 * <p>
	 * There can be more than one value reference per register, for example a
	 * power register might be used to derive both instantaneous and energy
	 * datum properties.
	 * </p>
	 *
	 * @param integration
	 *        the integration
	 * @param datumStream
	 *        the datum stream
	 * @param deviceId
	 *        the device ID
	 * @param properties
	 *        the non-expression properties configured for the stream
	 * @return the mapping
	 */
	private Map<String, List<ValueRef>> resolveValueReferences(CloudIntegrationConfiguration integration,
			CloudDatumStreamConfiguration datumStream, String deviceId,
			List<CloudDatumStreamPropertyConfiguration> properties) {
		final CloudDataValue[] registers = resolveDeviceRegisters(integration, datumStream, deviceId);
		final Map<String, Object> placeholders = Map.of(DEVICE_ID_FILTER, deviceId);
		final Map<String, List<ValueRef>> result = new LinkedHashMap<>(properties.size());

		for ( CloudDatumStreamPropertyConfiguration config : properties ) {
			String ref = StringUtils.expandTemplateString(config.getValueReference(), placeholders);
			Matcher m = VALUE_REF_PATTERN.matcher(ref);
			if ( !m.matches() ) {
				continue;
			}
			// groups: 1 = deviceId, 2 = reg
			String regName = m.group(2);
			CloudDataValue reg = Arrays.stream(registers)
					.filter(r -> regName.equalsIgnoreCase(r.getName())).findAny().orElse(null);
			if ( reg == null || reg.getMetadata() == null ) {
				continue;
			}
			Object idx = reg.getMetadata().get(REGISTER_INDEX_METADATA);
			if ( idx instanceof Number n
					&& reg.getMetadata().get(REGISTER_TYPE_METADATA) instanceof String t ) {
				try {
					EgaugeTypeCode type = EgaugeTypeCode.fromValue(t);
					result.computeIfAbsent(regName, k -> new ArrayList<>(2))
							.add(new ValueRef(deviceId, regName, n.intValue(), type, config));
				} catch ( IllegalArgumentException e ) {
					// ignore and continue
				}
			}
		}

		return result;
	}

	/**
	 * Generate a {@code /registers} API {@code reg} query parameter value out
	 * of a set of property configurations.
	 *
	 * @param setsOfRefs
	 *        the value references
	 * @return the {@code reg} query parameter value
	 */
	private String registerQueryParam(Collection<List<ValueRef>> setsOfRefs) {
		final IntRangeSet indexSet = new IntRangeSet();

		for ( List<ValueRef> refs : setsOfRefs ) {
			for ( ValueRef ref : refs ) {
				indexSet.add(ref.registerIndex);
			}
		}

		StringBuilder buf = new StringBuilder();
		for ( IntRange range : indexSet.ranges() ) {
			if ( !buf.isEmpty() ) {
				buf.append('+');
			}
			buf.append(range.min());
			if ( !range.isSingleton() ) {
				buf.append(':');
				buf.append(range.max());
			}
		}
		return buf.toString();
	}

	/**
	 * Resolve a set of data values for a datum stream.
	 *
	 * <p>
	 * This will use the configured {@link #getDeviceRegistersCache()} if
	 * available.
	 * </p>
	 *
	 * @param integration
	 *        the integration
	 * @param datumStream
	 *        the datum stream
	 * @param deviceId
	 *        the device ID
	 * @return the data values
	 */
	private CloudDataValue[] resolveDeviceRegisters(CloudIntegrationConfiguration integration,
			CloudDatumStreamConfiguration datumStream, String deviceId) {
		assert datumStream != null && deviceId != null;
		final var cache = getDeviceRegistersCache();

		CloudDataValue[] result = (cache != null ? cache.get(deviceId) : null);
		if ( result != null ) {
			return result;
		}

		List<CloudDataValue> response = deviceRegisters(integration, datumStream, deviceId);
		if ( response != null ) {
			result = response.toArray(CloudDataValue[]::new);
			if ( cache != null ) {
				cache.put(deviceId, result);
			}
		}

		return result;
	}

	/**
	 * Parse a fractional Unix epoch timestamp value from a JSON field.
	 *
	 * <p>
	 * This method can return a timestamp value for fields like these examples:
	 * </p>
	 *
	 * <pre>{@code
	 * {
	 *   "ts":    "1729907680",
	 *   "ts2":   "1729907680.123",
	 *   "delta": 60.000,
	 * }
	 * }</pre>
	 *
	 * @param json
	 *        the JSON node
	 * @param field
	 *        the name of the JSON field to extract the timestamp from
	 * @return the parsed timestamp, or {@literal null}
	 */
	private static Instant parseTimestamp(JsonNode json, String field) {
		JsonNode fieldNode = json.path(field);
		if ( !(fieldNode.isNumber() || fieldNode.isTextual()) ) {
			return null;
		}
		BigDecimal n = null;
		if ( fieldNode.isNumber() ) {
			n = fieldNode.decimalValue();
		} else {
			try {
				n = new BigDecimal(fieldNode.asText());
			} catch ( NumberFormatException e ) {
				// ignore, return null
				return null;
			}
		}
		BigInteger secs = NumberUtils.wholePartToInteger(n);
		BigInteger nanos = NumberUtils.fractionalPartScaledToInteger(n, 9);
		return Instant.ofEpochSecond(secs.longValue(), nanos.longValue());
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private static List<GeneralDatum> parseDatum(JsonNode json,
			CloudDatumStreamConfiguration datumStream, Map<String, List<ValueRef>> refsByRegisterName) {
		if ( json == null ) {
			return Collections.emptyList();
		}
		List<GeneralDatum> result = new ArrayList<>(32);
		/*- EXAMPLE JSON:
		{
		    "ts": "1729879790",
		    "registers": [
		        {
		            "name": "use",
		            "type": "P",
		            "idx": 0,
		            "rate": 133455
		        },
		        {
		            "name": "gen",
		            "type": "P",
		            "idx": 1,
		            "rate": 0
		        },
		    ],
		    "ranges": [
		        {
		            "ts": "1729879620",
		            "delta": 60,
		            "rows": [
		                [
		                    "10044218005705",
		                    "2268120634",
		                ],
		                [
		                    "-7441499",
		                    "0",
		                ],
		 */

		JsonNode regsNode = json.findPath("registers");
		if ( !regsNode.isArray() ) {
			return result;
		}
		String[] regNames = new String[regsNode.size()];

		for ( int i = 0, len = regsNode.size(); i < len; i++ ) {
			JsonNode regNode = regsNode.get(i);
			regNames[i] = regNode.path("name").asText();
		}

		for ( JsonNode rangeNode : json.findPath("ranges") ) {
			Instant ts = parseTimestamp(rangeNode, "ts");
			if ( ts == null ) {
				continue;
			}
			// parse delta as an Instant just to support fractional seconds
			Instant deltaTs = parseTimestamp(rangeNode, "delta");
			if ( deltaTs == null ) {
				continue;
			}
			BigDecimal deltaSecs = BigDecimal.valueOf(deltaTs.getEpochSecond())
					.add(BigDecimal.valueOf(deltaTs.getNano(), 9));
			Duration deltaDur = Duration.ofSeconds(deltaTs.getEpochSecond(), deltaTs.getNano());

			// iterate up to n-1 of rows, as we calculate differences between n, n+1
			JsonNode rowsNode = rangeNode.path("rows");
			int rowCount = rowsNode.size();
			for ( int rowIdx = 0, maxRowIdx = rowCount - 1; rowIdx < maxRowIdx; rowIdx++ ) {
				JsonNode rowNode = rowsNode.get(rowIdx);
				DatumSamples samples = new DatumSamples();
				ts = ts.minus(deltaDur); // datum timestamp will be start of delta period
				for ( int i = 0, len = rowNode.size(); i < len && i < regNames.length; i++ ) {
					String regName = regNames[i];
					List<ValueRef> refs = refsByRegisterName.get(regName);
					if ( refs == null ) {
						continue;
					}
					String val = nonEmptyString(rowNode.get(i).asText());
					String nextVal = nonEmptyString(rowsNode.get(rowIdx + 1).path(i).asText());
					if ( val == null || nextVal == null ) {
						continue;
					}
					BigDecimal n = new BigDecimal(val);
					BigDecimal n1 = new BigDecimal(nextVal);
					for ( ValueRef ref : refs ) {
						EgaugeTypeCode type = ref.registerType;
						CloudDatumStreamPropertyConfiguration property = ref.property;
						BigDecimal datumVal = null;
						if ( property.getPropertyType() == DatumSamplesType.Accumulating ) {
							// take just the older value as the meter reading, converting from X/s to X/h
							datumVal = n1.multiply(type.getQuantum()).divide(SECS_PER_HOUR,
									RoundingMode.DOWN);
						} else {
							// calculate the average over the time difference
							datumVal = n.subtract(n1).multiply(type.getQuantum()).divide(deltaSecs,
									RoundingMode.DOWN);
						}
						samples.putSampleValue(property.getPropertyType(), property.getPropertyName(),
								property.applyValueTransforms(datumVal));
					}
				}
				if ( !samples.isEmpty() ) {
					result.add(new GeneralDatum(new DatumId(datumStream.getKind(),
							datumStream.getObjectId(), datumStream.getSourceId(), ts), samples));
				}
			}
		}

		return result;
	}

	/**
	 * Get the cache to use for device register information.
	 *
	 * @return the cache
	 */
	public final Cache<String, CloudDataValue[]> getDeviceRegistersCache() {
		return deviceRegistersCache;
	}

	/**
	 * Set the cache to use for device register information.
	 *
	 * @param deviceRegistersCache
	 *        the cache to set
	 */
	public final void setDeviceRegistersCache(Cache<String, CloudDataValue[]> deviceRegistersCache) {
		this.deviceRegistersCache = deviceRegistersCache;
	}

}
