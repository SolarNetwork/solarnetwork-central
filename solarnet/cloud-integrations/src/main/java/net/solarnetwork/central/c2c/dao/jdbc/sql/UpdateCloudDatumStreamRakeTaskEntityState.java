/* ==================================================================
 * UpdateCloudDatumStreamRakeTaskEntityState.java - 20/09/2025 6:53:19 pm
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareDateRange;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereDateRange;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.domain.BasicClaimableJobState;

/**
 * Support for UPDATE for {@link CloudDatumStreamRakeTaskEntity} entity state.
 *
 * @author matt
 * @version 1.0
 */
public class UpdateCloudDatumStreamRakeTaskEntityState implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			UPDATE solardin.cin_datum_stream_rake_task
			SET status = ?
			""";

	private final BasicClaimableJobState desiredState;
	private final CloudDatumStreamRakeTaskFilter filter;

	/**
	 * Constructor.
	 *
	 * @param desiredState
	 *        the desired state
	 * @param filter
	 *        a filter to restrict the update to
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpdateCloudDatumStreamRakeTaskEntityState(BasicClaimableJobState desiredState,
			CloudDatumStreamRakeTaskFilter filter) {
		super();
		this.desiredState = requireNonNullArgument(desiredState, "desiredState");
		this.filter = requireNonNullArgument(filter, "filter");
	}

	@Override
	public String getSql() {
		if ( filter == null ) {
			return SQL;
		}
		StringBuilder buf = new StringBuilder(SQL);
		sqlWhere(buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		int p = 0;
		stmt.setString(++p, desiredState.keyValue());

		prepareCore(con, stmt, p);

		return stmt;
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getUserIds(), "user_id", where);
		}
		if ( filter.hasTaskCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getTaskIds(), "id", where);
		}
		if ( filter.hasDatumStreamCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getDatumStreamIds(), "ds_id", where);
		}
		if ( filter.hasClaimableJobStateCriteria() ) {
			idx += whereOptimizedArrayContains(filter.claimableJobStateKeys(), "status", where);
		}
		if ( filter.hasDate() ) {
			idx += whereDateRange(filter, "exec_at", where);
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( filter.hasUserCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		}
		if ( filter.hasTaskCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getTaskIds());
		}
		if ( filter.hasDatumStreamCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getDatumStreamIds());
		}
		if ( filter.hasClaimableJobStateCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.claimableJobStateKeys());
		}
		if ( filter.hasDate() ) {
			p = prepareDateRange(filter, stmt, p);
		}
		return p;
	}

}
