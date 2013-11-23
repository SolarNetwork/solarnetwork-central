/* ==================================================================
 * JdbcConfig.java - Nov 22, 2013 2:28:00 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.cassandra;

import java.sql.Driver;
import javax.sql.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@ImportResource("classpath:env.xml")
public class JdbcConfig {

	@Value("${jdbc.driver}")
	private String jdbcDriver;

	@Value("${jdbc.url}")
	private String jdbcUrl;

	@Value("${jdbc.user}")
	private String jdbcUser;

	@Value("${jdbc.pass}")
	private String jdbcPassword;

	@Value("${jdbc.pool.maxIdle}")
	private final int jdbcPoolMaxIdle = 1;

	@Value("${jdbc.pool.maxActive}")
	private final int jdbcPoolMaxActive = 10;

	@Value("${jdbc.pool.maxWait}")
	private final int jdbcPoolMaxWait = 20000;

	@Value("${jdbc.pool.timeBetweenEvictionRunsMillis}")
	private final int jdbcPoolTimeBetweenEvictionRunsMillis = 120000;

	@Value("${jdbc.pool.minEvictableIdleTimeMillis}")
	private final int jdbcPoolMinEvictableIdleTimeMillis = 300000;

	@Value("${jdbc.pool.validationQuery}")
	private final String jdbcPoolValidationQuery = "SELECT CURRENT_DATE";

	public PoolProperties poolProperties() throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		PoolProperties props = new PoolProperties();
		props.setJmxEnabled(true);
		props.setTestWhileIdle(false);
		props.setTestOnBorrow(true);
		props.setValidationQuery(jdbcPoolValidationQuery);
		props.setTestOnReturn(false);
		props.setValidationInterval(30000);
		props.setTimeBetweenEvictionRunsMillis(jdbcPoolTimeBetweenEvictionRunsMillis);
		props.setMaxActive(jdbcPoolMaxActive);
		props.setInitialSize(0);
		props.setMaxWait(jdbcPoolMaxWait);
		props.setRemoveAbandonedTimeout(60);
		props.setMinEvictableIdleTimeMillis(jdbcPoolMinEvictableIdleTimeMillis);
		props.setMinIdle(0);
		props.setMaxIdle(jdbcPoolMaxIdle);
		props.setLogAbandoned(true);
		props.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");

		Class<?> driverClass = Class.forName(jdbcDriver);
		Driver d = driverClass.asSubclass(Driver.class).newInstance();
		SimpleDriverDataSource ds = new SimpleDriverDataSource(d, jdbcUrl, jdbcUser, jdbcPassword);
		props.setDataSource(ds);

		return props;
	}

	@Bean
	public DataSource dataSource() throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		return new org.apache.tomcat.jdbc.pool.DataSource(poolProperties());
	}

	@Bean
	public JdbcOperations jdbcOperations() {
		try {
			return new JdbcTemplate(dataSource());
		} catch ( ClassNotFoundException e ) {
			throw new RuntimeException(e);
		} catch ( InstantiationException e ) {
			throw new RuntimeException(e);
		} catch ( IllegalAccessException e ) {
			throw new RuntimeException(e);
		}
	}

}
