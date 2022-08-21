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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.mutable.MutableBoolean;
import net.solarnetwork.central.oscp.dao.ExternalSystemSupportDao;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to post OSCP heartbeat messages to configured systems.
 * 
 * @author matt
 * @version 1.0
 */
public class HeartbeatJob extends JobSupport {

	private final ExternalSystemSupportDao systemSupportDao;

	/**
	 * Construct with properties.
	 * 
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public HeartbeatJob(ExternalSystemSupportDao systemSupportDao) {
		super();
		this.systemSupportDao = requireNonNullArgument(systemSupportDao, "systemSupportDao");
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
		int processedCount = 0;
		MutableBoolean invoked = new MutableBoolean(false);
		do {
			systemSupportDao.processExternalSystemWithExpiredHeartbeat((info) -> {

				return null;
			});
		} while ( invoked.booleanValue() && remainingIterataions.get() > 0 );
		return processedCount;
	}

}
