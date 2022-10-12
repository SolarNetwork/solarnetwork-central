/* ==================================================================
 * HeartbeatJob.java - 24/08/2022 6:47:13 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.oscp.sim.cp.jobs;

import static net.solarnetwork.central.oscp.web.OscpWebUtils.tokenAuthorizer;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.oscp.web.OscpWebUtils;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.oscp.sim.cp.dao.CapacityProviderDao;
import net.solarnetwork.oscp.sim.cp.web.SystemHttpTask;
import oscp.v20.Heartbeat;

/**
 * Post heartbeat messages to configured systems.
 * 
 * @author matt
 * @version 1.0
 */
public class HeartbeatJob extends JobSupport {

	private static final Logger log = LoggerFactory.getLogger(HeartbeatJob.class);

	private final CapacityProviderDao dao;
	private final AsyncTaskExecutor taskExecutor;
	private final RestOperations restOps;

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        the DAO
	 * @param taskExecutor
	 *        the task executor
	 * @param restOps
	 *        the REST operations
	 */
	public HeartbeatJob(CapacityProviderDao dao, AsyncTaskExecutor taskExecutor,
			RestOperations restOps) {
		super();
		this.dao = requireNonNullArgument(dao, "dao");
		this.taskExecutor = requireNonNullArgument(taskExecutor, "taskExecutor");
		this.restOps = requireNonNullArgument(restOps, "restOps");
		setGroupId("OSCP");
		setId("Heartbeat");
		setMaximumWaitMs(1800000L);
	}

	@Override
	public void run() {
		log.info("Processing expired heartbeats...");
		int updated = dao.processExpiredHeartbeats((conf) -> {
			if ( !V20.equals(conf.getOscpVersion()) ) {
				log.error("OSCP version [{}] is not supported (must be {})."
						.formatted(conf.getOscpVersion(), V20));
				return null;
			}

			URI uri = URI.create(conf.getBaseUrl() + OscpWebUtils.UrlPaths_20.HEARTBEAT_URL_PATH);
			Heartbeat req = new Heartbeat(
					Instant.now().plusSeconds(conf.getSettings().heartbeatSeconds()));
			log.info("Sending heartbeat for {} to [{}]: {}", conf.getId(), uri, req);
			Future<?> f = taskExecutor.submit(new SystemHttpTask<>("Heartbeat", restOps, null,
					HttpMethod.POST, uri, req, tokenAuthorizer(conf.getOutToken())));
			try {
				f.get(10, TimeUnit.SECONDS);
			} catch ( TimeoutException e ) {
				throw new RuntimeException("Timeout waiting for heartbeat completion.");
			} catch ( ExecutionException e ) {
				throw new RuntimeException("Heartbeat task threw exception.", e);
			} catch ( InterruptedException e ) {
				throw new RuntimeException("Interrupted waiting for heartbeat completion.", e);
			}
			return Instant.now();
		});
		if ( log.isDebugEnabled() ) {
			log.debug("Sent {} heartbeats", updated);
		} else {
			log.info("Sent {} heartbeats", updated);

		}
	}

}
