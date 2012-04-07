/* ==================================================================
 * JobSupport.java - Jun 30, 2011 5:09:59 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.scheduler;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Base helper class for a scheduled job.
 * 
 * @author matt
 * @version $Revision$
 */
public abstract class JobSupport extends EventHandlerSupport {
	
	private EventAdmin eventAdmin;
	
	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 */
	public JobSupport(EventAdmin eventAdmin) {
		super();
		this.eventAdmin = eventAdmin;
	}

	@Override
	protected final void handleEventInternal(Event event) throws Exception {
		Event ack = null;
		try {
			if ( handleJob(event) ) {
				ack = SchedulerUtils.createJobCompleteEvent(event);
			} else {
				ack = SchedulerUtils.createJobFailureEvent(event, null);
			}
		} catch ( Exception e ) {
			log.warn("Exception in job {}", event.getTopic(), e);
			ack = SchedulerUtils.createJobFailureEvent(event, e);
		} finally {
			if ( ack != null ) {
				eventAdmin.postEvent(ack);
			}
		}
	}

	/**
	 * Handle the job.
	 * 
	 * @param job the job details
	 * @return <em>true</em> if job completed successfully, <em>false</em> otherwise
	 * @throws Exception if any error occurs
	 */
	protected abstract boolean handleJob(Event job) throws Exception;
	
}
