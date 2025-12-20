/* ==================================================================
 * UpdateCloudControlConfiguration.java - 3/11/2025 7:49:16 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Support for UPDATE for {@link CloudControlConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public class UpdateCloudControlConfiguration implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			UPDATE solardin.cin_control
			SET modified = ?
				, cname = ?
				, sident = ?
				, int_id = ?
				, node_id = ?
				, control_id = ?
				, cref = ?
				, sprops = ?::jsonb
			WHERE user_id = ? AND id = ?
			""";

	private final UserLongCompositePK id;
	private final CloudControlConfiguration entity;

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
	public UpdateCloudControlConfiguration(UserLongCompositePK id, CloudControlConfiguration entity) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.entity = requireNonNullArgument(entity, "entity");
		if ( !id.entityIdIsAssigned() ) {
			throw new IllegalArgumentException("Entity ID must be assigned");
		}
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		Timestamp mod = Timestamp
				.from(entity.getModified() != null ? entity.getModified() : Instant.now());
		int p = 0;
		stmt.setTimestamp(++p, mod);

		stmt.setString(++p, entity.getName());
		stmt.setString(++p, entity.getServiceIdentifier());
		stmt.setObject(++p, entity.getIntegrationId());
		stmt.setObject(++p, entity.getNodeId());
		stmt.setString(++p, entity.getControlId());
		stmt.setString(++p, entity.getControlReference());
		stmt.setString(++p, entity.getServicePropsJson());

		stmt.setObject(++p, id.getUserId());
		stmt.setObject(++p, id.getEntityId());

		return stmt;
	}

}
