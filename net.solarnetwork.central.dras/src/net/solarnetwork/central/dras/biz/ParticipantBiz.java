/* ==================================================================
 * ParticipantBiz.java - Jun 12, 2011 2:58:05 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
 * 
 * This participant is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This participant is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this participant; if not, write to the Free Software 
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
import net.solarnetwork.central.dras.dao.ParticipantFilter;
import net.solarnetwork.central.dras.dao.ParticipantGroupFilter;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.ParticipantGroup;
import net.solarnetwork.central.dras.support.CapableParticipant;
import net.solarnetwork.central.dras.support.CapableParticipantGroup;

/**
 * Participant observer API.
 * 
 * @author matt
 * @version $Revision$
 */
public interface ParticipantBiz {

	/**
	 * Find participants, optionally sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are {@link Participant} property names.
	 * If no {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code id}, in an ascending manner.</p>
	 * 
	 * @param criteria an optional search criteria
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return set of participants, or an empty set if none found
	 */
	List<Match> findParticipants(ObjectCriteria<ParticipantFilter> criteria, 
			List<SortDescriptor> sortDescriptors);
	
	/**
	 * Get a single Participant by its ID.
	 * 
	 * @param participantId the ID of the participant to get
	 * @return the Participant
	 */
	Participant getParticipant(Long participantId);
	
	/**
	 * Get a single CapableParticipant by its ID.
	 * 
	 * @param participantId the participantId
	 * @return the CapableParticipant
	 */
	CapableParticipant getCapableParticipant(Long participantId);

	/**
	 * Find participant groups, optionally sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are {@link ParticipantGroup}
	 * property names. If no {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code name}, in an ascending manner.</p>
	 * 
	 * @param criteria an optional search criteria
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return set of participant groups, or an empty set if none found
	 */
	List<Match> findParticipantGroups(ObjectCriteria<ParticipantGroupFilter> criteria, 
			List<SortDescriptor> sortDescriptors);
	
	/**
	 * Get a single ParticipantGroup by its ID.
	 * 
	 * @param participantGroupId the ID of the participant to get
	 * @return the ParticipantGroup
	 */
	ParticipantGroup getParticipantGroup(Long participantGroupId);

	/**
	 * Get a single CapableParticipantGroup by its ID.
	 * 
	 * @param participantGroupId the participantId
	 * @return the CapableParticipantGroup
	 */
	CapableParticipantGroup getCapableParticipantGroup(Long participantGroupId);

	/**
	 * Get the complete set of participant constraints.
	 * 
	 * @param participantId the participant ID to get the constraints for
	 * @return the constraints, never <em>null</em>
	 */
	Set<Constraint> getParticipantConstraints(Long participantId);
	
	/**
	 * Get the complete set of participant constraints for a specific program.
	 * 
	 * @param participantId the user ID to get the constraints for
	 * @return the constraints, never <em>null</em>
	 */
	Set<Constraint> getParticipantProgramConstraints(
			Long participantId, Long programId);

}
