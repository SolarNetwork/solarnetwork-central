/* ==================================================================
 * SelectObjectDatumStreamAliasGrantStream.java - 31/10/2026 8:17:22 pm
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
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantStream;

/**
 * Support for SELECT for {@link ObjectDatumStreamAliasGrantStream} objects.
 *
 * <p>
 * The grants are only used to find candidate streams.
 * {@code solardatm.da_datm_alias_allowed()} has the final say, so the results
 * are always the streams an alias on the given node would resolve to. Only the
 * user, grant, and pagination criteria of the filter are used.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectObjectDatumStreamAliasGrantStream
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final Long granteeId;
	private final Long aliasNodeId;
	private final ObjectDatumStreamAliasGrantFilter filter;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * @param granteeId
	 *        the ID of the user granted access
	 * @param aliasNodeId
	 *        the ID of the node that would host the aliases
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SelectObjectDatumStreamAliasGrantStream(Long granteeId, Long aliasNodeId,
			ObjectDatumStreamAliasGrantFilter filter) {
		this(granteeId, aliasNodeId, filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 *
	 * @param granteeId
	 *        the ID of the user granted access
	 * @param aliasNodeId
	 *        the ID of the node that would host the aliases
	 * @param filter
	 *        the filter
	 * @param fetchSize
	 *        the fetch size
	 * @throws IllegalArgumentException
	 *         if {@code granteeId}, {@code aliasNodeId}, or {@code filter} is
	 *         {@code null}
	 */
	public SelectObjectDatumStreamAliasGrantStream(Long granteeId, Long aliasNodeId,
			ObjectDatumStreamAliasGrantFilter filter, int fetchSize) {
		super();
		this.granteeId = requireNonNullArgument(granteeId, "granteeId");
		this.aliasNodeId = requireNonNullArgument(aliasNodeId, "aliasNodeId");
		this.filter = requireNonNullArgument(filter, "filter");
		this.fetchSize = fetchSize;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		buf.append("ORDER BY dag.user_id, s.node_id, s.source_id");
		CommonSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("""
				SELECT DISTINCT dag.user_id, s.node_id, s.source_id
				FROM solaruser.da_datm_alias_grant dag
				INNER JOIN solaruser.user_node un ON un.user_id = dag.user_id
					AND (dag.node_ids IS NULL OR un.node_id = ANY(dag.node_ids))
				INNER JOIN solardatm.da_datm_meta s ON s.node_id = un.node_id
					AND (dag.source_regexps IS NULL OR s.source_id ~ ANY(dag.source_regexps))
				WHERE dag.grantee_id = ?
					AND dag.enabled = TRUE
				""");
		whereOptimizedArrayContains(filter.getUserIds(), "dag.user_id", buf);
		whereOptimizedArrayContains(filter.getGrantIds(), "dag.id", buf);
		buf.append("\tAND solardatm.da_datm_alias_allowed(s.node_id, s.source_id::TEXT, ?)\n");
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
		stmt.setObject(++p, granteeId);
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getGrantIds());
		stmt.setObject(++p, aliasNodeId);
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
