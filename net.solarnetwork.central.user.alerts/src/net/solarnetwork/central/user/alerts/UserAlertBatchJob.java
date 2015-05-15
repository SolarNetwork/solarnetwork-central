/* ==================================================================
 * UserAlertBatchJob.java - 15/05/2015 2:24:52 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.alerts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.central.user.domain.UserAlertType;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Job to look for {@link UserAlertType#NodeStaleData} needing of creating /
 * updating a {@link UserAlertSituation} for.
 * 
 * @author matt
 * @version 1.0
 */
public class UserAlertBatchJob extends JobSupport {

	/**
	 * The job property for the starting alert ID to use. If not specified,
	 * start with the smallest alert ID available.
	 */
	public static final String JOB_PROP_STARTING_ID = "AlertIdStart";

	private final UserAlertBatchProcessor processor;

	private static final ThreadLocal<Long> lastId = new ThreadLocal<Long>();

	/**
	 * Construct with properties.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param userAlertDao
	 *        the UserAlertDao to use
	 */
	public UserAlertBatchJob(EventAdmin eventAdmin, UserAlertBatchProcessor processor) {
		super(eventAdmin);
		this.processor = processor;
		setJobGroup("UserAlert");
		setMaximumWaitMs(1800000L);
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, ?> inputProperties = (Map<String, ?>) job
				.getProperty(SchedulerConstants.JOB_PROPERTIES);

		// reset thread-local
		lastId.remove();

		Long startingId = (Long) inputProperties.get(JOB_PROP_STARTING_ID);
		if ( processor != null ) {
			startingId = processor.processAlerts(startingId);
			lastId.set(startingId);
		}

		return true;
	}

	@Override
	protected Event handleJobCompleteEvent(Event jobEvent, boolean complete, Throwable thrown) {
		Event ack = super.handleJobCompleteEvent(jobEvent, complete, thrown);
		Long nextStartingId = lastId.get();
		if ( nextStartingId != null ) {
			lastId.remove();

			// add JOB_PROPERTIES Map with JOB_PROP_STARTING_NODE_ID to save with job
			Map<String, Object> props = new HashMap<String, Object>();
			for ( String key : ack.getPropertyNames() ) {
				props.put(key, ack.getProperty(key));
			}

			props.put(SchedulerConstants.JOB_PROPERTIES,
					Collections.singletonMap(JOB_PROP_STARTING_ID, nextStartingId));

			ack = new Event(ack.getTopic(), props);
		}
		return ack;
	}

	public UserAlertBatchProcessor getUserAlertDao() {
		return processor;
	}

}
