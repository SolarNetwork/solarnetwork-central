/* ==================================================================
 * IbatisGeneralNodeDatumMetadataDao.java - Oct 3, 2014 10:28:39 AM
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

package net.solarnetwork.central.datum.dao.ibatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.ibatis.IbatisBaseGenericDaoSupport;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumMetadataDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadata;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;

/**
 * Ibatis implementation of {@link GeneralNodeDatumMetadataDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisGeneralNodeDatumMetadataDao extends
		IbatisBaseGenericDaoSupport<GeneralNodeDatumMetadata, NodeSourcePK> implements
		GeneralNodeDatumMetadataDao {

	/** The query parameter for a general {@link Filter} object value. */
	public static final String PARAM_FILTER = "filter";

	/**
	 * Default constructor.
	 */
	public IbatisGeneralNodeDatumMetadataDao() {
		super(GeneralNodeDatumMetadata.class, NodeSourcePK.class);
	}

	private Long executeCountQuery(final String countQueryName, final Map<String, ?> sqlProps) {
		Number n = (Number) getSqlMapClientTemplate().queryForObject(countQueryName, sqlProps,
				Number.class);
		if ( n != null ) {
			return n.longValue();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> executeQueryForList(final String query, Map<String, Object> sqlProps,
			Integer offset, Integer max) {
		List<T> rows = null;
		if ( max != null && max > 0 ) {
			rows = getSqlMapClientTemplate().queryForList(query, sqlProps,
					(offset == null || offset.intValue() < 0 ? 0 : offset.intValue()), max);
		} else {
			rows = getSqlMapClientTemplate().queryForList(query, sqlProps);
		}
		return rows;
	}

	private String getQueryForFilter(GeneralNodeDatumMetadataFilter filter) {
		return getQueryForAll() + "-GeneralNodeDatumMetadataMatch";
	}

	@Override
	public FilterResults<GeneralNodeDatumMetadataFilterMatch> findFiltered(
			GeneralNodeDatumMetadataFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
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

		List<GeneralNodeDatumMetadataFilterMatch> rows = executeQueryForList(query, sqlProps, offset,
				max);

		BasicFilterResults<GeneralNodeDatumMetadataFilterMatch> results = new BasicFilterResults<GeneralNodeDatumMetadataFilterMatch>(
				rows, (totalCount != null ? totalCount : Long.valueOf(rows.size())), offset, rows.size());

		return results;
	}

}
