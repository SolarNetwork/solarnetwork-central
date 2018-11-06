/* ==================================================================
 * DatumImportJobInfoDao.java - 7/11/2018 11:24:47 AM
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

package net.solarnetwork.central.datum.imp.dao;

import java.util.UUID;
import org.joda.time.DateTime;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.user.dao.UserRelatedGenericDao;

/**
 * DAO API for {@link DatumImportJobInfo} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumImportJobInfoDao extends UserRelatedGenericDao<DatumImportJobInfo, UUID> {

	/**
	 * Claim a queued task.
	 * 
	 * This method will "claim" a task that is currently in the
	 * {@link net.solarnetwork.central.datum.export.domain.DatumImportState#Queued}
	 * state, changing the state to
	 * {@link net.solarnetwork.central.datum.export.domain.DatumImportState#Claimed}.
	 * 
	 * @return a claimed task, or {@literal null} if none could be claimed
	 */
	DatumImportJobInfo claimQueuedTask();

	/**
	 * Purge tasks that have reached a
	 * {@link net.solarnetwork.central.datum.export.domain.DatumImportState#Completed}
	 * and are older than a given date.
	 * 
	 * @param olderThanDate
	 *        the maximum date for which to purge completed tasks
	 * @return the number of tasks deleted
	 */
	long purgeCompletedTasks(DateTime olderThanDate);

}
