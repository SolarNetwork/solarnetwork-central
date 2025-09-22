/* ==================================================================
 * UserCloudIntegrationsBizConfig.java - 30/09/2024 11:22:23 am
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

package net.solarnetwork.central.user.c2c.config;

import static net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS;
import java.time.Clock;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.dao.UserSettingsEntityDao;
import net.solarnetwork.central.security.jdbc.JdbcOAuth2AuthorizedClientService;
import net.solarnetwork.central.user.c2c.biz.impl.DaoUserCloudIntegrationsBiz;

/**
 * Configuration for user cloud integrations services.
 *
 * @author matt
 * @version 1.4
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class UserCloudIntegrationsBizConfig {

	@Autowired
	private CloudIntegrationConfigurationDao integrationDao;

	@Autowired
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Autowired
	private CloudDatumStreamMappingConfigurationDao datumStreamMappingDao;

	@Autowired
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao;

	@Autowired
	private CloudDatumStreamPollTaskDao datumStreamPollTaskDao;

	@Autowired
	private CloudDatumStreamRakeTaskDao datumStreamRakeTaskDao;

	@Autowired
	private UserSettingsEntityDao userSettingsDao;

	@Autowired
	private CloudDatumStreamSettingsEntityDao datumStreamSettingsDao;

	@Autowired
	private Collection<CloudIntegrationService> integrationServices;

	@Qualifier(CLOUD_INTEGRATIONS)
	@Autowired
	private TextEncryptor textEncryptor;

	@Autowired
	@Qualifier(CLOUD_INTEGRATIONS)
	private BytesEncryptor bytesEncryptor;

	@Autowired
	private JdbcOperations jdbcOperations;

	@Bean
	public DaoUserCloudIntegrationsBiz userCloudIntegrationsBiz() {
		var clientAccessTokenDao = new JdbcOAuth2AuthorizedClientService(bytesEncryptor, jdbcOperations,
				new ClientRegistrationRepository() {

					@Override
					public ClientRegistration findByRegistrationId(String registrationId) {
						// we're not using this API here
						throw new UnsupportedOperationException();
					}
				});
		DaoUserCloudIntegrationsBiz biz = new DaoUserCloudIntegrationsBiz(Clock.systemUTC(),
				userSettingsDao, integrationDao, datumStreamDao, datumStreamSettingsDao,
				datumStreamMappingDao, datumStreamPropertyDao, datumStreamPollTaskDao,
				datumStreamRakeTaskDao, clientAccessTokenDao, textEncryptor, integrationServices);
		return biz;
	}

}
