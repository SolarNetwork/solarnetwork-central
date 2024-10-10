/* ==================================================================
 * CloudDatumStreamPollService.java - 10/10/2024 5:40:31 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz;

import java.util.concurrent.Future;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;

/**
 * Service to manage cloud datum stream poll tasks.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudDatumStreamPollService {

	/**
	 * Claim a queued poll task.
	 *
	 * <p>
	 * This method will "claim" a task that is currently in a "queued" state,
	 * changing the state to "claimed".
	 * </p>
	 *
	 * @return a claimed task, or {@literal null} if none could be claimed
	 */
	CloudDatumStreamPollTaskEntity claimQueuedTask();

	/**
	 * Execute a poll task.
	 *
	 * @param task
	 *        the task to execute
	 * @return the task future
	 */
	Future<CloudDatumStreamPollTaskEntity> executeTask(CloudDatumStreamPollTaskEntity task);
}
