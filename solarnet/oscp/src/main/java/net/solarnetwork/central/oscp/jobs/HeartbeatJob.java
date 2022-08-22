/* ==================================================================
 * HeartbeatJob.java - 21/08/2022 3:58:02 pm
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

package net.solarnetwork.central.oscp.jobs;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HEARTBEAT_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.http.HttpMethod;
import net.solarnetwork.central.oscp.dao.ExternalSystemSupportDao;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to post OSCP heartbeat messages to configured systems.
 * 
 * @author matt
 * @version 1.0
 */
public class HeartbeatJob extends JobSupport {

	private final ExternalSystemSupportDao systemSupportDao;
	private final ExternalSystemClient client;

	/**
	 * Construct with properties.
	 * 
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public HeartbeatJob(ExternalSystemSupportDao systemSupportDao, ExternalSystemClient client) {
		super();
		this.systemSupportDao = requireNonNullArgument(systemSupportDao, "systemSupportDao");
		this.client = requireNonNullArgument(client, "client");
		setGroupId("OSCP");
		setId("Heartbeat");
		setMaximumWaitMs(1800000L);
	}

	@Override
	public void run() {
		executeParallelJob("Heartbeat");
	}

	@Override
	protected int executeJobTask(AtomicInteger remainingIterataions) throws Exception {
		int totalProcessedCount = 0;
		Set<String> supportedOscpVersions = singleton(V20);
		MutableInt processedCount = new MutableInt();
		do {
			processedCount.setValue(0);
			systemSupportDao.processExternalSystemWithExpiredHeartbeat((ctx) -> {
				processedCount.increment();
				ctx.verifySystemOscpVersion(supportedOscpVersions);
				client.systemExchange(ctx, HttpMethod.POST, HEARTBEAT_URL_PATH, null);
				return Instant.now();
			});
			totalProcessedCount += processedCount.intValue();
		} while ( processedCount.intValue() > 0 && remainingIterataions.get() > 0 );
		return totalProcessedCount;
	}

}
