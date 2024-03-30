/* ==================================================================
 * InstructionInputDaoConfig.java - 29/03/2024 8:52:27 am
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

package net.solarnetwork.central.inin.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.inin.dao.CredentialConfigurationDao;
import net.solarnetwork.central.inin.dao.EndpointAuthConfigurationDao;
import net.solarnetwork.central.inin.dao.EndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.TransformConfigurationDao;
import net.solarnetwork.central.inin.dao.jdbc.JdbcCredentialConfigurationDao;
import net.solarnetwork.central.inin.dao.jdbc.JdbcEndpointAuthConfigurationDao;
import net.solarnetwork.central.inin.dao.jdbc.JdbcEndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.jdbc.JdbcTransformConfigurationDao.JdbcRequestTransformConfigurationDao;
import net.solarnetwork.central.inin.dao.jdbc.JdbcTransformConfigurationDao.JdbcResponseTransformConfigurationDao;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;

/**
 * Instruction input DAO configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class InstructionInputDaoConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	/**
	 * The instruction input credential configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public CredentialConfigurationDao ininCredentialConfigurationDao() {
		return new JdbcCredentialConfigurationDao(jdbcOperations);
	}

	/**
	 * The instruction input endpoint authorization configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public EndpointAuthConfigurationDao ininEndpointAuthConfigurationDao() {
		return new JdbcEndpointAuthConfigurationDao(jdbcOperations);
	}

	/**
	 * The instruction input endpoint configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public EndpointConfigurationDao ininEndpointConfigurationDao() {
		return new JdbcEndpointConfigurationDao(jdbcOperations);
	}

	/**
	 * The instruction input endpoint request transform configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	@Qualifier(SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT_REQUEST)
	public TransformConfigurationDao<RequestTransformConfiguration> ininRequestTransformConfigurationDao() {
		return new JdbcRequestTransformConfigurationDao(jdbcOperations);
	}

	/**
	 * The instruction input endpoint response transform configuration DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	@Qualifier(SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT_RESPONSE)
	public TransformConfigurationDao<ResponseTransformConfiguration> ininResponseTransformConfigurationDao() {
		return new JdbcResponseTransformConfigurationDao(jdbcOperations);
	}

}
