/* ==================================================================
 * SelectServerAuthConfiguration.java - 6/08/2023 6:15:18 pm
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
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.dnp3.dao.ServerFilter;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;

/**
 * Select {@link ServerAuthConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectServerAuthConfiguration
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final ServerFilter filter;
	private final int fetchSize;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 */
	public SelectServerAuthConfiguration(ServerFilter filter) {
		this(filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 */
	public SelectServerAuthConfiguration(ServerFilter filter, int fetchSize) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.fetchSize = fetchSize;
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

	private void sqlCore(StringBuilder buf) {
		buf.append("""
				SELECT dsa.user_id, dsa.server_id, dsa.ident, dsa.created, dsa.modified, dsa.enabled
					, dsa.cname
				FROM solardnp3.dnp3_server_auth dsa
				""");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getUserIds(), "dsa.user_id", where);
		}
		if ( filter.hasServerCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getServerIds(), "dsa.server_id", where);
		}
		if ( filter.hasIdentifierCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getIdentifiers(), "dsa.ident", where);
		}
		if ( filter.hasEnabledCriteria() ) {
			where.append("\tAND dsa.enabled = ?\n");
			idx += 1;
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY dsa.user_id, dsa.server_id, dsa.ident");
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = prepareCore(con, stmt, 0);
		CommonSqlUtils.prepareLimitOffset(filter, con, stmt, p);
		if ( fetchSize > 0 ) {
			stmt.setFetchSize(fetchSize);
		}
		return stmt;
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( filter.hasUserCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		}
		if ( filter.hasServerCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getServerIds());
		}
		if ( filter.hasIdentifierCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getIdentifiers());
		}
		if ( filter.hasEnabledCriteria() ) {
			stmt.setBoolean(++p, filter.getEnabled().booleanValue());
		}
		return p;
	}

	@Override
	public PreparedStatementCreator countPreparedStatementCreator() {
		return new CountPreparedStatementCreator();
	}

	private final class CountPreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

		@Override
		public String getSql() {
			StringBuilder buf = new StringBuilder();
			sqlCore(buf);
			return CommonSqlUtils.wrappedCountQuery(buf.toString());
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			PreparedStatement stmt = con.prepareStatement(getSql());
			prepareCore(con, stmt, 0);
			return stmt;
		}

	}

}
