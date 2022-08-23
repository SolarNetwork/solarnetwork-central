/* ==================================================================
 * OfflineAlertJob.java - 24/08/2022 10:13:29 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.oscp.sim.cp.dao.CapacityProviderDao;

/**
 * Job to raise an alert when a system's "offline at" date has passed.
 * 
 * The "alert" raised by this job is a {@code WARN} log entry.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class OfflineAlertJob extends JobSupport {

	private static final Logger log = LoggerFactory.getLogger(HeartbeatJob.class);

	private final CapacityProviderDao dao;

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        the DAO
	 */
	public OfflineAlertJob(CapacityProviderDao dao) {
		super();
		this.dao = requireNonNullArgument(dao, "dao");
		setGroupId("OSCP");
		setId("OfflineAlert");
		setMaximumWaitMs(1800000L);
	}

	@Override
	public void run() {
		log.info("Processing expired heartbeats...");
		int updated = dao.processExpiredOfflines((conf) -> {
			log.warn("Discovered offline system {}, offline since {}", conf, conf.getOfflineDate());
		});
		if ( log.isDebugEnabled() ) {
			log.debug("Raised {} offline alerts", updated);
		} else {
			log.info("Raised {} offline alerts", updated);

		}
	}

}
