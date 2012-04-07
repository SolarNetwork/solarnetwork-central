/* ==================================================================
 * DRASOperatorBiz.java - Apr 29, 2011 8:15:52 PM
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

import java.util.Set;
import java.util.SortedSet;

import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.EventParticipants;
import net.solarnetwork.central.dras.domain.EventTarget;
import net.solarnetwork.central.dras.domain.EventTargets;
import net.solarnetwork.central.dras.domain.Program;
import net.solarnetwork.central.dras.domain.ProgramParticipants;

import org.joda.time.ReadableDateTime;

/**
 * API for operating the DRAS, including initiating and maintaining 
 * demand-response events.
 * 
 * <p>All methods assume any necessary security filtering and authorization
 * are handled in the implementation, and access to the active user of the
 * API can be determined by the implementation.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public interface DRASOperatorBiz {
	
	/*
	 * Create and persist a new group based on location data.
	 * 
	 * @param location the location information to assign to the new group
	 * @return the new, persisted group instance
	 *
	NodeGroupInformation createGroup(Location location);*/

	/**
	 * Create and persist a new Program.
	 * 
	 * @param name the program name
	 * @return the new, persisted Event instance
	 */
	Program createProgram(String name);
	
	/**
	 * Assign the participants of a program, by creating a new {@link ProgramParticipants}
	 * entity.
	 * 
	 * <p>Any change to the participants of a program requires a new {@link ProgramParticipants}
	 * entity to be created, so that a history of participant changes is maintained.</p>
	 * 
	 * @param program the program to assign participants to
	 * @param participants the set of participants to assign
	 * @return the new, persisted ProgramParticipants instance
	 */
	ProgramParticipants assignProgramParticipants(Program program, Set<Identity<Long>> participants);
	
	/**
	 * Create and persist a new Event for a given Program.
	 * 
	 * @param program the program to create the event for
	 * @param name the event name
	 * @param eventDate the date for the event to occur
	 * @param endDate the date for the event to end
	 * @return the new, persisted Event instance
	 */
	Event createEvent(Program program, String name, ReadableDateTime eventDate, ReadableDateTime endDate);
	
	/**
	 * Assign the participants of an event, by creating a new {@link EventParticipants}
	 * entity.
	 * 
	 * <p>Any change to the participants of an event requires a new {@link EventParticipants}
	 * entity to be created, so that a history of participant changes is maintained.</p>
	 * 
	 * @param event the event to assign participants to
	 * @param participants the set of participants to assign
	 * @return the new, persisted EventParticipants instance
	 */
	EventParticipants assignEventParticipants(Event event, Set<Identity<Long>> participants);
	
	/**
	 * Assign the participant groups of an event, by creating a new {@link EventParticipants}
	 * entity.
	 * 
	 * <p>Any change to the participant groups of an event requires a new {@link EventParticipants}
	 * entity to be created, so that a history of participant changes is maintained.</p>
	 * 
	 * @param event the event to assign participant groups to
	 * @param groups the set of participant groups to assign
	 * @return the new, persisted EventParticipants instance
	 */
	EventParticipants assignEventParticipantGroups(Event event, Set<Identity<Long>> groups);
	
	/**
	 * Assign the event targets for an event, by creating a new {@link EventTargets} entity.
	 * 
	 * <p>Any change to the targets of an event requires a new {@link EventTargets}
	 * entity to be created, so that a history of target changes is maintained.</p>
	 * 
	 * @param event
	 * @param targets
	 * @return
	 */
	EventTargets assignEventTargets(Event event, SortedSet<EventTarget> targets);
	
}
