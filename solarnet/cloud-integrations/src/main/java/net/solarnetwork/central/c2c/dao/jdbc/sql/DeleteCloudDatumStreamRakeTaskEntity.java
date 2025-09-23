/* ==================================================================
 * DeleteCloudDatumStreamRakeTaskEntity.java - 24/09/2025 6:40:17 am
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

package net.solarnetwork.central.c2c.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;

/**
 * Support for DELETE for {@link CloudDatumStreamRakeTaskEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public class DeleteCloudDatumStreamRakeTaskEntity implements PreparedStatementCreator, SqlProvider {

	private final CloudDatumStreamRakeTaskFilter filter;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 */
	public DeleteCloudDatumStreamRakeTaskEntity(CloudDatumStreamRakeTaskFilter filter) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		requireNonNullArgument(filter.getUserId(), "filter.userId");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		return buf.toString();
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("""
				DELETE FROM solardin.cin_datum_stream_rake_task
				""");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		idx += whereOptimizedArrayContains(filter.getUserIds(), "user_id", where);
		if ( filter.hasTaskCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getTaskIds(), "id", where);
		}
		if ( filter.hasDatumStreamCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getDatumStreamIds(), "ds_id", where);
		}
		if ( filter.hasClaimableJobStateCriteria() ) {
			idx += whereOptimizedArrayContains(filter.claimableJobStateKeys(), "status", where);
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		prepareCore(con, stmt, 0);
		return stmt;
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		if ( filter.hasTaskCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getTaskIds());
		}
		if ( filter.hasDatumStreamCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getDatumStreamIds());
		}
		if ( filter.hasClaimableJobStateCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.claimableJobStateKeys());
		}
		return p;
	}

}
