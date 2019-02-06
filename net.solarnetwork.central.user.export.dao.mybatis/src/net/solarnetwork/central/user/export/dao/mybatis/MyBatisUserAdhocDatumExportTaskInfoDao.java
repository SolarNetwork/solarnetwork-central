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
import org.joda.time.DateTime;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.user.dao.mybatis.BaseMyBatisUserRelatedGenericDao;
import net.solarnetwork.central.user.export.dao.UserAdhocDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.domain.UserAdhocDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;

/**
 * MyBatis implementation of {@link UserDatumExportTaskInfoDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public class MyBatisUserAdhocDatumExportTaskInfoDao
		extends BaseMyBatisUserRelatedGenericDao<UserAdhocDatumExportTaskInfo, UUID>
		implements UserAdhocDatumExportTaskInfoDao {

	/** The query name used for {@link #getForTaskId(UUID)}. */
	public static final String QUERY_TASK_INFO_FOR_TASK_ID = "get-UserAdhocDatumExportTaskInfo-for-task-id";

	/**
	 * The {@code DELETE} query name used for
	 * {@link #purgeCompletedTasks(DateTime)}.
	 */
	public static final String UPDATE_PURGE_COMPLETED = "delete-UserAdhocDatumExportTaskInfo-completed";

	/**
	 * The {@code INSERT} query name used for
	 * {@link #addAdHocDatumExport(UserDatumExportTaskInfo)}.
	 */
	public static final String ADD_AD_HOC_TASK = "add-ad-hoc-task";

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

	//	@Override
	//	public UserAdhocDatumExportTaskInfo getForTaskId(UUID taskId) {
	//		return selectFirst(QUERY_TASK_INFO_FOR_TASK_ID, taskId);
	//	}

	@Override
	public long purgeCompletedTasks(DateTime olderThanDate) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("date", olderThanDate);
		getSqlSession().update(UPDATE_PURGE_COMPLETED, params);
		Long result = (Long) params.get("result");
		return (result == null ? 0 : result.longValue());
	}

	//	@Override
	//	public UUID addAdHocDatumExport(UserAdhocDatumExportTaskInfo info) {
	//		UUID uuid = getSqlSession().selectOne(ADD_AD_HOC_TASK, info);
	//		return uuid;
	//	}

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
