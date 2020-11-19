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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.v2.dao.BasicDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.LocationMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.NodeMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.StreamMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.InsertDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectLocationStreamMetadata;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectNodeStreamMetadata;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.LocationDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.SortDescriptor;

/**
 * {@link JdbcOperations} based implementation of {@link DatumEntityDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class JdbcDatumEntityDao implements DatumEntityDao, DatumStreamMetadataDao {

	private final JdbcOperations jdbcTemplate;

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
	public DatumEntity get(DatumPK id) {
		List<Datum> result = jdbcTemplate.query(new SelectDatum(id), DatumEntityRowMapper.INSTANCE);
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
		PreparedStatementCreator creator = new SelectDatum(filter);

		Long totalCount = null;
		if ( filter.getMax() != null && creator instanceof CountPreparedStatementCreatorProvider ) {
			totalCount = DatumSqlUtils.executeCountQuery(jdbcTemplate,
					((CountPreparedStatementCreatorProvider) creator).countPreparedStatementCreator());
		}

		List<Datum> data = jdbcTemplate.query(creator, DatumEntityRowMapper.INSTANCE);
		if ( filter.getMax() == null ) {
			totalCount = (long) data.size();
		}

		Iterable<? extends ObjectDatumStreamMetadata> meta = null;
		if ( filter.getLocationIds() != null ) {
			meta = findLocationDatumStreamMetadata(filter);
		} else {
			meta = findNodeDatumStreamMetadata(filter);
		}
		Map<UUID, DatumStreamMetadata> metaMap = StreamSupport.stream(meta.spliterator(), false).collect(
				toMap(DatumStreamMetadata::getStreamId, identity(), (u, v) -> u, LinkedHashMap::new));

		return new BasicDatumStreamFilterResults(metaMap, data, totalCount,
				(filter.getOffset() != null ? filter.getOffset().intValue() : 0), data.size());
	}

	@Override
	public ObjectDatumStreamMetadata findStreamMetadata(StreamMetadataCriteria filter) {
		List<ObjectDatumStreamMetadata> results = jdbcTemplate.query(new SelectStreamMetadata(filter),
				ObjectDatumStreamMetadataRowMapper.INSTANCE);
		return (results.isEmpty() ? null : results.get(0));
	}

	@Override
	public Iterable<NodeDatumStreamMetadata> findNodeDatumStreamMetadata(NodeMetadataCriteria filter) {
		return jdbcTemplate.query(new SelectNodeStreamMetadata(filter),
				ObjectDatumStreamMetadataRowMapper.NODE_INSTANCE);
	}

	@Override
	public Iterable<LocationDatumStreamMetadata> findLocationDatumStreamMetadata(
			LocationMetadataCriteria filter) {
		return jdbcTemplate.query(new SelectLocationStreamMetadata(filter),
				ObjectDatumStreamMetadataRowMapper.LOCATION_INSTANCE);
	}

}
