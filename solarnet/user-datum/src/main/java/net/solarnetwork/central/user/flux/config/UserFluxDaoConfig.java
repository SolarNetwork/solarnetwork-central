/* ==================================================================
 * UserFluxDaoConfig.java - 25/06/2024 7:39:49 am
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

package net.solarnetwork.central.user.flux.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.user.flux.dao.UserFluxAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.dao.UserFluxDefaultAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.dao.jdbc.JdbcUserFluxAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.dao.jdbc.JdbcUserFluxDefaultAggregatePublishConfigurationDao;

/**
 * User SolarFlux DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class UserFluxDaoConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	/**
	 * The user SolarFlux default aggregate publish configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public UserFluxDefaultAggregatePublishConfigurationDao userFluxDefaultAggregatePublishConfigurationDao() {
		return new JdbcUserFluxDefaultAggregatePublishConfigurationDao(jdbcOperations);
	}

	/**
	 * The user SolarFlux aggregate publish configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public UserFluxAggregatePublishConfigurationDao userFluxAggregatePublishConfigurationDao() {
		return new JdbcUserFluxAggregatePublishConfigurationDao(jdbcOperations);
	}

}
