/* ==================================================================
 * EventParticipants.java - Apr 30, 2011 10:18:16 AM
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
import java.util.Set;

import net.solarnetwork.central.domain.Identity;

/**
 * A set of participants within an Event.
 * 
 * <p>An Event can be assigned individual participants or participant groups,
 * or both. The overall set of participants for the event is the union of 
 * all individual participants and the participants that are members of the
 * assigned participant groups.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class EventParticipants implements Cloneable, Serializable {

	private static final long serialVersionUID = -3824602118268175968L;

	private Long eventId;
	private Set<Identity<Long>> participants;
	private Set<Identity<Long>> groups;
	
	/**
	 * Default constructor.
	 */
	public EventParticipants() {
		super();
	}
	
	/**
	 * Construct with values and participants.
	 * 
	 * @param id the ID
	 * @param eventId the event ID
	 * @param participants the participants
	 */
	public EventParticipants(Long id, Long eventId, Set<Identity<Long>> participants) {
		super();
		setEventId(eventId);
		setParticipants(participants);
	}
	
	/**
	 * Construct with values and participants.
	 * 
	 * @param id the ID
	 * @param eventId the event ID
	 * @param participants the participants
	 * @param groups the groups
	 */
	public EventParticipants(Long id, Long eventId, Set<Identity<Long>> participants, 
			Set<Identity<Long>> groups) {
		this(id, eventId, participants);
		setGroups(groups);
	}

	public Long getEventId() {
		return eventId;
	}
	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}
	public Set<Identity<Long>> getParticipants() {
		return participants;
	}
	public void setParticipants(Set<Identity<Long>> participants) {
		this.participants = participants;
	}
	public Set<Identity<Long>> getGroups() {
		return groups;
	}
	public void setGroups(Set<Identity<Long>> groups) {
		this.groups = groups;
	}
	
}
