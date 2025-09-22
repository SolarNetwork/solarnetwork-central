/* ==================================================================
 * UpdateCloudDatumStreamRakeTaskEntity.java - 20/09/2025 6:53:04 pm
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
import java.sql.Statement;
import java.sql.Timestamp;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Support for UPDATE for {@link CloudDatumStreamRakeTaskEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public class UpdateCloudDatumStreamRakeTaskEntity implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			UPDATE solardin.cin_datum_stream_rake_task
			SET ds_id = ?
				, status = ?
				, exec_at = ?
				, start_offset = ?::interval
				, message = ?
				, sprops = ?::jsonb
			WHERE user_id = ? AND id = ?
			""";

	private final UserLongCompositePK id;
	private final CloudDatumStreamRakeTaskEntity entity;
	private final CloudDatumStreamRakeTaskFilter filter;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param entity
	 *        the entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpdateCloudDatumStreamRakeTaskEntity(UserLongCompositePK id,
			CloudDatumStreamRakeTaskEntity entity) {
		this(id, entity, null);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param entity
	 *        the entity
	 * @param filter
	 *        an optional filter to restrict the update to
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}, other than {@code filter}
	 */
	public UpdateCloudDatumStreamRakeTaskEntity(UserLongCompositePK id,
			CloudDatumStreamRakeTaskEntity entity, CloudDatumStreamRakeTaskFilter filter) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.entity = requireNonNullArgument(entity, "entity");
		if ( !id.entityIdIsAssigned() ) {
			throw new IllegalArgumentException("Entity ID must be assigned");
		}
		this.filter = filter;
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
		stmt.setObject(++p, entity.getDatumStreamId());
		stmt.setString(++p, entity.getState().keyValue());
		stmt.setTimestamp(++p, Timestamp.from(entity.getExecuteAt()));
		stmt.setString(++p, entity.getOffset().toString());
		stmt.setString(++p, entity.getMessage());
		stmt.setString(++p, entity.getServicePropsJson());

		stmt.setObject(++p, id.getUserId());
		stmt.setObject(++p, id.getEntityId());

		prepareCore(con, stmt, p);

		return stmt;
	}

	private void sqlWhere(StringBuilder buf) {
		if ( filter == null ) {
			return;
		}
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasClaimableJobStateCriteria() ) {
			idx += whereOptimizedArrayContains(filter.claimableJobStateKeys(), "status", where);
		}
		if ( idx > 0 ) {
			buf.append("AND").append(where.substring(4));
		}
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( filter != null ) {
			if ( filter.hasClaimableJobStateCriteria() ) {
				p = prepareOptimizedArrayParameter(con, stmt, p, filter.claimableJobStateKeys());
			}
		}
		return p;
	}

}
