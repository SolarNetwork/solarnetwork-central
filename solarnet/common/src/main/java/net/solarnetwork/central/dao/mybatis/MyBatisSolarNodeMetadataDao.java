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

import static java.util.stream.Collectors.toList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.util.MapPathMatcher;
import net.solarnetwork.util.SearchFilter;

/**
 * MyBatis implementation of {@link SolarNodeMetadataDao}.
 *
 * @author matt
 * @version 2.2
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

	@Override
	public FilterResults<SolarNodeMetadataFilterMatch, Long> findFiltered(SolarNodeMetadataFilter filter,
			List<SortDescriptor> sortDescriptors, Long offset, Integer max) {
		final String query = getQueryForFilter(filter);
		Map<String, Object> sqlProps = new HashMap<>(1);
		sqlProps.put(PARAM_FILTER, filter);
		if ( sortDescriptors != null && !sortDescriptors.isEmpty() ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}

		List<SolarNodeMetadataFilterMatch> rows = selectList(query, sqlProps, null, null);

		SearchFilter sf = SearchFilter.forLDAPSearchFilterString(filter.getMetadataFilter());
		if ( sf != null ) {
			// filter out only those matching the SearchFilter
			rows = rows.stream().filter(m -> {
				Map<String, Object> map = JsonUtils.getStringMap(m.getMetaJson());
				return (map != null && MapPathMatcher.matches(map, sf));
			}).collect(toList());
		}

		return new BasicFilterResults<>(rows, Long.valueOf(rows.size()), offset != null ? offset : 0L,
				rows.size());
	}
}
