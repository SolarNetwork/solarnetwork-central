/* ==================================================================
 * UpsertInputDataEntity.java - 5/03/2024 11:07:11 am
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

package net.solarnetwork.central.din.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.din.domain.InputDataEntity;

/**
 * Support for INSERT ... ON CONFLICT {@link InputDataEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public class UpsertInputDataEntity implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solardin.din_input_data (
				  user_id,node_id,source_id,created,input_data
			)
			VALUES (?,?,?,?,?)
			ON CONFLICT (user_id, node_id, source_id) DO UPDATE
				SET created = COALESCE(EXCLUDED.created, CURRENT_TIMESTAMP)
					, input_data = EXCLUDED.input_data
			""";

	private final InputDataEntity entity;

	/**
	 * Constructor.
	 *
	 * @param entity
	 *        the entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpsertInputDataEntity(InputDataEntity entity) {
		super();
		this.entity = requireNonNullArgument(entity, "entity");
		requireNonNullArgument(entity.getId(), "entity.id");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : Instant.now());
		stmt.setObject(1, entity.getId().getUserId());
		stmt.setObject(2, entity.getId().getGroupId());
		stmt.setObject(3, entity.getId().getEntityId());
		stmt.setTimestamp(4, ts);
		stmt.setBytes(5, entity.getData());
		return stmt;
	}

}
