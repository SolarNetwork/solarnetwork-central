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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

/**
 * Abstract supporting class for migrating Datum.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class MigrateDatumSupport implements MigrationTask {

	public static final String DEFAULT_CQL = "INSERT INTO solardata.node_datum (node_id, dtype, source_id, year, ts, data_num) "
			+ "VALUES (?, ?, ?, ?, ?, ?)";

	private Integer maxResults = 1;
	private Integer fetchSize = 1000;
	private String cassandraKeyspace = "solardata";
	private JdbcOperations jdbcOperations;
	private Cluster cluster;
	private String sql;
	private String countSql;
	private String cql = DEFAULT_CQL;
	private Integer startingOffset = null;

	protected final Calendar gmtCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public MigrationResult call() throws Exception {
		return migrate(startingOffset);
	}

	protected MigrationResult migrate(final Integer offset) {
		final Session cSession = cluster.connect(getCassandraKeyspace());
		final com.datastax.driver.core.PreparedStatement cStmt = cSession.prepare(cql);
		final MigrationResult result = new MigrationResult(getDatumTypeDescription());
		result.setProcessedCount(0L);
		result.setSuccess(false);
		try {
			if ( countSql != null ) {
				long count = jdbcOperations.queryForLong(countSql);
				log.info("Found {} {} rows to process", count, getDatumTypeDescription());
			}
			// execute SQL
			jdbcOperations.execute(new PreparedStatementCreator() {

				@Override
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					con.setAutoCommit(false);
					log.info("Task SQL: {}", sql);
					PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_READ_ONLY);
					if ( getMaxResults() != null ) {
						stmt.setMaxRows(getMaxResults());
					}
					if ( getFetchSize() != null ) {
						stmt.setFetchSize(getFetchSize());
					}
					return stmt;
				}
			}, new PreparedStatementCallback<Object>() {

				@Override
				public MigrationResult doInPreparedStatement(PreparedStatement ps) throws SQLException,
						DataAccessException {
					boolean haveResults = ps.execute();
					if ( haveResults ) {
						handleResults(result, ps, offset, cSession, cStmt);
					}
					return null;
				}
			});
		} finally {
			if ( cSession != null ) {
				cSession.shutdown();
			}
		}
		result.finished();
		return result;
	}

	protected void handleResults(MigrationResult result, PreparedStatement ps, Integer offset,
			Session cSession, com.datastax.driver.core.PreparedStatement cStmt) throws SQLException {
		ResultSet rs = ps.getResultSet();
		long count = 0;
		try {
			int currRow = 1;
			int maxRow = (getMaxResults() == null ? -1 : getMaxResults().intValue());
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
				if ( (count % 200) == 0 && log.isInfoEnabled() ) {
					if ( log.isInfoEnabled() ) {
						log.info("Processing {} {}: {}", count, getDatumTypeDescription(),
								rowMapper.mapRow(rs, currRow++));
					}
				}

				handleInputResultRow(rs, cSession, cStmt);
			}
			result.setSuccess(true);
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

}
