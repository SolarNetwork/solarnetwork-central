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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.orderBySorts;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.SolarNodeMetadataFilter;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.support.SearchFilterUtils;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.SearchFilter;

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
 * @version 1.3
 */
public final class SelectSolarNodeMetadata
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** Sort by the metadata creation date. */
	public static final String SORT_BY_CREATED = "created";

	/** Sort by the node ID. */
	public static final String SORT_BY_NODE = "node";

	/** Sort by the metadata modification date. */
	public static final String SORT_BY_UPDATED = "updated";

	/**
	 * The mapping of sort keys to SQL column names.
	 * 
	 * <p>
	 * This map contains the following entries:
	 * </p>
	 * 
	 * <ol>
	 * <li>created -&gt; nm.created</li>
	 * <li>node -&gt; nm.node_id</li>
	 * <li>updated -&gt; nm.updated</li>
	 * </ol>
	 * 
	 * @since 1.2
	 * @see CommonSqlUtils#orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> SORT_KEY_MAPPING;

	static {
		Map<String, String> map = new LinkedHashMap<>(4);
		map.put(SORT_BY_CREATED, "nm.created");
		map.put(SORT_BY_NODE, "nm.node_id");
		map.put(SORT_BY_UPDATED, "nm.updated");
		SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
	}

	/**
	 * SQL expression for the node metadata restricted to the token policy's
	 * node metadata paths.
	 */
	private static final String SQL_PRUNED_JDATA = "solarcommon.jsonb_prune_ant_paths(nm.jdata, t.jpolicy -> 'nodeMetadataPaths')";

	private final SolarNodeMetadataFilter filter;
	private final @Nullable SearchFilter searchFilter;

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
		this.searchFilter = filter.toSearchFilter();
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("SELECT nm.node_id, nm.created, nm.updated, ");
		if ( filter.hasTokenCriteria() ) {
			// restrict the metadata to the token policy's node metadata paths
			buf.append(SQL_PRUNED_JDATA).append(" AS jdata\n");
		} else {
			buf.append("nm.jdata\n");
		}
		buf.append("FROM solarnet.sn_node_meta nm\n");
		if ( filter.hasTokenCriteria() ) {
			buf.append("INNER JOIN solaruser.user_node un ON un.node_id = nm.node_id\n");
			// NOTE the user_auth_token_login view is used because SolarQuery has no
			// privileges on the solaruser.user_auth_token table
			buf.append("INNER JOIN solaruser.user_auth_token_login t ON t.user_id = un.user_id\n");
		}
	}

	private void sqlWhere(StringBuilder buf) {
		final var where = new StringBuilder();
		int idx = 0;
		idx += whereOptimizedArrayContains(filter.getNodeIds(), "nm.node_id", where);
		if ( searchFilter != null ) {
			where.append("\tAND jsonb_path_exists(nm.jdata, ?::jsonpath)\n");
			idx += 1;
		}
		if ( filter.hasTokenCriteria() ) {
			where.append("\tAND t.username = ?\n");
			// omit results whose metadata is restricted to nothing
			where.append("\tAND ").append(SQL_PRUNED_JDATA).append(" IS NOT NULL\n");
			idx++;
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		if ( filter.hasNodeCriteria() && filter.nodeIds().length == 1 ) {
			// at most one result, skip order
			return;
		}
		final var order = new StringBuilder();
		int idx = 2;
		if ( filter.hasSorts() ) {
			idx = orderBySorts(filter.sorts(), SORT_KEY_MAPPING, order);
		}
		if ( order.isEmpty() ) {
			order.append(", nm.node_id");
		}
		buf.append("\nORDER BY ").append(order.substring(idx));
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
		int p = prepareCore(con, stmt, 0);
		CommonSqlUtils.prepareLimitOffset(filter, stmt, p);
		return stmt;
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getNodeIds());
		if ( searchFilter != null ) {
			stmt.setString(++p, SearchFilterUtils.toSqlJsonPath(searchFilter));
		}
		if ( filter.hasTokenCriteria() ) {
			stmt.setString(++p, filter.tokenId());
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
