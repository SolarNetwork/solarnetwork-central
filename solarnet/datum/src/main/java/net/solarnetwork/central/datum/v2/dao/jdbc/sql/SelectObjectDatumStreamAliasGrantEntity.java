/* ==================================================================
 * SelectObjectDatumStreamAliasGrantEntity.java - 31/10/2026 4:25:10 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereEqual;
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
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;

/**
 * Support for SELECT for {@link ObjectDatumStreamAliasGrantEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public final class SelectObjectDatumStreamAliasGrantEntity
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final ObjectDatumStreamAliasGrantFilter filter;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SelectObjectDatumStreamAliasGrantEntity(ObjectDatumStreamAliasGrantFilter filter) {
		this(filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @param fetchSize
	 *        the fetch size
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@code null}
	 */
	public SelectObjectDatumStreamAliasGrantEntity(ObjectDatumStreamAliasGrantFilter filter,
			int fetchSize) {
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
				SELECT dag.user_id, dag.id, dag.created, dag.modified, dag.enabled
					, dag.allow_public, dag.grantee_id, dag.node_ids, dag.source_ids
				FROM solaruser.da_datm_alias_grant dag
				""");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		idx += whereOptimizedArrayContains(filter.getUserIds(), "dag.user_id", where);
		idx += whereOptimizedArrayContains(filter.getGrantIds(), "dag.id", where);
		idx += whereOptimizedArrayContains(filter.getGranteeIds(), "dag.grantee_id", where);
		idx += whereEqual(filter.getEnabled(), "dag.enabled", where);
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY dag.user_id, dag.id");
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = prepareCore(con, stmt, 0);
		CommonSqlUtils.prepareLimitOffset(filter, stmt, p);
		if ( fetchSize > 0 ) {
			stmt.setFetchSize(fetchSize);
		}
		return stmt;
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getGrantIds());
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getGranteeIds());
		p = prepareParameter(stmt, p, filter.getEnabled());
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
			sqlWhere(buf);
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
