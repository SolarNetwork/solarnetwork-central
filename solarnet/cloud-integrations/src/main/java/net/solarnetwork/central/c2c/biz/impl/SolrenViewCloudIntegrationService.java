/* ==================================================================
 * SolrenViewCloudIntegrationService.java - 17/10/2024 11:06:04 am
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.domain.Result;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * SolrenView implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.1
 */
public class SolrenViewCloudIntegrationService extends BaseRestOperationsCloudIntegrationService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.i9n.solrenview";

	/** The base URL to the SolrenView API. */
	public static final URI BASE_URI = URI.create("https://solrenview.com");

	/**
	 * The relative path to the XML feed API.
	 *
	 * @since 1.1
	 */
	public static final String XML_FEED_PATH = "/xmlfeed/ss-xmlN.php";

	/** The URL for the XML feed. */
	public static final URI XML_FEED_URI = BASE_URI.resolve(XML_FEED_PATH);

	/** The XML feed URL parameter for the site ID. */
	public static final String XML_FEED_SITE_ID_PARAM = "site_id";

	/**
	 * The XML feed URL parameter for the start date, in ISO 8601 timestamp
	 * form.
	 */
	public static final String XML_FEED_START_DATE_PARAM = "ts_start";

	/**
	 * The XML feed URL parameter for the end date, in ISO 8601 timestamp form.
	 */
	public static final String XML_FEED_END_DATE_PARAM = "ts_end";

	/** The XML feed URL parameter to include lifetime energy meter readings. */
	public static final String XML_FEED_INCLUDE_LIFETIME_ENERGY_PARAM = "show_whl";

	/** The XML feed URL parameter to use the UTC time zone. */
	public static final String XML_FEED_USE_UTC_PARAM = "use_utc";

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
		settings.add(new BasicTextFieldSettingSpecifier(BASE_URL_SETTING, null));
		SETTINGS = Collections.unmodifiableList(settings);
	}

	/** The service secure setting keys. */
	public static final Set<String> SECURE_SETTINGS = Collections
			.unmodifiableSet(SettingUtils.secureKeys(SETTINGS));

	/**
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
	public SolrenViewCloudIntegrationService(Collection<CloudDatumStreamService> datumStreamServices,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor, RestOperations restOps) {
		super(SERVICE_IDENTIFIER, "SolrenView", datumStreamServices, userEventAppenderBiz, encryptor,
				SETTINGS, WELL_KNOWN_URLS,
				new RestOperationsHelper(
						LoggerFactory.getLogger(SolrenViewCloudIntegrationService.class),
						userEventAppenderBiz, restOps, HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> SECURE_SETTINGS));
	}

	@Override
	public Result<Void> validate(CloudIntegrationConfiguration integration, Locale locale) {
		// as there is no authentication this just works
		return Result.success();
	}

}
