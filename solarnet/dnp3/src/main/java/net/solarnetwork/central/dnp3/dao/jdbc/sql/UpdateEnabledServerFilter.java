/* ==================================================================
 * UpdateEnabledServerFilter.java - 8/08/2023 8:29:22 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.dnp3.dao.ServerFilter;

/**
 * Update the enabled flag of server-related entities.
 * 
 * @author matt
 * @version 1.0
 */
public class UpdateEnabledServerFilter implements PreparedStatementCreator, SqlProvider {

	private String tableName;
	private String serverIdColumnName;
	private Long userId;
	private ServerFilter filter;
	private boolean enabled;

	/**
	 * Constructor.
	 * 
	 * @param tableName
	 *        the table name to update
	 * @param serverIdColumnName
	 *        the server ID column name
	 * @param userId
	 *        the user ID of the the entity to update
	 * @param filter
	 *        the optional filter
	 * @param enabled
	 *        the desired enabled state
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code filter} is {@literal null}
	 */
	public UpdateEnabledServerFilter(String tableName, String serverIdColumnName, Long userId,
			ServerFilter filter, boolean enabled) {
		super();
		this.tableName = requireNonNullArgument(tableName, "tableName");
		this.serverIdColumnName = requireNonNullArgument(serverIdColumnName, "serverIdColumnName");
		this.userId = requireNonNullArgument(userId, "userId");
		this.filter = filter;
		this.enabled = enabled;
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
				UPDATE %s
				SET enabled = ?, modified = CURRENT_TIMESTAMP
				WHERE user_id = ?
				""".formatted(tableName));
	}

	private void sqlWhere(StringBuilder where) {
		if ( filter == null ) {
			return;
		}
		if ( filter.hasServerCriteria() ) {
			whereOptimizedArrayContains(filter.getServerIds(), serverIdColumnName, where);
		}
		if ( filter.hasIdentifierCriteria() ) {
			whereOptimizedArrayContains(filter.getIdentifiers(), "ident", where);
		}
		if ( filter.hasIndexCriteria() ) {
			whereOptimizedArrayContains(filter.getIndexes(), "idx", where);
		}
		if ( filter.hasEnabledCriteria() ) {
			where.append("\tAND enabled = ?\n");
		}
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setBoolean(++p, enabled);
		stmt.setObject(++p, userId);
		if ( filter != null ) {
			p = prepareWhere(con, stmt, p);
		}
		return stmt;
	}

	private int prepareWhere(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( filter.hasServerCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getServerIds());
		}
		if ( filter.hasIdentifierCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getIdentifiers());
		}
		if ( filter.hasIndexCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getIndexes());
		}
		if ( filter.hasEnabledCriteria() ) {
			stmt.setBoolean(++p, filter.getEnabled().booleanValue());
		}
		return p;
	}

}
