/* ==================================================================
 * BaseOAuth2ClientCloudIntegrationService.java - 7/10/2024 7:32:03 am
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Abstract base implementation of
 * {@link net.solarnetwork.central.c2c.biz.CloudIntegrationService} with
 * {@link OAuth2AuthorizedClientManager} support.
 *
 * @author matt
 * @version 1.0
 */
public abstract class BaseOAuth2ClientCloudIntegrationService
		extends BaseRestOperationsCloudIntegrationService {

	/** The OAuth client manager. */
	protected final OAuth2AuthorizedClientManager oauthClientManager;

	/**
	 * Constructor.
	 *
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param displayName
	 *        the display name
	 * @param datumStreamServices
	 *        the datum stream services
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param settings
	 *        the service settings
	 * @param wellKnownUrls
	 *        the well known URLs
	 * @param restOpsHelper
	 *        the REST operations helper
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseOAuth2ClientCloudIntegrationService(String serviceIdentifier, String displayName,
			Collection<CloudDatumStreamService> datumStreamServices,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			List<SettingSpecifier> settings, Map<String, URI> wellKnownUrls,
			RestOperationsHelper restOpsHelper, OAuth2AuthorizedClientManager oauthClientManager) {
		super(serviceIdentifier, displayName, datumStreamServices, userEventAppenderBiz, encryptor,
				settings, wellKnownUrls, restOpsHelper);
		this.oauthClientManager = requireNonNullArgument(oauthClientManager, "oauthClientManager");
	}

}
