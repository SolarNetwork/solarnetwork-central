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

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper.mapperForAggregate;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.FindObjectStreamMetadataIds.FIND_METADATA_IDS_FOR_STREAM_ID;
import static net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.cache.Cache;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ObjectSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumMaintenanceDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.ProviderObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumDao;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.dao.StreamMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.MetadataSelectStyle;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DeleteDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.GetDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.InsertDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.InsertStaleAggregateDatumSelect;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumAvailableTimeRange;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumCalculatedAt;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumPartialAggregate;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumRecordCounts;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumRunningTotal;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectStreamMetadata;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectReadingDifference;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStaleAggregateDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStreamMetadata;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.StoreDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.StoreLocationDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.StoreNodeDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.UpdateObjectStreamMetadataIdAttributes;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.UpdateObjectStreamMetadataJson;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumDateInterval;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StreamKindPK;
import net.solarnetwork.central.datum.v2.domain.StreamRange;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.dao.BasicBulkExportResult;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.BulkLoadingDao;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.jdbc.JdbcBulkLoadingContextSupport;
import net.solarnetwork.domain.Location;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;

/**
 * {@link JdbcOperations} based implementation of {@link DatumEntityDao}.
 * 
 * @author matt
 * @version 2.1
 * @since 3.8
 */
public class JdbcDatumEntityDao
		implements DatumEntityDao, DatumStreamMetadataDao, DatumMaintenanceDao, ReadingDatumDao {

	/**
	 * The default value for the {@code bulkLoadJdbcCall} property.
	 */
	public static final String DEFAULT_BULK_LOADING_JDBC_CALL = "{? = call solardatm.store_datum(?, ?, ?, ?, ?, FALSE)}";

	/**
	 * The default value for the {@code bulkLoadMarkStaleJdbcCall} property.
	 */
	public static final String DEFAULT_BULK_LOADING_MARK_STALE_JDBC_CALL = "{call solardatm.mark_stale_datm_hours(?, ?, ?)}";

	/**
	 * The default value for the {@code bulkLoadAuditJdbcCall} property.
	 */
	public static final String DEFAULT_BULK_LOADING_AUDIT_CALL = "{call solardatm.audit_increment_datum_count(?, ?, ?, ?)}";

	/**
	 * The {@code maxMinuteAggregationHours} property default value.
	 * 
	 * <p>
	 * This represents a 5 week time span to that full months can be queried.
	 * </p>
	 */
	public static final int DEFAULT_MAX_MINUTE_AGG_HOURS = (24 * 7 * 5);

	private final JdbcOperations jdbcTemplate;
	private Cache<UUID, ObjectDatumStreamMetadata> streamMetadataCache;
	private Cache<UUID, ObjectDatumStreamMetadataId> streamMetadataIdCache;
	private PlatformTransactionManager bulkLoadTransactionManager;
	private DataSource bulkLoadDataSource;
	private String bulkLoadJdbcCall = DEFAULT_BULK_LOADING_JDBC_CALL;
	private String bulkLoadMarkStaleJdbcCall = DEFAULT_BULK_LOADING_MARK_STALE_JDBC_CALL;
	private String bulkLoadAuditJdbcCall = DEFAULT_BULK_LOADING_AUDIT_CALL;
	private int maxMinuteAggregationHours = DEFAULT_MAX_MINUTE_AGG_HOURS;

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
		this.jdbcTemplate = requireNonNullArgument(jdbcTemplate, "jdbcTemplate");
	}

	@Override
	public Class<? extends DatumEntity> getObjectType() {
		return DatumEntity.class;
	}

	@Override
	public DatumPK save(DatumEntity entity) {
		requireNonNullArgument(entity, "entity");
		requireNonNullArgument(entity.getStreamId(), "streamId");
		requireNonNullArgument(entity.getTimestamp(), "timestamp");
		jdbcTemplate.update(new InsertDatum(entity));
		return entity.getId();
	}

	@Override
	public DatumPK store(DatumEntity datum) {
		StoreDatum sql = new StoreDatum(datum);
		return jdbcTemplate.execute(sql, new CallableStatementCallback<DatumPK>() {

			@Override
			public DatumPK doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.execute();
				return datum.getId();
			}
		});
	}

	@Override
	public DatumPK store(GeneralNodeDatum datum) {
		if ( datum == null || datum.getNodeId() == null || datum.getSourceId() == null ) {
			return null;
		}
		DatumSamples s = datum.getSamples();
		if ( s == null || s.isEmpty() ) {
			return null;
		}
		StoreNodeDatum sql = new StoreNodeDatum(datum);
		return jdbcTemplate.execute(sql, new CallableStatementCallback<DatumPK>() {

			@Override
			public DatumPK doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.execute();
				UUID streamId = uuidFromCall(cs, 1);
				return new DatumPK(streamId, sql.getTimestamp());
			}
		});
	}

	private static UUID uuidFromCall(CallableStatement call, int parameterIndex) throws SQLException {
		Object streamId = call.getObject(1);
		return (streamId instanceof UUID ? (UUID) streamId
				: streamId != null ? UUID.fromString(streamId.toString()) : null);
	}

	@Override
	public DatumPK store(GeneralLocationDatum datum) {
		if ( datum == null || datum.getLocationId() == null || datum.getSourceId() == null ) {
			return null;
		}
		DatumSamples s = datum.getSamples();
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

	private static PreparedStatementCreator filterSql(DatumCriteria filter) {
		if ( filter.getPartialAggregation() != null || filter.getAggregation() == Aggregation.Year ) {
			return new SelectDatumPartialAggregate(filter,
					filter.getPartialAggregation() != null ? filter.getPartialAggregation()
							: Aggregation.Month);
		} else if ( filter.getAggregation() == Aggregation.RunningTotal ) {
			return new SelectDatumRunningTotal(filter);
		}
		return new SelectDatum(filter);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static RowMapper<Datum> mapper(DatumCriteria filter) {
		if ( filter.hasIdMappings() ) {
			return (RowMapper) new VirtualAggregateDatumEntityRowMapper(filter.getAggregation(),
					filter.getObjectKind() == ObjectDatumKind.Location ? ObjectDatumKind.Location
							: ObjectDatumKind.Node);
		}
		return filter.getAggregation() != null && filter.getAggregation() != Aggregation.None
				? (RowMapper) mapperForAggregate(filter.getAggregation(),
						filter.getReadingType() != null)
				: DatumEntityRowMapper.INSTANCE;
	}

	private void validateFilter(DatumCriteria filter) {
		// restrict Minute level aggregation to maximum length
		final int maxHours = getMaxMinuteAggregationHours();
		if ( maxHours > 0 && filter.getAggregation() != null
				&& filter.getAggregation() != Aggregation.None
				&& filter.getAggregation().compareLevel(Aggregation.Hour) < 0 ) {
			if ( filter.hasDateOrLocalDateRange() ) {
				LocalDateTime s = (filter.hasLocalDateRange() ? filter.getLocalStartDate()
						: filter.getStartDate().atOffset(ZoneOffset.UTC).toLocalDateTime());
				LocalDateTime e = (filter.hasLocalDateRange() ? filter.getLocalEndDate()
						: filter.getEndDate().atOffset(ZoneOffset.UTC).toLocalDateTime());
				long hours = ChronoUnit.HOURS.between(s, e);
				if ( hours > maxHours ) {
					throw new IllegalArgumentException(
							"Minute level aggregation time span must be at most " + maxHours
									+ " hours.");
				}
			}
		}
	}

	@Override
	public ObjectDatumStreamFilterResults<Datum, DatumPK> findFiltered(DatumCriteria filter,
			List<SortDescriptor> sorts, Integer offset, Integer max) {
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument must be provided.");
		}
		validateFilter(filter);
		final PreparedStatementCreator sql = filterSql(filter);
		final RowMapper<Datum> mapper = mapper(filter);

		FilterResults<Datum, DatumPK> results = DatumJdbcUtils.executeFilterQuery(jdbcTemplate, filter,
				sql, mapper);

		if ( mapper instanceof ObjectDatumStreamMetadataProvider ) {
			// virtual streams use this
			return new ProviderObjectDatumStreamFilterResults<>(
					(ObjectDatumStreamMetadataProvider) mapper, results.getResults(),
					results.getTotalResults(), results.getStartingOffset(),
					results.getReturnedResultCount());
		}

		Map<UUID, ObjectDatumStreamMetadata> metaMap = null;
		if ( filter.getStreamIds() != null && filter.getStreamIds().length == 1 ) {
			ObjectDatumStreamMetadata meta = findStreamMetadata(filter);
			if ( meta != null ) {
				metaMap = Collections.singletonMap(meta.getStreamId(), meta);
			} else {
				metaMap = Collections.emptyMap();
			}
		} else {
			ObjectStreamCriteria metaCriteria = DatumUtils.criteriaWithoutDates(filter);
			Iterable<ObjectDatumStreamMetadata> metas = findDatumStreamMetadata(metaCriteria);
			metaMap = stream(metas.spliterator(), false).collect(toMap(DatumStreamMetadata::getStreamId,
					identity(), (u, v) -> u, LinkedHashMap::new));
		}
		return new BasicObjectDatumStreamFilterResults<>(metaMap, results.getResults(),
				results.getTotalResults(), results.getStartingOffset(),
				results.getReturnedResultCount());
	}

	@Override
	public void findFilteredStream(DatumCriteria filter, StreamDatumFilteredResultsProcessor processor,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) throws IOException {
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument must be provided.");
		}
		validateFilter(filter);
		final PreparedStatementCreator sql = filterSql(filter);
		final RowMapper<Datum> mapper = mapper(filter);

		ObjectDatumStreamMetadataProvider metadataProvider = null;
		if ( mapper instanceof ObjectDatumStreamMetadataProvider ) {
			metadataProvider = (ObjectDatumStreamMetadataProvider) mapper;
		} else if ( filter.getStreamIds() != null && filter.getStreamIds().length == 1 ) {
			ObjectDatumStreamMetadata meta = findStreamMetadata(filter);
			if ( meta != null ) {
				metadataProvider = staticProvider(singleton(meta));
			}
		} else {
			ObjectStreamCriteria metaCriteria = DatumUtils.criteriaWithoutDates(filter);
			Iterable<ObjectDatumStreamMetadata> metas = findDatumStreamMetadata(metaCriteria);
			metadataProvider = staticProvider(metas);
		}
		if ( metadataProvider == null ) {
			throw new DataRetrievalFailureException(
					"No streams available that match the given criteria.");
		}
		processor.start(null, null, null, singletonMap(METADATA_PROVIDER_ATTR, metadataProvider)); // TODO: support count total results/offset/max
		try {
			jdbcTemplate.execute(sql, new PreparedStatementCallback<Void>() {

				@Override
				public Void doInPreparedStatement(PreparedStatement ps)
						throws SQLException, DataAccessException {
					try (ResultSet rs = ps.executeQuery()) {
						int row = 0;
						while ( rs.next() ) {
							Datum d = mapper.mapRow(rs, ++row);
							processor.handleResultItem(d);
						}
					} catch ( IOException e ) {
						throw new RuntimeException(e);
					}
					return null;
				}
			});
		} catch ( RuntimeException e ) {
			if ( e.getCause() instanceof IOException ) {
				throw (IOException) e.getCause();
			}
			throw e;
		}
	}

	@Override
	public Iterable<DatumDateInterval> findAvailableInterval(ObjectStreamCriteria filter) {
		return jdbcTemplate.query(new SelectDatumAvailableTimeRange(filter),
				DatumDateIntervalRowMapper.INSTANCE);
	}

	@Override
	public DatumRecordCounts countDatumRecords(ObjectStreamCriteria filter) {
		List<AuditDatum> result = jdbcTemplate.query(new SelectDatumRecordCounts(filter),
				AuditDatumAccumulativeEntityRowMapper.INSTANCE);
		return (result.isEmpty() ? null : result.get(0));
	}

	@Override
	public long deleteFiltered(ObjectStreamCriteria filter) {
		return jdbcTemplate.query(new DeleteDatum(filter), new ResultSetExtractor<Long>() {

			@Override
			public Long extractData(ResultSet rs) throws SQLException, DataAccessException {
				if ( rs.next() ) {
					return rs.getLong(1);
				}
				return 0L;
			}
		});
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
		ObjectDatumKind kind = filter.effectiveObjectKind();
		RowMapper<ObjectDatumStreamMetadata> mapper;
		if ( filter.hasLocationCriteria() ) {
			mapper = (kind == ObjectDatumKind.Location
					? ObjectDatumStreamMetadataGeoRowMapper.LOCATION_INSTANCE
					: ObjectDatumStreamMetadataGeoRowMapper.NODE_INSTANCE);
		} else {
			mapper = (kind == ObjectDatumKind.Location
					? ObjectDatumStreamMetadataRowMapper.LOCATION_INSTANCE
					: ObjectDatumStreamMetadataRowMapper.NODE_INSTANCE);
		}
		PreparedStatementCreator sql = new SelectObjectStreamMetadata(filter, kind);
		return jdbcTemplate.query(sql, mapper);
	}

	@Override
	public Iterable<ObjectDatumStreamMetadataId> findDatumStreamMetadataIds(
			ObjectStreamCriteria filter) {
		ObjectDatumKind kind = filter.effectiveObjectKind();
		RowMapper<ObjectDatumStreamMetadataId> mapper = (kind == ObjectDatumKind.Location
				? ObjectDatumStreamMetadataIdRowMapper.LOCATION_INSTANCE
				: ObjectDatumStreamMetadataIdRowMapper.NODE_INSTANCE);
		PreparedStatementCreator sql = new SelectObjectStreamMetadata(filter, kind,
				MetadataSelectStyle.Minimum);
		return jdbcTemplate.query(sql, mapper);
	}

	@Override
	public Map<UUID, ObjectDatumStreamMetadataId> getDatumStreamMetadataIds(UUID... streamIds) {
		if ( streamIds == null || streamIds.length < 1 ) {
			return Collections.emptyMap();
		}

		final Map<UUID, ObjectDatumStreamMetadataId> result = new LinkedHashMap<>(streamIds.length);
		final List<UUID> queryList = (streamMetadataIdCache != null ? new ArrayList<>(streamIds.length)
				: Arrays.asList(streamIds));
		if ( streamMetadataIdCache != null ) {
			for ( UUID streamId : streamIds ) {
				ObjectDatumStreamMetadataId id = streamMetadataIdCache.get(streamId);
				if ( id != null ) {
					result.put(streamId, id);
				} else {
					queryList.add(streamId);
				}
			}
		}

		if ( queryList.isEmpty() ) {
			return result;
		}

		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {

				try (PreparedStatement stmt = con.prepareStatement(FIND_METADATA_IDS_FOR_STREAM_ID)) {
					int resultNum = 0;
					for ( UUID streamId : queryList ) {
						stmt.setObject(1, streamId, Types.OTHER);
						try (ResultSet rs = stmt.executeQuery()) {
							if ( rs.next() ) {
								ObjectDatumStreamMetadataId id = ObjectDatumStreamMetadataIdRowMapper.INSTANCE
										.mapRow(rs, ++resultNum);
								result.put(streamId, id);
								if ( streamMetadataIdCache != null ) {
									streamMetadataIdCache.put(streamId, id);
								}
							}
						}
					}
				}

				return null;
			}
		});
		return result;
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
	public ObjectDatumStreamMetadataId updateIdAttributes(ObjectDatumKind kind, UUID streamId,
			Long objectId, String sourceId) {
		UpdateObjectStreamMetadataIdAttributes sql = new UpdateObjectStreamMetadataIdAttributes(kind,
				streamId, objectId, sourceId);
		ObjectDatumStreamMetadataId result = jdbcTemplate.execute(sql,
				new PreparedStatementCallback<ObjectDatumStreamMetadataId>() {

					@Override
					public ObjectDatumStreamMetadataId doInPreparedStatement(PreparedStatement ps)
							throws SQLException, DataAccessException {
						RowMapper<ObjectDatumStreamMetadataId> rowMapper;
						if ( kind == ObjectDatumKind.Location ) {
							rowMapper = ObjectDatumStreamMetadataIdRowMapper.LOCATION_INSTANCE;
						} else {
							rowMapper = ObjectDatumStreamMetadataIdRowMapper.NODE_INSTANCE;
						}

						if ( ps.execute() ) {
							try (ResultSet rs = ps.getResultSet()) {
								if ( rs.next() ) {
									return rowMapper.mapRow(rs, 1);
								}
							}
						}
						return null;
					}
				});
		if ( result != null ) {
			// remove from cache
			final Cache<UUID, ObjectDatumStreamMetadata> cache = getStreamMetadataCache();
			if ( cache != null ) {
				cache.remove(streamId);
			}
		}
		return result;
	}

	@Override
	public int markDatumAggregatesStale(DatumStreamCriteria criteria) {
		return jdbcTemplate.update(new InsertStaleAggregateDatumSelect(criteria));

	}

	@Override
	public FilterResults<StaleAggregateDatum, StreamKindPK> findStaleAggregateDatum(
			DatumStreamCriteria filter) {
		final PreparedStatementCreator sql = new SelectStaleAggregateDatum(filter);
		return DatumJdbcUtils.executeFilterQuery(jdbcTemplate, filter, sql,
				StaleAggregateDatumEntityRowMapper.INSTANCE);
	}

	private static RowMapper<ReadingDatum> readingMapper(Aggregation agg) {
		if ( agg == null ) {
			agg = Aggregation.None;
		}
		switch (agg) {
			case Hour:
				return ReadingDatumEntityRowMapper.HOUR_INSTANCE;

			case Day:
				return ReadingDatumEntityRowMapper.DAY_INSTANCE;

			case Month:
				return ReadingDatumEntityRowMapper.MONTH_INSTANCE;

			default:
				return ReadingDatumEntityRowMapper.INSTANCE;
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public ObjectDatumStreamFilterResults<ReadingDatum, DatumPK> findDatumReadingFiltered(
			ReadingDatumCriteria filter) {
		if ( filter == null || filter.getReadingType() == null ) {
			throw new IllegalArgumentException("The filter reading type must be provided.");
		}
		PreparedStatementCreator sql = null;
		DatumReadingType readingType = filter.getReadingType();
		switch (readingType) {
			case Difference:
			case DifferenceWithin:
			case NearestDifference:
			case CalculatedAtDifference:
				sql = new SelectReadingDifference(filter);
				break;

			case CalculatedAt:
				sql = new SelectDatumCalculatedAt(filter);
				break;

			default:
				throw new UnsupportedOperationException(
						"Reading type " + readingType + " is not supported.");

		}

		FilterResults<ReadingDatum, DatumPK> results;
		if ( readingType == DatumReadingType.CalculatedAt ) {
			// this returns Datum, not ReadingDatum... so adapt
			FilterResults<Datum, DatumPK> datumResults = DatumJdbcUtils.executeFilterQuery(jdbcTemplate,
					filter, sql, DatumEntityRowMapper.INSTANCE);
			List<ReadingDatum> readingDatum = stream(datumResults.spliterator(), false)
					.map(e -> new ReadingDatumEntity(e.getStreamId(), e.getTimestamp(), null, null,
							e.getProperties(), null))
					.collect(Collectors.toList());
			results = new BasicFilterResults<>(readingDatum, datumResults.getTotalResults(),
					datumResults.getStartingOffset(), datumResults.getReturnedResultCount());
		} else {
			results = DatumJdbcUtils.executeFilterQuery(jdbcTemplate, filter, sql,
					readingMapper(filter.getAggregation()));
		}

		Map<UUID, ObjectDatumStreamMetadata> metaMap = null;
		if ( filter.getStreamIds() != null && filter.getStreamIds().length == 1 ) {
			ObjectDatumStreamMetadata meta = findStreamMetadata(filter);
			if ( meta != null ) {
				metaMap = Collections.singletonMap(meta.getStreamId(), meta);
			} else {
				metaMap = Collections.emptyMap();
			}
		} else {
			ObjectStreamCriteria metaCriteria = DatumUtils.criteriaWithoutDates(filter);
			Iterable<ObjectDatumStreamMetadata> metas = findDatumStreamMetadata(metaCriteria);
			metaMap = stream(metas.spliterator(), false).collect(toMap(DatumStreamMetadata::getStreamId,
					identity(), (u, v) -> u, LinkedHashMap::new));
		}
		return new BasicObjectDatumStreamFilterResults<>(metaMap, results.getResults(),
				results.getTotalResults(), results.getStartingOffset(),
				results.getReturnedResultCount());

	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public ExportResult bulkExport(ExportCallback<GeneralNodeDatumFilterMatch> callback,
			ExportOptions options) {
		if ( options == null ) {
			throw new IllegalArgumentException("ExportOptions is required");
		}

		// filter
		DatumCriteria filter = options.getParameter(DatumEntityDao.EXPORT_PARAMETER_DATUM_CRITERIA);
		if ( filter == null ) {
			throw new IllegalArgumentException(
					format("DatumCriteria is required on export options parameter '%s'",
							DatumEntityDao.EXPORT_PARAMETER_DATUM_CRITERIA));
		}

		Aggregation agg = filter.getAggregation();
		if ( agg != null ) {
			if ( agg.getLevel() > 0 && agg.compareLevel(Aggregation.FiveMinute) < 0 ) {
				throw new IllegalArgumentException(
						"Must be FiveMinute aggregation or more. For finer granularity results, request without any aggregation.");
			} else if ( agg == Aggregation.RunningTotal && filter.getSourceId() == null ) {
				// source ID is required for RunningTotal currently
				throw new IllegalArgumentException(
						"A sourceId is required for RunningTotal aggregation.");
			}
		} else {
			agg = Aggregation.None;
		}

		/*- TODO
		// combining
		CombiningConfig combining = getCombiningFilterProperties(filter);
		if ( combining != null ) {
			sqlProps.put(PARAM_COMBINING, combining);
		}
		
		// get query name to execute
		String query = getQueryForFilter(filter);
		*/

		SelectDatum sql = new SelectDatum(filter);
		RowMapper<? extends Datum> mapper = (agg != Aggregation.None
				? mapperForAggregate(agg, filter.getReadingType() != null)
				: DatumEntityRowMapper.INSTANCE);

		// attempt count first, if NOT mostRecent query and NOT a *Minute, *DayOfWeek, or *HourOfDay, or RunningTotal aggregate levels
		Long totalCount = null;
		if ( !filter.isMostRecent() && !filter.isWithoutTotalResultsCount()
				&& (agg.getLevel() < 1 || agg.compareTo(Aggregation.Hour) >= 0)
				&& agg != Aggregation.DayOfWeek && agg != Aggregation.SeasonalDayOfWeek
				&& agg != Aggregation.HourOfDay && agg != Aggregation.SeasonalHourOfDay
				&& agg != Aggregation.RunningTotal ) {
			totalCount = DatumJdbcUtils.executeCountQuery(jdbcTemplate,
					sql.countPreparedStatementCreator());
		}

		callback.didBegin(totalCount);
		return jdbcTemplate.query(sql, new ExportResultSetExtractor(mapper, callback));
	}

	private class ExportResultSetExtractor implements ResultSetExtractor<ExportResult> {

		private final RowMapper<? extends Datum> mapper;
		private final ExportCallback<GeneralNodeDatumFilterMatch> callback;
		private long count;

		private ExportResultSetExtractor(RowMapper<? extends Datum> mapper,
				ExportCallback<GeneralNodeDatumFilterMatch> callback) {
			super();
			this.mapper = mapper;
			this.callback = callback;
			this.count = 0;
		}

		@Override
		public ExportResult extractData(ResultSet rs) throws SQLException, DataAccessException {
			final StreamIdStreamMetadataCriteria metaCriteria = new StreamIdStreamMetadataCriteria();
			while ( rs.next() ) {
				Datum d = mapper.mapRow(rs, (int) ++count);
				metaCriteria.setStreamId(d.getStreamId());
				ObjectDatumStreamMetadata meta = findStreamMetadata(metaCriteria);
				ReportingGeneralNodeDatum gnd = DatumUtils.toGeneralNodeDatum(d, meta);
				callback.handle(gnd);
			}
			return new BasicBulkExportResult(count);
		}

	}

	/**
	 * A {@link StreamMetadataCriteria} for internal use where the stream ID is
	 * mutable.
	 */
	private static class StreamIdStreamMetadataCriteria implements StreamMetadataCriteria {

		private final UUID[] streamIds = new UUID[1];

		/**
		 * Set the stream ID.
		 * 
		 * @param streamId
		 *        the stream ID to set
		 */
		public void setStreamId(UUID streamId) {
			streamIds[0] = streamId;
		}

		@Override
		public UUID getStreamId() {
			return streamIds[0];
		}

		@Override
		public UUID[] getStreamIds() {
			return streamIds;
		}

		@Override
		public String getSourceId() {
			return null;
		}

		@Override
		public String[] getSourceIds() {
			return null;
		}

		@Override
		public Long getLocationId() {
			return null;
		}

		@Override
		public Long[] getLocationIds() {
			return null;
		}

		@Override
		public Location getLocation() {
			return null;
		}

		@Override
		public Long getUserId() {
			return null;
		}

		@Override
		public Long[] getUserIds() {
			return null;
		}

		@Override
		public String getTokenId() {
			return null;
		}

		@Override
		public String[] getTokenIds() {
			return null;
		}

		@Override
		public List<SortDescriptor> getSorts() {
			return null;
		}

		@Override
		public String getSearchFilter() {
			return null;
		}

	}

	@Override
	public LoadingContext<GeneralNodeDatum> createBulkLoadingContext(LoadingOptions options,
			LoadingExceptionHandler<GeneralNodeDatum> exceptionHandler) {
		return new BulkLoadingContext(options, exceptionHandler);
	}

	private class BulkLoadingContext extends JdbcBulkLoadingContextSupport<GeneralNodeDatum> {

		private final Timestamp start;

		private final Map<UUID, BulkLoadStats> streamStats = new HashMap<>(32);

		public BulkLoadingContext(LoadingOptions options,
				LoadingExceptionHandler<GeneralNodeDatum> exceptionHandler) {
			super(bulkLoadTransactionManager, bulkLoadDataSource, bulkLoadJdbcCall, options,
					exceptionHandler);
			start = new Timestamp(System.currentTimeMillis());
		}

		@Override
		protected PreparedStatement createJdbcStatement(Connection con) throws SQLException {
			CallableStatement call = (CallableStatement) super.createJdbcStatement(con);
			call.registerOutParameter(1, Types.OTHER);
			return call;
		}

		@Override
		protected boolean doLoad(GeneralNodeDatum d, PreparedStatement stmt, long index)
				throws SQLException {
			stmt.setTimestamp(2, Timestamp.from(d.getCreated()));
			stmt.setLong(3, d.getNodeId());
			stmt.setString(4, d.getSourceId());
			stmt.setTimestamp(5, d.getPosted() != null ? Timestamp.from(d.getPosted()) : start);
			stmt.setString(6, d.getSampleJson());
			stmt.executeUpdate();
			UUID streamId = uuidFromCall((CallableStatement) stmt, 1);

			// keep track of import min/max date ranges, so they can be updated at end
			BulkLoadStats stats = streamStats.compute(streamId, (k, v) -> {
				return (v != null ? v : new BulkLoadStats(k));
			});
			stats.updateStatsForDatum(d);

			return true;
		}

		@Override
		public void commit() {
			commitDateRanges();
			super.commit();
		}

		private void commitDateRanges() {
			try (CallableStatement auditStmt = getConnection().prepareCall(bulkLoadAuditJdbcCall);
					CallableStatement staleStmt = getConnection()
							.prepareCall(bulkLoadMarkStaleJdbcCall)) {
				for ( BulkLoadStats stat : streamStats.values() ) {
					auditStmt.setObject(1, stat.getStreamId(), Types.OTHER);
					auditStmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
					auditStmt.setInt(3, stat.dcount);
					auditStmt.setInt(4, stat.pcount);
					auditStmt.execute();

					staleStmt.setObject(1, stat.getStreamId(), Types.OTHER);
					staleStmt.setTimestamp(2, Timestamp.from(stat.getStartDate()));
					staleStmt.setTimestamp(3, Timestamp.from(stat.getEndDate()));
					staleStmt.execute();
				}
			} catch ( SQLException e ) {
				BulkLoadingDao.LoadingExceptionHandler<GeneralNodeDatum> handler = getExceptionHandler();
				String msg = "Error updating audit/stale datum records.";
				if ( handler != null ) {
					handler.handleLoadingException(new RuntimeException(msg, e), this);
				} else {
					log.error(msg, e);
				}
			}
			streamStats.clear();
		}

	}

	/**
	 * Internal class used to track statistics during bulk import.
	 */
	private static class BulkLoadStats extends StreamRange {

		private int dcount;
		private int pcount;

		private BulkLoadStats(UUID streamId) {
			super(streamId);
		}

		/**
		 * Add statistics for a given datum.
		 * 
		 * @param d
		 *        the datum to calculate the datum/property counts from
		 */
		public void updateStatsForDatum(GeneralNodeDatum d) {
			if ( d == null ) {
				return;
			}

			if ( getStartDate() == null || d.getCreated().isBefore(getStartDate()) ) {
				setStartDate(d.getCreated());
			}
			if ( getEndDate() == null || d.getCreated().isAfter(getEndDate()) ) {
				setEndDate(d.getCreated());
			}

			dcount += 1;
			DatumSamples s = d.getSamples();
			if ( s == null ) {
				return;
			}
			addPropCounts(s.getInstantaneous());
			addPropCounts(s.getAccumulating());
			addPropCounts(s.getStatus());
			Set<String> tags = s.getTags();
			if ( tags != null ) {
				pcount += tags.size();
			}
		}

		private void addPropCounts(Map<?, ?> m) {
			if ( m != null ) {
				pcount += m.size();
			}
		}

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

	/**
	 * Get the stream metadata ID cache.
	 * 
	 * @return the cache, or {@literal null}
	 * @since 2.1
	 */
	public Cache<UUID, ObjectDatumStreamMetadataId> getStreamMetadataIdCache() {
		return streamMetadataIdCache;
	}

	/**
	 * Set the stream metadata ID cache.
	 * 
	 * @param streamMetadataIdCache
	 *        the cache to set
	 * @since 2.1
	 */
	public void setStreamMetadataIdCache(
			Cache<UUID, ObjectDatumStreamMetadataId> streamMetadataIdCache) {
		this.streamMetadataIdCache = streamMetadataIdCache;
	}

	/**
	 * Get the bulk load transaction manager.
	 * 
	 * @return the manager
	 */
	public PlatformTransactionManager getBulkLoadTransactionManager() {
		return bulkLoadTransactionManager;
	}

	/**
	 * Set the bulk load transaction manager.
	 * 
	 * @param bulkLoadTransactionManager
	 *        the manager to set
	 */
	public void setBulkLoadTransactionManager(PlatformTransactionManager bulkLoadTransactionManager) {
		this.bulkLoadTransactionManager = bulkLoadTransactionManager;
	}

	/**
	 * Get the bulk load data source.
	 * 
	 * @return the data source
	 */
	public DataSource getBulkLoadDataSource() {
		return bulkLoadDataSource;
	}

	/**
	 * Set the bulk load data source.
	 * 
	 * @param bulkLoadDataSource
	 *        the data source to set
	 */
	public void setBulkLoadDataSource(DataSource bulkLoadDataSource) {
		this.bulkLoadDataSource = bulkLoadDataSource;
	}

	/**
	 * Get the bulk load JDBC call.
	 * 
	 * @return the bulk load JDBC call; defaults to
	 *         {@link #DEFAULT_BULK_LOADING_JDBC_CALL}
	 */
	public String getBulkLoadJdbcCall() {
		return bulkLoadJdbcCall;
	}

	/**
	 * Set the bulk load JDBC call.
	 * 
	 * @param bulkLoadJdbcCall
	 *        the call to set
	 */
	public void setBulkLoadJdbcCall(String bulkLoadJdbcCall) {
		this.bulkLoadJdbcCall = bulkLoadJdbcCall;
	}

	/**
	 * Get the bulk load "mark stale" JDBC call.
	 * 
	 * @return the call; defaults to
	 *         {@link #DEFAULT_BULK_LOADING_MARK_STALE_JDBC_CALL}
	 */
	public String getBulkLoadMarkStaleJdbcCall() {
		return bulkLoadMarkStaleJdbcCall;
	}

	/**
	 * Set the bulk load "mark stale" JDBC call.
	 * 
	 * @param bulkLoadMarkStaleJdbcCall
	 *        the call to set
	 */
	public void setBulkLoadMarkStaleJdbcCall(String bulkLoadMarkStaleJdbcCall) {
		this.bulkLoadMarkStaleJdbcCall = bulkLoadMarkStaleJdbcCall;
	}

	/**
	 * Get the bulk load "update audit counts" JDBC call.
	 * 
	 * @return the call; defaults to {@link #DEFAULT_BULK_LOADING_AUDIT_CALL}
	 */
	public String getBulkLoadAuditJdbcCall() {
		return bulkLoadAuditJdbcCall;
	}

	/**
	 * Set the bulk load "update audit counts" JDBC call.
	 * 
	 * @param bulkLoadAuditJdbcCall
	 *        the call to set
	 */
	public void setBulkLoadAuditJdbcCall(String bulkLoadAuditJdbcCall) {
		this.bulkLoadAuditJdbcCall = bulkLoadAuditJdbcCall;
	}

	/**
	 * Get the maximum number of hours to allow in minute-level aggregation
	 * queries.
	 * 
	 * @return the maximum hours; defaults to
	 *         {@link #DEFAULT_MAX_MINUTE_AGG_HOURS}
	 */
	public int getMaxMinuteAggregationHours() {
		return maxMinuteAggregationHours;
	}

	/**
	 * Set the maximum number of hours to allow in minute-level aggregation
	 * queries.
	 * 
	 * @param maxMinuteAggregationHours
	 *        the maximum hours to set; anything less than {@literal 1} means
	 *        there is no maximum
	 */
	public void setMaxMinuteAggregationHours(int maxMinuteAggregationHours) {
		this.maxMinuteAggregationHours = maxMinuteAggregationHours;
	}

}
