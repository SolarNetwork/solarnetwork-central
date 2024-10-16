/* ==================================================================
 * LocusEnergyCloudDatumStreamService.java - 30/09/2024 8:13:21 am
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
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Collections.unmodifiableMap;
import static net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudIntegrationService.V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.COUNTRY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_MODEL_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.LOCALITY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.MANUFACTURER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.POSTAL_CODE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STATE_PROVINCE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STREET_ADDRESS_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.TIME_ZONE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.UNIT_OF_MEASURE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.NumberUtils.narrow;
import static net.solarnetwork.util.NumberUtils.parseNumber;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.util.StringUtils.collectionToCommaDelimitedString;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import net.solarnetwork.central.c2c.http.OAuth2RestOperationsHelper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.MultiValueSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;

/**
 * Locus Energy implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.1
 */
public class LocusEnergyCloudDatumStreamService extends BaseOAuth2ClientCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.locus";

	/** The data value filter key for a site ID. */
	public static final String SITE_ID_FILTER = "siteId";

	/** The data value filter key for a component ID. */
	private static final String COMPONENT_ID_FILTER = "componentId";

	/** The setting for granularity. */
	public static final String GRANULARITY_SETTING = "granularity";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// menu for granularity
		var granularitySpec = new BasicMultiValueSettingSpecifier(GRANULARITY_SETTING,
				LocusEnergyGranularity.Latest.getKey());
		var granularityTitles = unmodifiableMap(Arrays.stream(LocusEnergyGranularity.values())
				.collect(Collectors.toMap(LocusEnergyGranularity::getKey, LocusEnergyGranularity::getKey,
						(l, r) -> r,
						() -> new LinkedHashMap<>(LocusEnergyGranularity.values().length))));
		granularitySpec.setValueTitles(granularityTitles);

		SETTINGS = List.of(granularitySpec);
	}

	private AsyncTaskExecutor executor;

	/**
	 * Constructor.
	 *
	 * @param executor
	 *        an executor
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
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LocusEnergyCloudDatumStreamService(AsyncTaskExecutor executor,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager) {
		super(SERVICE_IDENTIFIER, "Locus Energy Datum Stream Service", userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new OAuth2RestOperationsHelper(
						LoggerFactory.getLogger(LocusEnergyCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> LocusEnergyCloudIntegrationService.SECURE_SETTINGS,
						oauthClientManager),
				oauthClientManager);
		this.executor = requireNonNullArgument(executor, "executor");
	}

	@Override
	protected void populateInfoMessages(Locale locale, SettingSpecifier spec, Map<String, String> msgs,
			MessageSource ms) {
		super.populateInfoMessages(locale, spec, msgs, ms);
		if ( spec instanceof KeyedSettingSpecifier<?> k ) {
			if ( GRANULARITY_SETTING.equals(k.getKey()) ) {
				MultiValueSettingSpecifier mv = (MultiValueSettingSpecifier) spec;
				for ( String valueKey : mv.getValueTitles().keySet() ) {
					String titleKey = "granularity." + valueKey;
					msgs.put(titleKey, ms.getMessage(titleKey, null, valueKey, locale));
				}
			}
		}
	}

	@Override
	public Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale) {
		MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");
		List<LocalizedServiceInfo> result = new ArrayList<>(2);
		for ( String key : new String[] { SITE_ID_FILTER, COMPONENT_ID_FILTER } ) {
			result.add(new BasicLocalizedServiceInfo(key, locale,
					ms.getMessage("dataValueFilter.%s.key".formatted(key), null, key, locale),
					ms.getMessage("dataValueFilter.%s.desc".formatted(key), null, null, locale), null));
		}
		return result;
	}

	@Override
	public Iterable<CloudDataValue> dataValues(UserLongCompositePK id, Map<String, ?> filters) {
		final CloudDatumStreamConfiguration datumStream = requireNonNullObject(
				datumStreamDao.get(requireNonNullArgument(id, "id")), "datumStream");
		final CloudDatumStreamMappingConfiguration mapping = requireNonNullObject(
				datumStreamMappingDao.get(new UserLongCompositePK(datumStream.getUserId(),
						datumStream.getDatumStreamMappingId())),
				"datumStreamMapping");
		final CloudIntegrationConfiguration integration = integrationDao
				.get(new UserLongCompositePK(datumStream.getUserId(), mapping.getIntegrationId()));
		List<CloudDataValue> result = Collections.emptyList();
		if ( filters != null && filters.get(SITE_ID_FILTER) != null
				&& filters.get(COMPONENT_ID_FILTER) != null ) {
			result = nodesForComponent(integration, filters);
		} else if ( filters != null && filters.get(SITE_ID_FILTER) != null ) {
			result = componentsForSite(integration, filters);
		} else {
			result = sitesForPartner(integration);
		}
		Collections.sort(result);
		return result;
	}

	private List<CloudDataValue> sitesForPartner(CloudIntegrationConfiguration integration) {
		return restOpsHelper.httpGet("List sites", integration, ObjectNode.class,
				(req) -> UriComponentsBuilder.fromUri(BASE_URI)
						.path(LocusEnergyCloudIntegrationService.V3_SITES_FOR_PARTNER_ID_URL_TEMPLATE)
						.buildAndExpand(integration.getServiceProperties()).toUri(),
				res -> parseSites(res.getBody()));
	}

	private static List<CloudDataValue> parseSites(ObjectNode json) {
		assert json != null;
		/*- EXAMPLE JSON:
		{
		  "statusCode": 200,
		  "sites": [
		    {
		      "id": 123456,
		      "countryCode": "US",
		      "locale1": "California",
		      "localeCode1": "CA",
		      "locale3": "Los Angeles ",
		      "postalCode": "90210",
		      "address1": "123 Main Street",
		      "latitude": 33.0,
		      "longitude": -111.0,
		      "clientId": 234567,
		      "name": "Example Site",
		      "locationTimezone": "America/Los_Angeles"
		    }
		  ]
		}
		*/
		List<CloudDataValue> result = new ArrayList<>(4);
		for ( JsonNode siteNode : json.path("sites") ) {
			final String id = siteNode.path("id").asText();
			final String name = siteNode.path("name").asText().trim();
			final var meta = new LinkedHashMap<String, Object>(4);
			if ( siteNode.hasNonNull("address1") ) {
				meta.put(STREET_ADDRESS_METADATA, siteNode.path("address1").asText().trim());
			}
			if ( siteNode.hasNonNull("locale3") ) {
				meta.put(LOCALITY_METADATA, siteNode.path("locale3").asText().trim());
			}
			if ( siteNode.hasNonNull("locale1") ) {
				meta.put(STATE_PROVINCE_METADATA, siteNode.path("locale1").asText().trim());
			}
			if ( siteNode.hasNonNull("postalCode") ) {
				meta.put(POSTAL_CODE_METADATA, siteNode.path("postalCode").asText().trim());
			}
			if ( siteNode.hasNonNull("countryCode") ) {
				meta.put(COUNTRY_METADATA, siteNode.path("countryCode").asText().trim());
			}
			if ( siteNode.hasNonNull("locationTimezone") ) {
				meta.put(TIME_ZONE_METADATA, siteNode.path("locationTimezone").asText().trim());
			}
			result.add(intermediateDataValue(List.of(id), name, meta.isEmpty() ? null : meta));
		}
		return result;
	}

	private List<CloudDataValue> componentsForSite(CloudIntegrationConfiguration integration,
			Map<String, ?> filters) {
		return restOpsHelper.httpGet("List components for site", integration, ObjectNode.class,
				(req) -> UriComponentsBuilder.fromUri(BASE_URI)
						.path(LocusEnergyCloudIntegrationService.V3_COMPONENTS_FOR_SITE_ID_URL_TEMPLATE)
						.buildAndExpand(filters).toUri(),
				res -> parseComponents(res.getBody()));
	}

	private static List<CloudDataValue> parseComponents(ObjectNode json) {
		assert json != null;
		/*- EXAMPLE JSON:
		{
		  "statusCode": 200,
		  "components": [
		    {
		      "id": 123456,
		      "siteId": 234567,
		      "clientId": 345678,
		      "parentId": 456789,
		      "parentType": "SITE",
		      "nodeId": "AA.BBBBBBBBBB.11",
		      "name": "Something",
		      "nodeType": "METER",
		      "application": "GENERATION",
		      "generationType": "SOLAR",
		      "oem": "Manufacturer",
		      "model": "Device Model",
		      "isConceptualNode": false
		    }
		  ]
		}
		*/
		List<CloudDataValue> result = new ArrayList<>(32);
		for ( JsonNode compNode : json.path("components") ) {
			final String id = compNode.path("id").asText();
			final String siteId = compNode.path("siteId").asText();
			final String name = compNode.path("name").asText().trim();
			final var meta = new LinkedHashMap<String, Object>(8);
			if ( compNode.hasNonNull("oem") ) {
				meta.put(MANUFACTURER_METADATA, compNode.path("oem").asText().trim());
			}
			if ( compNode.hasNonNull("model") ) {
				meta.put(DEVICE_MODEL_METADATA, compNode.path("model").asText().trim());
			}
			if ( compNode.hasNonNull("nodeId") ) {
				meta.put("nodeId", compNode.path("nodeId").asText().trim());
			}
			if ( compNode.hasNonNull("nodeType") ) {
				meta.put("nodeType", compNode.path("nodeType").asText().trim());
			}
			if ( compNode.hasNonNull("application") ) {
				meta.put("application", compNode.path("application").asText().trim());
			}
			if ( compNode.hasNonNull("generationType") ) {
				meta.put("generationType", compNode.path("generationType").asText().trim());
			}
			result.add(intermediateDataValue(List.of(siteId, id), name, meta.isEmpty() ? null : meta));
		}
		return result;
	}

	private List<CloudDataValue> nodesForComponent(CloudIntegrationConfiguration integration,
			Map<String, ?> filters) {
		return restOpsHelper.httpGet("List fields for component", integration, ObjectNode.class,
				(req) -> UriComponentsBuilder.fromUri(BASE_URI)
						.path(LocusEnergyCloudIntegrationService.V3_NODES_FOR_COMPOENNT_ID_URL_TEMPLATE)
						.buildAndExpand(filters).toUri(),
				res -> parseNodes(res.getBody(), filters));
	}

	private static List<CloudDataValue> parseNodes(ObjectNode json, Map<String, ?> filters) {
		assert json != null;
		assert filters != null && filters.containsKey(SITE_ID_FILTER)
				&& filters.containsKey(COMPONENT_ID_FILTER);
		/*- EXAMPLE JSON:
		{
		  "statusCode": 200,
		  "baseFields": [
		    {
		      "baseField": "AphA",
		      "longName": "AC Phase A Current",
		      "source": "Measured",
		      "unit": "A",
		      "aggregations": [
		        {
		          "shortName": "AphA_avg",
		          "aggregation": "avg"
		        },
		        {
		          "shortName": "AphA_max",
		          "aggregation": "max"
		        },
		        {
		          "shortName": "AphA_min",
		          "aggregation": "min"
		        }
		      ],
		      "granularities": [
		        "latest",
		        "1min",
		        "5min",
		        "15min",
		        "hourly",
		        "daily",
		        "monthly",
		        "yearly"
		      ]
		    },
		  ]
		}
		*/
		final var siteId = filters.get(SITE_ID_FILTER).toString();
		final var compId = filters.get(COMPONENT_ID_FILTER).toString();
		List<CloudDataValue> result = new ArrayList<>(32);
		for ( JsonNode fieldNode : json.path("baseFields") ) {
			final String id = fieldNode.path("baseField").asText().trim();
			final String name = fieldNode.path("longName").asText().trim();
			final var meta = new LinkedHashMap<String, Object>(4);
			if ( fieldNode.hasNonNull("source") ) {
				meta.put("source", fieldNode.path("source").asText().trim());
			}
			if ( fieldNode.hasNonNull("unit") ) {
				meta.put(UNIT_OF_MEASURE_METADATA, fieldNode.path("unit").asText().trim());
			}
			if ( fieldNode.hasNonNull("model") ) {
				meta.put(DEVICE_MODEL_METADATA, fieldNode.path("model").asText().trim());
			}
			List<CloudDataValue> children = new ArrayList<>(4);
			for ( JsonNode aggNode : fieldNode.path("aggregations") ) {
				if ( aggNode.hasNonNull("shortName") && aggNode.hasNonNull("aggregation") ) {
					final String aggId = aggNode.path("shortName").asText().trim();
					final String agg = aggNode.path("aggregation").asText().trim();
					if ( agg.isEmpty() ) {
						continue;
					}
					final String aggName = name + ' ' + agg;
					final var aggMeta = Map.of("aggregation", agg);
					children.add(dataValue(List.of(siteId, compId, id, aggId), aggName,
							aggMeta.isEmpty() ? null : aggMeta));
				}
			}
			result.add(dataValue(List.of(siteId, compId, id), name, meta.isEmpty() ? null : meta,
					children.isEmpty() ? null : children));
		}
		return result;
	}

	@Override
	public Datum latestDatum(CloudDatumStreamConfiguration datumStream) {
		final var data = queryForDatum(datumStream, null);
		if ( data.isEmpty() ) {
			return null;
		}
		return data.getResults().getLast();
	}

	@Override
	public CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter) {
		requireNonNullArgument(datumStream, "datumStream");
		requireNonNullArgument(filter, "filter");
		return queryForDatum(datumStream, filter);
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
	 * <li>baseField</li>
	 * <li>field</li>
	 * </ol>
	 */
	private static final Pattern VALUE_REF_PATTERN = Pattern.compile("/(-?\\d+)/(-?\\d+)/([^/]+)/(.+)");

	private LocusEnergyGranularity resolveGranularity(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter) {
		if ( filter == null || !filter.hasDateRange() ) {
			return LocusEnergyGranularity.Latest;
		}
		LocusEnergyGranularity granularity = null;
		try {
			String granSetting = null;
			if ( filter.hasParameterCriteria()
					&& filter.getParameters().get(GRANULARITY_SETTING) instanceof String s ) {
				granSetting = s;
			} else {
				granSetting = datumStream.serviceProperty(GRANULARITY_SETTING, String.class);
			}
			if ( granSetting != null && !granSetting.isEmpty() ) {
				granularity = LocusEnergyGranularity.fromValue(granSetting);
			}
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		return (granularity != null ? granularity : LocusEnergyGranularity.FiveMinute);
	}

	/**
	 * Query for datum.
	 *
	 * @param datumStream
	 *        the stream to query
	 * @param filter
	 *        an optional filter; if not provided then query for the latest
	 *        datum only
	 * @param locale
	 *        the locale for messages
	 * @return the results
	 */
	private CloudDatumStreamQueryResult queryForDatum(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter) {
		requireNonNullArgument(datumStream, "datumStream");

		final MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");

		if ( !datumStream.isFullyConfigured() ) {
			String msg = "Datum stream is not fully configured.";
			Errors errors = new BindException(datumStream, "datumStream");
			errors.reject("error.datumStream.notFullyConfigured", null, msg);
			throw new ValidationException(msg, errors, ms);
		}

		final var mappingId = new UserLongCompositePK(datumStream.getUserId(), requireNonNullArgument(
				datumStream.getDatumStreamMappingId(), "datumStream.datumStreamMappingId"));
		final CloudDatumStreamMappingConfiguration mapping = requireNonNullObject(
				datumStreamMappingDao.get(mappingId), "datumStreamMapping");

		final var integrationId = new UserLongCompositePK(datumStream.getUserId(),
				requireNonNullArgument(mapping.getIntegrationId(), "datumStreamMapping.integrationId"));
		final CloudIntegrationConfiguration integration = requireNonNullObject(
				integrationDao.get(integrationId), "integration");

		final var allProperties = datumStreamPropertyDao.findAll(datumStream.getUserId(),
				mapping.getConfigId(), null);
		final var valueProps = new ArrayList<CloudDatumStreamPropertyConfiguration>(
				allProperties.size());
		final var exprProps = new ArrayList<CloudDatumStreamPropertyConfiguration>(allProperties.size());
		for ( CloudDatumStreamPropertyConfiguration conf : allProperties ) {
			if ( !(conf.isEnabled() && conf.isFullyConfigured()) ) {
				continue;
			}
			if ( conf.getValueType().isExpression() ) {
				exprProps.add(conf);
			} else {
				valueProps.add(conf);
			}
		}
		if ( valueProps.isEmpty() ) {
			String msg = "Datum stream has no properties.";
			Errors errors = new BindException(datumStream, "datumStream");
			errors.reject("error.datumStream.noProperties", null, msg);
			throw new ValidationException(msg, errors, ms);
		}

		final LocusEnergyGranularity granularity = resolveGranularity(datumStream, filter);
		final Instant filterEndDate = (granularity != LocusEnergyGranularity.Latest
				? filter.getEndDate().truncatedTo(ChronoUnit.SECONDS)
				: null);
		final Instant startDate;
		final Instant endDate;
		if ( granularity != LocusEnergyGranularity.Latest ) {
			// add date range
			startDate = filter.getStartDate().truncatedTo(ChronoUnit.SECONDS);

			var end = filterEndDate;
			if ( granularity.getConstraint() != null ) {
				// enforce max time constraint
				var maxEnd = startDate.plus(granularity.getConstraint());
				if ( end.isAfter(maxEnd) ) {
					end = maxEnd;
				}
			}
			endDate = end;
		} else {
			startDate = null;
			endDate = null;
		}

		// group requests by component, field names
		final var fieldNamesByComponent = new LinkedHashMap<String, Set<String>>(8);
		final var fieldNamesByProperty = new LinkedHashMap<UserLongIntegerCompositePK, String>(8);
		for ( CloudDatumStreamPropertyConfiguration config : valueProps ) {
			Matcher m = VALUE_REF_PATTERN.matcher(config.getValueReference());
			if ( !m.matches() ) {
				continue;
			}
			fieldNamesByComponent.computeIfAbsent(m.group(2), k -> new LinkedHashSet<String>(8))
					.add(m.group(4));
			fieldNamesByProperty.put(config.getId(), m.group(4));
		}

		if ( fieldNamesByComponent.isEmpty() ) {
			String msg = "Datum stream has no valid property references.";
			Errors errors = new BindException(datumStream, "datumStream");
			errors.reject("error.datumStream.noProperties", null, msg);
			throw new ValidationException(msg, errors, ms);
		}

		List<List<Map<String, JsonNode>>> data = new ArrayList<>(fieldNamesByComponent.size());
		try {
			List<Future<List<Map<String, JsonNode>>>> futures = new ArrayList<>(
					fieldNamesByComponent.size());
			for ( Entry<String, Set<String>> reqEntry : fieldNamesByComponent.entrySet() ) {
				Set<String> fieldNames = reqEntry.getValue();
				futures.add(executor.submit(() -> {
					ObjectNode json = restOpsHelper.httpGet("Fetch data", integration, ObjectNode.class,
							(headers) -> {
							// @formatter:off
								UriComponentsBuilder b = UriComponentsBuilder.fromUri(BASE_URI)
										.path(V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE)
										.queryParam("gran", granularity.getKey())
										.queryParam("tz", "UTC")
										.queryParam("fields", collectionToCommaDelimitedString(fieldNames))
										;
								// @formatter:on
								if ( granularity != LocusEnergyGranularity.Latest ) {
									// add date range
									b.queryParam("start",
											ISO_LOCAL_DATE_TIME.format(startDate.atOffset(UTC)));
									b.queryParam("end",
											ISO_LOCAL_DATE_TIME.format(endDate.atOffset(UTC)));
								}
								return b.buildAndExpand(Map.of(COMPONENT_ID_FILTER, reqEntry.getKey()))
										.toUri();

							}, (res) -> res.getBody());
					List<Map<String, JsonNode>> datumValuesList = new ArrayList<>();
					for ( JsonNode dataNode : json.path("data") ) {
						if ( dataNode instanceof ObjectNode o && o.has("ts") ) {
							Map<String, JsonNode> datumValues = new LinkedHashMap<>(fieldNames.size());
							for ( Iterator<Entry<String, JsonNode>> itr = o.fields(); itr.hasNext(); ) {
								Entry<String, JsonNode> e = itr.next();
								if ( "ts".equals(e.getKey()) || fieldNames.contains(e.getKey()) ) {
									datumValues.put(e.getKey(), e.getValue());
								}
							}
							// if did not find ts + at least one property, ignore
							if ( datumValues.size() > 1 ) {
								datumValuesList.add(datumValues);
							}
						}
					}
					return datumValuesList;
				}));
			}
			for ( var f : futures ) {
				data.add(f.get());
			}
		} catch ( Exception e ) {
			String msg = "Error requesting data.";
			Throwable t = e;
			while ( t.getCause() != null ) {
				t = t.getCause();
			}
			Errors errors = new BindException(datumStream, "datumStream");
			errors.reject("error.dataRequest", new Object[] { t.getMessage() }, msg);
			throw new ValidationException(msg, errors, ms, t);
		}

		// merge multiple streams based on timestamp
		final Map<Instant, GeneralDatum> result = new HashMap<>(data.size());
		for ( List<Map<String, JsonNode>> datumValuesList : data ) {
			for ( Map<String, JsonNode> datumValues : datumValuesList ) {
				final Instant ts;
				try {
					ts = Instant.parse(datumValues.get("ts").asText());
				} catch ( DateTimeParseException dtpe ) {
					// ignore and continue
					continue;
				}

				final GeneralDatum datum = result.computeIfAbsent(ts,
						k -> new GeneralDatum(datumStream.datumId(k), new DatumSamples()));
				final DatumSamples samples = datum.getSamples();

				for ( CloudDatumStreamPropertyConfiguration property : valueProps ) {
					String fieldName = fieldNamesByProperty.get(property.getId());
					JsonNode val = datumValues.get(fieldName);
					if ( val != null ) {
						DatumSamplesType propType = property.getPropertyType();
						Object propVal = switch (propType) {
							case Accumulating, Instantaneous -> {
								// convert to number
								if ( val.isBigDecimal() ) {
									yield val.decimalValue();
								} else if ( val.isFloat() ) {
									yield val.floatValue();
								} else if ( val.isDouble() ) {
									yield val.doubleValue();
								} else if ( val.isBigInteger() ) {
									yield val.bigIntegerValue();
								} else if ( val.isLong() ) {
									yield val.longValue();
								} else if ( val.isFloat() ) {
									yield val.floatValue();
								} else {
									yield narrow(parseNumber(val.asText(), BigDecimal.class), 2);
								}
							}
							case Status, Tag -> val.asText();
						};
						propVal = property.applyValueTransforms(propVal);
						samples.putSampleValue(propType, property.getPropertyName(), propVal);
					}
				}
			}
		}

		// evaluate expressions on merged datum
		if ( !exprProps.isEmpty() ) {
			for ( GeneralDatum datum : result.values() ) {
				evaulateExpressions(exprProps, datum,
						Map.of("datumStreamMappingId", datumStream.getDatumStreamMappingId(),
								"integrationId", mapping.getIntegrationId()));
			}
		}

		BasicQueryFilter nextQueryFilter = null;
		if ( granularity != LocusEnergyGranularity.Latest && endDate.isBefore(filterEndDate) ) {
			// provide next date range to try
			nextQueryFilter = BasicQueryFilter.copyOf(filter);
			nextQueryFilter.setStartDate(endDate);

			var end = filterEndDate;
			if ( granularity.getConstraint() != null ) {
				// enforce max time constraint
				end = endDate.plus(granularity.getConstraint());
				if ( end.isAfter(filterEndDate) ) {
					end = filterEndDate;
				}
			}
			nextQueryFilter.setEndDate(end);
		}

		return new BasicCloudDatumStreamQueryResult(nextQueryFilter, result.values().stream()
				.sorted(Identity.sortByIdentity()).map(Datum.class::cast).toList());
	}
}
