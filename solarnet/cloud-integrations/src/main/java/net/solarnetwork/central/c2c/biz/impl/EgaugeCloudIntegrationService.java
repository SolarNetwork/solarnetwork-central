/* ==================================================================
 * EgaugeCloudIntegrationService.java - 25/10/2024 11:48:21 am
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
 * eGauge implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.1
 */
public class EgaugeCloudIntegrationService extends BaseRestOperationsCloudIntegrationService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.i9n.egauge";

	/** The base URL to the eGauge API. */
	public static final String BASE_URI_TEMPLATE = "https://{deviceId}.d.egauge.net";

	/**
	 * The well-known URLs.
	 */
	public static final Map<String, URI> WELL_KNOWN_URLS = Collections.emptyMap();

	/** The service settings . */
	public static final List<SettingSpecifier> SETTINGS;

	static {
		SETTINGS = List.of(new BasicTextFieldSettingSpecifier(BASE_URL_SETTING, null));
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
	public EgaugeCloudIntegrationService(Collection<CloudDatumStreamService> datumStreamServices,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor, RestOperations restOps) {
		super(SERVICE_IDENTIFIER, "eGauge", datumStreamServices, List.of(), userEventAppenderBiz,
				encryptor, SETTINGS, WELL_KNOWN_URLS,
				new RestOperationsHelper(LoggerFactory.getLogger(EgaugeCloudIntegrationService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> SECURE_SETTINGS));
	}

	@Override
	public Result<Void> validate(CloudIntegrationConfiguration integration, Locale locale) {
		// not supported because base URL is device-specific
		return Result.success();
	}

}
