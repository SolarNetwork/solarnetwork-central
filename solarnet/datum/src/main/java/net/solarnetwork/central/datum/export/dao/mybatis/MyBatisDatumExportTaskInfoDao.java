/* ==================================================================
 * MyBatisDatumExportTaskInfoDao.java - 19/04/2018 10:44:12 AM
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

package net.solarnetwork.central.datum.export.dao.mybatis;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.datum.export.dao.DatumExportTaskInfoDao;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;

/**
 * MyBatis implementation of {@link DatumExportTaskInfoDao}.
 *
 * @author matt
 * @version 1.0
 */
public class MyBatisDatumExportTaskInfoDao extends BaseMyBatisGenericDao<DatumExportTaskInfo, UUID>
		implements DatumExportTaskInfoDao {

	/** The default query name used for {@link #claimQueuedTask()}. */
	public static final String QUERY_FOR_CLAIMING_TASK = "get-DatumExportTaskInfo-for-claim";

	/**
	 * The {@code DELETE} query name used for
	 * {@link #purgeCompletedTasks(Instant)}.
	 */
	public static final String UPDATE_PURGE_COMPLETED = "delete-DatumExportTaskInfo-completed";

	private String queryForClaimQueuedTask;
	private String updateDeleteCompletedTasks;

	/**
	 * Constructor.
	 */
	public MyBatisDatumExportTaskInfoDao() {
		super(DatumExportTaskInfo.class, UUID.class);
		setQueryForClaimQueuedTask(QUERY_FOR_CLAIMING_TASK);
		setUpdateDeleteCompletedTasks(UPDATE_PURGE_COMPLETED);
	}

	@Override
	public DatumExportTaskInfo claimQueuedTask() {
		DatumExportTaskInfo info = selectFirst(queryForClaimQueuedTask, null);
		if ( info != null ) {
			// re-fetch
			info = get(info.getId());
		}
		return info;
	}

	@Override
	public long purgeCompletedTasks(Instant olderThanDate) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("date", olderThanDate);
		getSqlSession().update(updateDeleteCompletedTasks, params);
		Long result = (Long) params.get("result");
		return (result == null ? 0 : result.longValue());
	}

	/**
	 * Set the query name for the {@link #claimQueuedTask()} method to use.
	 *
	 * @param queryForClaimQueuedTask
	 *        the query name; defaults to {@link #QUERY_FOR_CLAIMING_TASK}
	 */
	public void setQueryForClaimQueuedTask(String queryForClaimQueuedTask) {
		this.queryForClaimQueuedTask = queryForClaimQueuedTask;
	}

	/**
	 * Set the statement name for the {@link #purgeCompletedTasks(Instant)}
	 * method to use.
	 *
	 * @param updateDeleteCompletedTasks
	 *        the statement name; defaults to {@link #UPDATE_PURGE_COMPLETED}
	 */
	public void setUpdateDeleteCompletedTasks(String updateDeleteCompletedTasks) {
		this.updateDeleteCompletedTasks = updateDeleteCompletedTasks;
	}

}
