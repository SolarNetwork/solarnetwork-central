/* ==================================================================
 * JdbcNodeServiceAuditorConfig - 23/01/2023 9:05:51 am
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

package net.solarnetwork.central.common.config;

import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.AUDIT;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.common.dao.jdbc.JdbcNodeServiceAuditor;

/**
 * Node service auditor configuration.
 * 
 * @author matt
 * @version 1.2
 */
@Configuration(proxyBeanMethods = false)
@Profile(JdbcNodeServiceAuditorConfig.NODE_SERVICE_AUDITOR)
public class JdbcNodeServiceAuditorConfig {

	/** A qualifier for node service auditor. */
	public static final String NODE_SERVICE_AUDITOR = "node-service-auditor";

	@Autowired
	private DataSource dataSource;

	/**
	 * A read-write non-primary data source, for use in apps where the primary
	 * data source is read-only.
	 */
	@Autowired(required = false)
	@Qualifier(AUDIT)
	private DataSource readWriteDataSource;

	/**
	 * Configuration settings for the node service auditor.
	 * 
	 * @return the settings
	 */
	@Qualifier(NODE_SERVICE_AUDITOR)
	@Bean
	@ConfigurationProperties(prefix = "app.node-service-auditor")
	public ServiceAuditorSettings nodeServiceAuditorSettings() {
		return new ServiceAuditorSettings();
	}

	/**
	 * Auditor for node service events.
	 * 
	 * @param settings
	 *        the settings
	 * @return the service
	 */
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public JdbcNodeServiceAuditor nodeServiceAuditor(
			@Qualifier(NODE_SERVICE_AUDITOR) ServiceAuditorSettings settings) {
		DataSource ds = (readWriteDataSource != null ? readWriteDataSource : dataSource);
		JdbcNodeServiceAuditor auditor = new JdbcNodeServiceAuditor(ds);
		auditor.setUpdateDelay(settings.getUpdateDelay());
		auditor.setFlushDelay(settings.getFlushDelay());
		auditor.setConnectionRecoveryDelay(settings.getConnectionRecoveryDelay());
		auditor.setStatLogUpdateCount(settings.getStatLogUpdateCount());
		auditor.setShutdownMaxWait(settings.getShutdownMaxWait());
		return auditor;
	}

}
