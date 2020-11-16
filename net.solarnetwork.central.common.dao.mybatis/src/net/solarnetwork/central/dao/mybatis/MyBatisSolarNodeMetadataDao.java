/* ==================================================================
 * MyBatisSolarNodeMetadataDao.java - 11/11/2016 1:50:01 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.mybatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;

/**
 * MyBatis implementation of {@link SolarNodeMetadataDao}.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisSolarNodeMetadataDao extends BaseMyBatisGenericDao<SolarNodeMetadata, Long>
		implements SolarNodeMetadataDao {

	/** The query parameter for a general {@code Filter} object value. */
	public static final String PARAM_FILTER = "filter";

	/**
	 * Default constructor.
	 */
	public MyBatisSolarNodeMetadataDao() {
		super(SolarNodeMetadata.class, Long.class);
	}

	private String getQueryForFilter(SolarNodeMetadataFilter filter) {
		return getQueryForAll() + "-SolarNodeMetadataMatch";
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<SolarNodeMetadataFilterMatch> findFiltered(SolarNodeMetadataFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		final String query = getQueryForFilter(filter);
		Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(PARAM_FILTER, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}

		// attempt count first, if max NOT specified as -1
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 ) {
			totalCount = executeCountQuery(query + "-count", sqlProps);
		}

		List<SolarNodeMetadataFilterMatch> rows = selectList(query, sqlProps, offset, max);

		BasicFilterResults<SolarNodeMetadataFilterMatch> results = new BasicFilterResults<SolarNodeMetadataFilterMatch>(
				rows, (totalCount != null ? totalCount : Long.valueOf(rows.size())), offset,
				rows.size());

		return results;
	}
}
