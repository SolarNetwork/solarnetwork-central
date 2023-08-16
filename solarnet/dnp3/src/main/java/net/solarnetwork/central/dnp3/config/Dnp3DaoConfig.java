/* ==================================================================
 * Dnp3DaoConfig.java - 6/08/2023 7:51:57 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.dnp3.dao.ServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerControlConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerMeasurementConfigurationDao;
import net.solarnetwork.central.dnp3.dao.TrustedIssuerCertificateDao;
import net.solarnetwork.central.dnp3.dao.jdbc.JdbcServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.jdbc.JdbcServerConfigurationDao;
import net.solarnetwork.central.dnp3.dao.jdbc.JdbcServerControlConfigurationDao;
import net.solarnetwork.central.dnp3.dao.jdbc.JdbcServerMeasurementConfigurationDao;
import net.solarnetwork.central.dnp3.dao.jdbc.JdbcTrustedIssuerCertificateDao;

/**
 * DNP3 DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class Dnp3DaoConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	/**
	 * The DNP3 trusted issuer certificate DAO.
	 * 
	 * @return the DAO
	 */
	@Bean
	public TrustedIssuerCertificateDao dnp3TrustedIssuerCertificateDao() {
		return new JdbcTrustedIssuerCertificateDao(jdbcOperations);
	}

	/**
	 * The DNP3 server DAO.
	 * 
	 * @return the DAO
	 */
	@Bean
	public ServerConfigurationDao dnp3ServerConfigurationDao() {
		return new JdbcServerConfigurationDao(jdbcOperations);
	}

	/**
	 * The DNP3 server auth DAO.
	 * 
	 * @return the DAO
	 */
	@Bean
	public ServerAuthConfigurationDao dnp3ServerAuthConfigurationDao() {
		return new JdbcServerAuthConfigurationDao(jdbcOperations);
	}

	/**
	 * The DNP3 server measurement DAO.
	 * 
	 * @return the DAO
	 */
	@Bean
	public ServerMeasurementConfigurationDao dnp3ServerMeasurementConfigurationDao() {
		return new JdbcServerMeasurementConfigurationDao(jdbcOperations);
	}

	/**
	 * The DNP3 server control DAO.
	 * 
	 * @return the DAO
	 */
	@Bean
	public ServerControlConfigurationDao dnp3ServerControlConfigurationDao() {
		return new JdbcServerControlConfigurationDao(jdbcOperations);
	}

}
