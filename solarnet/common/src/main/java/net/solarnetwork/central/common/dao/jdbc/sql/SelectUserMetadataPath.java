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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.central.common.dao.jdbc.sql.SelectUserMetadataEntity.SQL_PRUNED_JDATA;
import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.UserMetadata;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.util.StringUtils;

/**
 * Select for {@link UserMetadata} JSON.
 * 
 * <p>
 * The result columns in the SQL are:
 * </p>
 * 
 * <ol>
 * <li>jdata (TEXT)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class SelectUserMetadataPath implements PreparedStatementCreator, SqlProvider {

	private final UserMetadataFilter filter;
	private final String path;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @param path
	 *        the path to extract
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null} or {@code path} is empty or
	 *         {@code filter.userId} is {@code null}
	 */
	public SelectUserMetadataPath(UserMetadataFilter filter, String path) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.path = requireNonEmptyArgument(StringUtils.nonEmptyString(path), "path");
		requireNonNullArgument(requireNonEmptyArgument(filter.getUserIds(), "filter.userId")[0],
				"filter.userId");
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("SELECT ");
		if ( filter.hasTokenCriteria() ) {
			buf.append(SQL_PRUNED_JDATA);
		} else {
			buf.append("um.jdata");
		}
		buf.append(" #> regexp_split_to_array(ltrim(?, '/'), '/')\n");

		buf.append("FROM solaruser.user_meta um\n");
		if ( filter.hasTokenCriteria() ) {
			buf.append("INNER JOIN solaruser.user_auth_token_login t ON t.user_id = um.user_id\n");
		}
	}

	private void sqlWhere(StringBuilder buf) {
		final var where = new StringBuilder();
		int idx = 0;
		idx += whereOptimizedArrayContains(filter.getUserIds(), "um.user_id", where);
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

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		prepareCore(con, stmt, 0);
		return stmt;
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		stmt.setString(++p, path);
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		if ( filter.hasTokenCriteria() ) {
			stmt.setString(++p, filter.tokenId());
		}
		return p;
	}

}
