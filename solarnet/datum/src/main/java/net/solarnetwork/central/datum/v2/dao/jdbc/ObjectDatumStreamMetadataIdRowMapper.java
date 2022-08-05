/* ==================================================================
 * ObjectDatumStreamMetadataRowMapper.java - 6/11/2020 3:38:49 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getUuid;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Map object datum stream metadata rows into
 * {@link ObjectDatumStreamMetadataId} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>obj_id</li>
 * <li>source_id</li>
 * <li>kind - only used if dynamic type is used</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class ObjectDatumStreamMetadataIdRowMapper implements RowMapper<ObjectDatumStreamMetadataId> {

	/** A default mapper instance using {@link MetadataKind#Dynamic}. */
	public static final RowMapper<ObjectDatumStreamMetadataId> INSTANCE = new ObjectDatumStreamMetadataIdRowMapper(
			MetadataKind.Dynamic);

	/** A default mapper instance using {@link MetadataKind#Node}. */
	public static final RowMapper<ObjectDatumStreamMetadataId> NODE_INSTANCE = new ObjectDatumStreamMetadataIdRowMapper(
			MetadataKind.Node);

	/** A default mapper instance using {@link MetadataKind#Location}. */
	public static final RowMapper<ObjectDatumStreamMetadataId> LOCATION_INSTANCE = new ObjectDatumStreamMetadataIdRowMapper(
			MetadataKind.Location);

	private final MetadataKind kind;

	/**
	 * Constructor.
	 * 
	 * @param kind
	 *        the type of metadata to parse; if {@link MetadataKind#Dynamic}
	 *        then an extra {@literal kind} row must be provided by the query
	 *        results
	 */
	public ObjectDatumStreamMetadataIdRowMapper(MetadataKind kind) {
		super();
		this.kind = kind;
	}

	@Override
	public ObjectDatumStreamMetadataId mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = getUuid(rs, 1);
		Long objId = rs.getLong(2);
		String sourceId = rs.getString(3);

		MetadataKind k = this.kind;
		if ( this.kind == MetadataKind.Dynamic ) {
			String kindStr = rs.getString(4);
			k = ("l".equalsIgnoreCase(kindStr) ? MetadataKind.Location : MetadataKind.Node);
		}

		ObjectDatumKind objKind = null;
		switch (k) {
			case Node:
				objKind = ObjectDatumKind.Node;
				break;

			case Location:
				objKind = ObjectDatumKind.Location;
				break;

			default:
				// ignore
		}

		return new ObjectDatumStreamMetadataId(streamId, objKind, objId, sourceId);
	}

}
