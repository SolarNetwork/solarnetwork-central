/* ==================================================================
 * CloudIntegrationsDaoConfig.java - 2/10/2024 4:56:36 pm
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

package net.solarnetwork.central.c2c.config;

import static net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.dao.UserSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcUserSettingsEntityDao;

/**
 * Cloud integrations DAO configuration.
 *
 * @author matt
 * @version 1.3
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class CloudIntegrationsDaoConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	/**
	 * The cloud integration configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public CloudIntegrationConfigurationDao cloudIntegrationConfigurationDao() {
		return new JdbcCloudIntegrationConfigurationDao(jdbcOperations);
	}

	/**
	 * The cloud datum stream configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public CloudDatumStreamConfigurationDao cloudDatumStreamConfigurationDao() {
		return new JdbcCloudDatumStreamConfigurationDao(jdbcOperations);
	}

	/**
	 * The cloud datum stream mapping configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public CloudDatumStreamMappingConfigurationDao cloudDatumStreamMappingConfigurationDao() {
		return new JdbcCloudDatumStreamMappingConfigurationDao(jdbcOperations);
	}

	/**
	 * The cloud datum stream property configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public CloudDatumStreamPropertyConfigurationDao cloudDatumStreamPropertyConfigurationDao() {
		return new JdbcCloudDatumStreamPropertyConfigurationDao(jdbcOperations);
	}

	/**
	 * The cloud datum stream poll task DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public CloudDatumStreamPollTaskDao cloudDatumStreamPollTaskDaoDao() {
		return new JdbcCloudDatumStreamPollTaskDao(jdbcOperations);
	}

	/**
	 * The cloud datum stream rake task DAO.
	 *
	 * @return the DAO
	 * @since 1.3
	 */
	@Bean
	public CloudDatumStreamRakeTaskDao cloudDatumStreamRakeTaskDaoDao() {
		return new JdbcCloudDatumStreamRakeTaskDao(jdbcOperations);
	}

	/**
	 * The user settings DAO.
	 *
	 * @return the DAO
	 * @since 1.2
	 */
	@Bean
	public UserSettingsEntityDao cloudIntegrationsUserSettingsEntityDao() {
		return new JdbcUserSettingsEntityDao(jdbcOperations);
	}

	/**
	 * The cloud datum stream settings DAO.
	 *
	 * @return the DAO
	 * @since 1.2
	 */
	@Bean
	public CloudDatumStreamSettingsEntityDao cloudDatumStreamSettingsEntityDao() {
		return new JdbcCloudDatumStreamSettingsEntityDao(jdbcOperations);
	}

}
