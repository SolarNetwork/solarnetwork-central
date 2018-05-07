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
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTime;
import net.solarnetwork.central.user.dao.mybatis.BaseMyBatisUserRelatedGenericDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskPK;

/**
 * MyBatis implementation of {@link UserDatumExportTaskInfoDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserDatumExportTaskInfoDao
		extends BaseMyBatisUserRelatedGenericDao<UserDatumExportTaskInfo, UserDatumExportTaskPK>
		implements UserDatumExportTaskInfoDao {

	/** The query name used for {@link #getForTaskId(UUID)}. */
	public static final String QUERY_TASK_INFO_FOR_TASK_ID = "get-UserDatumExportTaskInfo-for-task-id";

	/**
	 * The {@code DELETE} query name used for
	 * {@link #purgeCompletedTasks(DateTime)}.
	 */
	public static final String UPDATE_PURGE_COMPLETED = "delete-UserDatumExportTaskInfo-completed";

	/**
	 * Default constructor.
	 */
	public MyBatisUserDatumExportTaskInfoDao() {
		super(UserDatumExportTaskInfo.class, UserDatumExportTaskPK.class);
	}

	@Override
	public UserDatumExportTaskInfo getForTaskId(UUID taskId) {
		return selectFirst(QUERY_TASK_INFO_FOR_TASK_ID, taskId);
	}

	@Override
	public long purgeCompletedTasks(DateTime olderThanDate) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("date", olderThanDate);
		getSqlSession().update(UPDATE_PURGE_COMPLETED, params);
		Long result = (Long) params.get("result");
		return (result == null ? 0 : result.longValue());
	}

}
