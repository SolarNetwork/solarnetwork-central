/* ==================================================================
 * UserDatumExportTaskInfoDao.java - 18/04/2018 9:33:07 AM
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

package net.solarnetwork.central.user.export.dao;

import java.util.UUID;
import org.joda.time.DateTime;
import net.solarnetwork.central.user.dao.UserRelatedGenericDao;
import net.solarnetwork.central.user.export.domain.UserAdhocDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskPK;

/**
 * DAO API for {@link UserDatumExportTaskInfo} entities for ad hoc export tasks.
 * 
 * <p>
 * This DAO uses the {@link UserDatumExportTaskInfo} entity, but does not use
 * the populate the {@code userDatumExportConfigurationId} property as there is
 * only the ad hoc configuration associated with each task directly.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public interface UserAdhocDatumExportTaskInfoDao
		extends UserRelatedGenericDao<UserAdhocDatumExportTaskInfo, UserDatumExportTaskPK> {

	/**
	 * Get an export task by its task ID.
	 * 
	 * @param taskId
	 *        the task ID to get
	 * @return the export task, or {@literal null} if not available
	 */
	UserAdhocDatumExportTaskInfo getForTaskId(UUID taskId);

	/**
	 * Purge tasks that have reached a
	 * {@link net.solarnetwork.central.datum.export.domain.DatumExportState#Completed}
	 * and are older than a given date.
	 * 
	 * @param olderThanDate
	 *        the maximum date for which to purge completed tasks
	 * @return the number of tasks deleted
	 */
	long purgeCompletedTasks(DateTime olderThanDate);

	/**
	 * Add an ad hoc datum export task.
	 * 
	 * @param info
	 *        the ad-hoc task to submit
	 * @return the resulting datum export task UUID
	 */
	UUID addAdHocDatumExport(UserAdhocDatumExportTaskInfo info);

}
