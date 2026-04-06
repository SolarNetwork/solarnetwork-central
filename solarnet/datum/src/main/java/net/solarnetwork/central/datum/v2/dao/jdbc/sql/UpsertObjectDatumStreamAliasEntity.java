/* ==================================================================
 * UpsertObjectDatumStreamAliasEntity.java - 28/03/2026 6:37:15 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.domain.EntityConstants;

/**
 * Support for INSERT ... ON CONFLICT {@link ObjectDatumStreamAliasEntity}
 * entities.
 *
 * @author matt
 * @version 1.0
 */
public class UpsertObjectDatumStreamAliasEntity implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solardatm.da_datm_alias (
				stream_id,created,modified,node_id,source_id,alias_node_id,alias_source_id
			)
			VALUES (?,?,?,?,?,?,?)
			ON CONFLICT (stream_id) DO UPDATE
				SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
					, node_id = EXCLUDED.node_id
					, source_id = EXCLUDED.source_id
					, alias_node_id = EXCLUDED.alias_node_id
					, alias_source_id = EXCLUDED.alias_source_id
			""";

	private final UUID streamId;
	private final ObjectDatumStreamAliasEntity entity;

	/**
	 * Constructor.
	 *
	 * @param streamId
	 *        the stream ID
	 * @param entity
	 *        the entity to persist
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null} or {@code streamId} is an
	 *         unassigned value
	 */
	public UpsertObjectDatumStreamAliasEntity(UUID streamId, ObjectDatumStreamAliasEntity entity) {
		super();
		this.streamId = requireNonNullArgument(streamId, "streamId");
		this.entity = requireNonNullArgument(entity, "entity");
		if ( !EntityConstants.isAssigned(streamId) ) {
			throw new IllegalArgumentException("The streamId must be assigned.");
		}
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : Instant.now());
		Timestamp mod = entity.getModified() != null ? Timestamp.from(entity.getModified()) : ts;
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		int p = 0;
		stmt.setObject(++p, streamId, Types.OTHER);
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, mod);
		stmt.setObject(++p, entity.getOriginalObjectId());
		stmt.setObject(++p, entity.getOriginalSourceId());
		stmt.setObject(++p, entity.getObjectId());
		stmt.setObject(++p, entity.getSourceId());
		return stmt;
	}

}
