/* ==================================================================
 * SelectCloudIntegrationConfiguration.java - 2/10/2024 8:46:14 am
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

package net.solarnetwork.central.c2c.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.dao.CloudIntegrationFilter;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;

/**
 * Support for SELECT for {@link CloudIntegrationConfiguration} entities.
 *
 * @author matt
 * @version 1.1
 */
public final class SelectCloudIntegrationConfiguration
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final CloudIntegrationFilter filter;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 */
	public SelectCloudIntegrationConfiguration(CloudIntegrationFilter filter) {
		this(filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 */
	public SelectCloudIntegrationConfiguration(CloudIntegrationFilter filter, int fetchSize) {
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
				SELECT ci.user_id, ci.id, ci.created, ci.modified, ci.enabled
					, ci.cname, ci.sident, ci.sprops
				FROM solardin.cin_integration ci
				""");
		if ( filter.hasDatumStreamCriteria() ) {
			buf.append("""
					INNER JOIN solardin.cin_datum_stream_map cdsm ON cdsm.int_id = ci.id
					INNER JOIN solardin.cin_datum_stream cds ON cds.map_id = cdsm.id
					""");
		}
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getUserIds(), "ci.user_id", where);
		}
		if ( filter.hasIntegrationCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getIntegrationIds(), "ci.id", where);
		}
		if ( filter.hasServiceIdentifierCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getServiceIdentifiers(), "ci.sident", where);
		}
		if ( filter.hasDatumStreamCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getDatumStreamIds(), "cds.id", where);
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY ci.user_id, ci.id");
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
		if ( filter.hasIntegrationCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getIntegrationIds());
		}
		if ( filter.hasServiceIdentifierCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getServiceIdentifiers());
		}
		if ( filter.hasDatumStreamCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getDatumStreamIds());
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
