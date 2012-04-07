/* ==================================================================
 * DRASOperationBiz.java - Apr 29, 2011 3:31:23 PM
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
import java.util.Set;

import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.dras.dao.EventFilter;
import net.solarnetwork.central.dras.dao.ParticipantFilter;
import net.solarnetwork.central.dras.domain.CapabilityInformation;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.EventParticipants;
import net.solarnetwork.central.dras.domain.EventTargets;
import net.solarnetwork.central.dras.domain.Program;

/**
 * API for observing the operation of the DRAS.
 * 
 * <p>All methods assume any necessary security filtering and authorization
 * are handled in the implementation, and access to the active user of the
 * API can be determined by the implementation.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public interface DRASObserverBiz {
	
	/**
	 * Get all available participant groups, optionally sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are {@link CapabilityInformation}
	 * property names. If no {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code name}, in an ascending manner.</p>
	 * 
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return list of groups, or an empty list if none found
	 */
	List<CapabilityInformation> getAllParticipantGroups(List<SortDescriptor> sortDescriptor);
	
	/**
	 * Get the members of a participant group, optionally filtered or sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are 
	 * {@link CapabilityInformation} property names. If no 
	 * {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code id}, in an ascending manner. The returned set will respect the
	 * sort order by way of iteration over the set's items.</p>
	 * 
	 * @param groupId the ID of the group to get members of
	 * @param criteria an optional search criteria
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return set of participant group member identities, or an empty set if none found
	 */
	Set<CapabilityInformation> getParticipantGroupMembers(Long groupId,
			ObjectCriteria<ParticipantFilter> criteria,
			List<SortDescriptor> sortDescriptors);

	/**
	 * Get all available programs, optionally sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are {@link Program} property names.
	 * If no {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code name}, in an ascending manner.</p>
	 * 
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return list of programs, or an empty list if none found
	 */
	List<Program> getAllPrograms(List<SortDescriptor> sortDescriptors);
	
	/**
	 * Get a single Program by its ID.
	 * 
	 * @param programId the ID of the program to get
	 * @return the Program
	 */
	Program getProgram(Long programId);
	
	/**
	 * Get the set of participants associated with a program, optionally
	 * filtered by a search criteria or sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are 
	 * {@link CapabilityInformation} property names. If no 
	 * {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code id}, in an ascending manner. The returned set will respect the
	 * sort order by way of iteration over the set's items.</p>
	 * 
	 * @param program the program to get participants for
	 * @param criteria an optional search criteria
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return set of participant identities, or an empty set if none found
	 */
	Set<CapabilityInformation> getProgramParticipants(Program program,
			ObjectCriteria<ParticipantFilter> criteria,
			List<SortDescriptor> sortDescriptors);
	
	/**
	 * Get a single Event by its ID.
	 * 
	 * @param eventId the ID of the event to get
	 * @return the Event
	 */
	Event getEvent(Long eventId);
	
	/**
	 * Get a list of Events, optionally filtered by a search criteria and optionally
	 * sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are {@link Event} property names.
	 * If no {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code created}, in a descending manner.</p>
	 * 
	 * @param program the program to get events for
	 * @param criteria an optional search criteria
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return list of programs, or an empty list if none found
	 */
	List<Event> getEvents(Program program, 
			ObjectCriteria<EventFilter> criteria, 
			List<SortDescriptor> sortDescriptors);
	
	/**
	 * Get the current {@link EventParticipants} entity for a given event.
	 * 
	 * @param event the event to get participant groups for
	 * @return current EventParticipants entity
	 */
	EventParticipants getCurrentEventParticipants(Event event);
	
	/**
	 * Get the current set of participants participating in a given event, optionally
	 * filtered by a search criteria or sorted in some way.
	 * 
	 * <p>The results will contain the union of all participants and participants
	 * of groups assigned to the given Event.</p>
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are 
	 * {@link net.solarnetwork.central.domain.NodeInformation} property names. If no 
	 * {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code id}, in an ascending manner. The returned set will respect the
	 * sort order by way of iteration over the set's items.</p>
	 * 
	 * @param event the event to get participants for
	 * @param criteria an optional search criteria
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return set of participant identities, or an empty set if none found
	 */
	Set<CapabilityInformation> getEventParticipants(Event event,
			ObjectCriteria<ParticipantFilter> criteria,
			List<SortDescriptor> sortDescriptors);
	
	/**
	 * Get the current set of targets for a given event.
	 * 
	 * @param event the event to get the targets for
	 * @return the event targets entity
	 */
	EventTargets getEventTargets(Event event);
}
