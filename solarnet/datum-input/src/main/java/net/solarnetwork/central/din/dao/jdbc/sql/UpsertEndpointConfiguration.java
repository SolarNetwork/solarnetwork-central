/* ==================================================================
 * UpsertEndpointConfiguration.java - 21/02/2024 3:06:31 pm
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
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.domain.UserUuidPK;

/**
 * Support for INSERT ... ON CONFLICT {@link EndpointConfiguration} entities.
 *
 * @author matt
 * @version 1.1
 */
public class UpsertEndpointConfiguration implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solardin.din_endpoint (
				  created,modified,user_id,id
				, enabled,cname,node_id,source_id,xform_id,pub_flux,track_prev
			)
			VALUES (
				  ?,?,?,?
				, ?,?,?,?,?,?,?)
			ON CONFLICT (user_id, id) DO UPDATE
				SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
					, enabled = EXCLUDED.enabled
					, cname = EXCLUDED.cname
					, node_id = EXCLUDED.node_id
					, source_id = EXCLUDED.source_id
					, xform_id = EXCLUDED.xform_id
					, pub_flux = EXCLUDED.pub_flux
					, track_prev = EXCLUDED.track_prev
			""";

	private final Long userId;
	private final UUID endpointId;
	private final EndpointConfiguration entity;

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
	public UpsertEndpointConfiguration(Long userId, EndpointConfiguration entity) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.entity = requireNonNullArgument(entity, "entity");
		this.endpointId = UserUuidPK.UNASSIGNED_UUID_ID.equals(entity.getEndpointId())
				? UUID.randomUUID()
				: entity.getEndpointId();
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
		stmt.setBoolean(++p, entity.isEnabled());
		stmt.setString(++p, entity.getName());
		stmt.setObject(++p, entity.getNodeId());
		stmt.setString(++p, entity.getSourceId());
		stmt.setObject(++p, entity.getTransformId());
		stmt.setBoolean(++p, entity.isPublishToSolarFlux());
		stmt.setBoolean(++p, entity.isPreviousInputTracking());
		return stmt;
	}

	/**
	 * Get the endpoint ID.
	 *
	 * <p>
	 * If the given entity did not have an assigned UUID, a random one will be
	 * generated and returned by this method.
	 * </p>
	 *
	 * @return the endpoint ID
	 */
	public UUID getEndpointId() {
		return endpointId;
	}

}
