/* ==================================================================
 * MigrateConsumptionDatum.java - Nov 22, 2013 3:05:41 PM
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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

/**
 * Migrate the ConsumptionDatum table.
 * 
 * @author matt
 * @version 1.0
 */
public class MigrateConsumptionDatum {

	private final String sql = "SELECT id, created, posted, node_id, source_id, "
			+ "price_loc_id, watts, watt_hour, prev_datum FROM solarnet.sn_consum_datum";
	private final Integer maxResults = 25;
	private final Integer fetchSize = 250;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public void migrate(JdbcOperations ops) {
		// execute SQL
		ops.execute(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_READ_ONLY);
				if ( maxResults != null ) {
					stmt.setMaxRows(maxResults);
				}
				if ( fetchSize != null ) {
					stmt.setFetchSize(fetchSize);
				}
				return stmt;
			}
		}, new PreparedStatementCallback<Object>() {

			@Override
			public Object doInPreparedStatement(PreparedStatement ps) throws SQLException,
					DataAccessException {
				boolean haveResults = ps.execute();
				if ( haveResults ) {
					handleResults(ps, null);
				}
				return null;
			}
		});
	}

	private void handleResults(PreparedStatement ps, Integer offset) throws SQLException {
		ResultSet rs = ps.getResultSet();
		try {
			int currRow = 1;
			int maxRow = (maxResults == null ? -1 : maxResults.intValue());
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
			while ( rs.next() && (maxRow-- != 0) ) {
				log.debug("Got row: {}", rowMapper.mapRow(rs, currRow++));
			}
		} finally {
			if ( rs != null ) {
				rs.close();
			}
		}
	}
}
