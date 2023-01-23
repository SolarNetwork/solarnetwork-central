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
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile("node-service-auditor")
public class JdbcNodeServiceAuditorConfig {

	/** A qualifier for audit JDBC access. */
	public static final String AUDIT = "audit";

	public static class NodeServiceAuditorSettings {

		private long updateDelay = 100;
		private long flushDelay = 10000;
		private long connectionRecoveryDelay = 15000;
		private int statLogUpdateCount = 1000;

		public long getUpdateDelay() {
			return updateDelay;
		}

		public void setUpdateDelay(long updateDelay) {
			this.updateDelay = updateDelay;
		}

		public long getFlushDelay() {
			return flushDelay;
		}

		public void setFlushDelay(long flushDelay) {
			this.flushDelay = flushDelay;
		}

		public long getConnectionRecoveryDelay() {
			return connectionRecoveryDelay;
		}

		public void setConnectionRecoveryDelay(long connectionRecoveryDelay) {
			this.connectionRecoveryDelay = connectionRecoveryDelay;
		}

		public int getStatLogUpdateCount() {
			return statLogUpdateCount;
		}

		public void setStatLogUpdateCount(int statLogUpdateCount) {
			this.statLogUpdateCount = statLogUpdateCount;
		}

	}

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
	@Bean
	@ConfigurationProperties(prefix = "app.node-service-auditor")
	public NodeServiceAuditorSettings nodeServiceAuditorSettings() {
		return new NodeServiceAuditorSettings();
	}

	/**
	 * Auditor for node service events.
	 * 
	 * @param settings
	 *        the settings
	 * @return the service
	 */
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public JdbcNodeServiceAuditor nodeServiceAuditor(NodeServiceAuditorSettings settings) {
		DataSource ds = (readWriteDataSource != null ? readWriteDataSource : dataSource);
		JdbcNodeServiceAuditor auditor = new JdbcNodeServiceAuditor(ds);
		auditor.setUpdateDelay(settings.updateDelay);
		auditor.setFlushDelay(settings.flushDelay);
		auditor.setConnectionRecoveryDelay(settings.connectionRecoveryDelay);
		auditor.setStatLogUpdateCount(settings.statLogUpdateCount);
		return auditor;
	}

}
