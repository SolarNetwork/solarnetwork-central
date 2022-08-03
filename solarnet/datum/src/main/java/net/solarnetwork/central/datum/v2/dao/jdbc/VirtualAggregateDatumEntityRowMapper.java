/* ==================================================================
 * AggregateDatumEntityRowMapper.java - 31/10/2020 8:39:11 am
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
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;

/**
 * Map rollup virtual aggregate rows into {@link AggregateDatumEntity}
 * instances.
 * 
 * <p>
 * This mapper is not meant to be re-used across queries, as it populates a
 * stream metadata map based on the query results, and make that available via
 * the {@link ObjectDatumStreamMetadataProvider} API.
 * </p>
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>ts_start</li>
 * <li>data_i</li>
 * <li>data_a</li>
 * <li>data_s</li>
 * <li>data_t</li>
 * <li>stat_i</li>
 * <li>read_a</li>
 * <li>names_i</li>
 * <li>names_a</li>
 * <li>obj_id</li>
 * <li>source_id</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class VirtualAggregateDatumEntityRowMapper
		implements RowMapper<AggregateDatum>, ObjectDatumStreamMetadataProvider {

	private final Aggregation aggregation;
	private final ObjectDatumKind kind;
	private final Map<UUID, ObjectDatumStreamMetadata> metadata;

	/**
	 * Constructor.
	 * 
	 * @param aggregation
	 *        the aggregation kind to assign
	 * @param kind
	 *        the object kind
	 */
	public VirtualAggregateDatumEntityRowMapper(Aggregation aggregation, ObjectDatumKind kind) {
		super();
		this.aggregation = aggregation;
		this.kind = kind;
		this.metadata = new LinkedHashMap<>(4);
	}

	@Override
	public AggregateDatumEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = getUuid(rs, 1);
		Instant ts = rs.getTimestamp(2).toInstant();
		BigDecimal[] data_i = CommonJdbcUtils.getArray(rs, 3);
		BigDecimal[] data_a = CommonJdbcUtils.getArray(rs, 4);
		String[] data_s = CommonJdbcUtils.getArray(rs, 5);
		String[] data_t = CommonJdbcUtils.getArray(rs, 6);
		BigDecimal[][] stat_i = CommonJdbcUtils.getArray(rs, 7);
		BigDecimal[][] stat_a = CommonJdbcUtils.getArray(rs, 8);

		if ( !metadata.containsKey(streamId) ) {
			String[] names_i = CommonJdbcUtils.getArray(rs, 9);
			String[] names_a = CommonJdbcUtils.getArray(rs, 10);
			Long objectId = rs.getLong(11);
			String sourceId = rs.getString(12);
			metadata.put(streamId, new BasicObjectDatumStreamMetadata(streamId, null, kind, objectId,
					sourceId, names_i, names_a, null));
		}

		DatumProperties props = DatumProperties.propertiesOf(data_i, data_a, data_s, data_t);
		DatumPropertiesStatistics stats = DatumPropertiesStatistics.statisticsOf(stat_i, stat_a);
		return new AggregateDatumEntity(streamId, ts, aggregation, props, stats);
	}

	@Override
	public Collection<UUID> metadataStreamIds() {
		return metadata.keySet();
	}

	@Override
	public ObjectDatumStreamMetadata metadataForStreamId(UUID streamId) {
		return metadata.get(streamId);
	}

	@Override
	public ObjectDatumStreamMetadata metadataForObjectSource(Long objectId, String sourceId) {
		for ( ObjectDatumStreamMetadata meta : metadata.values() ) {
			if ( objectId.equals(meta.getObjectId()) && sourceId.equals(meta.getSourceId()) ) {
				return meta;
			}
		}
		return null;
	}

}
