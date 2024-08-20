/* ==================================================================
 * UpdateObjectStreamMetadataAttributes.java - 21/11/2021 3:29:45 PM
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
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Update the object and/or source ID values, and/or property names, of a datum
 * metadata stream as an UPDATE, returning the updated row.
 *
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class UpdateObjectStreamMetadataAttributes implements PreparedStatementCreator, SqlProvider {

	private final ObjectDatumKind kind;
	private final UUID streamId;
	private final Long objectId;
	private final String sourceId;
	private final String[] instantaneousProperties;
	private final String[] accumulatingProperties;
	private final String[] statusProperties;

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
	 * @throws IllegalArgumentException
	 *         if either {@code streamId} or {@code kind} is {@literal null}, or
	 *         both {@code objectId} and {@code sourceId} are {@literal null}
	 */
	public UpdateObjectStreamMetadataAttributes(ObjectDatumKind kind, UUID streamId, Long objectId,
			String sourceId) {
		this(kind, streamId, objectId, sourceId, null, null, null);
	}

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
	 * @param instantaneousProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @param accumulatingProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @param statusProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @throws IllegalArgumentException
	 *         if either {@code streamId} or {@code kind} is {@literal null}, or
	 *         all other arguments are {@literal null}
	 */
	public UpdateObjectStreamMetadataAttributes(ObjectDatumKind kind, UUID streamId, Long objectId,
			String sourceId, String[] instantaneousProperties, String[] accumulatingProperties,
			String[] statusProperties) {
		super();
		this.streamId = requireNonNullArgument(streamId, "streamId");
		this.kind = requireNonNullArgument(kind, "kind");
		if ( objectId == null && sourceId == null && instantaneousProperties == null
				&& accumulatingProperties == null && statusProperties == null ) {
			throw new IllegalArgumentException("At least one attribute to change must be provided.");
		}
		this.objectId = objectId;
		this.sourceId = sourceId;
		this.instantaneousProperties = instantaneousProperties;
		this.accumulatingProperties = accumulatingProperties;
		this.statusProperties = statusProperties;
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
		buf.append("WITH z AS (\n");
		if ( kind == ObjectDatumKind.Location ) {
			buf.append("""
					SELECT m.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_loc_datm_meta m
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = m.loc_id
					WHERE m.stream_id = ?::uuid
					""");
		} else {
			buf.append("""
					SELECT m.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta m
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = m.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE m.stream_id = ?::uuid
					""");
		}
		buf.append(")\n");
		buf.append("UPDATE solardatm.").append(sqlTableName()).append(" SET\n");
		int idx = 0;
		if ( objectId != null ) {
			idx++;
			buf.append("\t").append(sqlObjectIdColumnName()).append(" = ?\n");
		}
		if ( sourceId != null ) {
			buf.append("\t");
			if ( ++idx > 1 ) {
				buf.append(", ");
			}
			buf.append("source_id = ?\n");
		}
		if ( instantaneousProperties != null ) {
			buf.append("\t");
			if ( ++idx > 1 ) {
				buf.append(", ");
			}
			buf.append("names_i = ?\n");
		}
		if ( accumulatingProperties != null ) {
			buf.append("\t");
			if ( ++idx > 1 ) {
				buf.append(", ");
			}
			buf.append("names_a = ?\n");
		}
		if ( statusProperties != null ) {
			buf.append("\t");
			if ( ++idx > 1 ) {
				buf.append(", ");
			}
			buf.append("names_s = ?\n");
		}
		buf.append("FROM z\n");
		buf.append("WHERE ").append(sqlTableName()).append(".stream_id = z.stream_id\n");
		if ( instantaneousProperties != null ) {
			buf.append("\tAND cardinality(names_i) <= ?\n");
		}
		if ( accumulatingProperties != null ) {
			buf.append("\tAND cardinality(names_a) <= ?\n");
		}
		if ( statusProperties != null ) {
			buf.append("\tAND cardinality(names_s) <= ?\n");
		}
		buf.append("RETURNING ").append(sqlTableName()).append(".stream_id, ")
				.append(sqlObjectIdColumnName());
		buf.append(", source_id, names_i, names_a, names_s, jdata, '");
		buf.append(kind.getKey()).append("' AS kind, z.time_zone");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int col = 0;
		stmt.setObject(++col, streamId, Types.OTHER);
		if ( objectId != null ) {
			stmt.setObject(++col, objectId);
		}
		if ( sourceId != null ) {
			stmt.setString(++col, sourceId);
		}
		if ( instantaneousProperties != null ) {
			Array a = con.createArrayOf("TEXT", instantaneousProperties);
			stmt.setArray(++col, a);
			a.free();
		}
		if ( accumulatingProperties != null ) {
			Array a = con.createArrayOf("TEXT", accumulatingProperties);
			stmt.setArray(++col, a);
			a.free();
		}
		if ( statusProperties != null ) {
			Array a = con.createArrayOf("TEXT", statusProperties);
			stmt.setArray(++col, a);
			a.free();
		}
		if ( instantaneousProperties != null ) {
			stmt.setInt(++col, instantaneousProperties.length);
		}
		if ( accumulatingProperties != null ) {
			stmt.setInt(++col, accumulatingProperties.length);
		}
		if ( statusProperties != null ) {
			stmt.setInt(++col, statusProperties.length);
		}
		return stmt;
	}

}
