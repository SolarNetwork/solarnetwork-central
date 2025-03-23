/* ==================================================================
 * SelectUserSecretEntity.java - 22/03/2025 7:17:56 am
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
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.user.dao.UserSecretFilter;
import net.solarnetwork.central.user.domain.UserSecretEntity;

/**
 * Support for SELECT for {@link UserSecretEntity} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectUserSecretEntity
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 100;

	private final UserSecretFilter filter;
	private int fetchSize;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SelectUserSecretEntity(UserSecretFilter filter) {
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
	 *         if any argument is {@code null}
	 */
	public SelectUserSecretEntity(UserSecretFilter filter, int fetchSize) {
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
				SELECT us.user_id, us.topic_id, us.skey, us.created, us.modified, us.sdata
				FROM solaruser.user_secret us
				""");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getUserIds(), "us.user_id", where);
		}
		if ( filter.hasTopicCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getTopicIds(), "us.topic_id", where);
		}
		if ( filter.hasKeyCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getKeys(), "us.skey", where);
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY us.user_id, us.topic_id, us.skey");
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
		if ( filter.hasUserCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		}
		if ( filter.hasTopicCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getTopicIds());
		}
		if ( filter.hasKeyCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getKeys());
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
