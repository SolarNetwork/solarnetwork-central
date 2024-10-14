/* ==================================================================
 * UpsertCloudDatumStreamPollTaskEntity.java - 10/10/2024 11:56:46 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;

/**
 * Support for INSERT ... ON CONFLICT {@link CloudDatumStreamPollTaskEntity}
 * entities.
 *
 * @author matt
 * @version 1.0
 */
public class UpsertCloudDatumStreamPollTaskEntity implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solardin.cin_datum_stream_poll_task (
				  user_id,ds_id
				, status,exec_at,start_at,message,sprops
			)
			VALUES (
				  ?,?
				, ?,?,?,?,?::jsonb)
			ON CONFLICT (user_id, ds_id) DO UPDATE
				SET status = EXCLUDED.status
					, exec_at = EXCLUDED.exec_at
					, start_at = EXCLUDED.start_at
					, message = EXCLUDED.message
					, sprops = EXCLUDED.sprops
			""";

	private final Long userId;
	private final Long datumStreamId;
	private final CloudDatumStreamPollTaskEntity entity;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param datumStreamId
	 *        the datum stream ID
	 * @param entity
	 *        the entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpsertCloudDatumStreamPollTaskEntity(Long userId, Long datumStreamId,
			CloudDatumStreamPollTaskEntity entity) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.datumStreamId = requireNonNullArgument(datumStreamId, "datumStreamId");
		this.entity = requireNonNullArgument(entity, "entity");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		int p = 0;
		stmt.setObject(++p, userId);
		stmt.setObject(++p, datumStreamId);
		stmt.setString(++p, entity.getState().keyValue());
		stmt.setTimestamp(++p, Timestamp.from(entity.getExecuteAt()));
		stmt.setTimestamp(++p, Timestamp.from(entity.getStartAt()));
		stmt.setString(++p, entity.getMessage());
		stmt.setString(++p, entity.getServicePropsJson());
		return stmt;
	}

}
