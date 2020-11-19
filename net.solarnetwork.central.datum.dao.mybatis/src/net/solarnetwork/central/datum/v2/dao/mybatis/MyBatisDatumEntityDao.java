/* ==================================================================
 * MyBatisDatumEntityDao.java - 26/10/2020 12:53:23 pm
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

package net.solarnetwork.central.datum.v2.dao.mybatis;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDaoSupport;
import net.solarnetwork.central.datum.v2.dao.BasicDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * MyBatis implementation of {@link DatumEntityDao}.
 * 
 * <p>
 * <b>Pagination note:</b> this implementation makes use of the
 * {@link DatumCriteria#getMax()} and {@link DatumCriteria#getOffset()} values
 * to handle pagination more efficiently on the server side, rather than here on
 * the client. This has been implemented with Postgres in mind, where
 * {@code FORWARD_ONLY} must be used for server-side cursor support on large
 * results. With that in mind, {@code LIMIT} and {@code OFFSET} SQL constructs
 * are used so JDBC does not have to stream back all results just to skip to a
 * particular offset.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class MyBatisDatumEntityDao
		extends BaseMyBatisFilterableDaoSupport<DatumEntity, DatumPK, Datum, DatumCriteria>
		implements DatumEntityDao {

	/** Query name enumeration. */
	public enum QueryName {

		LocationMetadataForFilter("find-LocationDatumStreamMetadata-for-filter"),

		NodeMetadataForFilter("find-NodeDatumStreamMetadata-for-filter");

		private final String queryName;

		private QueryName(String queryName) {
			this.queryName = queryName;
		}

		/**
		 * Get the query name.
		 * 
		 * @return the query name
		 */
		public String getQueryName() {
			return queryName;
		}
	}

	private final JdbcOperations jdbcTemplate;

	/**
	 * Constructor.
	 * 
	 * @param jdbcTemplate
	 *        a JDBC operations to use
	 */
	public MyBatisDatumEntityDao(JdbcOperations jdbcTemplate) {
		super(DatumEntity.class, DatumPK.class, DatumEntity.class);
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public FilterResults<Datum, DatumPK> createFilterResults(DatumCriteria filter,
			Map<String, Object> sqlProps, Iterable<Datum> rows, Long totalCount, Integer offset,
			Integer returnedCount) {
		final String queryName = filter.getLocationId() != null
				? QueryName.LocationMetadataForFilter.getQueryName()
				: QueryName.NodeMetadataForFilter.getQueryName();
		List<DatumStreamMetadata> metaList = selectList(queryName, sqlProps, null, null);
		Map<UUID, DatumStreamMetadata> meta = metaList.stream().collect(
				toMap(DatumStreamMetadata::getStreamId, identity(), (u, v) -> u, LinkedHashMap::new));
		return new BasicDatumStreamFilterResults(meta, rows, totalCount,
				(offset != null ? offset.intValue() : 0),
				(returnedCount != null ? returnedCount.intValue() : 0));
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	@Override
	public DatumStreamFilterResults findFiltered(DatumCriteria filter, List<SortDescriptor> sorts,
			Integer offset, Integer max) {
		return (DatumStreamFilterResults) doFindFiltered(filter, sorts, offset, max);
	}

}
