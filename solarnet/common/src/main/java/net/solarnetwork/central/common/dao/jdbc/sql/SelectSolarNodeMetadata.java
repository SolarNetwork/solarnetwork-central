/* ==================================================================
 * SelectSolarNodeMetadata.java - 12/11/2024 8:36:55 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.util.ObjectUtils;

/**
 * Select for {@link SolarNodeMetadata} instances.
 * 
 * <p>
 * The result columns in the SQL are:
 * </p>
 * 
 * <ol>
 * <li>node_id (BIGINT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>jdata (TEXT)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.1
 */
public final class SelectSolarNodeMetadata implements PreparedStatementCreator, SqlProvider {

	private final SolarNodeMetadataFilter filter;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SelectSolarNodeMetadata(SolarNodeMetadataFilter filter) {
		super();
		this.filter = ObjectUtils.requireNonNullArgument(filter, "filter");
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("""
				SELECT nm.node_id, nm.created, nm.updated, nm.jdata
				FROM solarnet.sn_node_meta nm
				""");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		idx += whereOptimizedArrayContains(filter.getNodeIds(), "nm.node_id", where);
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		if ( filter.hasNodeCriteria() && filter.getNodeIds().length == 1 ) {
			// at most one result, skip order
			return;
		}
		buf.append("\nORDER BY nm.node_id");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		sqlOrderBy(buf);
		CommonSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		prepareOptimizedArrayParameter(con, stmt, 0, filter.getNodeIds());
		return stmt;
	}

}
