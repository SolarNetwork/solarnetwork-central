/* ==================================================================
 * DeleteUserNodeInstructionTaskEntity.java - 10/11/2025 5:03:19 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskFilter;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;

/**
 * Support for DELETE for {@link UserNodeInstructionTaskEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public class DeleteUserNodeInstructionTaskEntity implements PreparedStatementCreator, SqlProvider {

	private final UserNodeInstructionTaskFilter filter;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 */
	public DeleteUserNodeInstructionTaskEntity(UserNodeInstructionTaskFilter filter) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		requireNonNullArgument(filter.getUserId(), "filter.userId");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		return buf.toString();
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("""
				DELETE FROM solaruser.user_node_instr_task
				""");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		idx += whereOptimizedArrayContains(filter.getUserIds(), "user_id", where);
		if ( filter.hasNodeCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getNodeIds(), "node_id", where);
		}
		if ( filter.hasTopicCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getTopics(), "topic", where);
		}
		if ( filter.hasTaskCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getTaskIds(), "id", where);
		}
		if ( filter.hasClaimableJobStateCriteria() ) {
			idx += whereOptimizedArrayContains(filter.claimableJobStateKeys(), "status", where);
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		prepareCore(con, stmt, 0);
		return stmt;
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		if ( filter.hasNodeCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getNodeIds());
		}
		if ( filter.hasTopicCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getTopics());
		}
		if ( filter.hasTaskCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getTaskIds());
		}
		if ( filter.hasClaimableJobStateCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.claimableJobStateKeys());
		}
		return p;
	}

}
