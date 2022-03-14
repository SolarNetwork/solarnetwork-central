/* ==================================================================
 * AccountTaskHandler.java - 21/07/2020 11:04:18 AM
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

package net.solarnetwork.central.user.billing.snf.jobs;

import net.solarnetwork.central.user.billing.snf.domain.AccountTask;

/**
 * API for handling tasks.
 * 
 * @author matt
 * @version 1.0
 */
public interface AccountTaskHandler {

	/**
	 * Handle the given task.
	 * 
	 * @param task
	 *        the task to handle
	 * @return {@literal true} if the task was handled
	 */
	boolean handleTask(AccountTask task);

}
