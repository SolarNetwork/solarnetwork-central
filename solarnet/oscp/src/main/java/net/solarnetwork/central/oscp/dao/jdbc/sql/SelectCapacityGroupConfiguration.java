/* ==================================================================
 * SelectCapacityGroupConfiguration.java - 12/08/2022 4:41:12 pm
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
import net.solarnetwork.central.oscp.dao.CapacityGroupFilter;
import net.solarnetwork.central.oscp.dao.ConfigurationFilter;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;

/**
 * Select {@link CapacityGroupConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public final class SelectCapacityGroupConfiguration
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private static final String[] LOCK_TABLE_NAMES = new String[] { "ocg" };

	private final ConfigurationFilter filter;
	private final CapacityGroupFilter groupFilter;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter criteria; {@link CapacityGroupFilter} instances are
	 *        supported as well
	 */
	public SelectCapacityGroupConfiguration(ConfigurationFilter filter) {
		this(filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter criteria; {@link CapacityGroupFilter} instances are
	 *        supported as well
	 * @param fetchSize
	 *        the fetch size to use, or {@literal 0} to leave unspecified
	 */
	public SelectCapacityGroupConfiguration(ConfigurationFilter filter, int fetchSize) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.groupFilter = (filter instanceof CapacityGroupFilter f) ? f : null;
		this.fetchSize = fetchSize;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		sqlOrderBy(buf);
		CommonSqlUtils.limitOffset(filter, buf);
		if ( filter.isLockResults() ) {
			CommonSqlUtils.forUpdate(filter.isSkipLockedResults(), LOCK_TABLE_NAMES, buf);
		}
		return buf.toString();
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("""
				SELECT ocg.id,ocg.created,ocg.modified,ocg.user_id,ocg.enabled,ocg.cname
					,ocg.ident,ocg.cp_meas_secs,ocg.co_meas_secs,ocg.cp_id,ocg.co_id,ocg.sprops
					,ocgcpm.meas_at AS cp_meas_at,ocgcom.meas_at AS co_meas_at
				FROM solaroscp.oscp_cg_conf ocg
				LEFT OUTER JOIN solaroscp.oscp_cg_cp_meas ocgcpm
					ON ocgcpm.user_id = ocg.user_id AND ocgcpm.cg_id = ocg.id
				LEFT OUTER JOIN solaroscp.oscp_cg_co_meas ocgcom
					ON ocgcom.user_id = ocg.user_id AND ocgcom.cg_id = ocg.id
				""");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getUserIds(), "ocg.user_id", where);
		}
		if ( filter.hasConfigurationCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getConfigurationIds(), "ocg.id", where);
		}
		if ( filter.hasProviderCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getProviderIds(), "ocg.cp_id", where);
		}
		if ( groupFilter != null && groupFilter.hasOptimizerCriteria() ) {
			idx += whereOptimizedArrayContains(groupFilter.getOptimizerIds(), "ocg.co_id", where);
		}
		if ( groupFilter != null && groupFilter.hasIdentifierCriteria() ) {
			idx += whereOptimizedArrayContains(groupFilter.getIdentifiers(), "ocg.ident", where);
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY ocg.user_id,ocg.id");
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
		if ( filter.hasConfigurationCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getConfigurationIds());
		}
		if ( filter.hasProviderCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getProviderIds());
		}
		if ( groupFilter != null && groupFilter.hasOptimizerCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, groupFilter.getOptimizerIds());
		}
		if ( groupFilter != null && groupFilter.hasIdentifierCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, groupFilter.getIdentifiers());
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
