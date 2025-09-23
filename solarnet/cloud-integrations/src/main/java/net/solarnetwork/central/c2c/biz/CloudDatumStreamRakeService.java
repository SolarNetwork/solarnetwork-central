/* ==================================================================
 * CloudDatumStreamRakeService.java - 20/09/2025 6:29:00 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import java.time.Instant;
import java.util.concurrent.Future;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;

/**
 * Service to manage Cloud Datum Stream "rake" tasks.
 *
 * <p>
 * A "rake" task is one that validates a datum stream's past data against the
 * cloud values, to catch updated or otherwise changed data after a poll task
 * has already collected.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public interface CloudDatumStreamRakeService {

	/**
	 * Claim a queued rake task.
	 *
	 * <p>
	 * This method will "claim" a task that is currently in a "queued" state,
	 * changing the state to "claimed".
	 * </p>
	 *
	 * @return a claimed task, or {@literal null} if none could be claimed
	 */
	CloudDatumStreamRakeTaskEntity claimQueuedTask();

	/**
	 * Execute a rake task.
	 *
	 * @param task
	 *        the task to execute
	 * @return the task future
	 */
	Future<CloudDatumStreamRakeTaskEntity> executeTask(CloudDatumStreamRakeTaskEntity task);

	/**
	 * Reset rake tasks that are in the executing state but have an execute date
	 * older than a given date.
	 *
	 * <p>
	 * The intention of this method is to "reset" a task that was inadvertently
	 * left in an executing state, for example after a server restart.
	 * </p>
	 *
	 * @return the number of tasks reset
	 */
	int resetAbandondedExecutingTasks(Instant olderThan);

}
