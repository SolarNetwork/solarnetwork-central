/* ==================================================================
 * MyBatisGeneralLocationDatumMetadataDao.java - Nov 13, 2014 6:43:25 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao.mybatis;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.datum.dao.GeneralLocationDatumMetadataDao;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadata;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;

/**
 * MyBatis implementation of {@link GeneralLocationDatumMetadataDao}.
 * 
 * @author matt
 * @version 1.2
 */
public class MyBatisGeneralLocationDatumMetadataDao
		extends BaseMyBatisGenericDao<GeneralLocationDatumMetadata, LocationSourcePK>
		implements GeneralLocationDatumMetadataDao {

	/** The query parameter for a general filter object value. */
	public static final String PARAM_FILTER = "filter";

	/**
	 * The default query name used for
	 * {@link #getFilteredSources(Long[], String)}.
	 * 
	 * @since 1.1
	 */
	public static final String QUERY_FOR_SOURCES = "find-loc-metadata-distinct-sources";

	/**
	 * Default constructor.
	 */
	public MyBatisGeneralLocationDatumMetadataDao() {
		super(GeneralLocationDatumMetadata.class, LocationSourcePK.class);
	}

	private String getQueryForFilter(GeneralLocationDatumMetadataFilter filter) {
		return getQueryForAll() + "-GeneralLocationDatumMetadataMatch";
	}

	@Override
	// Propagation.REQUIRED for server-side cursors
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public FilterResults<GeneralLocationDatumMetadataFilterMatch> findFiltered(
			GeneralLocationDatumMetadataFilter filter, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		final String query = getQueryForFilter(filter);
		Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(PARAM_FILTER, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}

		addFullTextSearchFilterProperties(filter, sqlProps);

		// attempt count first, if max NOT specified as -1
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 ) {
			totalCount = executeCountQuery(query + "-count", sqlProps);
		}

		List<GeneralLocationDatumMetadataFilterMatch> rows = selectList(query, sqlProps, offset, max);

		BasicFilterResults<GeneralLocationDatumMetadataFilterMatch> results = new BasicFilterResults<GeneralLocationDatumMetadataFilterMatch>(
				rows, (totalCount != null ? totalCount : Long.valueOf(rows.size())), offset,
				rows.size());

		return results;
	}

	protected void addFullTextSearchFilterProperties(GeneralLocationDatumMetadataFilter filter,
			Map<String, Object> sqlProps) {
		if ( filter != null && filter.getLocation() != null ) {
			Location loc = filter.getLocation();
			StringBuilder fts = new StringBuilder();
			spaceAppend(loc.getName(), fts);
			spaceAppend(loc.getCountry(), fts);
			spaceAppend(loc.getRegion(), fts);
			spaceAppend(loc.getStateOrProvince(), fts);
			spaceAppend(loc.getLocality(), fts);
			spaceAppend(loc.getPostalCode(), fts);
			if ( fts.length() > 0 ) {
				sqlProps.put("fts", fts.toString());
			}
		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<LocationSourcePK> getFilteredSources(Long[] locationIds, String metadataFilter) {
		Map<String, Object> sqlProps = new HashMap<String, Object>(2);
		sqlProps.put("locationIds", locationIds);
		sqlProps.put(PARAM_FILTER, metadataFilter);
		List<LocationSourcePK> rows = selectList(QUERY_FOR_SOURCES, sqlProps, null, null);
		return new LinkedHashSet<LocationSourcePK>(rows);
	}

}
