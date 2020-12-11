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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.BasicLocation;

/**
 * Map object datum stream metadata rows with geographic data into
 * {@link ObjectDatumStreamMetadata} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>obj_id</li>
 * <li>source_id</li>
 * <li>jdata</li>
 * <li>country</li>
 * <li>region</li>
 * <li>state_prov</li>
 * <li>locality</li>
 * <li>postal_code</li>
 * <li>address</li>
 * <li>latitude</li>
 * <li>longitude</li>
 * <li>elevation</li>
 * <li>time_zone</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class ObjectDatumStreamMetadataGeoRowMapper implements RowMapper<ObjectDatumStreamMetadata> {

	/** A default mapper instance using {@link ObjectDatumKind#Node}. */
	public static final RowMapper<ObjectDatumStreamMetadata> NODE_INSTANCE = new ObjectDatumStreamMetadataGeoRowMapper(
			ObjectDatumKind.Node);

	/** A default mapper instance using {@link ObjectDatumKind#Location}. */
	public static final RowMapper<ObjectDatumStreamMetadata> LOCATION_INSTANCE = new ObjectDatumStreamMetadataGeoRowMapper(
			ObjectDatumKind.Location);

	private final ObjectDatumKind kind;

	/**
	 * Constructor.
	 * 
	 * @param kind
	 *        the type of metadata to parse; if {@link MetadataKind#Dynamic}
	 *        then an extra {@literal kind} row must be provided by the query
	 *        results
	 */
	public ObjectDatumStreamMetadataGeoRowMapper(ObjectDatumKind kind) {
		super();
		this.kind = kind;
	}

	@Override
	public ObjectDatumStreamMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = DatumJdbcUtils.getUuid(rs, 1);
		Long objId = rs.getLong(2);
		String sourceId = rs.getString(3);
		String jmeta = rs.getString(4);

		String country = rs.getString(5);
		String region = rs.getString(6);
		String state = rs.getString(7);
		String locality = rs.getString(8);
		String postalCode = rs.getString(9);
		String street = rs.getString(10);
		BigDecimal lat = rs.getBigDecimal(11);
		BigDecimal lon = rs.getBigDecimal(12);
		BigDecimal el = rs.getBigDecimal(13);
		String timeZoneId = rs.getString(14);

		BasicLocation l = new BasicLocation(null, country, region, state, locality, postalCode, street,
				lat, lon, el, timeZoneId);
		return new BasicObjectDatumStreamMetadata(streamId, timeZoneId, kind, objId, sourceId, l, null,
				null, null, jmeta);
	}

}
