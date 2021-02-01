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

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;

/**
 * Map object datum stream metadata rows into {@link ObjectDatumStreamMetadata}
 * instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>obj_id</li>
 * <li>source_id</li>
 * <li>names_i</li>
 * <li>names_a</li>
 * <li>names_s</li>
 * <li>jdata</li>
 * <li>kind - only used if dynamic type is used</li>
 * <li>timezone</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class ObjectDatumStreamMetadataRowMapper implements RowMapper<ObjectDatumStreamMetadata> {

	/** A default mapper instance using {@link MetadataKind#Dynamic}. */
	public static final RowMapper<ObjectDatumStreamMetadata> INSTANCE = new ObjectDatumStreamMetadataRowMapper(
			MetadataKind.Dynamic);

	/** A default mapper instance using {@link MetadataKind#Node}. */
	public static final RowMapper<ObjectDatumStreamMetadata> NODE_INSTANCE = new ObjectDatumStreamMetadataRowMapper(
			MetadataKind.Node);

	/** A default mapper instance using {@link MetadataKind#Location}. */
	public static final RowMapper<ObjectDatumStreamMetadata> LOCATION_INSTANCE = new ObjectDatumStreamMetadataRowMapper(
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
	public ObjectDatumStreamMetadataRowMapper(MetadataKind kind) {
		super();
		this.kind = kind;
	}

	@Override
	public ObjectDatumStreamMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = DatumJdbcUtils.getUuid(rs, 1);
		Long objId = rs.getLong(2);
		String sourceId = rs.getString(3);

		String[] namesI = null;
		String[] namesA = null;
		String[] namesS = null;

		Array a = rs.getArray(4);
		if ( a != null ) {
			namesI = (String[]) a.getArray();
			a.free();
		}

		a = rs.getArray(5);
		if ( a != null ) {
			namesA = (String[]) a.getArray();
			a.free();
		}

		a = rs.getArray(6);
		if ( a != null ) {
			namesS = (String[]) a.getArray();
			a.free();
		}

		String jmeta = rs.getString(7);

		MetadataKind k = this.kind;
		if ( this.kind == MetadataKind.Dynamic ) {
			String kindStr = rs.getString(8);
			k = ("l".equalsIgnoreCase(kindStr) ? MetadataKind.Location : MetadataKind.Node);
		}

		String timeZoneId = rs.getString(9);

		return new BasicObjectDatumStreamMetadata(streamId, timeZoneId,
				k == MetadataKind.Location ? ObjectDatumKind.Location : ObjectDatumKind.Node, objId,
				sourceId, namesI, namesA, namesS, jmeta);
	}

}
