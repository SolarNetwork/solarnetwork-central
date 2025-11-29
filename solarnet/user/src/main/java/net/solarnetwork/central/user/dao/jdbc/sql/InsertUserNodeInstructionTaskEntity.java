/* ==================================================================
 * InsertUserNodeInstructionTaskEntity.java - 10/11/2025 3:48:33 pm
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

package net.solarnetwork.central.user.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;

/**
 * Support for INSERT for {@link UserNodeInstructionTaskEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public class InsertUserNodeInstructionTaskEntity implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solaruser.user_node_instr_task (
				    user_id,enabled,cname,node_id,topic
				  , schedule,status,exec_at,sprops
			)
			VALUES (?,?,?,?,?,?,?,?,?::jsonb)
			""";

	private final Long userId;
	private final UserNodeInstructionTaskEntity entity;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param entity
	 *        the entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public InsertUserNodeInstructionTaskEntity(Long userId, UserNodeInstructionTaskEntity entity) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.entity = requireNonNullArgument(entity, "entity");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.RETURN_GENERATED_KEYS);
		int p = 0;
		stmt.setObject(++p, userId);
		stmt.setBoolean(++p, entity.isEnabled());
		stmt.setString(++p, entity.getName());
		stmt.setObject(++p, entity.getNodeId());
		stmt.setString(++p, entity.getTopic());
		stmt.setString(++p, entity.getSchedule());
		stmt.setString(++p, entity.getState().keyValue());
		stmt.setTimestamp(++p, Timestamp.from(entity.getExecuteAt()));
		stmt.setString(++p, entity.getServicePropsJson());
		return stmt;
	}

}
