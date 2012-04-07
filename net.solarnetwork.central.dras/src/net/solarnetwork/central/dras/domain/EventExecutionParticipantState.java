/* ==================================================================
 * EventExecutionParticipantState.java - Jun 27, 2011 6:47:16 PM
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

package net.solarnetwork.central.dras.domain;

import java.io.Serializable;
import java.util.List;

import org.joda.time.DateTime;

/**
 * Complete state information for a specific participant in a specific event.
 * 
 * @author matt
 * @version $Revision$
 */
public class EventExecutionParticipantState implements Serializable {

	private static final long serialVersionUID = -2355947478513569321L;

	private Long participantId;
	
	// "simple" info
	private DateTime date;
	private EventStatus status;
	private EventOperationMode mode;
	private List<EventOperationModeTarget> targets;
	
	// "smart" info
	private Event event;
	private EventTargets eventTargets;
	
	public Long getParticipantId() {
		return participantId;
	}
	public void setParticipantId(Long participantId) {
		this.participantId = participantId;
	}
	public DateTime getDate() {
		return date;
	}
	public void setDate(DateTime date) {
		this.date = date;
	}
	public EventStatus getStatus() {
		return status;
	}
	public void setStatus(EventStatus status) {
		this.status = status;
	}
	public EventOperationMode getMode() {
		return mode;
	}
	public void setMode(EventOperationMode mode) {
		this.mode = mode;
	}
	public List<EventOperationModeTarget> getTargets() {
		return targets;
	}
	public void setTargets(List<EventOperationModeTarget> targets) {
		this.targets = targets;
	}
	public Event getEvent() {
		return event;
	}
	public void setEvent(Event event) {
		this.event = event;
	}
	public EventTargets getEventTargets() {
		return eventTargets;
	}
	public void setEventTargets(EventTargets eventTargets) {
		this.eventTargets = eventTargets;
	}
	
}
