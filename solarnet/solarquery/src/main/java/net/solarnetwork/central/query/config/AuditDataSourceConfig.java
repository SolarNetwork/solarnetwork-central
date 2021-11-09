/* ==================================================================
 * AuditDataSourceConfig.java - 9/11/2021 5:12:01 PM
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

package net.solarnetwork.central.query.config;

import static net.solarnetwork.central.datum.config.JdbcQueryAuditorConfig.AUDIT;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.dao.jdbc.DataSourcePingTest;
import net.solarnetwork.service.PingTest;

/**
 * Audit data source configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@ConditionalOnProperty("app.datasource-read-write.url")
public class AuditDataSourceConfig {

	@Value("${app.datasource-read-write.ping-query:SELECT CURRENT_TIMESTAMP}")
	private String pingQuery = "SELECT CURRENT_TIMESTAMP";

	@Value("${app.datasource-read-write.ping-id:audit}")
	private String pingId = "audit";

	@Autowired
	@Qualifier(AUDIT)
	private DataSource auditDataSource;

	@Bean
	@Qualifier(AUDIT)
	public PingTest auditDataSourcePingTest() {
		return new DataSourcePingTest(auditDataSource, pingQuery,
				String.format("%s-%s", DataSourcePingTest.class.getName(), pingId));
	}

}
