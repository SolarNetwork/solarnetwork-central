/* ==================================================================
 * CommonJdbcUtils.java - 3/08/2022 11:12:21 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.sql;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.OptimizedQueryCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.domain.Identity;

/**
 * Common JDBC utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class CommonJdbcUtils {

	private CommonJdbcUtils() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Get an array result column value.
	 * 
	 * @param <T>
	 *        the expected array type
	 * @param rs
	 *        the result set
	 * @param colNum
	 *        the column number
	 * @return the array
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @throws ClassCastException
	 *         if a casting error occurs
	 * @since 2.1
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getArray(ResultSet rs, int colNum) throws SQLException {
		Array a = rs.getArray(colNum);
		if ( a == null ) {
			return null;
		}
		return (T) a.getArray();
	}

	/**
	 * Get a UUID column value.
	 * 
	 * <p>
	 * This method can be more efficient than calling
	 * {@link ResultSet#getString(int)} if the JDBC driver returns a UUID
	 * instance natively. Otherwise this method will call {@code toString()} on
	 * the column value and parse that as a UUID.
	 * </p>
	 * 
	 * @param rs
	 *        the result set to read from
	 * @param column
	 *        the column number to get as a UUID
	 * @return the UUID, or {@literal null} if the column value is null
	 * @throws SQLException
	 *         if an error occurs
	 * @throws IllegalArgumentException
	 *         if the column value is non-null but does not conform to the
	 *         string representation as described in {@link UUID#toString()}
	 * @since 2.1
	 */
	public static UUID getUuid(ResultSet rs, int column) throws SQLException {
		Object sid = rs.getObject(column);
		return (sid instanceof UUID ? (UUID) sid : sid != null ? UUID.fromString(sid.toString()) : null);
	}

	/**
	 * Execute a query for a count result.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param creator
	 *        the statement creator; if implements
	 *        {@link CountPreparedStatementCreatorProvider} then
	 *        {@link CountPreparedStatementCreatorProvider#countPreparedStatementCreator()}
	 *        will be used
	 * @return the result, or {@literal null} if no result count is available
	 * @since 2.1
	 */
	public static Long executeCountQuery(JdbcOperations jdbcTemplate, PreparedStatementCreator creator) {
		return jdbcTemplate.query(creator, new ResultSetExtractor<Long>() {
	
			@Override
			public Long extractData(ResultSet rs) throws SQLException, DataAccessException {
				return rs.next() ? rs.getLong(1) : null;
			}
		});
	}

	/**
	 * Standardized utility to execute a filter based query.
	 * 
	 * @param <M>
	 *        the filter result type
	 * @param <K>
	 *        the filter result key type
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param filter
	 *        the pagination criteria
	 * @param sql
	 *        the SQL to execute
	 * @param mapper
	 *        the row mapper to use
	 * @return the results, never {@literal null}
	 * @since 2.1
	 */
	public static <M extends Identity<K>, K> FilterResults<M, K> executeFilterQuery(
			JdbcOperations jdbcTemplate, PaginationCriteria filter, PreparedStatementCreator sql,
			RowMapper<M> mapper) {
		Long totalCount = null;
		if ( filter.getMax() != null && sql instanceof CountPreparedStatementCreatorProvider
				&& !(filter instanceof OptimizedQueryCriteria
						&& ((OptimizedQueryCriteria) filter).isWithoutTotalResultsCount()) ) {
			totalCount = executeCountQuery(jdbcTemplate,
					((CountPreparedStatementCreatorProvider) sql).countPreparedStatementCreator());
		}
	
		List<M> results = jdbcTemplate.query(sql, mapper);
	
		if ( filter.getMax() == null ) {
			totalCount = (long) results.size();
		}
	
		int offset = (filter.getOffset() != null ? filter.getOffset() : 0);
		return new BasicFilterResults<>(results, totalCount, offset, results.size());
	}

}