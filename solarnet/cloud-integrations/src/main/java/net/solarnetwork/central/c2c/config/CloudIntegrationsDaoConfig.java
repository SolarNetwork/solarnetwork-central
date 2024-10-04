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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudIntegrationConfigurationDao;

/**
 * Cloud integrations DAO configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class CloudIntegrationsDaoConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	/**
	 * The cloud integration configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public CloudIntegrationConfigurationDao cloudIntegrationConfigurationConfigurationDao() {
		return new JdbcCloudIntegrationConfigurationDao(jdbcOperations);
	}

	/**
	 * The cloud datum stream configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public CloudDatumStreamConfigurationDao cloudDatumStreamConfigurationConfigurationDao() {
		return new JdbcCloudDatumStreamConfigurationDao(jdbcOperations);
	}

	/**
	 * The cloud datum stream property configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public CloudDatumStreamPropertyConfigurationDao cloudDatumStreamPropertyConfigurationConfigurationDao() {
		return new JdbcCloudDatumStreamPropertyConfigurationDao(jdbcOperations);
	}

}
