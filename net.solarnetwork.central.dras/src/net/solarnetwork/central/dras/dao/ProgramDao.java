/* ==================================================================
 * ProgramDao.java - Apr 29, 2011 2:28:01 PM
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
 * $Id: ProgramDAO.java 1282 2011-04-29 08:33:55Z msqr $
 * ==================================================================
 */

package net.solarnetwork.central.dras.dao;

import java.util.Set;

import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.EventRule;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Program;

import org.joda.time.DateTime;

/**
 * DAO API for Program entities.
 * 
 * @author matt
 * @version $Revision: 1282 $
 */
public interface ProgramDao extends GenericDao<Program, Long>, 
FilterableDao<Match, Long, ProgramFilter> {
	
	/**
	 * Resolve all user members for a program, including those users indirectly
	 * assigned via user groups, participants, etc.
	 * 
	 * @param programId the Program ID
	 * @param effectiveDate the effective date, or <em>null</em> for
	 * the current date
	 * @return set of User members, never <em>null</em>
	 */
	Set<Member> resolveUserMembers(Long programId, DateTime effectiveDate);
	
	/**
	 * Get the set of program User members.
	 * 
	 * @param programId the program ID to get the members of
	 * @param effectiveDate the effective date, or <em>null</em> for
	 * the current date
	 * @return set of User members, never <em>null</em>
	 */
	Set<Member> getUserMembers(Long programId, DateTime effectiveDate);
	
	/**
	 * Assign all User members of a program.
	 * 
	 * @param programId the program ID to assign members to
	 * @param userIdSet the set of user IDs that define the program membership
	 * @param effectiveId the effective ID
	 */
	void assignUserMembers(Long programId, Set<Long> userIdSet, Long effectiveId);
	
	/**
	 * Get the set of program Participant members.
	 * 
	 * @param programId the program ID to get the participants of
	 * @param effectiveDate the effective date, or <em>null</em> for
	 * the current date
	 * @return set of Participant members, never <em>null</em>
	 */
	Set<Member> getParticipantMembers(Long programId, DateTime effectiveDate);

	/**
	 * Assign all Participant members of a program.
	 * 
	 * @param programId the program ID to assign members to
	 * @param participantIdSet the set of participant IDs that define the program participants
	 * @param effectiveId the effective ID
	 */
	void assignParticipantMembers(Long programId, Set<Long> participantIdSet, 
			Long effectiveId);
	
	/**
	 * Get the set of program rules.
	 * 
	 * @param programId the program to get the rules for
	 * @param effectiveDate the effective date, or <em>null</em> for
	 * the current date
	 * @return set of EventRule members, never <em>null</em>
	 */
	Set<EventRule> getProgramEventRules(Long programId, DateTime effectiveDate);
	
	/**
	 * Assign all EventRule values of a program.
	 * 
	 * @param programId the program ID to assign rules to
	 * @param eventRuleIdSet the set of rule IDs that define the program rules
	 * @param effectiveId the effective ID
	 */
	void assignProgramEventRules(Long programId, Set<Long> eventRuleIdSet, 
			Long effectiveId);
	
	/**
	 * Get the set of program constraints.
	 * 
	 * @param programId the program ID to get the constraints for
	 * @param effectiveDate the effective date, or <em>null</em> for
	 * the current date
	 * @return set of Constraint members, never <em>null</em>
	 */
	Set<Constraint> getConstraints(Long programId, DateTime effectiveDate);
	
	/**
	 * Assign all program constraints.
	 * 
	 * @param programId the program ID to assign constraints to
	 * @param constraintIdSet the set of constraint IDs to assign
	 * @param effectiveId the effective ID
	 */
	void assignConstraints(Long programId, Set<Long> constraintIdSet, Long effectiveId);
	
}
