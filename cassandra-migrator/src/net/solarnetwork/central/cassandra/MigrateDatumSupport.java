/* ==================================================================
 * MigrateDatumSupport.java - Nov 23, 2013 2:11:36 PM
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import net.solarnetwork.util.ClassUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.QueryTimeoutException;

/**
 * Abstract supporting class for migrating Datum.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class MigrateDatumSupport implements MigrationTask {

	public static final String DEFAULT_CQL = "INSERT INTO solardata.node_datum (node_id, dtype, source_id, year, ts, data_num) "
			+ "VALUES (?, ?, ?, ?, ?, ?)";

	public static final String DATE_RANGE_SQL_TEMPLATE = "SELECT "
			+ "%1$s as pk, min(%2$s) as start, max(%2$s) as end FROM %3$s GROUP BY %1$s "
			+ "ORDER BY pk";

	private JdbcOperations jdbcOperations;
	private Cluster cluster;
	private ExecutorService executorService;
	private Integer maxResults = null;
	private Integer fetchSize = 1000;
	private String cassandraKeyspace = "solardata";
	private String dateRangeSql; // query must return pk, min date, max date
	private String sql;
	private String countSql;
	private String cql = DEFAULT_CQL;
	private Integer startingOffset = null;
	private Integer maxWriteTries = 25;
	private Integer writeRetryDelaySeconds = 30;
	private List<Object> sqlParameters;
	private Map<String, ?> resultProperties;

	protected final Calendar gmtCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected void setupGroupedDateRangeSql(String tableName, String pkColumnName, String dateColumnName) {
		dateRangeSql = String.format(DATE_RANGE_SQL_TEMPLATE, pkColumnName, dateColumnName, tableName);
	}

	public MigrateDatumSupport() {
		super();
	}

	public MigrateDatumSupport(MigrateDatumSupport other) {
		super();
		ClassUtils.copyBeanProperties(other, this, null);
	}

	@Override
	public MigrationResult call() throws Exception {
		return migrate(startingOffset);
	}

	/**
	 * Get a BoundStatement with the primary key values bound.
	 * 
	 * <p>
	 * The ResultSet must provide the following result columns, in this order:
	 * </p>
	 * 
	 * <ol>
	 * <li>primary key (node_id, loc_id, etc)</li>
	 * <li>source_id string</li>
	 * <li>created timestamp</li>
	 * </ol>
	 * 
	 * @param rs
	 *        the ResultSet
	 * @param cStmt
	 *        the PreparedStatement
	 * @return a new BoundStatement
	 * @throws SQLException
	 */
	protected BoundStatement getBoundStatementForResultRowMapping(ResultSet rs,
			com.datastax.driver.core.PreparedStatement cStmt) throws SQLException {
		BoundStatement bs = new BoundStatement(cStmt);
		bs.setString(0, rs.getObject(1).toString());
		bs.setInt(1, getDatumType());
		bs.setString(2, rs.getString(2));

		Timestamp created = rs.getTimestamp(3);
		gmtCalendar.setTimeInMillis(created.getTime());
		bs.setInt(3, gmtCalendar.get(Calendar.YEAR));

		bs.setDate(4, created);
		return bs;
	}

	/**
	 * Get the {@link BoundStatement} parameter index for the Map collection.
	 * 
	 * @return the parameter index
	 */
	protected int getBoundStatementMapParameterIndex() {
		return 5;
	}

	/**
	 * Get a BigDecimal from a float using a provided precision.
	 * 
	 * @param f
	 *        the float
	 * @param precision
	 *        the desired precision
	 * @return the BigDecimal
	 */
	protected final BigDecimal getBigDecimal(float f, int precision) {
		String str = String.format("%." + precision + "f", f);
		return new BigDecimal(str);
	}

	/**
	 * Get a BigDecimal from a double using a provided precision.
	 * 
	 * @param d
	 *        the double
	 * @param precision
	 *        the desired precision
	 * @return the BigDecimal
	 */
	protected final BigDecimal getBigDecimal(double d, int precision) {
		String str = String.format("%." + precision + "f", d);
		return new BigDecimal(str);
	}

	protected Timestamp getTimestamp(String key, Map<String, Object> map) {
		Object o = map.get(key);
		if ( o instanceof Timestamp ) {
			return (Timestamp) o;
		} else if ( o instanceof java.sql.Date || o instanceof java.util.Date ) {
			final LocalDate d = new LocalDate(o);
			return new Timestamp(d.toDateTimeAtStartOfDay(DateTimeZone.UTC).getMillis());
		}
		return null;
	}

	protected void handleDateRangeSql(final MigrationResult result, String sql) {
		final List<Map<String, Object>> ranges = jdbcOperations.queryForList(dateRangeSql);
		final Calendar cal = (Calendar) gmtCalendar.clone();
		for ( Map<String, Object> range : ranges ) {
			final Long pk = (Long) range.get("pk");
			final Timestamp start = getTimestamp("start", range);
			final Timestamp end = getTimestamp("end", range);
			cal.setTime(start);
			int yearStart = cal.get(Calendar.YEAR);
			cal.setTime(end);
			final int yearEnd = cal.get(Calendar.YEAR);
			log.info("Breaking up {} into subtasks for pk {} years {}-{}", getDatumTypeDescription(),
					pk, yearStart, yearEnd);
			cal.setTime(start);
			cal.set(Calendar.MONTH, cal.getMinimum(Calendar.MONTH));
			cal.set(Calendar.DAY_OF_MONTH, cal.getMinimum(Calendar.DAY_OF_MONTH));
			cal.set(Calendar.HOUR_OF_DAY, cal.getMinimum(Calendar.HOUR_OF_DAY));
			cal.set(Calendar.MINUTE, cal.getMinimum(Calendar.MINUTE));
			cal.set(Calendar.SECOND, cal.getMinimum(Calendar.SECOND));
			cal.set(Calendar.MILLISECOND, cal.getMinimum(Calendar.MILLISECOND));
			while ( yearStart <= yearEnd ) {
				Class<? extends MigrateDatumSupport> clazz = getClass();
				try {
					Constructor<? extends MigrateDatumSupport> cstr = clazz
							.getConstructor(MigrateDatumSupport.class);
					MigrateDatumSupport subtask = cstr.newInstance(this);
					subtask.setDateRangeSql(null);
					subtask.setCountSql(null);
					List<Object> params = new ArrayList<Object>(3);
					params.add(pk);
					params.add(new Timestamp(cal.getTimeInMillis()));
					cal.add(Calendar.YEAR, 1);
					params.add(new Timestamp(cal.getTimeInMillis()));
					subtask.setSqlParameters(params);
					Map<String, Object> resultProps = new LinkedHashMap<String, Object>(3);
					resultProps.put("pk", pk);
					resultProps.put("year", yearStart);
					subtask.resultProperties = resultProps;
					result.addSubtask(executorService.submit(subtask));
					yearStart++;
				} catch ( SecurityException e ) {
					throw new RuntimeException(e);
				} catch ( NoSuchMethodException e ) {
					throw new RuntimeException(e);
				} catch ( IllegalArgumentException e ) {
					throw new RuntimeException(e);
				} catch ( InstantiationException e ) {
					throw new RuntimeException(e);
				} catch ( IllegalAccessException e ) {
					throw new RuntimeException(e);
				} catch ( InvocationTargetException e ) {
					throw new RuntimeException(e);
				}
			}
		}
		result.finished();
		result.setSuccess(true);
	}

	protected MigrationResult migrate(final Integer offset) {
		final Session cSession = cluster.connect(getCassandraKeyspace());
		final com.datastax.driver.core.PreparedStatement cStmt = cSession.prepare(cql);
		final MigrationResult result = new MigrationResult(getDatumTypeDescription());
		result.setProcessedCount(0L);
		result.setSuccess(false);
		if ( resultProperties != null ) {
			result.getTaskProperties().putAll(resultProperties);
		}
		try {
			if ( countSql != null ) {
				long count = jdbcOperations.queryForLong(countSql);
				log.info("Found {} {} rows to process", count, getDatumTypeDescription());
			}
			if ( dateRangeSql != null ) {
				handleDateRangeSql(result, dateRangeSql);
			} else {
				// execute SQL
				jdbcOperations.execute(new PreparedStatementCreator() {

					@Override
					public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
						con.setAutoCommit(false);
						log.info("Task SQL: {}; params {}", sql, sqlParameters);
						PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
								ResultSet.CONCUR_READ_ONLY);
						if ( getMaxResults() != null ) {
							int max = getMaxResults();
							if ( offset != null ) {
								max += offset;
							}
							stmt.setMaxRows(max);
						}
						if ( getFetchSize() != null ) {
							stmt.setFetchSize(getFetchSize());
						}
						return stmt;
					}
				}, new PreparedStatementCallback<Object>() {

					@Override
					public MigrationResult doInPreparedStatement(PreparedStatement ps)
							throws SQLException, DataAccessException {
						if ( sqlParameters != null ) {
							for ( int i = 0; i < sqlParameters.size(); i++ ) {
								ps.setObject(i + 1, sqlParameters.get(i));
							}
						}
						boolean haveResults = ps.execute();
						if ( haveResults ) {
							handleResults(result, ps, offset, cSession, cStmt);
						}
						return null;
					}
				});
			}
		} catch ( RuntimeException e ) {
			log.error("Migrate task {} failed: {}", getDatumTypeDescription(), e.getMessage());
		} finally {
			result.finished();
			if ( cSession != null ) {
				cSession.shutdown();
			}
		}
		return result;
	}

	protected void handleResults(MigrationResult result, PreparedStatement ps, Integer offset,
			Session cSession, com.datastax.driver.core.PreparedStatement cStmt) throws SQLException {
		ResultSet rs = ps.getResultSet();
		long count = 0;
		try {
			int currRow = 1;
			if ( offset != null ) {
				currRow = offset.intValue();
				if ( currRow > 0 ) {
					try {
						rs.relative(currRow);
					} catch ( Exception e ) {
						if ( log.isWarnEnabled() ) {
							log.warn("Unable to call ResultSet.relative(" + currRow
									+ "), reverting to inefficient rs.next() " + currRow + " times");
						}
						for ( int i = 0; i < currRow; i++ ) {
							rs.next();
						}
					}
				}
			}

			RowMapper<Map<String, Object>> rowMapper = new ColumnMapRowMapper();
			while ( rs.next() ) {
				count++;
				currRow++;
				if ( (count % 200) == 0 && log.isInfoEnabled() ) {
					if ( log.isInfoEnabled() ) {
						log.info("Processing {} {}: {}", count, getDatumTypeDescription(),
								rowMapper.mapRow(rs, currRow));
					}
				}
				int tries = (maxWriteTries == null || maxWriteTries.intValue() < 1 ? 1 : maxWriteTries
						.intValue());
				while ( tries > 0 ) {
					try {
						handleInputResultRow(rs, cSession, cStmt);
						break;
					} catch ( QueryTimeoutException e ) {
						// pause for just a tad, then retry
						tries--;
						if ( tries > 0 ) {
							long sleepSeconds = (long) Math
									.ceil((Math.random() * writeRetryDelaySeconds));
							log.warn(
									"Timeout writing {} row {}, sleeping for {}s to retry ({} retries remaining)",
									getDatumTypeDescription(), rowMapper.mapRow(rs, currRow),
									sleepSeconds, tries);
							try {
								Thread.sleep(sleepSeconds * 1000L);
							} catch ( InterruptedException e1 ) {
								log.warn("Interrupted sleeping for retry on row {}",
										rowMapper.mapRow(rs, currRow));
							}
						} else {
							log.error("Timeout writing {} row {}, giving up", getDatumTypeDescription(),
									rowMapper.mapRow(rs, currRow));
							throw e;
						}
					}
				}
			}
			result.setSuccess(true);
		} catch ( SQLException e ) {
			log.error("SQL error", e);
			throw e;
		} catch ( RuntimeException e ) {
			log.error("Exception: " + e.getMessage(), e);
			throw e;
		} finally {
			if ( rs != null ) {
				rs.close();
			}
			result.setProcessedCount(count);
			log.info("Processed {} {} rows", count, getDatumTypeDescription());
		}
	}

	protected abstract int getDatumType();

	protected abstract String getDatumTypeDescription();

	protected abstract void handleInputResultRow(ResultSet rs, Session cSession,
			com.datastax.driver.core.PreparedStatement cStmt) throws SQLException;

	public Integer getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
	}

	public Integer getFetchSize() {
		return fetchSize;
	}

	public void setFetchSize(Integer fetchSize) {
		this.fetchSize = fetchSize;
	}

	public String getCassandraKeyspace() {
		return cassandraKeyspace;
	}

	public void setCassandraKeyspace(String cassandraKeyspace) {
		this.cassandraKeyspace = cassandraKeyspace;
	}

	public JdbcOperations getJdbcOperations() {
		return jdbcOperations;
	}

	public void setJdbcOperations(JdbcOperations jdbcOperations) {
		this.jdbcOperations = jdbcOperations;
	}

	public Cluster getCluster() {
		return cluster;
	}

	public void setCluster(Cluster cluster) {
		this.cluster = cluster;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public String getCql() {
		return cql;
	}

	public void setCql(String cql) {
		this.cql = cql;
	}

	public Integer getStartingOffset() {
		return startingOffset;
	}

	public void setStartingOffset(Integer startingOffset) {
		this.startingOffset = startingOffset;
	}

	public String getCountSql() {
		return countSql;
	}

	public void setCountSql(String countSql) {
		this.countSql = countSql;
	}

	public Integer getMaxWriteTries() {
		return maxWriteTries;
	}

	public void setMaxWriteTries(Integer timeoutMaxRetries) {
		this.maxWriteTries = timeoutMaxRetries;
	}

	public Integer getWriteRetryDelaySeconds() {
		return writeRetryDelaySeconds;
	}

	public void setWriteRetryDelaySeconds(Integer writeRetryDelaySeconds) {
		this.writeRetryDelaySeconds = writeRetryDelaySeconds;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	public String getDateRangeSql() {
		return dateRangeSql;
	}

	public void setDateRangeSql(String dateRangeSql) {
		this.dateRangeSql = dateRangeSql;
	}

	public List<Object> getSqlParameters() {
		return sqlParameters;
	}

	public void setSqlParameters(List<Object> sqlParameters) {
		this.sqlParameters = sqlParameters;
	}

}
