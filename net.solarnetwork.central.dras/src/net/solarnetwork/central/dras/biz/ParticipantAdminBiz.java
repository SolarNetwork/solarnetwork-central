/* ==================================================================
 * ParticipantAdminBiz.java - Jun 12, 2011 2:58:16 PM
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

import net.solarnetwork.central.dras.domain.Capability;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.ParticipantGroup;
import net.solarnetwork.central.dras.support.MembershipCommand;

/**
 * Participant administration API.
 * 
 * @author matt
 * @version $Revision$
 */
public interface ParticipantAdminBiz {

	/**
	 * Create or update a Participant.
	 * 
	 * @param template the program template
	 * @return the persisted Participant instance
	 */
	Participant storeParticipant(Participant template);

	/**
	 * Create or update a ParticipantGroup.
	 * 
	 * @param template the program template
	 * @return the persisted ParticipantGroup instance
	 */
	ParticipantGroup storeParticipantGroup(ParticipantGroup template);
	
	/**
	 * Store a participant capability.
	 * 
	 * @param participantId the participant ID
	 * @param template the capability template
	 * @return the Capability
	 */
	Capability storeParticipantCapability(Long participantId, Capability template);

	/**
	 * Store a participant group capability.
	 * 
	 * @param participantGroupId the participant ID
	 * @param template the capability template
	 * @return the ParticipantGroupCapability
	 */
	Capability storeParticipantGroupCapability(Long participantGroupId, 
			Capability template);

	/**
	 * Manage the participants of a participant group.
	 * 
	 * @param membership the members to assign
	 * @param userIds the set of Participant IDs to assign as members to the group
	 * @return the EffectiveCollection
	 */
	EffectiveCollection<ParticipantGroup, Member> assignParticipantGroupMembers(
			MembershipCommand membership);
	
	/**
	 * Store constraints for a specific participant.
	 * 
	 * <p>These constraints serve as defaults for the given participant, 
	 * in all programs.</p>
	 * 
	 * @param participantId the participant ID to save the constraints with
	 * @param constraints the list of constraints
	 * @return the EffectiveCollection
	 */
	EffectiveCollection<Participant, Constraint> storeParticipantConstraints(
			Long participantId, List<Constraint> constraints);
	
	/**
	 * Store constraints for a participant within a specific program.
	 * 
	 * <p>These constraints serve as defaults for the given participant, in the
	 * given program only.</p>
	 * 
	 * @param programId the program ID to save the constraints with
	 * @param constraints the list of constraints
	 * @return the EffectiveCollection
	 */
	EffectiveCollection<Participant, Constraint> storeParticipantProgramConstraints(
			Long participantId, Long programId, List<Constraint> constraints);
	
}
