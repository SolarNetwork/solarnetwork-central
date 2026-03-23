/* ==================================================================
 * MyBatisUserDatumExportTaskInfoDao.java - 18/04/2018 9:41:40 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.dao.mybatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.user.dao.mybatis.BaseMyBatisUserRelatedGenericDao;
import net.solarnetwork.central.user.export.dao.UserAdhocDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.domain.UserAdhocDatumExportTaskInfo;

/**
 * MyBatis implementation of {@link UserDatumExportTaskInfoDao}.
 *
 * @author matt
 * @version 1.2
 * @since 1.1
 */
public class MyBatisUserAdhocDatumExportTaskInfoDao
		extends BaseMyBatisUserRelatedGenericDao<UserAdhocDatumExportTaskInfo, UUID>
		implements UserAdhocDatumExportTaskInfoDao {

	/**
	 * The query name used for {@link #findTasksForUser(Long, Set, Boolean)}.
	 */
	public static final String QUERY_TASKS_FOR_USER = "find-UserAdhocDatumExportTaskInfo-for-user";

	/**
	 * Default constructor.
	 */
	public MyBatisUserAdhocDatumExportTaskInfoDao() {
		super(UserAdhocDatumExportTaskInfo.class, UUID.class);
	}

	@Override
	public List<UserAdhocDatumExportTaskInfo> findTasksForUser(Long userId, Set<DatumExportState> states,
			Boolean success) {
		Map<String, Object> params = new HashMap<>(3);
		params.put("user", userId);
		if ( states != null && !states.isEmpty() ) {
			String[] array = states.stream().map(s -> String.valueOf(s.getKey())).toArray(String[]::new);
			params.put("states", array);
		}
		params.put("success", success);
		return selectList(QUERY_TASKS_FOR_USER, params, null, null);
	}

}
