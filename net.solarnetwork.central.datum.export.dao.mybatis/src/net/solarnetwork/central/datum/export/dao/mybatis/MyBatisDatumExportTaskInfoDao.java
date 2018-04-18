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

	/** The default query name used for {@link #claimQueuedTask(Long)}. */
	public static final String QUERY_FOR_CLAIMING_TASK = "get-DatumExportTaskInfo-for-claim";

	private String queryForClaimQueuedTask;

	/**
	 * Constructor.
	 */
	public MyBatisDatumExportTaskInfoDao() {
		super(DatumExportTaskInfo.class, UUID.class);
		setQueryForClaimQueuedTask(QUERY_FOR_CLAIMING_TASK);
	}

	@Override
	public DatumExportTaskInfo claimQueuedTask() {
		return selectFirst(queryForClaimQueuedTask, null);
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

}
