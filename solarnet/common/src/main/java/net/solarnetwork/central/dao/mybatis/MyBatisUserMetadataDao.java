/* ==================================================================
 * MyBatisUserMetadataDao.java - 11/11/2016 5:42:00 PM
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.UserMetadataDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDaoSupport;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * MyBatis implementation of {@link UserMetadataDao}.
 * 
 * @author matt
 * @version 2.1
 * @since 1.8
 */
public class MyBatisUserMetadataDao extends
		BaseMyBatisFilterableDaoSupport<UserMetadataEntity, Long, UserMetadataEntity, UserMetadataFilter>
		implements UserMetadataDao {

	/**
	 * The query used by {@link #metadataAtPath(Long, String)}.
	 */
	public static final String QUERY_FOR_METADATA_PATH = "get-user-metadata-at-path";

	/**
	 * Default constructor.
	 */
	public MyBatisUserMetadataDao() {
		super(UserMetadataEntity.class, Long.class, UserMetadataEntity.class);
	}

	@Override
	public String jsonMetadataAtPath(Long userId, String path) {
		Map<String, Object> params = new HashMap<String, Object>(1);
		params.put("userId", requireNonNullArgument(userId, "userId"));
		params.put("path", requireNonNullArgument(path, "path"));
		return selectFirst(QUERY_FOR_METADATA_PATH, params);
	}

	@Override
	public FilterResults<UserMetadataEntity, Long> findFiltered(UserMetadataFilter filter,
			List<SortDescriptor> sorts, Integer offset, Integer max) {
		return doFindFiltered(filter, sorts, offset, max);
	}

}
