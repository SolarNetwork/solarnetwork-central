/* ==================================================================
 * EventHandlerSupport.java - Jun 30, 2011 3:42:43 PM
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
 */

package net.solarnetwork.central.scheduler;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of {@link EventHandler}.
 * 
 * <p>
 * This class traps all Exceptions and logs an error message. This helps prevent
 * the event handler from becoming black-listed by the EventAdmin service, as
 * the Apache Felix implementation does.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public abstract class EventHandlerSupport implements EventHandler {

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public final void handleEvent(Event event) {
		try {
			handleEventInternal(event);
		} catch ( Exception e ) {
			log.error("Exception in OSGi Event handler", e);
		}
	}

	/**
	 * Execute the event handler.
	 * 
	 * @param event
	 *        the event
	 * @throws Exception
	 *         if any error occurs
	 */
	protected abstract void handleEventInternal(Event event) throws Exception;

}
