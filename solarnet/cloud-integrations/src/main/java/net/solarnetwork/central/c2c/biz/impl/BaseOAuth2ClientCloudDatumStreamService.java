/* ==================================================================
 * BaseOAuth2ClientCloudDatumStreamService.java - 7/10/2024 7:45:23 am
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
import java.time.Clock;
import java.util.List;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Base implementation of
 * {@link net.solarnetwork.central.c2c.biz.CloudDatumStreamService} with
 * {@link OAuth2AuthorizedClientManager} support.
 *
 * @author matt
 * @version 1.2
 */
public abstract class BaseOAuth2ClientCloudDatumStreamService
		extends BaseRestOperationsCloudDatumStreamService {

	/** The OAuth client manager. */
	protected final OAuth2AuthorizedClientManager oauthClientManager;

	/**
	 * Constructor.
	 *
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param displayName
	 *        the display name
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
	 * @param datumStreamPropertyDao
	 *        the datum stream property DAO
	 * @param settings
	 *        the service settings
	 * @param restOpsHelper
	 *        the REST operations helper
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseOAuth2ClientCloudDatumStreamService(String serviceIdentifier, String displayName,
			Clock clock, UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationsExpressionService expressionService,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
			List<SettingSpecifier> settings, RestOperationsHelper restOpsHelper,
			OAuth2AuthorizedClientManager oauthClientManager) {
		super(serviceIdentifier, displayName, clock, userEventAppenderBiz, encryptor, expressionService,
				integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao, settings,
				restOpsHelper);
		this.oauthClientManager = requireNonNullArgument(oauthClientManager, "oauthClientManager");
	}

}
