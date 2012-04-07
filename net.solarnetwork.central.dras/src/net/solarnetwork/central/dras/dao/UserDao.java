/* ==================================================================
 * UserDao.java - Jun 3, 2011 8:45:35 PM
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
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserRole;

/**
 * DAO API for User entities.
 * 
 * @author matt
 * @version $Revision$
 */
public interface UserDao extends GenericDao<User, Long>, FilterableDao<Match, Long, UserFilter> {
	
	/**
	 * Get a complete set of available roles.
	 * 
	 * @return the user roles
	 */
	Set<UserRole> getAllUserRoles();
	
	/**
	 * Get the complete set of roles for a given user.
	 * 
	 * @param userId the ID of the user to get the roles for
	 * @return the found roles, never <em>null</em>
	 */
	Set<UserRole> getUserRoles(Long userId);
	
	/**
	 * Assign the complete set of roles for a given user.
	 * 
	 * @param userId the user ID
	 * @param roles the user roles
	 */
	void assignUserRoles(Long userId, Set<String> roles);
	
	/**
	 * Get the set of user constraints.
	 * 
	 * @param userId the user ID to get the constraints for
	 * @param effectiveDate the effective date, or <em>null</em> for
	 * the current date
	 * @return set of Constraint members, never <em>null</em>
	 */
	Set<Constraint> getConstraints(Long userId, DateTime effectiveDate);
	
	/**
	 * Assign all user constraints.
	 * 
	 * @param userId the user ID to assign constraints to
	 * @param constraintIdSet the set of constraint IDs to assign
	 * @param effectiveId the effective ID
	 */
	void assignConstraints(Long userId, Set<Long> constraintIdSet, Long effectiveId);
	
	/**
	 * Get the set of user constraints for a specific program.
	 * 
	 * @param userId the user ID to get the constraints for
	 * @param programId the program ID to get the constraints for
	 * @param effectiveDate the effective date, or <em>null</em> for
	 * the current date
	 * @return set of Constraint members, never <em>null</em>
	 */
	Set<Constraint> getUserProgramConstraints(Long userId, Long programId, DateTime effectiveDate);
	
	/**
	 * Assign all user constraints for a specific program.
	 * 
	 * @param userId the user ID to assign constraints to
	 * @param programId the program ID to assign constraints to
	 * @param constraintIdSet the set of constraint IDs to assign
	 * @param effectiveId the effective ID
	 */
	void assignUserProgramConstraints(Long userId, Long programId, Set<Long> constraintIdSet, 
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
