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

import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.central.user.domain.UserAlertType;
import org.joda.time.DateTime;
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

	/**
	 * The job property for the valid date to use, as milliseconds since the
	 * epoch. If not specified, use the current date.
	 */
	public static final String JOB_PROP_VALID_DATE = "AlertValidDate";

	private final UserAlertBatchProcessor processor;

	private static final ThreadLocal<Map<String, Object>> props = new ThreadLocal<Map<String, Object>>() {

		@Override
		protected Map<String, Object> initialValue() {
			return new HashMap<String, Object>(2);
		}

	};

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
		props.get().clear();

		Long startingId = null;
		Long validDateMs = null;
		if ( inputProperties != null ) {
			startingId = (Long) inputProperties.get(JOB_PROP_STARTING_ID);
			validDateMs = (Long) inputProperties.get(JOB_PROP_VALID_DATE);
		}
		DateTime validDate = (validDateMs == null ? new DateTime() : new DateTime(validDateMs));
		if ( processor != null ) {
			startingId = processor.processAlerts(startingId, validDate);
			if ( startingId != null ) {
				props.get().put(JOB_PROP_STARTING_ID, startingId);
				props.get().put(JOB_PROP_VALID_DATE, validDate.getMillis());
			}
		}

		return true;
	}

	@Override
	protected Event handleJobCompleteEvent(Event jobEvent, boolean complete, Throwable thrown) {
		Event ack = super.handleJobCompleteEvent(jobEvent, complete, thrown);

		// add JOB_PROPERTIES Map with JOB_PROP_STARTING_NODE_ID to save with job
		Map<String, Object> jobProps = new HashMap<String, Object>();
		for ( String key : ack.getPropertyNames() ) {
			jobProps.put(key, ack.getProperty(key));
		}

		jobProps.put(SchedulerConstants.JOB_PROPERTIES, new HashMap<String, Object>(props.get()));

		ack = new Event(ack.getTopic(), jobProps);

		return ack;
	}

	public UserAlertBatchProcessor getUserAlertDao() {
		return processor;
	}

}
