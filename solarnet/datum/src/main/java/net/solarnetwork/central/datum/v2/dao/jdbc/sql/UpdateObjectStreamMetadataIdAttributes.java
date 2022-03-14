/* ==================================================================
 * UpdateObjectStreamMetadataIdAttributes.java - 21/11/2021 3:29:45 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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
import java.sql.Types;
import java.util.UUID;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Update the object and/or source ID values of a datum metadata stream as an
 * UPDATE, returning the updated row.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class UpdateObjectStreamMetadataIdAttributes implements PreparedStatementCreator, SqlProvider {

	private final ObjectDatumKind kind;
	private final UUID streamId;
	private final Long objectId;
	private final String sourceId;

	/**
	 * Constructor.
	 * 
	 * @param kind
	 *        the stream datum kind
	 * @param streamId
	 *        the ID of the stream metadata to update
	 * @param objectId
	 *        the object ID to set, or {@literal null} to leave unchanged
	 * @param sourceId
	 *        the source ID to set, or {@literal null} to leave unchanged
	 * 
	 * @throws IllegalArgumentException
	 *         if either {@code streamId} or {@code kind} is {@literal null}, or
	 *         both {@code objectId} and {@code sourceId} are {@literal null}
	 */
	public UpdateObjectStreamMetadataIdAttributes(ObjectDatumKind kind, UUID streamId, Long objectId,
			String sourceId) {
		super();
		this.streamId = requireNonNullArgument(streamId, "streamId");
		this.kind = requireNonNullArgument(kind, "kind");
		if ( objectId == null && sourceId == null ) {
			throw new IllegalArgumentException(
					"One of the objectId or sourceId arguments must not be null.");
		}
		this.objectId = objectId;
		this.sourceId = sourceId;
	}

	private String sqlTableName() {
		if ( kind == ObjectDatumKind.Location ) {
			return "da_loc_datm_meta";
		}
		return "da_datm_meta";
	}

	private String sqlObjectIdColumnName() {
		if ( kind == ObjectDatumKind.Location ) {
			return "loc_id";
		} else {
			return "node_id";
		}
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("UPDATE solardatm.").append(sqlTableName()).append(" SET\n");
		if ( objectId != null ) {
			buf.append("    ").append(sqlObjectIdColumnName()).append(" = ?\n");
		}
		if ( sourceId != null ) {
			buf.append("  ");
			if ( objectId != null ) {
				buf.append(", ");
			} else {
				buf.append("  ");
			}
			buf.append("source_id = ?\n");
		}
		buf.append("WHERE stream_id = ?::uuid\n");
		buf.append("RETURNING stream_id, ").append(sqlObjectIdColumnName()).append(", source_id");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int col = 0;
		if ( objectId != null ) {
			stmt.setObject(++col, objectId);
		}
		if ( sourceId != null ) {
			stmt.setString(++col, sourceId);
		}
		stmt.setObject(++col, streamId, Types.OTHER);
		return stmt;
	}

}
