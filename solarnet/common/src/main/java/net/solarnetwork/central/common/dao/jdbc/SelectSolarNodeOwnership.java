/* ==================================================================
 * SelectSolarNodeOwnership.java - 6/10/2021 9:43:09 AM
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

package net.solarnetwork.central.common.dao.jdbc;

import static java.util.Objects.requireNonNull;
import static net.solarnetwork.central.common.dao.jdbc.CommonSqlUtils.prepareOptimizedArrayParameter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;

/**
 * Select for {@link BasicSolarNodeOwnership} instances.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectSolarNodeOwnership implements PreparedStatementCreator, SqlProvider {

	private final Long[] nodeIds;
	private final Long[] userIds;

	/**
	 * Select for a single node ID.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @return the select statement
	 * @throws NullPointerException
	 *         if {@code nodeId} is {@literal null}
	 */
	public static SelectSolarNodeOwnership selectForNode(Long nodeId) {
		return new SelectSolarNodeOwnership(
				new Long[] { requireNonNull(nodeId, "The nodeId argument must not be null.") }, null);
	}

	/**
	 * Select for a single user ID.
	 * 
	 * @param userId
	 *        the user ID
	 * @return the select statement
	 * @throws NullPointerException
	 *         if {@code userId} is {@literal null}
	 */
	public static SelectSolarNodeOwnership selectForUser(Long userId) {
		return new SelectSolarNodeOwnership(null,
				new Long[] { requireNonNull(userId, "The userId argument must not be null.") });
	}

	/**
	 * Select for a single node and user ID.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param userId
	 *        the user ID
	 * @return the select statement
	 * @throws NullPointerException
	 *         if {@code nodeId} or {@code userId} is {@literal null}
	 */
	public static SelectSolarNodeOwnership selectForNodeUser(Long nodeId, Long userId) {
		return new SelectSolarNodeOwnership(
				new Long[] { requireNonNull(nodeId, "The nodeId argument must not be null.") },
				new Long[] { requireNonNull(userId, "The userId argument must not be null.") });
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeIds
	 *        the optional node IDs to filter on
	 * @param userIds
	 *        the optional user IDs to filter on
	 */
	public SelectSolarNodeOwnership(Long[] nodeIds, Long[] userIds) {
		super();
		this.nodeIds = nodeIds;
		this.userIds = userIds;
	}

	private void sqlCore(StringBuilder buf, boolean ordered) {
		buf.append("SELECT node_id, user_id, private, archived\nFROM solaruser.user_node");
		sqlWhere(buf);
	}

	private void sqlWhere(StringBuilder buf) {
		boolean haveWhere = false;
		if ( nodeIds != null && nodeIds.length > 0 ) {
			haveWhere = true;
			buf.append("\nWHERE node_id = ");
			if ( nodeIds.length > 1 ) {
				buf.append("ANY(?)");
			} else {
				buf.append("?");
			}
		}
		if ( userIds != null && userIds.length > 0 ) {
			if ( !haveWhere ) {
				buf.append("\nWHERE ");
				haveWhere = true;
			} else {
				buf.append("\nAND ");
			}
			buf.append("user_id = ");
			if ( userIds.length > 1 ) {
				buf.append("ANY(?)");
			} else {
				buf.append("?");
			}
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		if ( nodeIds != null && nodeIds.length == 1 ) {
			// at most one result, skip order
			return;
		}
		buf.append("\nORDER BY node_id");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf, true);
		sqlOrderBy(buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = 0;
		p = prepareOptimizedArrayParameter(con, stmt, p, nodeIds);
		p = prepareOptimizedArrayParameter(con, stmt, p, userIds);
		return stmt;
	}

}
