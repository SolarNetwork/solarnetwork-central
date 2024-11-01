/* ==================================================================
 * OpenWeatherMapWeatherCloudDatumStreamService.java - 31/10/2024 3:17:06 pm
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

import static net.solarnetwork.central.c2c.biz.impl.OpenWeatherMapCloudIntegrationService.WEATHER_URL_PATH;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DayDatum;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * OpenWeatherMap implementation of {@link CloudDatumStreamService} using the
 * weather API.
 *
 * @author matt
 * @version 1.0
 */
public class OpenWeatherMapWeatherCloudDatumStreamService
		extends BaseOpenWeatherMapCloudDatumStreamService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.ds.owm.weather";

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// @formatter:off
		SETTINGS = List.of(
				new BasicTextFieldSettingSpecifier(LATITUDE_SETTING, null),
				new BasicTextFieldSettingSpecifier(LONGITUDE_SETTING, null),
				new BasicTextFieldSettingSpecifier(LOCATION_ID_SETTING, null)
				);
		// @formatter:on
	}

	/** The service secure setting keys. */
	public static final Set<String> SECURE_SETTINGS = Collections.emptySet();

	/** The supported placeholder keys. */
	public static final List<String> SUPPORTED_PLACEHOLDERS = Collections.emptyList();

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
	public OpenWeatherMapWeatherCloudDatumStreamService(UserEventAppenderBiz userEventAppenderBiz,
			TextEncryptor encryptor, CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao, RestOperations restOps,
			Clock clock) {
		super(SERVICE_IDENTIFIER, "OpenWeatherMap Weather Datum Stream Service", userEventAppenderBiz,
				encryptor, expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, SETTINGS, restOps,
				LoggerFactory.getLogger(OpenWeatherMapWeatherCloudDatumStreamService.class), clock);
	}

	@Override
	public Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream) {
		requireNonNullArgument(datumStream, "datumStream");
		return performAction(datumStream, (ms, ds, mapping, integration, valueProps, exprProps) -> {

			final UriComponentsBuilder uriBuilder = locationBasedUrl(ms, ds, integration,
					WEATHER_URL_PATH);

			final GeneralDatum datum = restOpsHelper.httpGet("Get weather conditions", integration,
					JsonNode.class, req -> uriBuilder.buildAndExpand().toUri(),
					res -> parseDatum(res.getBody(), ds));

			final List<GeneralDatum> resultDatum = (datum != null ? List.of(datum)
					: Collections.emptyList());

			// evaluate expressions on merged datum
			evaluateExpressions(exprProps, resultDatum, mapping.getConfigId(),
					integration.getConfigId());

			return new BasicCloudDatumStreamQueryResult(resultDatum.stream()
					.sorted(Identity.sortByIdentity()).map(Datum.class::cast).toList());
		});
	}

	private GeneralDatum parseDatum(JsonNode json, CloudDatumStreamConfiguration datumStream) {
		GeneralDatum d = parseWeatherData(json, datumStream.getKind(), datumStream.getObjectId(),
				datumStream.getSourceId());
		if ( d != null ) {
			// remove DayDatum keys from weather
			d.getSamples().putInstantaneousSampleValue(DayDatum.TEMPERATURE_MINIMUM_KEY, null);
			d.getSamples().putInstantaneousSampleValue(DayDatum.TEMPERATURE_MAXIMUM_KEY, null);
			d.getSamples().putStatusSampleValue(DayDatum.SUNRISE_KEY, null);
			d.getSamples().putStatusSampleValue(DayDatum.SUNSET_KEY, null);
		}
		return d;
	}

}
