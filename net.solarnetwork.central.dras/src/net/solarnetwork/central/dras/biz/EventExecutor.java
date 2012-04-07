/* ==================================================================
 * EventExecutor.java - Jun 27, 2011 4:57:23 PM
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

package net.solarnetwork.central.dras.biz;

import java.util.List;

import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.EventExecutionTargets;
import net.solarnetwork.central.dras.domain.EventOperationModeTarget;
import net.solarnetwork.central.dras.domain.Member;

/**
 * API for service that can execute an Event.
 * 
 * <p>This API is designed such that the implemented service can be deployed
 * physically independent of the main DRAS environment. The DRAS dispatches
 * events to this service, a receipt is returned, and the DRAS must persist
 * this receipt to receive status updates from the EventExecutor. When the
 * event info changes in any way, the DRAS will post the updated event details
 * and get a new receipt.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public interface EventExecutor {

	/**
	 * Complete information for executing an event.
	 */
	interface EventExecutionRequest {
		
		/**
		 * Get the event domain instance.
		 * 
		 * @return the event ID
		 */
		Event getEvent();
		
		/**
		 * Get the list of EventOperationModeTarget values for the event.
		 * 
		 * @return list of modes
		 */
		List<EventOperationModeTarget> getModes();
		
		/**
		 * Get the list of EventExecutionTargets for the event.
		 * 
		 * @return list of targets
		 */
		List<EventExecutionTargets> getTargets();
		
		/**
		 * Get the fully resolved list of participants for the event.
		 * 
		 * @return list of participant members
		 */
		List<Member> getParticipants();
	}

	/**
	 * Information returned by the EventExecutor service each time an event
	 * execution is requested.
	 */
	interface EventExecutionReceipt {
		
		/**
		 * A unique ID for this event execution.
		 * 
		 * <p>Event executions are considered unique each time the 
		 * {@link EventExecutor#executeEvent(EventExecutionRequest)}
		 * is called. If the same event is submitted multiple times,
		 * the execution's version number will be incremented and a
		 * new {@code executionId} generated for that request.</p>
		 * 
		 * @return execution ID
		 */
		String getExecutionId();
		
	}
	
	/**
	 * Request an event be executed.
	 * 
	 * @param request the request
	 * @return the confirmation receipt
	 */
	EventExecutionReceipt executeEvent(EventExecutionRequest request);
}
