/* ==================================================================
 * SelectUserEvent.java - 3/08/2022 7:08:39 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.UserEventFilter;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;

/**
 * Select for user events.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectUserEvent
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final UserEventFilter filter;
	private final int fetchSize;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter criteria
	 */
	public SelectUserEvent(UserEventFilter filter) {
		this(filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter criteria
	 * @param fetchSize
	 *        the fetch size to use, or {@literal 0} to leave unspecified
	 */
	public SelectUserEvent(UserEventFilter filter, int fetchSize) {
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
		buf.append("SELECT uel.user_id,uel.ts,uel.id,uel.kind,uel.message,uel.jdata\n");
		buf.append("FROM solaruser.user_event_log uel\n");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += CommonSqlUtils.whereOptimizedArrayContains(filter.getUserIds(), "uel.user_id", where);
		}
		if ( filter.hasKindCriteria() ) {
			where.append("\tAND string_to_array(uel.kind,'/') @> ?\n");
			idx += 1;
		}
		idx += CommonSqlUtils.whereDateRange(filter, "uel.created", where);
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY uel.user_id,uel.ts,uel.id");
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
			p = CommonSqlUtils.prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		}
		if ( filter.hasKindCriteria() ) {
			p = CommonSqlUtils.prepareArrayParameter(con, stmt, p, filter.getKinds());
		}
		if ( filter.hasDate() ) {
			p = CommonSqlUtils.prepareDateRange(filter, con, stmt, p);
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
