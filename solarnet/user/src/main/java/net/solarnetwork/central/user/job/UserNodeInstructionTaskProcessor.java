/* ==================================================================
 * UserNodeInstructionTaskProcessor.java - 18/11/2025 11:05:55 am
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

package net.solarnetwork.central.user.job;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.biz.UserNodeInstructionService;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.util.ObjectUtils;

/**
 * Job to process ready-to-execute node instruction tasks.
 * 
 * @author matt
 * @version 1.0
 */
public class UserNodeInstructionTaskProcessor extends JobSupport {

	private final UserNodeInstructionService service;

	/**
	 * Constructor.
	 * 
	 * @param service
	 *        the service
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserNodeInstructionTaskProcessor(UserNodeInstructionService service) {
		super();
		this.service = ObjectUtils.requireNonNullArgument(service, "service");
		setGroupId("User");
		setId("UserNodeInstructionTaskProcessor");
	}

	@Override
	public void run() {
		executeParallelJob("node instruction processor");
	}

	@Override
	protected int executeJobTask(AtomicInteger remainingIterations) throws Exception {
		int count = 0;
		while ( remainingIterations.getAndDecrement() > 0 ) {
			UserNodeInstructionTaskEntity task = service.claimQueuedTask();
			if ( task == null ) {
				break;
			}
			try {
				@SuppressWarnings("unused")
				Future<?> unused = service.executeTask(task);
			} catch ( RejectedExecutionException e ) {
				log.debug("Task [{}] rejected, aborting any more claims", task.getId());
				remainingIterations.set(0);
				break;
			}
			count++;
		}
		return count;
	}

}
