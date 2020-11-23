/* ==================================================================
 * ObjectDatumIdRowMapper.java - 22/11/2020 10:14:03 pm
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId.LocationDatumId;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId.NodeDatumId;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Map object datum ID rows into {@link ObjectDatumStreamMetadata} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>ts</li>
 * <li>agg_kind</li>
 * <li>obj_id</li>
 * <li>source_id</li>
 * <li>kind</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class ObjectDatumIdRowMapper implements RowMapper<ObjectDatumId> {

	/** A default mapper instance using {@link MetadataKind#Dynamic}. */
	public static final RowMapper<ObjectDatumId> INSTANCE = new ObjectDatumIdRowMapper(
			MetadataKind.Dynamic);

	/** A default mapper instance using {@link MetadataKind#Node}. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final RowMapper<NodeDatumId> NODE_INSTANCE = (RowMapper) new ObjectDatumIdRowMapper(
			MetadataKind.Node);

	/** A default mapper instance using {@link MetadataKind#Location}. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final RowMapper<LocationDatumId> LOCATION_INSTANCE = (RowMapper) new ObjectDatumIdRowMapper(
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
	public ObjectDatumIdRowMapper(MetadataKind kind) {
		super();
		this.kind = kind;
	}

	@Override
	public ObjectDatumId mapRow(ResultSet rs, int rowNum) throws SQLException {
		Object sid = rs.getObject(1);
		UUID streamId = (sid instanceof UUID ? (UUID) sid
				: sid != null ? UUID.fromString(sid.toString()) : null);
		Instant ts = rs.getTimestamp(2).toInstant();
		Aggregation agg = Aggregation.forKey(rs.getString(3));
		Object objId = rs.getObject(4);
		String sourceId = rs.getString(5);

		MetadataKind k = this.kind;
		if ( this.kind == MetadataKind.Dynamic ) {
			k = MetadataKind.forKey(rs.getString(6));
		}

		if ( k == MetadataKind.Location ) {
			return new LocationDatumId(streamId,
					objId instanceof Number ? ((Number) objId).longValue() : null, sourceId, ts, agg);
		}

		return new NodeDatumId(streamId, objId instanceof Number ? ((Number) objId).longValue() : null,
				sourceId, ts, agg);
	}

}
