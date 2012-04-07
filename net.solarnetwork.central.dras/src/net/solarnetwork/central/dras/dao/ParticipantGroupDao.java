/* ==================================================================
 * ParticipantGroupDao.java - Jun 6, 2011 4:09:47 PM
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

import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.dras.domain.Fee;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.ParticipantGroup;

import org.joda.time.DateTime;

/**
 * DAO API for ParticipantGroup entities.
 * 
 * @author matt
 * @version $Revision$
 */
public interface ParticipantGroupDao extends GenericDao<ParticipantGroup, Long>,
FilterableDao<Match, Long, ParticipantGroupFilter> {

	/**
	 * Get the set of program Participant members.
	 * 
	 * @param participantGroupId the group ID to get the participants of
	 * @param effectiveDate the effective date, or <em>null</em> for
	 * the current date
	 * @return set of Participant members, never <em>null</em>
	 */
	Set<Member> getParticipantMembers(Long participantGroupId, DateTime effectiveDate);

	/**
	 * Assign all Participant members of a participant group.
	 * 
	 * @param participantGroupId the group ID to assign members to
	 * @param participantIdSet the set of participant IDs that define the group participants
	 * @param effectiveId the effective ID
	 */
	void assignParticipantMembers(Long participantGroupId, Set<Long> participantIdSet, 
			Long effectiveId);

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
