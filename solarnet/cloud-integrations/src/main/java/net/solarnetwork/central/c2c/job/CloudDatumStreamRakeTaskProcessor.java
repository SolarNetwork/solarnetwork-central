/* ==================================================================
 * CloudDatumStreamRakeTaskProcessor.java - 22/09/2025 11:50:33 am
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

package net.solarnetwork.central.c2c.job;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamRakeService;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.service.RemoteServiceException;

/**
 * Job to process ready-to-execute cloud datum stream rake tasks.
 *
 * @author matt
 * @version 1.0
 */
public class CloudDatumStreamRakeTaskProcessor extends JobSupport {

	private final CloudDatumStreamRakeService service;

	/**
	 * Constructor.
	 *
	 * @param service
	 *        the service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CloudDatumStreamRakeTaskProcessor(CloudDatumStreamRakeService service) {
		super();
		this.service = requireNonNullArgument(service, "service");
		setGroupId("CloudIntegrations");
		setId("DatumStreamRakeTaskProcessor");
	}

	@Override
	public void run() {
		executeParallelJob("cloud datum stream rake");
	}

	@Override
	protected int executeJobTask(AtomicInteger remainingIterations) throws Exception {
		final long maxWaitMs = getMaximumWaitMs();
		int count = 0;
		while ( remainingIterations.getAndDecrement() > 0 ) {
			CloudDatumStreamRakeTaskEntity task = service.claimQueuedTask();
			if ( task == null ) {
				break;
			}
			count++;
			Future<?> f = service.executeTask(task);
			try {
				f.get(maxWaitMs, TimeUnit.MILLISECONDS);
			} catch ( TimeoutException e ) {
				log.info("Timeout waiting for task [{}] to complete within {}ms; moving on",
						task.getId(), maxWaitMs);
			} catch ( ExecutionException e ) {
				Throwable t = e.getCause();
				if ( !(t instanceof RemoteServiceException) ) {
					log.warn("Exception processing task [{}]; moving on", task.getId(), t);
				}
			}
		}
		return count;
	}

}
