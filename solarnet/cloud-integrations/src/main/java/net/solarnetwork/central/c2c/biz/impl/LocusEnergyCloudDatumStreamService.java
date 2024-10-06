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

import static net.solarnetwork.central.c2c.domain.CloudDataValue.COUNTRY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_MODEL_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.LOCALITY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.MANUFACTURER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STATE_PROVINCE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STREET_ADDRESS_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.TIME_ZONE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.UNIT_OF_MEASURE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.http.OAuth2Utils.addOAuthBearerAuthorization;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.MultiValueSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;

/**
 * Locus Energy implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.0
 */
public class LocusEnergyCloudDatumStreamService extends
		BaseSettingsSpecifierLocalizedServiceInfoProvider<String> implements CloudDatumStreamService {

	/** The data value filter key for a site ID. */
	public static final String SITE_ID_FILTER = "siteId";

	/** The data value filter key for a component ID. */
	private static final String COMPONENT_ID_FILTER = "componentId";

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.locus";

	/** The setting for granularity. */
	public static final String GRANULARITY_SETTING = "granularity";

	private static final List<SettingSpecifier> SETTINGS;
	static {
		var settings = new ArrayList<SettingSpecifier>(1);

		// @formatter:off
		// menu for granularity
		var granularitySpec = new BasicMultiValueSettingSpecifier(
				GRANULARITY_SETTING, "latest");
		var granularityTitles = new LinkedHashMap<String, String>(2);
		for (String g : new String[] {
				"latest",
		        "1min",
		        "5min",
		        "15min",
		        "hourly",
		        "daily",
		        "monthly",
		        "yearly"}) {
			granularityTitles.put(g, g);
		}
		granularitySpec.setValueTitles(granularityTitles);
		settings.add(granularitySpec);
		// @formatter:on

		SETTINGS = Collections.unmodifiableList(settings);
	}

	private final UserEventAppenderBiz userEventAppenderBiz;
	private final RestOperations restOps;
	private final OAuth2AuthorizedClientManager oauthClientManager;
	private final CloudIntegrationConfigurationDao integrationDao;
	private final CloudDatumStreamConfigurationDao datumStreamDao;

	/**
	 * Constructor.
	 *
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param restOps
	 *        the REST operations
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @param integrationDao
	 *        the integration DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LocusEnergyCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			RestOperations restOps, OAuth2AuthorizedClientManager oauthClientManager,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao) {
		super(SERVICE_IDENTIFIER);
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.restOps = requireNonNullArgument(restOps, "restOps");
		this.oauthClientManager = requireNonNullArgument(oauthClientManager, "oauthClientManager");
		this.integrationDao = requireNonNullArgument(integrationDao, "integrationDao");
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
	}

	@Override
	public String getDisplayName() {
		return "Locus Energy Datum Stream Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return SETTINGS;
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
		final CloudIntegrationConfiguration integration = integrationDao
				.get(new UserLongCompositePK(datumStream.getUserId(), datumStream.getIntegrationId()));
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

	private <T> T httpGet(CloudIntegrationConfiguration integration,
			Function<HttpEntity<Void>, URI> setup, Function<ResponseEntity<ObjectNode>, T> handler) {
		HttpHeaders headers = new HttpHeaders();
		addOAuthBearerAuthorization(integration, headers, oauthClientManager, userEventAppenderBiz);
		final HttpEntity<Void> req = new HttpEntity<>(null, headers);
		final URI uri = setup.apply(req);
		try {
			final ResponseEntity<ObjectNode> res = restOps.exchange(uri, HttpMethod.GET, req,
					ObjectNode.class);
			return handler.apply(res);
		} catch ( HttpClientErrorException e ) {
			// TODO auth error, try refresh token
			throw e;
		}
	}

	private List<CloudDataValue> sitesForPartner(CloudIntegrationConfiguration integration) {
		return httpGet(integration,
				(req) -> UriComponentsBuilder.fromUri(LocusEnergyCloudIntegrationService.BASE_URI)
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
			if ( siteNode.hasNonNull("countryCode") ) {
				meta.put(COUNTRY_METADATA, siteNode.path("countryCode").asText().trim());
			}
			if ( siteNode.hasNonNull("locationTimezone") ) {
				meta.put(TIME_ZONE_METADATA, siteNode.path("locationTimezone").asText().trim());
			}
			result.add(dataValue(List.of(id), name, meta.isEmpty() ? null : meta));
		}
		return result;
	}

	private List<CloudDataValue> componentsForSite(CloudIntegrationConfiguration integration,
			Map<String, ?> filters) {
		return httpGet(integration,
				(req) -> UriComponentsBuilder.fromUri(LocusEnergyCloudIntegrationService.BASE_URI)
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
			result.add(dataValue(List.of(siteId, id), name, meta.isEmpty() ? null : meta));
		}
		return result;
	}

	private List<CloudDataValue> nodesForComponent(CloudIntegrationConfiguration integration,
			Map<String, ?> filters) {
		return httpGet(integration,
				(req) -> UriComponentsBuilder.fromUri(LocusEnergyCloudIntegrationService.BASE_URI)
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
					final String aggName = name + ' ' + agg;
					final var aggMeta = new LinkedHashMap<String, Object>(meta);
					aggMeta.put("aggregation", agg);
					children.add(dataValue(List.of(siteId, compId, id, aggId), aggName,
							aggMeta.isEmpty() ? null : aggMeta));
				}
			}
			result.add(dataValue(List.of(siteId, compId, id), name, meta.isEmpty() ? null : meta,
					children.isEmpty() ? null : children));
		}
		return result;
	}

}
