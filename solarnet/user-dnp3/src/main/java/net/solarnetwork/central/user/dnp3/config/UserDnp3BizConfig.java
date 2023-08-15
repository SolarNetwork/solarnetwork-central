/* ==================================================================
 * UserDnp3BizConfig.java - 7/08/2023 10:07:10 am
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

package net.solarnetwork.central.user.dnp3.config;

import static net.solarnetwork.central.dnp3.config.SolarNetDnp3Configuration.DNP3;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import net.solarnetwork.central.dnp3.dao.ServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerControlConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerMeasurementConfigurationDao;
import net.solarnetwork.central.dnp3.dao.TrustedIssuerCertificateDao;
import net.solarnetwork.central.user.dnp3.biz.dao.DaoUserDnp3Biz;

/**
 * Configuration for User OSCP services.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(DNP3)
public class UserDnp3BizConfig {

	@Autowired
	private TrustedIssuerCertificateDao trustedCertDao;

	@Autowired
	private ServerConfigurationDao serverDao;

	@Autowired
	private ServerAuthConfigurationDao serverAuthDao;

	@Autowired
	private ServerMeasurementConfigurationDao serverMeasurementDao;

	@Autowired
	private ServerControlConfigurationDao serverControlDao;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private Validator validator;

	@ConfigurationProperties("app.dnp3.server-import-csv-example-resources")
	@Bean
	@Qualifier("server-import-csv-example-resources")
	public Map<String, String> testTrustSettings() {
		return new LinkedHashMap<>();
	}

	@Bean
	public DaoUserDnp3Biz userDnp3Biz(
			@Qualifier("server-import-csv-example-resources") Map<String, String> exampleResoruces) {
		var biz = new DaoUserDnp3Biz(trustedCertDao, serverDao, serverAuthDao, serverMeasurementDao,
				serverControlDao, resourceLoader);
		biz.setValidator(validator);
		if ( exampleResoruces != null && !exampleResoruces.isEmpty() ) {
			biz.setCsvImportExampleResources(exampleResoruces);
		}
		return biz;
	}

}
