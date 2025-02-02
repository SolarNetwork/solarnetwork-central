/* ==================================================================
 * SelectUserFluxAggregatePublishConfiguration.java - 24/06/2024 12:31:35 pm
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

package net.solarnetwork.central.user.flux.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereArrayColContains;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.flux.dao.UserFluxAggregatePublishConfigurationFilter;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfiguration;
import net.solarnetwork.util.ObjectUtils;

/**
 * Select {@link UserFluxAggregatePublishConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectUserFluxAggregatePublishConfiguration
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final UserLongCompositePK key;
	private final UserFluxAggregatePublishConfigurationFilter filter;
	private final int fetchSize;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectUserFluxAggregatePublishConfiguration(
			UserFluxAggregatePublishConfigurationFilter filter) {
		this(filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @param fetchSize
	 *        the row fetch size
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectUserFluxAggregatePublishConfiguration(
			UserFluxAggregatePublishConfigurationFilter filter, int fetchSize) {
		super();
		this.filter = ObjectUtils.requireNonNullArgument(filter, "filter");
		this.key = null;
		this.fetchSize = fetchSize;
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the primary key to select
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectUserFluxAggregatePublishConfiguration(UserLongCompositePK key) {
		super();
		this.filter = null;
		this.key = ObjectUtils.requireNonNullArgument(key, "key");
		this.fetchSize = 1;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		if ( filter != null ) {
			sqlOrderBy(buf);
		}
		CommonSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("""
				SELECT user_id, id, created, modified, node_ids, source_ids, publish, retain
				FROM solaruser.user_flux_agg_pub_settings
				""");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( key != null ) {
			where.append("""
					\tAND user_id = ?
					\tAND id = ?
					""");
			idx += 2;
		} else {
			if ( filter.hasUserCriteria() ) {
				idx += whereOptimizedArrayContains(filter.getUserIds(), "user_id", where);
			}
			if ( filter.hasNodeCriteria() ) {
				idx += whereArrayColContains(filter.getNodeIds(), "node_ids", where);
			}
			if ( filter.hasSourceCriteria() ) {
				idx += whereArrayColContains(filter.getSourceIds(), "source_ids", where);
			}
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY user_id, id");
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
		if ( key != null ) {
			stmt.setObject(++p, key.getUserId());
			stmt.setObject(++p, key.getEntityId());
		} else {
			if ( filter.hasUserCriteria() ) {
				p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
			}
			if ( filter.hasNodeCriteria() ) {
				p = prepareArrayParameter(con, stmt, p, filter.getNodeIds());
			}
			if ( filter.hasSourceCriteria() ) {
				p = prepareArrayParameter(con, stmt, p, filter.getSourceIds());
			}
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
