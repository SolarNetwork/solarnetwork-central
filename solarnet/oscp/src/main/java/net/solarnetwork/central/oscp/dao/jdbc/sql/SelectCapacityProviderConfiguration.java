/* ==================================================================
 * SelectCapacityProviderConfiguration.java - 12/08/2022 4:41:12 pm
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

package net.solarnetwork.central.oscp.dao.jdbc.sql;

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
import net.solarnetwork.central.oscp.dao.ConfigurationFilter;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;

/**
 * Select {@link CapacityProviderConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectCapacityProviderConfiguration
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final ConfigurationFilter filter;
	private final int fetchSize;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter criteria
	 */
	public SelectCapacityProviderConfiguration(ConfigurationFilter filter) {
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
	public SelectCapacityProviderConfiguration(ConfigurationFilter filter, int fetchSize) {
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
				SELECT ocp.id, ocp.created, ocp.modified, ocp.user_id, ocp.enabled
					, ocp.fp_id, ocp.reg_status, ocp.cname, ocp.url, ocp.sprops
				FROM solaroscp.oscp_cp_conf ocp
				""");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getUserIds(), "ocp.user_id", where);
		}
		if ( filter.hasConfigurationCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getConfigurationIds(), "ocp.id", where);
		}
		if ( filter.hasProviderCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getProviderIds(), "ocp.fp_id", where);
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY ocp.user_id, ocp.id");
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
		if ( filter.hasConfigurationCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getConfigurationIds());
		}
		if ( filter.hasProviderCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getProviderIds());
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
