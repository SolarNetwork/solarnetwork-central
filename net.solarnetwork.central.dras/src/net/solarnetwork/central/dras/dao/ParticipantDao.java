/* ==================================================================
 * ParticipantDao.java - Jun 6, 2011 12:56:39 PM
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

package net.solarnetwork.central.dras.dao;

import java.util.Set;

import org.joda.time.DateTime;

import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Fee;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Participant;

/**
 * DAO API for Participant entities.
 * 
 * @author matt
 * @version $Revision$
 */
public interface ParticipantDao extends GenericDao<Participant, Long>,
FilterableDao<Match, Long, ParticipantFilter> {

	/**
	 * Get the set of participant constraints.
	 * 
	 * @param participantId the participant ID to get the constraints for
	 * @param effectiveDate the effective date, or <em>null</em> for
	 * the current date
	 * @return set of Constraint members, never <em>null</em>
	 */
	Set<Constraint> getConstraints(Long participantId, DateTime effectiveDate);
	
	/**
	 * Assign all participant constraints.
	 * 
	 * @param participantId the participant ID to assign constraints to
	 * @param constraintIdSet the set of constraint IDs to assign
	 * @param effectiveId the effective ID
	 */
	void assignConstraints(Long participantId, Set<Long> constraintIdSet, Long effectiveId);
	
	/**
	 * Get the set of participant constraints for a specific program.
	 * 
	 * @param participantId the participant ID to get the constraints for
	 * @param programId the program ID to get the constraints for
	 * @param effectiveDate the effective date, or <em>null</em> for
	 * the current date
	 * @return set of Constraint members, never <em>null</em>
	 */
	Set<Constraint> getParticipantProgramConstraints(Long participantId, Long programId, 
			DateTime effectiveDate);
	
	/**
	 * Assign all participant constraints for a specific program.
	 * 
	 * @param participantId the participant ID to assign constraints to
	 * @param programId the program ID to assign constraints to
	 * @param constraintIdSet the set of constraint IDs to assign
	 * @param effectiveId the effective ID
	 */
	void assignParticipantProgramConstraints(Long participantId, Long programId, 
			Set<Long> constraintIdSet, Long effectiveId);

	/**
	 * Get the Fee schedule for a given user.
	 * 
	 * @param userId the user ID
	 * @param effectiveDate the effective date
	 * @return the Fee, or <em>null</em> if none available
	 */
	Fee getFee(Long userId, DateTime effectiveDate);
	
	/**
	 * Set the Fee schedule for a given user.
	 * 
	 * @param userId the user ID
	 * @param feeId the fee ID
	 * @param effectiveId the effective ID
	 */
	void setFee(Long userId, Long feeId, Long effectiveId);
	
}
