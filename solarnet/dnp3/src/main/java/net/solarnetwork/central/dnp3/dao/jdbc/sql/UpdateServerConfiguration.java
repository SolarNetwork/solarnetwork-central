/* ==================================================================
 * UpdateServerConfiguration.java - 7/08/2023 6:02:15 am
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Update {@link ServerConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public final class UpdateServerConfiguration implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			UPDATE solardnp3.dnp3_server SET
				  modified = ?
				, enabled = ?
				, cname = ?
			WHERE user_id = ? AND id = ?
			""";

	private final UserLongCompositePK id;
	private final ServerConfiguration entity;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID of the entity to update
	 * @param entity
	 *        the entity data to update
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpdateServerConfiguration(UserLongCompositePK id, ServerConfiguration entity) {
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
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setTimestamp(++p,
				Timestamp.from(entity.getModified() != null ? entity.getModified() : Instant.now()));
		stmt.setBoolean(++p, entity.isEnabled());
		stmt.setString(++p, entity.getName());
		stmt.setObject(++p, id.getUserId());
		stmt.setObject(++p, id.getEntityId());
		return stmt;
	}

}
