/* ==================================================================
 * SolarEdgeCloudDatumStreamService.java - 7/10/2024 7:03:25 am
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
import static net.solarnetwork.central.c2c.domain.CloudDataValue.LOCALITY_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.POSTAL_CODE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STATE_PROVINCE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.STREET_ADDRESS_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;

/**
 * SolarEdge implementation of {@link CloudDatumStreamService}.
 *
 * @author matt
 * @version 1.1
 */
public class SolarEdgeCloudDatumStreamService extends BaseRestOperationsCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.solaredge";

	/** The data value filter key for a site ID. */
	public static final String SITE_ID_FILTER = "siteId";

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
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SolarEdgeCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps) {
		super(SERVICE_IDENTIFIER, "SolarEdge Datum Stream Service", userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, Collections.emptyList(),
				new SolarEdgeRestOperationsHelper(
						LoggerFactory.getLogger(SolarEdgeCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> SolarEdgeCloudIntegrationService.SECURE_SETTINGS));
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
			// TODO
		} else {
			result = sites(integration);
		}
		Collections.sort(result);
		return result;
	}

	private List<CloudDataValue> sites(CloudIntegrationConfiguration integration) {
		return restOpsHelper.httpGet("List sites", integration, ObjectNode.class,
				(req) -> UriComponentsBuilder.fromUri(SolarEdgeCloudIntegrationService.BASE_URI)
						.path(SolarEdgeCloudIntegrationService.V2_SITES_LIST_URL).buildAndExpand()
						.toUri(),
				res -> parseSites(res.getBody()));
	}

	private static List<CloudDataValue> parseSites(ObjectNode json) {
		assert json != null;
		/*- EXAMPLE JSON:
		[
		  {
		    "siteId": 93082,
		    "name": "Smith, John CRM1234",
		    "peakPower": 6.14,
		    "installationDate": "2022-11-10",
		    "location": {
		      "address": "2888 Main St",
		      "city": "Green Bay",
		      "state": "Wisconsin",
		      "zip": "54311",
		      "country": "United States"
		    },
		    "activationStatus": "Active",
		    "note": "Created via API, triggered from CRM"
		  }
		]
		*/
		List<CloudDataValue> result = new ArrayList<>(4);
		for ( JsonNode siteNode : json ) {
			final String id = siteNode.path("siteId").asText();
			final String name = siteNode.path("name").asText().trim();
			final var meta = new LinkedHashMap<String, Object>(4);
			final JsonNode locNode = siteNode.path("location");
			if ( locNode.hasNonNull("address") ) {
				meta.put(STREET_ADDRESS_METADATA, locNode.path("address").asText().trim());
			}
			if ( locNode.hasNonNull("city") ) {
				meta.put(LOCALITY_METADATA, locNode.path("city").asText().trim());
			}
			if ( locNode.hasNonNull("state") ) {
				meta.put(STATE_PROVINCE_METADATA, locNode.path("state").asText().trim());
			}
			if ( locNode.hasNonNull("country") ) {
				meta.put(COUNTRY_METADATA, locNode.path("country").asText().trim());
			}
			if ( locNode.hasNonNull("zip") ) {
				meta.put(POSTAL_CODE_METADATA, locNode.path("zip").asText().trim());
			}
			if ( siteNode.hasNonNull("activationStatus") ) {
				meta.put("activationStatus", siteNode.path("activationStatus").asText().trim());
			}
			if ( siteNode.hasNonNull("note") ) {
				meta.put("note", siteNode.path("note").asText().trim());
			}
			result.add(dataValue(List.of(id), name, meta.isEmpty() ? null : meta));
		}
		return result;
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");
		// TODO
		return null;
	}

	@Override
	public CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter) {
		requireNonNullArgument(datumStream, "datumStream");
		requireNonNullArgument(filter, "filter");
		// TODO Auto-generated method stub
		return null;
	}

}
