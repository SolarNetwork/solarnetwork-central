/* ==================================================================
 * OscpDaoConfig.java - 15/08/2022 9:55:47 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.oscp.dao.AssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.ExternalSystemSupportDao;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcAssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcExternalSystemSupportDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcFlexibilityProviderDao;

/**
 * OSCP DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class OscpDaoConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	/**
	 * The OSCP flexibility provider DAO.
	 * 
	 * @return the DAO
	 */
	@Bean
	public FlexibilityProviderDao oscpFlexibilityProviderDao() {
		return new JdbcFlexibilityProviderDao(jdbcOperations);
	}

	/**
	 * The OSCP asset configuration DAO.
	 * 
	 * @return the DAO
	 */
	@Bean
	public AssetConfigurationDao oscpAssetConfigurationDao() {
		return new JdbcAssetConfigurationDao(jdbcOperations);
	}

	/**
	 * The OSCP capacity group configuration DAO.
	 * 
	 * @return the DAO
	 */
	@Bean
	public CapacityGroupConfigurationDao oscpCapacityGroupConfigurationDao() {
		return new JdbcCapacityGroupConfigurationDao(jdbcOperations);
	}

	/**
	 * The OSCP capacity optimizer configuration DAO.
	 * 
	 * @return the DAO
	 */
	@Bean
	public CapacityOptimizerConfigurationDao oscpCapacityOptimizerConfigurationDao() {
		return new JdbcCapacityOptimizerConfigurationDao(jdbcOperations);
	}

	/**
	 * The OSCP capacity optimizer configuration DAO.
	 * 
	 * @return the DAO
	 */
	@Bean
	public CapacityProviderConfigurationDao oscpCapacityProviderConfigurationDao() {
		return new JdbcCapacityProviderConfigurationDao(jdbcOperations);
	}

	/**
	 * The OSCP external system support DAO.
	 * 
	 * @param capacityProviderDao
	 *        the capacity provider DAO
	 * @param capacityOptimizerDao
	 *        the capacity optimizer DAO
	 * @return the DAO
	 */
	@Bean
	public ExternalSystemSupportDao externalSystemSupportDao(
			CapacityProviderConfigurationDao capacityProviderDao,
			CapacityOptimizerConfigurationDao capacityOptimizerDao) {
		return new JdbcExternalSystemSupportDao(jdbcOperations, capacityProviderDao,
				capacityOptimizerDao);
	}

}
