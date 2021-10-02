/* ==================================================================
 * TieredStoredProcedureStaleDatumProcessor.java - 1/11/2019 3:22:26 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.agg;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Tiered stale datum processor that calls a stored procedure to process stale
 * rows.
 * 
 * @author matt
 * @version 1.1
 * @since 1.7
 */
public class TieredStoredProcedureStaleDatumProcessor extends TieredStaleDatumProcessor {

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 * @param taskDescription
	 *        a description of the task to use in log statements
	 */
	public TieredStoredProcedureStaleDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps,
			String taskDescription) {
		super(eventAdmin, jdbcOps, taskDescription);
	}

	private static final Pattern CALL_RETURN_COUNT = Pattern.compile("\\?\\s=\\s*call\\s+",
			Pattern.CASE_INSENSITIVE);

	@Override
	protected final int execute(final AtomicInteger remainingCount) {
		final String tierProcessType = getTierProcessType();
		final Integer tierProcessMax = getTierProcessMax();
		return getJdbcOps().execute(new ConnectionCallback<Integer>() {

			@Override
			public Integer doInConnection(Connection con) throws SQLException, DataAccessException {
				final String sql = getJdbcCall();
				final int paramCount = (int) sql.chars().filter(ch -> ch == '?').count();
				try (CallableStatement call = con.prepareCall(sql)) {
					int idx = 0;
					if ( CALL_RETURN_COUNT.matcher(sql).find() && idx < paramCount ) {
						call.registerOutParameter(++idx, Types.INTEGER);
					}
					if ( idx < paramCount ) {
						call.setString(++idx, tierProcessType);
					}
					if ( tierProcessMax != null && idx < paramCount ) {
						call.setInt(++idx, tierProcessMax);
					}
					con.setAutoCommit(true); // we want every execution of our loop to commit immediately
					int resultCount = 0;
					int processedCount = 0;
					do {
						if ( call.execute() ) {
							try (ResultSet rs = call.getResultSet()) {
								if ( rs.next() ) {
									processResultRow(rs);
									resultCount = 1;
								} else {
									resultCount = 0;
								}
							}
						} else {
							resultCount = call.getInt(1);
						}
						processedCount += resultCount;
						remainingCount.addAndGet(-resultCount);
					} while ( resultCount > 0 && remainingCount.get() > 0 );
					return processedCount;
				}
			}

		});
	}

	/**
	 * Process a procedure result set row.
	 * 
	 * <p>
	 * The {@link ResultSet} will be positioned on a valid result row when
	 * invoked. This implementation will log the column values at
	 * {@literal DEBUG} level.
	 * </p>
	 * 
	 * @param rs
	 *        the result set
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	protected void processResultRow(ResultSet rs) throws SQLException {
		// extending classes can override
		if ( log.isDebugEnabled() ) {
			ResultSetMetaData meta = rs.getMetaData();
			final int colCount = meta.getColumnCount();
			Map<String, Object> row = new LinkedHashMap<>(colCount);
			for ( int i = 1; i <= colCount; i++ ) {
				row.put(meta.getColumnName(i), rs.getObject(i));
			}
			log.debug("Processed stale row: {}", row);
		}
	}
}
