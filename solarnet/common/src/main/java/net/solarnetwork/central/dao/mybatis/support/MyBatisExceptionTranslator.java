/* ==================================================================
 * MyBatisExceptionTranslator.java - 16/05/2020 4:25:40 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.mybatis.support;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Extension of {@link org.mybatis.spring.MyBatisExceptionTranslator} to handle
 * connection pool exceptions like
 * <code>org.springframework.jdbc.CannotGetJdbcConnectionException</code> that
 * are returned themselves.
 * 
 * @author matt
 * @version 1.0
 * @since 2.2
 */
public class MyBatisExceptionTranslator extends org.mybatis.spring.MyBatisExceptionTranslator {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private MultiValueMap<String, SqlStateErrorConfig> sqlStateConfigurations;

	/**
	 * Constructor.
	 * 
	 * @param dataSource
	 *        the data source
	 * @param exceptionTranslatorLazyInit
	 *        the lazy status
	 */
	public MyBatisExceptionTranslator(DataSource dataSource, boolean exceptionTranslatorLazyInit) {
		super(dataSource, exceptionTranslatorLazyInit);
		if ( !exceptionTranslatorLazyInit ) {
			loadExceptionProperties();
		}
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException e) {
		if ( e != null && e.getCause() instanceof DataAccessException ) {
			// could be something like CannotGetJdbcConnectionException, so use that directly
			return (DataAccessException) e.getCause();
		}
		DataAccessException result = super.translateExceptionIfPossible(e);
		if ( result instanceof UncategorizedSQLException || result == null ) {
			SQLException sqlEx = null;
			Throwable t = e;
			while ( t != null && sqlEx == null ) {
				if ( t instanceof SQLException ) {
					sqlEx = (SQLException) t;
				} else if ( t instanceof UncategorizedSQLException ) {
					sqlEx = ((UncategorizedSQLException) t).getSQLException();
				}
				t = t.getCause();
			}
			if ( sqlEx != null ) {
				final String sqlState = sqlEx.getSQLState();
				final String sqlMessage = sqlEx.getMessage();
				if ( sqlStateConfigurations == null ) {
					loadExceptionProperties();
				}
				final List<SqlStateErrorConfig> configs = (sqlStateConfigurations != null
						? sqlStateConfigurations.get(sqlState)
						: null);
				if ( configs != null ) {
					for ( SqlStateErrorConfig config : configs ) {
						if ( config.messagePattern.matcher(sqlMessage).find() ) {
							result = config.type.toException(sqlEx, result);
						}
					}
				}
			}
		}
		return result;
	}

	private synchronized void loadExceptionProperties() {
		Properties props = new Properties();
		try {
			props.load(getClass().getResourceAsStream("sql-error-matches.properties"));
		} catch ( IOException e ) {
			log.warn("Error loading SQL error matches properties file: {}", e.toString());
		}
		this.sqlStateConfigurations = loadSqlStateProperties(props);
	}

	private synchronized MultiValueMap<String, SqlStateErrorConfig> loadSqlStateProperties(
			Properties props) {
		Map<String, Map<String, String>> sqlStateMap = new LinkedHashMap<>(8);
		Pattern sqlStatePat = Pattern.compile("state\\.([^\\.]+)\\.([^\\\\.]+)\\.([^\\\\.]+)");
		for ( Map.Entry<Object, Object> me : props.entrySet() ) {
			String propKey = me.getKey().toString();
			Matcher m = sqlStatePat.matcher(propKey);
			if ( m.matches() ) {
				String sqlState = m.group(1);
				String name = m.group(2);
				String prop = m.group(3);
				Map<String, String> propMap = sqlStateMap.computeIfAbsent(name,
						k -> new LinkedHashMap<String, String>());
				propMap.put("state", sqlState);
				propMap.put(prop, me.getValue().toString());
			}
		}
		MultiValueMap<String, SqlStateErrorConfig> result = new LinkedMultiValueMap<>(8);
		for ( Map.Entry<String, Map<String, String>> me : sqlStateMap.entrySet() ) {
			String name = me.getKey();
			Map<String, String> configProps = me.getValue();
			try {
				SqlStateErrorConfig cfg = new SqlStateErrorConfig(name, configProps.get("state"),
						configProps.get("pat"), configProps.get("type"));
				result.add(cfg.sqlState, cfg);
			} catch ( IllegalArgumentException e ) {
				log.warn("SQL state error properties syntax error: {}", e.toString());
			}
		}
		return result;
	}

	private static enum ExceptionType {

		ConcurrencyFailure;

		private DataAccessException toException(Throwable t, DataAccessException fallback) {
			switch (this) {
				case ConcurrencyFailure:
					return new ConcurrencyFailureException(t.getMessage(), t);

				default:
					return fallback;
			}
		}
	}

	private static class SqlStateErrorConfig {

		private final String name;
		private final String sqlState;
		private final Pattern messagePattern;
		private final ExceptionType type;

		private SqlStateErrorConfig(String name, String sqlState, String messagePattern, String type) {
			super();
			if ( name == null ) {
				throw new IllegalArgumentException("The name argument must not be null.");
			}
			this.name = name;
			if ( sqlState == null ) {
				throw new IllegalArgumentException("The sqlState argument must not be null.");
			}
			this.sqlState = sqlState;
			if ( messagePattern == null ) {
				throw new IllegalArgumentException("The messagePattern argument must not be null.");
			}
			this.messagePattern = Pattern.compile(messagePattern, Pattern.CASE_INSENSITIVE);
			if ( type == null ) {
				throw new IllegalArgumentException("The type argument must not be null.");
			}
			this.type = ExceptionType.valueOf(type);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("SqlStateErrorConfig{name=");
			builder.append(name);
			builder.append(", sqlState=");
			builder.append(sqlState);
			builder.append(", messagePattern=");
			builder.append(messagePattern);
			builder.append(", type=");
			builder.append(type);
			builder.append("}");
			return builder.toString();
		}

	}

}
