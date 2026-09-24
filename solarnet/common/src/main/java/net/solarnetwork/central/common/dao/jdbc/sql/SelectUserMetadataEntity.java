/* ==================================================================
 * SelectUserMetadataEntity.java - 24 Sept 2026 6:37:43 am
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
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.domain.UserMetadata;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.central.support.SearchFilterUtils;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.util.SearchFilter;

/**
 * Select for {@link UserMetadata} instances.
 * 
 * <p>
 * The result columns in the SQL are:
 * </p>
 * 
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>jdata (TEXT)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class SelectUserMetadataEntity
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** Sort by the metadata creation date. */
	public static final String SORT_BY_CREATED = "created";

	/** Sort by the user ID. */
	public static final String SORT_BY_USER = "user";

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
	 * <li>created -&gt; um.created</li>
	 * <li>user -&gt; um.user_id</li>
	 * <li>updated -&gt; um.updated</li>
	 * </ol>
	 * 
	 * @since 1.2
	 * @see CommonSqlUtils#orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> SORT_KEY_MAPPING;

	static {
		Map<String, String> map = new LinkedHashMap<>(4);
		map.put(SORT_BY_CREATED, "um.created");
		map.put(SORT_BY_USER, "um.user_id");
		map.put(SORT_BY_UPDATED, "um.updated");
		SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
	}

	/**
	 * SQL expression for the user metadata restricted to the token policy's
	 * user metadata paths.
	 */
	public static final String SQL_PRUNED_JDATA = "solarcommon.jsonb_prune_ant_paths(um.jdata, t.jpolicy -> 'userMetadataPaths')";

	private final net.solarnetwork.central.domain.UserMetadataFilter filter;
	private final @Nullable SearchFilter searchFilter;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SelectUserMetadataEntity(UserMetadataFilter filter) {
		this.filter = ObjectUtils.requireNonNullArgument(filter, "filter");
		this.searchFilter = filter.toSearchFilter();
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("SELECT um.user_id, um.created, um.updated, ");
		if ( filter.hasTokenCriteria() ) {
			// restrict the metadata to the token policy's node metadata paths
			buf.append(SQL_PRUNED_JDATA).append(" AS jdata\n");
		} else {
			buf.append("um.jdata\n");
		}
		buf.append("FROM solaruser.user_meta um\n");
		if ( filter.hasTokenCriteria() ) {
			// NOTE the user_auth_token_login view is used because SolarQuery has no
			// privileges on the solaruser.user_auth_token table
			buf.append("INNER JOIN solaruser.user_auth_token_login t ON t.user_id = um.user_id\n");
		}
	}

	private void sqlWhere(StringBuilder buf) {
		final var where = new StringBuilder();
		int idx = 0;
		idx += whereOptimizedArrayContains(filter.getUserIds(), "um.user_id", where);
		if ( searchFilter != null ) {
			where.append("\tAND jsonb_path_exists(um.jdata, ?::jsonpath)\n");
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
		if ( filter.hasUserCriteria() && filter.userIds().length == 1 ) {
			// at most one result, skip order
			return;
		}
		final var order = new StringBuilder();
		int idx = 2;
		if ( filter.hasSorts() ) {
			idx = orderBySorts(filter.sorts(), SORT_KEY_MAPPING, order);
		}
		if ( order.isEmpty() ) {
			order.append(", um.user_id");
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
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
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
