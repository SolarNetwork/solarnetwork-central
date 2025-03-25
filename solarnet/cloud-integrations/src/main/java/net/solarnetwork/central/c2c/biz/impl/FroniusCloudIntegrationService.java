/* ==================================================================
 * FroniusCloudIntegrationService.java - 3/12/2024 11:40:59 am
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
import static net.solarnetwork.util.StringUtils.nonEmptyString;
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
 * Fronius implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.0
 */
public class FroniusCloudIntegrationService extends BaseRestOperationsCloudIntegrationService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.i9n.fronius";

	/**
	 * The URL template for listing all available systems.
	 */
	public static final String LIST_SYSTEMS_URL = "/swqapi/pvsystems";

	/** The base URL to the Fronius API. */
	public static final URI BASE_URI = URI.create("https://api.solarweb.com");

	/** An access key ID setting name. */
	public static final String ACCESS_KEY_ID_SETTING = "accessKeyId";

	/** An access key secret setting name. */
	public static final String ACCESS_KEY_SECRET_SETTING = "accessKeySecret";

	/** The access key ID HTTP header name. */
	public static final String ACCESS_KEY_ID_HEADER = "AccessKeyId";

	/** The access key secret HTTP header name. */
	public static final String ACCES_KEY_SECRET_HEADER = "AccessKeyValue";

	/** The 0-based offset query parameter. */
	public static final String OFFSET_PARAM = "offset";

	/** The result limit (page size) query parameter. */
	public static final String LIMIT_PARAM = "limit";

	/**
	 * The well-known URLs.
	 */
	// @formatter:off
	public static final Map<String, URI> WELL_KNOWN_URLS = Map.of(
			API_BASE_WELL_KNOWN_URL, BASE_URI
			);
	// @formatter:on

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		var accessKeyId = new BasicTextFieldSettingSpecifier(ACCESS_KEY_ID_SETTING, null);
		var accessKeySecret = new BasicTextFieldSettingSpecifier(ACCESS_KEY_SECRET_SETTING, null, true);
		SETTINGS = List.of(accessKeyId, accessKeySecret, BASE_URL_SETTING_SPECIFIER);
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
	public FroniusCloudIntegrationService(Collection<CloudDatumStreamService> datumStreamServices,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor, RestOperations restOps) {
		super(SERVICE_IDENTIFIER, "Fronius", datumStreamServices, userEventAppenderBiz, encryptor,
				SETTINGS, WELL_KNOWN_URLS,
				new FroniusRestOperationsHelper(
						LoggerFactory.getLogger(FroniusCloudIntegrationService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> SECURE_SETTINGS));
	}

	@Override
	public Result<Void> validate(CloudIntegrationConfiguration integration, Locale locale) {
		// check that authentication settings provided
		final List<ErrorDetail> errorDetails = new ArrayList<>(2);
		final MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");

		final String apiKey = nonEmptyString(
				integration.serviceProperty(ACCESS_KEY_ID_SETTING, String.class));
		if ( apiKey == null ) {
			String errMsg = ms.getMessage("error.accessKeyId.missing", null, locale);
			errorDetails.add(new ErrorDetail(ACCESS_KEY_ID_SETTING, null, errMsg));
		}

		final String apiSecret = nonEmptyString(
				integration.serviceProperty(ACCESS_KEY_SECRET_SETTING, String.class));
		if ( apiSecret == null ) {
			String errMsg = ms.getMessage("error.accessKeySecret.missing", null, locale);
			errorDetails.add(new ErrorDetail(ACCESS_KEY_SECRET_SETTING, null, errMsg));
		}

		if ( !errorDetails.isEmpty() ) {
			String errMsg = ms.getMessage("error.settings.missing", null, locale);
			return Result.error("FRCI.0001", errMsg, errorDetails);
		}

		// validate by requesting the V1 available sites
		try {
			final String response = restOpsHelper.httpGet("List systems", integration, String.class,
					(req) -> UriComponentsBuilder.fromUri(resolveBaseUrl(integration, BASE_URI))
							.path(FroniusCloudIntegrationService.LIST_SYSTEMS_URL).buildAndExpand()
							.toUri(),
					res -> res.getBody());
			log.debug("Validation of config {} succeeded: {}", integration.getConfigId(), response);
			return Result.success();
		} catch ( Exception e ) {
			return Result.error("FRCI.0002", "Validation failed: " + e.getMessage());
		}
	}

}
