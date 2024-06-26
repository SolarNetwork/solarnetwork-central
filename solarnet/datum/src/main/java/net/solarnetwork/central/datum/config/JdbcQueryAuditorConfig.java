/* ==================================================================
 * JdbcQueryAuditorConfig.java - 8/10/2021 10:05:39 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcQueryAuditor;

/**
 * Query auditor configuration.
 *
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@Profile("query-auditor")
public class JdbcQueryAuditorConfig {

	/** A qualifier for audit JDBC access. */
	public static final String AUDIT = "audit";

	public static class QueryAuditorSettings {

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

	@Bean
	@ConfigurationProperties(prefix = "app.datum.query-auditor")
	public QueryAuditorSettings queryAuditorSettings() {
		return new QueryAuditorSettings();
	}

	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public JdbcQueryAuditor queryAuditor(QueryAuditorSettings settings) {
		DataSource ds = (readWriteDataSource != null ? readWriteDataSource : dataSource);
		JdbcQueryAuditor auditor = new JdbcQueryAuditor(ds);
		auditor.setUpdateDelay(settings.updateDelay);
		auditor.setFlushDelay(settings.flushDelay);
		auditor.setConnectionRecoveryDelay(settings.connectionRecoveryDelay);
		auditor.setStatLogUpdateCount(settings.statLogUpdateCount);
		return auditor;
	}

}
