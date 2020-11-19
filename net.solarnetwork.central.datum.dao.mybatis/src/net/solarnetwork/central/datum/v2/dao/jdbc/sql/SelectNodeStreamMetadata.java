/* ==================================================================
 * SelectNodeStreamMetadata.java - 19/11/2020 3:21:24 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql;

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils.NODE_STREAM_METADATA_SORT_KEY_MAPPING;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils.orderBySorts;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.NodeMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils;

/**
 * Generate dynamic SQL for a "find node metadata" query.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectNodeStreamMetadata
		implements PreparedStatementCreator, SqlProvider {

	private final NodeMetadataCriteria filter;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectNodeStreamMetadata(NodeMetadataCriteria filter) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument must not be null.");
		}
		this.filter = filter;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		DatumSqlUtils.nodeMetadataFilterSql(filter, buf);
		StringBuilder order = new StringBuilder();
		int idx = orderBySorts(filter.getSorts(), NODE_STREAM_METADATA_SORT_KEY_MAPPING, order);
		if ( idx > 0 ) {
			buf.append("\nORDER BY ");
			buf.append(order.substring(idx));
		}
		return buf.toString();
	}

	private PreparedStatement createStatement(Connection con, String sql) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		DatumSqlUtils.nodeMetadataFilterPrepare(filter, con, stmt, 0);
		return stmt;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		return createStatement(con, getSql());
	}

}
