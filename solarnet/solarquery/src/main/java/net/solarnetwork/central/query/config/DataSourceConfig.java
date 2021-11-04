/* ==================================================================
 * DataSourceConfig.java - 10/10/2021 5:43:26 PM
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import com.zaxxer.hikari.HikariDataSource;

/**
 * DataSource configuration for SolarQuery, with a read-only primary data source
 * and a secondary read-write data source for auditing.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class DataSourceConfig {

	@Bean
	@Primary
	@ConfigurationProperties("app.datasource")
	public DataSourceProperties dataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	@Primary
	@ConfigurationProperties("app.datasource.hikari")
	public HikariDataSource dataSource(DataSourceProperties properties) {
		return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
	}

	@Bean
	@Primary
	public PlatformTransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	@Primary
	public JdbcOperations jdbcOperations(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	@Qualifier(AUDIT)
	@ConditionalOnProperty("app.datasource-read-write.url")
	@ConfigurationProperties("app.datasource-read-write")
	public DataSourceProperties readWriteDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	@ConditionalOnProperty("app.datasource-read-write.url")
	@ConfigurationProperties(prefix = "app.datasource-read-write.hikari")
	@Qualifier(AUDIT)
	public HikariDataSource readWriteDataSource(@Qualifier(AUDIT) DataSourceProperties properties) {
		return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
	}

	@Bean
	@ConditionalOnProperty("app.datasource-read-write.url")
	@Qualifier(AUDIT)
	public PlatformTransactionManager readWriteTransactionManager(
			@Qualifier(AUDIT) DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	@ConditionalOnProperty("app.datasource-read-write.url")
	@Qualifier(AUDIT)
	public JdbcOperations readWriteJdbcOperations(@Qualifier(AUDIT) DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

}
