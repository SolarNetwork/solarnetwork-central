/* ==================================================================
 * SolarEdgeCloudIntegrationService.java - 7/10/2024 6:49:06 am
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
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.Result.ErrorDetail;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * SolarEdge implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.0
 */
public class SolarEdgeCloudIntegrationService extends BaseRestOperationsCloudIntegrationService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.i9n.solaredge";

	/** The URL template for listing sites. */
	public static final String V2_SITES_LIST_URL = "/v2/sites";

	/** The user API key authorization HTTP header name. */
	public static final String API_KEY_HEADER = "X-API-Key";

	/** The account API key authorization HTTP header name. */
	public static final String ACCOUNT_KEY_HEADER = "X-Account-Key";

	/** The JSON and {@code problem+json} accept HTTP header value. */
	public static final String JSON_AND_PROBLEM_ACCEPT_HEADER_VALUE = "application/json, application/problem+json";

	/** The base URL to the SolarEdge API. */
	public static final URI BASE_URI = URI.create("https://monitoringapi.solaredge.com");

	/** An API key setting name. */
	public static final String API_KEY_SETTING = "apiKey";

	/** An account key setting name. */
	public static final String ACCOUNT_KEY_SETTING = "accountKey";

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
		var settings = new ArrayList<SettingSpecifier>(1);
		settings.add(new BasicTextFieldSettingSpecifier(ACCOUNT_KEY_SETTING, null, true));
		settings.add(new BasicTextFieldSettingSpecifier(API_KEY_SETTING, null, true));
		SETTINGS = Collections.unmodifiableList(settings);
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
	public SolarEdgeCloudIntegrationService(Collection<CloudDatumStreamService> datumStreamServices,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor, RestOperations restOps) {
		super(SERVICE_IDENTIFIER, "SolarEdge", datumStreamServices, userEventAppenderBiz, encryptor,
				SETTINGS, WELL_KNOWN_URLS,
				new SolarEdgeRestOperationsHelper(
						LoggerFactory.getLogger(SolarEdgeCloudIntegrationService.class),
						userEventAppenderBiz, restOps, HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> SECURE_SETTINGS));
	}

	@Override
	public Result<Void> validate(CloudIntegrationConfiguration integration, Locale locale) {
		// check that authentication settings provided
		final List<ErrorDetail> errorDetails = new ArrayList<>(2);
		final MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");

		final String accountKey = integration.serviceProperty(ACCOUNT_KEY_SETTING, String.class);
		if ( accountKey == null || accountKey.isEmpty() ) {
			String errMsg = ms.getMessage("error.accountKey.missing", null, locale);
			errorDetails.add(new ErrorDetail(ACCOUNT_KEY_SETTING, null, errMsg));
		}

		final String apiKey = integration.serviceProperty(API_KEY_SETTING, String.class);
		if ( apiKey == null || apiKey.isEmpty() ) {
			String errMsg = ms.getMessage("error.apiKey.missing", null, locale);
			errorDetails.add(new ErrorDetail(API_KEY_SETTING, null, errMsg));
		}

		if ( !errorDetails.isEmpty() ) {
			String errMsg = ms.getMessage("error.settings.missing", null, locale);
			return Result.error("SECI.0001", errMsg, errorDetails);
		}

		// validate by requesting the available sites for the partner ID
		try {
			final String response = restOpsHelper.httpGet("List sites", integration, String.class,
					(req) -> UriComponentsBuilder.fromUri(SolarEdgeCloudIntegrationService.BASE_URI)
							.path(SolarEdgeCloudIntegrationService.V2_SITES_LIST_URL).buildAndExpand()
							.toUri(),
					res -> res.getBody());
			log.debug("Validation of config {} succeeded: {}", integration.getConfigId(), response);
			return Result.success();
		} catch ( Exception e ) {
			return Result.error("SECI.0002", "Validation failed: " + e.getMessage());
		}
	}
}
