/* ==================================================================
 * EventTargetsCommand.java - May 11, 2011 8:48:54 PM
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

package net.solarnetwork.central.dras.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.list.LazyList;

import net.solarnetwork.central.dras.domain.EventTarget;

/**
 * A command object for event target actions.
 * 
 * <p>The targets property uses a {@link LazyList} to help with Spring binding.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class EventTargetsCommand {
	
	private Long eventId;
	
	@SuppressWarnings("unchecked")
	private List<EventTarget> targets = 
		LazyList.decorate(new ArrayList<EventTarget>(), 
		FactoryUtils.instantiateFactory(EventTarget.class));

	/**
	 * @return the eventId
	 */
	public Long getEventId() {
		return eventId;
	}

	/**
	 * @param eventId the eventId to set
	 */
	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	/**
	 * @return the targets
	 */
	public List<EventTarget> getTargets() {
		return targets;
	}

	/**
	 * @param targets the targets to set
	 */
	public void setTargets(List<EventTarget> targets) {
		this.targets = targets;
	}
	
}
