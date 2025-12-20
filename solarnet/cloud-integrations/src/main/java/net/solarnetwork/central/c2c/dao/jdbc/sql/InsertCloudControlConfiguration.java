/* ==================================================================
 * InsertCloudControlConfiguration.java - 3/11/2025 7:46:06 am
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

/**
 * Support for INSERT for {@link CloudControlConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public class InsertCloudControlConfiguration implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solardin.cin_control (
				  created,modified,user_id,enabled,cname,sident
				, int_id,node_id,control_id,cref
				, sprops
			)
			VALUES (
				  ?,?,?,?,?,?
				, ?,?,?,?
				, ?::jsonb)
			""";

	private final Long userId;
	private final CloudControlConfiguration entity;

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
	public InsertCloudControlConfiguration(Long userId, CloudControlConfiguration entity) {
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
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : Instant.now());
		Timestamp mod = entity.getModified() != null ? Timestamp.from(entity.getModified()) : null;
		int p = 0;
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, mod);
		stmt.setObject(++p, userId);
		stmt.setBoolean(++p, entity.isEnabled());
		stmt.setString(++p, entity.getName());
		stmt.setString(++p, entity.getServiceIdentifier());
		stmt.setObject(++p, entity.getIntegrationId());
		stmt.setObject(++p, entity.getNodeId());
		stmt.setString(++p, entity.getControlId());
		stmt.setString(++p, entity.getControlReference());
		stmt.setString(++p, entity.getServicePropsJson());
		return stmt;
	}

}
