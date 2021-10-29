/* ==================================================================
 * AccountTaskDao.java - 21/07/2020 6:21:35 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.dao;

import java.util.UUID;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for {@link AccountTask} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface AccountTaskDao extends GenericDao<AccountTask, UUID> {

	/**
	 * Claim an account task, so that it can be processed.
	 * 
	 * <p>
	 * It is expected that a transaction exists before calling this method, and
	 * that {@link #taskCompleted(AccountTask)} will be invoked within the same
	 * transaction.
	 * </p>
	 * 
	 * @return the claimed task, or {@literal null} if no task is available
	 */
	AccountTask claimAccountTask();

	/**
	 * Mark a task as completed.
	 * 
	 * @param task
	 *        the task to mark as completed
	 * @see #claimAccountTask()
	 */
	void taskCompleted(AccountTask task);

}
