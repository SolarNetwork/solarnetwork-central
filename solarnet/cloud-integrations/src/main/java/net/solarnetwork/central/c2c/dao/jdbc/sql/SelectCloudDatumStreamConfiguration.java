/* ==================================================================
 * SelectCloudDatumStreamConfiguration.java - 3/10/2024 1:23:36 pm
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedLikeSubstringParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereEqual;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedLike;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Support for SELECT for {@link CloudDatumStreamConfiguration} entities.
 *
 * @author matt
 * @version 1.3
 */
public final class SelectCloudDatumStreamConfiguration
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final CloudDatumStreamFilter filter;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 */
	public SelectCloudDatumStreamConfiguration(CloudDatumStreamFilter filter) {
		this(filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 */
	public SelectCloudDatumStreamConfiguration(CloudDatumStreamFilter filter, int fetchSize) {
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
		CloudIntegrationsSqlUtils.withCloudDatumStreamSourceIdsFilter(filter, buf);
		buf.append("""
				SELECT cds.user_id, cds.id, cds.created, cds.modified, cds.enabled
					, cds.cname, cds.sident
					, cds.map_id, cds.schedule, cds.kind, cds.obj_id, cds.source_id
					, cds.sprops
				FROM solardin.cin_datum_stream cds
				""");
		CloudIntegrationsSqlUtils.joinCloudDatumStreamSourceIdsFilter(filter, buf);
		if ( filter.hasIntegrationCriteria() ) {
			buf.append("""
					INNER JOIN solardin.cin_datum_stream_map cdsm ON cdsm.id = cds.map_id
					""");

		}
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getUserIds(), "cds.user_id", where);
		}
		if ( filter.hasIntegrationCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getIntegrationIds(), "cdsm.int_id", where);
		}
		if ( filter.hasDatumStreamCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getDatumStreamIds(), "cds.id", where);
		}
		if ( filter.hasServiceIdentifierCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getServiceIdentifiers(), "cds.sident", where);
		}
		if ( filter.hasDatumStreamMappingCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getDatumStreamMappingIds(), "cds.map_id", where);
		}
		if ( filter.hasEnabledCriteria() ) {
			idx += whereEqual(filter.getEnabled(), "cds.enabled", where);
		}
		if ( filter.hasNameCriteria() ) {
			idx += whereOptimizedLike(filter.getNames(), "cds.cname", where);
		}
		if ( filter.hasNodeCriteria() ) {
			where.append("\tAND cds.kind = '").append(ObjectDatumKind.Node.getKey()).append("'\n");
			idx += whereOptimizedArrayContains(filter.getNodeIds(), "cds.obj_id", where);
		}
		CloudIntegrationsSqlUtils.whereCloudDatumStreamHasSourceIds(filter, where);
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY cds.user_id, cds.id");
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
		if ( filter.hasSourceCriteria() ) {
			p = prepareArrayParameter(con, stmt, p, filter.getSourceIds());
		}
		if ( filter.hasUserCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		}
		if ( filter.hasIntegrationCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getIntegrationIds());
		}
		if ( filter.hasDatumStreamCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getDatumStreamIds());
		}
		if ( filter.hasServiceIdentifierCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getServiceIdentifiers());
		}
		if ( filter.hasDatumStreamMappingCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getDatumStreamMappingIds());
		}
		if ( filter.hasEnabledCriteria() ) {
			p = prepareParameter(stmt, p, filter.getEnabled());
		}
		if ( filter.hasNameCriteria() ) {
			p = prepareOptimizedLikeSubstringParameter(stmt, p, filter.getNames());
		}
		if ( filter.hasNodeCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getNodeIds());
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
