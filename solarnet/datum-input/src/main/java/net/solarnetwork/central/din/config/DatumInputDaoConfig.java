/* ==================================================================
 * DatumInputDaoConfig.java - 23/02/2024 10:05:02 am
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

package net.solarnetwork.central.din.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.din.dao.CredentialConfigurationDao;
import net.solarnetwork.central.din.dao.EndpointAuthConfigurationDao;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.dao.TransformConfigurationDao;
import net.solarnetwork.central.din.dao.jdbc.JdbcCredentialConfigurationDao;
import net.solarnetwork.central.din.dao.jdbc.JdbcEndpointAuthConfigurationDao;
import net.solarnetwork.central.din.dao.jdbc.JdbcEndpointConfigurationDao;
import net.solarnetwork.central.din.dao.jdbc.JdbcTransformConfigurationDao;

/**
 * Datum input DAO configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class DatumInputDaoConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	/**
	 * The datum input credential configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public CredentialConfigurationDao dinCredentialConfigurationDao() {
		return new JdbcCredentialConfigurationDao(jdbcOperations);
	}

	/**
	 * The datum input endpoint authorization configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public EndpointAuthConfigurationDao dinEndpointAuthConfigurationDao() {
		return new JdbcEndpointAuthConfigurationDao(jdbcOperations);
	}

	/**
	 * The datum input endpoint configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public EndpointConfigurationDao dinEndpointConfigurationDao() {
		return new JdbcEndpointConfigurationDao(jdbcOperations);
	}

	/**
	 * The datum input endpoint configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public TransformConfigurationDao dinTransformConfigurationDao() {
		return new JdbcTransformConfigurationDao(jdbcOperations);
	}

}
