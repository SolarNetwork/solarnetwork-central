/* ==================================================================
 * OpenWeatherMapCloudIntegrationService.java - 31/10/2024 2:51:30 pm
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.Result.ErrorDetail;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * OpenWeatherMap API implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.2
 */
public class OpenWeatherMapCloudIntegrationService extends BaseRestOperationsCloudIntegrationService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.i9n.owm";

	/** The base URL to the OpenWeatherMap API. */
	public static final URI BASE_URI = URI.create("https://api.openweathermap.org");

	/** The URL path for weather data. */
	public static final String WEATHER_URL_PATH = "/data/2.5/weather";

	/** The API key URL query parameter name. */
	public static final String APPID_PARAM = "appid";

	/** The latitude URL query parameter name. */
	public static final String LATITUDE_PARAM = "lat";

	/** The longitude URL query parameter name. */
	public static final String LONGITUDE_PARAM = "lon";

	private static final String VALIDATION_LAT = "37.773972";
	private static final String VALIDATION_LON = "-122.431297";

	/**
	 * The well-known URLs.
	 */
	// @formatter:off
	public static final Map<String, URI> WELL_KNOWN_URLS = Map.of(
			API_BASE_WELL_KNOWN_URL, BASE_URI
			);
	// @formatter:on

	/** The service settings . */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		SETTINGS = List.of(API_KEY_SETTING_SPECIFIER, BASE_URL_SETTING_SPECIFIER);
	}

	/** The service secure setting keys. */
	public static final Set<String> SECURE_SETTINGS = Collections
			.unmodifiableSet(SettingUtils.secureKeys(SETTINGS));

	/**
	 * Constructor.
	 *
	 * @param datumStreamServices
	 *        the datum stream services
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param restOps
	 *        the REST operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public OpenWeatherMapCloudIntegrationService(Collection<CloudDatumStreamService> datumStreamServices,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor, RestOperations restOps) {
		super(SERVICE_IDENTIFIER, "OpenWeatherMap", datumStreamServices, List.of(), userEventAppenderBiz,
				encryptor, SETTINGS, WELL_KNOWN_URLS,
				new OpenWeatherMapRestOperationsHelper(
						LoggerFactory.getLogger(OpenWeatherMapCloudIntegrationService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						_ -> SECURE_SETTINGS));
	}

	@Override
	public Result<Void> validate(CloudIntegrationConfiguration integration, Locale locale) {
		// check that authentication settings provided
		final List<ErrorDetail> errorDetails = new ArrayList<>(2);
		final MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");

		final String apiKey = integration.serviceProperty(API_KEY_SETTING, String.class);
		if ( apiKey == null || apiKey.isEmpty() ) {
			String errMsg = ms.getMessage("error.apiKey.missing", null, locale);
			errorDetails.add(new ErrorDetail(API_KEY_SETTING, null, errMsg));
		}

		if ( !errorDetails.isEmpty() ) {
			String errMsg = ms.getMessage("error.settings.missing", null, locale);
			return Result.error("OWCI.0001", errMsg, errorDetails);
		}

		// validate by requesting weather for a fixed GPS coordinate
		try {
			final String response = restOpsHelper.httpGet("Validate connection", integration,
					String.class,
					// @formatter:off
					_ -> UriComponentsBuilder.fromUri(resolveBaseUrl(integration, BASE_URI))
							.path(WEATHER_URL_PATH)
							.queryParam(LATITUDE_PARAM, VALIDATION_LAT)
							.queryParam(LONGITUDE_PARAM, VALIDATION_LON)
							.build()
							.toUri(),
					// @formatter:on
					HttpEntity::getBody);
			log.debug("Validation of config {} succeeded: {}", integration.getConfigId(), response);
			return Result.success();
		} catch ( RemoteServiceException e ) {
			return validationResult(e, null);
		} catch ( Exception e ) {
			return Result.error("OWCI.0002", "Validation failed: " + e.getMessage());
		}
	}

}
