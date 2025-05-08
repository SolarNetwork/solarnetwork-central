/* ==================================================================
 * SolrenViewCloudDatumStreamService.java - 17/10/2024 11:23:21 am
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
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_END_DATE_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_INCLUDE_LIFETIME_ENERGY_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_PATH;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_SITE_ID_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_START_DATE_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_USE_UTC_PARAM;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.LOCALITY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.POSTAL_CODE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STATE_PROVINCE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STREET_ADDRESS_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.TIME_ZONE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.WILDCARD_IDENTIFIER;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity.resolvePlaceholders;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.NumberUtils.narrow;
import static net.solarnetwork.util.NumberUtils.parseNumber;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.client.RestOperations;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
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
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextAreaSettingSpecifier;
import net.solarnetwork.support.XmlSupport;
import net.solarnetwork.util.IntRange;

/**
 * SolrenView implementation of {@link CloudDatumStreamService}.
 *
 * <p>
 * When resolving
 * {@link CloudDatumStreamPropertyConfiguration#getValueReference()} values,
 * placeholder values will be resolved from the
 * {@link CloudIntegrationsConfigurationEntity#PLACEHOLDERS_SERVICE_PROPERTY}
 * service property on the associated {@link CloudDatumStreamConfiguration}
 * entity. This allows the property mappings to be shared, if a {@code {siteId}}
 * placeholder is provided, and a wildcard {@code *} used for the component ID.
 * For example a value reference like:
 * </p>
 *
 * <pre><code>/{siteId}/*{@literal /}W</code></pre>
 *
 * <p>
 * can be used across many datum streams by configuring the service properties
 * of each stream with placeholder values, like:
 * </p>
 *
 * <pre>{@code {
 *    "placeholders": {
 *      "siteId": 123
 *    }
 *  }}</pre>
 *
 * <h2>Mapping source IDs</h2>
 *
 * <p>
 * As the SolrenView API returns data for an entire site at once, datum for
 * multiple source IDs can be generated by including a
 * {@link #SOURCE_ID_MAP_SETTING} service property on the associated
 * {@link CloudDatumStreamConfiguration}. This property can be provided either
 * as a comma-delimited key-value pair string, or as a Map object. The mapping
 * will <b>override</b> the source ID configured on the associated
 * {@link CloudDatumStreamConfiguration}. The keys represent <b>component ID</b>
 * values, with associated <b>source ID</b> values. For example these two
 * configurations are equivalent, mapping two components into two sources:
 * </p>
 *
 * <pre>{@code {
 *    "sourceIdMap": {
 *      "123456": "source/1",
 *      "234567": "source/2"
 *    }
 * }}</pre>
 *
 * <p>
 * or as a delimited string:
 * </p>
 *
 * <pre>{@code {
 *    "sourceIdMap":  "123456=source/1, 234567=source/2"
 * }}</pre>
 *
 * <h2>Default wildcard source IDs</h2>
 *
 * <p>
 * If a wildcard component ID is used in a value reference and no
 * <code>sourceIdMap</code> datum stream property is configured as outlined in
 * the previous section, then all available components will be returned as
 * source IDs derived from the associated
 * {@link CloudDatumStreamConfiguration#getSourceId()} appended with {@code /X}
 * where {@code X} is the 1-based offset of the component as returned by
 * SolrenView. For example of SolrenView returns data for 2 components and the
 * {@link CloudDatumStreamConfiguration} is configured with a source ID value
 * {@code my/device} then these source IDs would be generated:
 * </p>
 *
 * <ul>
 * <li>my/device/1</li>
 * <li>my/device/2</li>
 * </ul>
 *
 * @author matt
 * @version 1.9
 */
public class SolrenViewCloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.solrenview";

	/** The data value filter key for a site ID. */
	public static final String SITE_ID_FILTER = "siteId";

	/** The setting for granularity. */
	public static final String GRANULARITY_SETTING = "granularity";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;

	static {
		// menu for granularity
		var granularitySpec = new BasicMultiValueSettingSpecifier(GRANULARITY_SETTING,
				SolrenViewGranularity.FiveMinute.getKey());
		var granularityTitles = unmodifiableMap(Arrays.stream(SolrenViewGranularity.values())
				.collect(Collectors.toMap(SolrenViewGranularity::getKey, SolrenViewGranularity::getKey,
						(l, r) -> r, () -> new LinkedHashMap<>(SolrenViewGranularity.values().length))));
		granularitySpec.setValueTitles(granularityTitles);

		var sourceIdMapSpec = new BasicTextAreaSettingSpecifier(SOURCE_ID_MAP_SETTING, null, true);

		SETTINGS = List.of(granularitySpec, sourceIdMapSpec);
	}

	/**
	 * The supported placeholder keys.
	 *
	 * @since 1.6
	 */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SITE_ID_FILTER);

	/**
	 * The supported data value wildcard levels.
	 *
	 * @since 1.6
	 */
	public static final List<Integer> SUPPORTED_DATA_VALUE_WILDCARD_LEVELS = List.of(1);

	/**
	 * The data value identifier levels source ID range.
	 *
	 * @since 1.7
	 */
	public static final IntRange DATA_VALUE_IDENTIFIER_LEVELS_SOURCE_ID_RANGE = IntRange.rangeOf(1);

	private static final XmlSupport XML_SUPPORT;
	private static final XPathExpression M_COMPONENTS_XPATH;

	static {
		XML_SUPPORT = new XmlSupport();
		XML_SUPPORT.getDocBuilderFactory();
		try {
			M_COMPONENTS_XPATH = XML_SUPPORT.getXPathExpression("//m");
		} catch ( XPathExpressionException e ) {
			throw new IllegalStateException(e);
		}
	}

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
	public SolrenViewCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			Clock clock) {
		super(SERVICE_IDENTIFIER, "SolrenView Datum Stream Service", clock, userEventAppenderBiz,
				encryptor, expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new RestOperationsHelper(
						LoggerFactory.getLogger(SolrenViewCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> SolrenViewCloudIntegrationService.SECURE_SETTINGS));
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
			result = componentsForSite(integration, filters);
		}
		Collections.sort(result);
		return result;
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		final SolrenViewGranularity granularity = resolveGranularity(datumStream, null);
		final Clock queryClock = Clock.tick(clock, granularity.getTickDuration());
		final Instant endDate = queryEndDate(queryClock, granularity);
		final Instant startDate = queryStartDate(endDate, granularity);

		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);

		final var result = datum(datumStream, filter);
		if ( result == null ) {
			return Collections.emptyList();
		}
		return result.getResults();
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
	 * <li>componentId</li>
	 * <li>field</li>
	 * </ol>
	 */
	private static final Pattern VALUE_REF_PATTERN = Pattern.compile("/(-?\\d+)/([^/]+)/(.+)");

	private static record ValueRef(Object siteId, String componentId, String fieldName,
			CloudDatumStreamPropertyConfiguration property) {

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

			final SolrenViewGranularity granularity = resolveGranularity(ds, null);

			Instant startDate = Clock
					.tick(Clock.fixed(filter.getStartDate(), UTC), granularity.getTickDuration())
					.instant();
			Instant endDate = Clock
					.tick(Clock.fixed(filter.getEndDate(), UTC), granularity.getTickDuration())
					.instant();

			if ( granularity == SolrenViewGranularity.Month ) {
				startDate = startDate.with(TemporalAdjusters.firstDayOfMonth());
				endDate = endDate.with(TemporalAdjusters.firstDayOfMonth());
			} else if ( granularity == SolrenViewGranularity.Year ) {
				startDate = startDate.with(TemporalAdjusters.firstDayOfYear());
				endDate = endDate.with(TemporalAdjusters.firstDayOfYear());
			}

			// group requests by site
			final var refsBySiteComponent = new LinkedHashMap<Long, Map<String, List<ValueRef>>>(2);

			for ( CloudDatumStreamPropertyConfiguration config : valueProps ) {
				String ref = resolvePlaceholders(config.getValueReference(), ds);
				Matcher m = VALUE_REF_PATTERN.matcher(ref);
				if ( !m.matches() ) {
					continue;
				}
				// groups: 1 = siteId, 2 = componentId, 3 = field
				Long siteId = Long.valueOf(m.group(1));
				String componentId = m.group(2);
				String fieldName = m.group(3);
				refsBySiteComponent.computeIfAbsent(siteId, k -> new LinkedHashMap<>(8))
						.computeIfAbsent(componentId, k -> new ArrayList<>(8))
						.add(new ValueRef(siteId, componentId, fieldName, config));
			}

			if ( refsBySiteComponent.isEmpty() ) {
				String msg = "Datum stream has no valid property references.";
				Errors errors = new BindException(ds, "datumStream");
				errors.reject("error.datumStream.noProperties", null, msg);
				throw new ValidationException(msg, errors, ms);
			}

			final Map<Instant, Map<String, GeneralDatum>> datum = new TreeMap<>();
			final BasicQueryFilter usedQueryFilter = new BasicQueryFilter();
			usedQueryFilter.setStartDate(startDate);

			while ( startDate.isBefore(endDate) ) {
				final var periodStartDate = startDate;
				final var periodEndDate = nextDate(periodStartDate, granularity);
				usedQueryFilter.setEndDate(periodEndDate);
				for ( Entry<Long, Map<String, List<ValueRef>>> e : refsBySiteComponent.entrySet() ) {
					final Long siteId = e.getKey();
					final Map<String, List<ValueRef>> refsByComponent = e.getValue();
					restOpsHelper.httpGet("Query for site", integration, String.class, (headers) -> {
						headers.setAccept(Collections.singletonList(MediaType.TEXT_XML));
						// @formatter:off
						return  fromUri(resolveBaseUrl(integration,BASE_URI))
								.path(XML_FEED_PATH)
								.queryParam(XML_FEED_USE_UTC_PARAM)
								.queryParam(XML_FEED_INCLUDE_LIFETIME_ENERGY_PARAM)
								.queryParam(XML_FEED_SITE_ID_PARAM, "{siteId}")
								.queryParam(XML_FEED_START_DATE_PARAM, "{startDate}")
								.queryParam(XML_FEED_END_DATE_PARAM, "{endDate}")
								.buildAndExpand(siteId, periodStartDate, periodEndDate)
								.toUri();
						// @formatter:on
					}, res -> parseDatum(ds, res.getBody(), periodStartDate, datum, refsByComponent));
				}
				startDate = periodEndDate;
			}

			// evaluate expressions on merged datum
			if ( !exprProps.isEmpty() ) {
				var allDatum = datum.values().stream().flatMap(e -> e.values().stream()).toList();
				evaluateExpressions(datumStream, usedQueryFilter, exprProps, allDatum,
						ds.getDatumStreamMappingId(), mapping.getIntegrationId());
			}

			return new BasicCloudDatumStreamQueryResult(usedQueryFilter, null, datum.values().stream()
					.flatMap(m -> m.values().stream()).map(Datum.class::cast).toList());
		});
	}

	private Instant nextDate(Instant date, SolrenViewGranularity granularity) {
		return switch (granularity) {
			case Year -> date.with(TemporalAdjusters.firstDayOfNextYear());
			case Month -> date.with(TemporalAdjusters.firstDayOfNextMonth());
			default -> date.plus(granularity.getTickDuration());
		};
	}

	private List<CloudDataValue> componentsForSite(CloudIntegrationConfiguration integration,
			Map<String, ?> filters) {
		final SolrenViewGranularity granularity = resolveGranularity(null, filters);
		final Clock queryClock = Clock.tick(clock, granularity.getTickDuration());
		final Instant endDate = queryEndDate(queryClock, granularity);
		final Instant startDate = queryStartDate(endDate, granularity);
		return restOpsHelper.httpGet("Query for site", integration, String.class, (headers) -> {
			headers.setAccept(Collections.singletonList(MediaType.TEXT_XML));
			// @formatter:off
			return  fromUri(resolveBaseUrl(integration,BASE_URI))
					.path(XML_FEED_PATH)
					.queryParam(XML_FEED_USE_UTC_PARAM)
					.queryParam(XML_FEED_INCLUDE_LIFETIME_ENERGY_PARAM)
					.queryParam(XML_FEED_SITE_ID_PARAM, "{siteId}")
					.queryParam(XML_FEED_START_DATE_PARAM, "{startDate}")
					.queryParam(XML_FEED_END_DATE_PARAM, "{endDate}")
					.buildAndExpand(filters.get(SITE_ID_FILTER), startDate, endDate)
					.toUri();
			// @formatter:on
		}, res -> parseComponents(filters.get(SITE_ID_FILTER), res.getBody()));
	}

	private Instant queryEndDate(Clock queryClock, SolrenViewGranularity granularity) {
		Instant ts = queryClock.instant(); //
		if ( granularity.getTickDuration().getSeconds() < 86400L
				|| granularity == SolrenViewGranularity.Day ) {
			return ts;
		}
		if ( granularity == SolrenViewGranularity.Month ) {
			return ts.atZone(UTC).with(TemporalAdjusters.firstDayOfMonth()).toInstant();
		}
		// year
		return ts.atZone(UTC).with(TemporalAdjusters.firstDayOfYear()).toInstant();
	}

	private Instant queryStartDate(Instant endDate, SolrenViewGranularity granularity) {
		if ( granularity.getTickDuration().getSeconds() < 86400L
				|| granularity == SolrenViewGranularity.Day ) {
			return endDate.minus(granularity.getTickDuration());
		}
		if ( granularity == SolrenViewGranularity.Month ) {
			return endDate.atZone(UTC).minusMonths(1).toInstant();
		}
		// year
		return endDate.atZone(UTC).minusYears(1).toInstant();
	}

	private SolrenViewGranularity resolveGranularity(CloudDatumStreamConfiguration datumStream,
			Map<String, ?> parameters) {
		SolrenViewGranularity granularity = null;
		try {
			String granSetting = null;
			if ( parameters != null && parameters.get(GRANULARITY_SETTING) instanceof String s ) {
				granSetting = s;
			} else if ( datumStream != null ) {
				granSetting = datumStream.serviceProperty(GRANULARITY_SETTING, String.class);
			}
			if ( granSetting != null && !granSetting.isEmpty() ) {
				granularity = SolrenViewGranularity.fromValue(granSetting);
			}
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		return (granularity != null ? granularity : SolrenViewGranularity.FiveMinute);
	}

	private List<CloudDataValue> parseComponents(Object siteId, String body) {
		/*- example XML:
		<sunSpecPlantExtract t="2024-10-16T22:57:51Z">
		  <plant id="ffffffff-ffff-ffff-ffff-02df51b432cf" v="1" locale="en-US">
		    <name>Example Solar Site - Carport</name>
		    <activationDate>2017-11-17</activationDate>
		    <location>
		      <latitude/>
		      <longitude/>
		      <line1>123 Main Street</line1>
		      <city>Anytown</city>
		      <state>CT</state>
		      <postal>06825</postal>
		      <timezone>-4:00</timezone>
		    </location>
		  </plant>
		  <sunspecData v="1" periodStart="2024-10-16T21:57:49Z" periodEnd="2024-10-16T22:57:49Z">
		    <d lid="FF:FF:FF:10:10:6A" man="solren" mod="254_3.2231" t="2024-10-16T22:27:44Z">
		      <m id="103" sn="123123123">
		        <p id="WH">1000</p>
		        <p id="WHL">517756000</p>
		        <p id="W">390.000</p>
		        <p id="PPVphAB">478</p>
		        <p id="PPVphBC">479</p>
		        <p id="PPVphCA">478</p>
		        <p id="DCV">576</p>
		        <p id="A">2</p>
		      </m>
		      ...
		 */
		final Document dom;
		try {
			dom = XML_SUPPORT.getDocBuilderFactory().newDocumentBuilder()
					.parse(new InputSource(new StringReader(body)));
		} catch ( Exception e ) {
			throw new RemoteServiceException(e.getMessage(), e);
		}

		final Element plantExtract = dom.getDocumentElement();

		final List<CloudDataValue> result = new ArrayList<>(4);
		final List<CloudDataValue> components = new ArrayList<>(8);

		final Node plant = plantExtract.getFirstChild();

		result.add(plantValue(plant, siteId, components));

		Node n = plant;
		Node sunspecData = null;
		while ( sunspecData == null && n != null ) {
			n = n.getNextSibling();
			if ( "sunspecData".equalsIgnoreCase(n.getLocalName()) ) {
				sunspecData = n;
			}
		}

		Node dataNode = null;
		if ( sunspecData != null && sunspecData.hasChildNodes() ) {
			n = sunspecData.getFirstChild();
			while ( dataNode == null && n != null ) {
				if ( "d".equals(n.getLocalName()) ) {
					dataNode = n;
				} else {
					n = n.getNextSibling();
				}
			}
		}

		if ( dataNode != null && dataNode.hasChildNodes() ) {
			NodeList elements = dataNode.getChildNodes();
			for ( int i = 0, len = elements.getLength(); i < len; i++ ) {
				n = elements.item(i);
				if ( "m".equals(n.getLocalName()) && n.hasAttributes() && n.hasChildNodes() ) {
					var component = componentValue(n, siteId);
					if ( component != null ) {
						components.add(component);
					}
				}
			}
		}

		return result;
	}

	private CloudDataValue plantValue(Node plant, Object siteId,
			Collection<CloudDataValue> childrenCollection) {
		final var meta = new LinkedHashMap<String, Object>(4);

		String name = siteId.toString();

		final NodeList elements = (plant != null ? plant.getChildNodes() : null);
		if ( elements != null && elements.getLength() > 0 ) {
			for ( int i = 0, len = elements.getLength(); i < len; i++ ) {
				Node n = elements.item(i);
				if ( "name".equals(n.getLocalName()) && n.hasChildNodes() ) {
					name = n.getTextContent();
				} else if ( "location".equals(n.getLocalName()) && n.hasChildNodes() ) {
					extractPlantLocation(meta, n.getChildNodes());
				}
			}
		}

		return intermediateDataValue(List.of(siteId.toString()), name, meta.isEmpty() ? null : meta,
				childrenCollection);
	}

	private void extractPlantLocation(final LinkedHashMap<String, Object> meta,
			final NodeList locElements) {
		/*- example XML:
		    <location>
		      <latitude/>
		      <longitude/>
		      <line1>123 Main Street</line1>
		      <city>Anytown</city>
		      <state>CT</state>
		      <postal>06825</postal>
		      <timezone>-4:00</timezone>
		    </location>
		 */
		for ( int i = 0, len = locElements.getLength(); i < len; i++ ) {
			Node n = locElements.item(i);
			if ( "line1".equals(n.getLocalName()) && n.hasChildNodes() ) {
				meta.put(STREET_ADDRESS_METADATA, n.getTextContent());
			} else if ( "city".equals(n.getLocalName()) && n.hasChildNodes() ) {
				meta.put(LOCALITY_METADATA, n.getTextContent());
			} else if ( "state".equals(n.getLocalName()) && n.hasChildNodes() ) {
				meta.put(STATE_PROVINCE_METADATA, n.getTextContent());
			} else if ( "postal".equals(n.getLocalName()) && n.hasChildNodes() ) {
				meta.put(POSTAL_CODE_METADATA, n.getTextContent());
			} else if ( "timezone".equals(n.getLocalName()) && n.hasChildNodes() ) {
				meta.put(TIME_ZONE_METADATA, n.getTextContent());
			}
		}
	}

	private CloudDataValue componentValue(Node n, Object siteId) {
		/*- example XML:
		      <m id="103" sn="123123123">
		        <p id="WH">1000</p>
		        <p id="WHL">517756000</p>
		        <p id="W">390.000</p>
		        <p id="PPVphAB">478</p>
		        <p id="PPVphBC">479</p>
		        <p id="PPVphCA">478</p>
		        <p id="DCV">576</p>
		        <p id="A">2</p>
		      </m>
		 */
		String id = n.getAttributes().getNamedItem("sn").getNodeValue();
		if ( id == null || id.isEmpty() ) {
			return null;
		}

		final var propCollection = new ArrayList<CloudDataValue>(8);

		final NodeList props = n.getChildNodes();
		for ( int i = 0, len = props.getLength(); i < len; i++ ) {
			n = props.item(i);
			if ( "p".equals(n.getLocalName()) && n.hasAttributes() && n.hasChildNodes() ) {
				String fieldName = n.getAttributes().getNamedItem("id").getNodeValue();
				propCollection
						.add(dataValue(List.of(siteId.toString(), id, fieldName), fieldName, null));
			}
		}

		return intermediateDataValue(List.of(siteId.toString(), id), id, null, propCollection);
	}

	private Void parseDatum(CloudDatumStreamConfiguration datumStream, String body, Instant ts,
			Map<Instant, Map<String, GeneralDatum>> datumByTimeSource,
			Map<String, List<ValueRef>> refsByComponent) {
		final Document dom;
		try {
			dom = XML_SUPPORT.getDocBuilderFactory().newDocumentBuilder()
					.parse(new InputSource(new StringReader(body)));
		} catch ( Exception e ) {
			throw new RemoteServiceException(e.getMessage(), e);
		}

		final NodeList componentNodes;
		try {
			componentNodes = (NodeList) M_COMPONENTS_XPATH.evaluate(dom, XPathConstants.NODESET);
		} catch ( XPathExpressionException e ) {
			throw new RemoteServiceException(e.getMessage(), e);
		}

		// get optional map of component ID -> source ID
		final Map<String, String> componentSourceIdMapping = componentSourceIdMap(datumStream);

		for ( int i = 0, len = componentNodes.getLength(); i < len; i++ ) {
			Node n = componentNodes.item(i);
			String componentId = n.getAttributes().getNamedItem("sn").getNodeValue();
			if ( componentId == null || componentId.isEmpty() || !n.hasChildNodes()
					|| !(refsByComponent.containsKey(componentId)
							|| refsByComponent.containsKey(WILDCARD_IDENTIFIER)) ) {
				continue;
			}

			String sourceId = componentSourceIdMapping != null
					? componentSourceIdMapping.get(componentId)
					: datumStream.getSourceId() + '/' + (i + 1);
			if ( sourceId == null ) {
				continue;
			}
			GeneralDatum datum = datumByTimeSource.computeIfAbsent(ts, k -> new LinkedHashMap<>(8))
					.compute(sourceId, (s, d) -> {
						if ( d == null ) {
							d = new GeneralDatum(new DatumId(datumStream.getKind(),
									datumStream.getObjectId(), sourceId, ts), new DatumSamples());
						}

						return d;
					});
			parseDatumProperties(n, componentId, datum, refsByComponent);
		}

		return null;
	}

	private Map<String, String> componentSourceIdMap(CloudDatumStreamConfiguration datumStream) {
		return servicePropertyStringMap(datumStream, SOURCE_ID_MAP_SETTING);
	}

	private void parseDatumProperties(Node componentNode, String componentId, GeneralDatum datum,
			Map<String, List<ValueRef>> refsByComponent) {
		assert refsByComponent != null;

		final List<ValueRef> refs = refsByComponent.containsKey(componentId)
				? refsByComponent.get(componentId)
				: refsByComponent.get(CloudDataValue.WILDCARD_IDENTIFIER);
		assert refs != null;

		final Map<String, ValueRef> refsByField = refs.stream()
				.collect(toMap(r -> r.fieldName, identity()));

		NodeList nodeList = componentNode.getChildNodes();
		for ( int i = 0, len = nodeList.getLength(); i < len; i++ ) {
			Node n = nodeList.item(i);
			if ( !("p".equals(n.getLocalName()) && n.hasAttributes() && n.hasChildNodes()) ) {
				continue;
			}

			String fieldName = n.getAttributes().getNamedItem("id").getNodeValue();
			if ( fieldName == null || fieldName.isEmpty() ) {
				continue;
			}

			ValueRef ref = refsByField.get(fieldName);
			if ( ref == null ) {
				continue;
			}

			String fieldValue = n.getTextContent();
			if ( fieldValue == null || fieldValue.isEmpty() ) {
				continue;
			}

			Object propVal = null;
			DatumSamplesType propType = ref.property.getPropertyType();
			if ( propType == DatumSamplesType.Instantaneous
					|| propType == DatumSamplesType.Accumulating ) {
				try {
					Number propNum = narrow(parseNumber(fieldValue, BigDecimal.class), 2);
					propVal = ref.property.applyValueTransforms(propNum);
				} catch ( NumberFormatException e ) {
					// ignore and skip
				}
			} else {
				propVal = fieldValue;
			}

			if ( propVal != null ) {
				datum.getSamples().putSampleValue(propType, ref.property.getPropertyName(), propVal);
			}
		}
	}

}
