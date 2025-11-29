/* ==================================================================
 * UpdateCloudDatumStreamPollTaskEntity.java - 10/10/2024 12:10:56 pm
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareDateRange;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereDateRange;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.domain.BasicClaimableJobState;

/**
 * Support for UPDATE for {@link CloudDatumStreamPollTaskEntity} entity state.
 *
 * @author matt
 * @version 1.1
 */
public final class UpdateCloudDatumStreamPollTaskEntityState
		implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			UPDATE solardin.cin_datum_stream_poll_task
			SET status = ?
			""";

	private final BasicClaimableJobState desiredState;
	private final CloudDatumStreamPollTaskFilter filter;
	private final CloudDatumStreamPollTaskEntity data;

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
	public UpdateCloudDatumStreamPollTaskEntityState(BasicClaimableJobState desiredState,
			CloudDatumStreamPollTaskFilter filter) {
		this(desiredState, filter, null);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * If {@code data} is {@code null} then the generated SQL will update just
	 * the {@code status} property of the rows matching {@code filter}. When
	 * {@code data} is provided, then the other runtime state properties will be
	 * updated to match the given values.
	 * </p>
	 *
	 * @param desiredState
	 *        the desired state
	 * @param filter
	 *        a filter to restrict the update to
	 * @param data
	 *        optional runtime properties to update
	 * @throws IllegalArgumentException
	 *         if any argument except {@code data} is {@literal null}
	 * @since 1.1
	 */
	public UpdateCloudDatumStreamPollTaskEntityState(BasicClaimableJobState desiredState,
			CloudDatumStreamPollTaskFilter filter, CloudDatumStreamPollTaskEntity data) {
		super();
		this.desiredState = requireNonNullArgument(desiredState, "desiredState");
		this.filter = requireNonNullArgument(filter, "filter");
		this.data = data;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder(SQL);
		if ( data != null ) {
			buf.append("""
						, exec_at = ?
						, start_at = ?
						, message = ?
						, sprops = ?::jsonb
					""");
		}
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
		if ( data != null ) {
			stmt.setTimestamp(++p, Timestamp.from(data.getExecuteAt()));
			stmt.setTimestamp(++p, Timestamp.from(data.getStartAt()));
			stmt.setString(++p, data.getMessage());
			stmt.setString(++p, data.getServicePropsJson());
		}
		if ( filter.hasUserCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
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
