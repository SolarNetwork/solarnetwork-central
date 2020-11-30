/* ==================================================================
 * JdbcDatumEntityDao.java - 19/11/2020 3:12:06 pm
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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper.mapperForAggregate;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils.executeFilterQuery;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import javax.cache.Cache;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ObjectSourcePK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.BasicDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumMaintenanceDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.StreamMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.GetDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.InsertDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.InsertStaleAggregateDatumSelect;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumAvailableTimeRange;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectStreamMetadata;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStaleAggregateDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStreamMetadata;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.StoreLocationDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.StoreNodeDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.UpdateObjectStreamMetadataJson;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumDateInterval;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.LocationDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StreamKindPK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.SortDescriptor;

/**
 * {@link JdbcOperations} based implementation of {@link DatumEntityDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class JdbcDatumEntityDao implements DatumEntityDao, DatumStreamMetadataDao, DatumMaintenanceDao {

	private final JdbcOperations jdbcTemplate;
	private Cache<UUID, ObjectDatumStreamMetadata> streamMetadataCache;

	/**
	 * Constructor.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @throws IllegalArgumentException
	 *         if {@code jdbcTemplate} is {@literal null}
	 */
	public JdbcDatumEntityDao(JdbcOperations jdbcTemplate) {
		super();
		if ( jdbcTemplate == null ) {
			throw new IllegalArgumentException("The jdbcTemplate argument must not be null.");
		}
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Class<? extends DatumEntity> getObjectType() {
		return DatumEntity.class;
	}

	@Override
	public DatumPK save(DatumEntity entity) {
		if ( entity.getTimestamp() == null ) {
			throw new IllegalArgumentException("The timestamp property is required.");
		}
		jdbcTemplate.update(new InsertDatum(entity));
		return entity.getId();
	}

	@Override
	public DatumPK store(GeneralNodeDatum datum) {
		if ( datum == null || datum.getNodeId() == null || datum.getSourceId() == null ) {
			return null;
		}
		GeneralDatumSamples s = datum.getSamples();
		if ( s == null || s.isEmpty() ) {
			return null;
		}
		StoreNodeDatum sql = new StoreNodeDatum(datum);
		return jdbcTemplate.execute(sql, new CallableStatementCallback<DatumPK>() {

			@Override
			public DatumPK doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.execute();
				Object streamId = cs.getObject(1);
				return new DatumPK(
						(streamId instanceof UUID ? (UUID) streamId
								: streamId != null ? UUID.fromString(streamId.toString()) : null),
						sql.getTimestamp());
			}
		});
	}

	@Override
	public DatumPK store(GeneralLocationDatum datum) {
		if ( datum == null || datum.getLocationId() == null || datum.getSourceId() == null ) {
			return null;
		}
		GeneralDatumSamples s = datum.getSamples();
		if ( s == null || s.isEmpty() ) {
			return null;
		}
		StoreLocationDatum sql = new StoreLocationDatum(datum);
		return jdbcTemplate.execute(sql, new CallableStatementCallback<DatumPK>() {

			@Override
			public DatumPK doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.execute();
				Object streamId = cs.getObject(1);
				return new DatumPK(
						(streamId instanceof UUID ? (UUID) streamId
								: streamId != null ? UUID.fromString(streamId.toString()) : null),
						sql.getTimestamp());
			}
		});
	}

	@Override
	public DatumEntity get(DatumPK id) {
		List<Datum> result = jdbcTemplate.query(new GetDatum(id), DatumEntityRowMapper.INSTANCE);
		return (!result.isEmpty() ? (DatumEntity) result.get(0) : null);
	}

	@Override
	public Collection<DatumEntity> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(DatumEntity entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DatumStreamFilterResults findFiltered(DatumCriteria filter, List<SortDescriptor> sorts,
			Integer offset, Integer max) {
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument must be provided.");
		}
		final PreparedStatementCreator sql = new SelectDatum(filter);

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final RowMapper<Datum> mapper = filter.getAggregation() != null
				? (RowMapper) mapperForAggregate(filter.getAggregation(),
						filter.getReadingType() != null)
				: DatumEntityRowMapper.INSTANCE;

		FilterResults<Datum, DatumPK> results = executeFilterQuery(jdbcTemplate, filter, sql, mapper);

		Map<UUID, ObjectDatumStreamMetadata> metaMap = null;
		if ( filter.getStreamIds() != null && filter.getStreamIds().length == 1 ) {
			ObjectDatumStreamMetadata meta = findStreamMetadata(filter);
			if ( meta != null ) {
				metaMap = Collections.singletonMap(meta.getStreamId(), meta);
			} else {
				metaMap = Collections.emptyMap();
			}
		} else {
			Iterable<? extends ObjectDatumStreamMetadata> metas = null;
			if ( filter.getLocationIds() != null ) {
				metas = findLocationDatumStreamMetadata(filter);
			} else {
				metas = findNodeDatumStreamMetadata(filter);
			}
			metaMap = StreamSupport.stream(metas.spliterator(), false).collect(toMap(
					DatumStreamMetadata::getStreamId, identity(), (u, v) -> u, LinkedHashMap::new));
		}
		return new BasicDatumStreamFilterResults(metaMap, results.getResults(),
				results.getTotalResults(), results.getStartingOffset(),
				results.getReturnedResultCount());
	}

	@Override
	public Iterable<DatumDateInterval> findAvailableInterval(DatumStreamCriteria filter) {
		return jdbcTemplate.query(new SelectDatumAvailableTimeRange(filter),
				DatumDateIntervalRowMapper.INSTANCE);
	}

	@Override
	public ObjectDatumStreamMetadata findStreamMetadata(StreamMetadataCriteria filter) {
		if ( filter.getStreamId() == null ) {
			throw new IllegalArgumentException("A stream ID is required.");
		}
		if ( streamMetadataCache != null ) {
			ObjectDatumStreamMetadata meta = streamMetadataCache.get(filter.getStreamId());
			if ( meta != null ) {
				return meta;
			}
		}
		List<ObjectDatumStreamMetadata> results = jdbcTemplate.query(new SelectStreamMetadata(filter),
				ObjectDatumStreamMetadataRowMapper.INSTANCE);
		ObjectDatumStreamMetadata meta = (results.isEmpty() ? null : results.get(0));
		if ( meta != null && streamMetadataCache != null ) {
			streamMetadataCache.put(filter.getStreamId(), meta);
		}
		return meta;
	}

	@Override
	public Iterable<ObjectDatumStreamMetadata> findDatumStreamMetadata(ObjectStreamCriteria filter) {
		ObjectDatumKind kind = filter.getObjectKind();
		if ( kind == null ) {
			kind = filter.effectiveObjectKind();
		} else {
			kind = ObjectDatumKind.Node;
		}
		PreparedStatementCreator sql = new SelectObjectStreamMetadata(filter, kind);
		return jdbcTemplate.query(sql, ObjectDatumStreamMetadataRowMapper.INSTANCE);
	}

	@Override
	public Iterable<NodeDatumStreamMetadata> findNodeDatumStreamMetadata(ObjectStreamCriteria filter) {
		return jdbcTemplate.query(new SelectObjectStreamMetadata(filter, ObjectDatumKind.Node),
				ObjectDatumStreamMetadataRowMapper.NODE_INSTANCE);
	}

	@Override
	public Iterable<LocationDatumStreamMetadata> findLocationDatumStreamMetadata(
			ObjectStreamCriteria filter) {
		return jdbcTemplate.query(new SelectObjectStreamMetadata(filter, ObjectDatumKind.Location),
				ObjectDatumStreamMetadataRowMapper.LOCATION_INSTANCE);
	}

	@Override
	public void replaceJsonMeta(ObjectSourcePK id, String json) {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceId(id.getSourceId());
		if ( id instanceof LocationSourcePK ) {
			filter.setObjectKind(ObjectDatumKind.Location);
			filter.setLocationId(((LocationSourcePK) id).getLocationId());
		} else {
			filter.setObjectKind(ObjectDatumKind.Node);
			filter.setNodeId(((NodeSourcePK) id).getNodeId());
		}
		jdbcTemplate.update(new UpdateObjectStreamMetadataJson(filter, json));

	}

	@Override
	public int markDatumAggregatesStale(DatumStreamCriteria criteria) {
		return jdbcTemplate.update(new InsertStaleAggregateDatumSelect(criteria));

	}

	@Override
	public FilterResults<StaleAggregateDatum, StreamKindPK> findStaleAggregateDatum(
			DatumStreamCriteria filter) {
		final PreparedStatementCreator sql = new SelectStaleAggregateDatum(filter);
		return executeFilterQuery(jdbcTemplate, filter, sql,
				StaleAggregateDatumEntityRowMapper.INSTANCE);
	}

	/**
	 * Get the stream metadata cache.
	 * 
	 * @return the cache, or {@literal null}
	 */
	public Cache<UUID, ObjectDatumStreamMetadata> getStreamMetadataCache() {
		return streamMetadataCache;
	}

	/**
	 * Set the stream metadata cache.
	 * 
	 * @param streamMetadataCache
	 *        the cache to set
	 */
	public void setStreamMetadataCache(Cache<UUID, ObjectDatumStreamMetadata> streamMetadataCache) {
		this.streamMetadataCache = streamMetadataCache;
	}

}
