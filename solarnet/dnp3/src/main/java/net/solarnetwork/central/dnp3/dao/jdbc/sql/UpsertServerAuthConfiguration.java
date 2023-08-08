/* ==================================================================
 * UpsertServerAuthConfiguration.java - 6/08/2023 6:31:04 pm
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
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;

/**
 * Support for INSERT ... ON CONFLICT {@link ServerAuthConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class UpsertServerAuthConfiguration implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solardnp3.dnp3_server_auth (
				  created,modified,user_id,server_id,ident
				, enabled,cname
			)
			VALUES (
				  ?,CAST(COALESCE(?, ?) AS TIMESTAMP WITH TIME ZONE),?,?,?
				, ?,?)
			ON CONFLICT (user_id, server_id, ident) DO UPDATE
				SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
					, enabled = EXCLUDED.enabled
					, ident = EXCLUDED.ident
			""";

	private final Long userId;
	private final Long serverId;
	private final ServerAuthConfiguration entity;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param serverId
	 *        the server ID
	 * @param entity
	 *        the entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpsertServerAuthConfiguration(Long userId, Long serverId, ServerAuthConfiguration entity) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.serverId = requireNonNullArgument(serverId, "serverId");
		this.entity = requireNonNullArgument(entity, "entity");
		requireNonNullArgument(entity.getIdentifier(), "entity.identifier");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : Instant.now());
		Timestamp mod = entity.getModified() != null ? Timestamp.from(entity.getModified()) : null;
		int p = 0;
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, mod);
		stmt.setTimestamp(++p, ts);
		stmt.setObject(++p, userId);
		stmt.setObject(++p, serverId);
		stmt.setString(++p, entity.getIdentifier());
		stmt.setBoolean(++p, entity.isEnabled());
		stmt.setString(++p, entity.getName());
		return stmt;
	}

}
