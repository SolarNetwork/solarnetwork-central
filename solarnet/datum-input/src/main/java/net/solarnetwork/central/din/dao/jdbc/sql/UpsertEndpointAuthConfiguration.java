/* ==================================================================
 * UpsertEndpointAuthAuthConfiguration.java - 21/02/2024 4:14:33 pm
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
import java.util.UUID;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.din.domain.EndpointAuthConfiguration;

/**
 * Support for INSERT ... ON CONFLICT {@link EndpointAuthConfiguration}
 * entities.
 *
 * @author matt
 * @version 1.0
 */
public class UpsertEndpointAuthConfiguration implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solardin.din_endpoint_auth_cred (
				  created,modified,user_id,endpoint_id,cred_id
				, enabled
			)
			VALUES (
				  ?,?,?,?,?
				, ?)
			ON CONFLICT (user_id, endpoint_id, cred_id) DO UPDATE
				SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
					, enabled = EXCLUDED.enabled
			""";

	private final Long userId;
	private final UUID endpointId;
	private final EndpointAuthConfiguration entity;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param endpointId
	 *        the endpoint ID
	 * @param entity
	 *        the entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpsertEndpointAuthConfiguration(Long userId, UUID endpointId,
			EndpointAuthConfiguration entity) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.endpointId = requireNonNullArgument(endpointId, "endpointId");
		this.entity = requireNonNullArgument(entity, "entity");
		requireNonNullArgument(entity.getCredentialId(), "entity.credentialId");
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
		stmt.setObject(++p, userId);
		stmt.setObject(++p, endpointId);
		stmt.setObject(++p, entity.getCredentialId());
		stmt.setBoolean(++p, entity.isEnabled());
		return stmt;
	}

}
