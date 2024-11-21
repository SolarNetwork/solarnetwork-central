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

import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.intermediateDataValue;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import com.fasterxml.jackson.databind.JsonNode;
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
import net.solarnetwork.central.c2c.http.OAuth2RestOperationsHelper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.settings.SettingSpecifier;

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

	/**
	 * The URI path to list the hardware for a given site.
	 *
	 * <p>
	 * Accepts a single {@code {siteId}} parameter.
	 * </p>
	 */
	public static final String SITE_HARDWARE_URL_TEMPLATE = "/sites/{siteId}/hardware";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {

		SETTINGS = List.of();
	}

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = List.of(SITE_ID_FILTER);

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
	public AlsoEnergyCloudDatumStreamService(AsyncTaskExecutor executor,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager) {
		super(SERVICE_IDENTIFIER, "AlsoEnergy Datum Stream Service", userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS,
				new OAuth2RestOperationsHelper(
						LoggerFactory.getLogger(AlsoEnergyCloudDatumStreamService.class),
						userEventAppenderBiz, restOps, HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> AlsoEnergyCloudIntegrationService.SECURE_SETTINGS,
						oauthClientManager),
				oauthClientManager);
		this.executor = requireNonNullArgument(executor, "executor");
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
		if ( false ) {
			// TODO
		} else if ( filters != null && filters.get(SITE_ID_FILTER) != null ) {
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter) {
		// TODO Auto-generated method stub
		return null;
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
				(req) -> fromUri(resolveBaseUrl(integration, AlsoEnergyCloudIntegrationService.BASE_URI))
						.path(SITE_HARDWARE_URL_TEMPLATE).queryParam("includeArchivedFields", true)
						.buildAndExpand(filters).toUri(),
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
		for ( JsonNode meterNode : json.path("hardware") ) {
			final JsonNode fieldsNode = meterNode.path("fieldsArchived");
			if ( !(fieldsNode.isArray() && fieldsNode.size() > 0) ) {
				continue;
			}
			final String id = meterNode.path("id").asText().trim();
			if ( id.isEmpty() ) {
				continue;
			}
			final String name = meterNode.path("name").asText().trim();
			final var meta = new LinkedHashMap<String, Object>(4);
			populateNonEmptyValue(meterNode, "functionCode", "functionCode", meta);
			for ( JsonNode configNode : meterNode.path("config") ) {
				populateNonEmptyValue(configNode, "serialNumber", DEVICE_SERIAL_NUMBER_METADATA, meta);
				populateNonEmptyValue(configNode, "deviceType", "deviceType", meta);
			}

			List<CloudDataValue> fields = new ArrayList<>(fieldsNode.size());
			for ( JsonNode fieldNode : fieldsNode ) {
				final String fieldName = fieldNode.asText();
				fields.add(dataValue(List.of(siteId, id, fieldName), fieldName));
			}

			result.add(intermediateDataValue(List.of(siteId, id), name, meta, fields));
		}
		return result;
	}

}
