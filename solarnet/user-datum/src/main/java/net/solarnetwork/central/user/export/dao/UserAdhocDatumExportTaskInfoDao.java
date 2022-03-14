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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.user.dao.UserRelatedGenericDao;
import net.solarnetwork.central.user.export.domain.UserAdhocDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;

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
		extends UserRelatedGenericDao<UserAdhocDatumExportTaskInfo, UUID> {

	/**
	 * Purge tasks that have reached a
	 * {@link net.solarnetwork.central.datum.export.domain.DatumExportState#Completed}
	 * and are older than a given date.
	 * 
	 * @param olderThanDate
	 *        the maximum date for which to purge completed tasks
	 * @return the number of tasks deleted
	 */
	long purgeCompletedTasks(Instant olderThanDate);

	/**
	 * Find all available ad hoc export tasks for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get tasks for
	 * @param states
	 *        if provided, a specific set of states to filter the results by
	 *        (only tasks in one of the given states are returned)
	 * @param success
	 *        if provided, filter the results to only include jobs with a
	 *        matching success flag
	 * @return the matching tasks, never {@literal null}
	 */
	List<UserAdhocDatumExportTaskInfo> findTasksForUser(Long userId, Set<DatumExportState> states,
			Boolean success);

}
