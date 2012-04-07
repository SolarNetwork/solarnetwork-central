/* ==================================================================
 * UserBiz.java - Jun 8, 2011 4:05:06 PM
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
import net.solarnetwork.central.dras.dao.UserFilter;
import net.solarnetwork.central.dras.dao.UserGroupFilter;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserGroup;
import net.solarnetwork.central.dras.domain.UserRole;
import net.solarnetwork.central.dras.support.UserInformation;

/**
 * User observer API.
 * 
 * @author matt
 * @version $Revision$
 */
public interface UserBiz {

	/** A role that all users are granted. */
	String DEFAULT_ROLE = "AUTHENTICATED_USER";
	
	/**
	 * Get a list of all available user roles.
	 * 
	 * @return the set of roles
	 */
	Set<UserRole> getAllUserRoles();

	/**
	 * Get a single User by its ID.
	 * 
	 * @param userId the ID of the user to get
	 * @return the User, or <em>null</em> if not found
	 */
	User getUser(Long userId);
	
	/**
	 * Get a list of roles assigned to a user.
	 * 
	 * @return the set of roles
	 */
	Set<UserRole> getUserRoles(Long userId);

	/**
	 * Get a {@link UserInformation} object for a given user.
	 * 
	 * @return the set of roles
	 */
	UserInformation getUserInfo(Long userId);

	/**
	 * Find a set of User objects, optionally filtered by a search criteria and optionally
	 * sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are {@link User} property names.
	 * If no {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code username}, in an ascending manner.</p>
	 * 
	 * @param criteria an optional search criteria
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return set of users, or an empty set if none found
	 */
	List<Match> findUsers(ObjectCriteria<UserFilter> criteria, 
			List<SortDescriptor> sortDescriptors);
	
	/**
	 * Get a single UserGroup by its ID.
	 * 
	 * @param groupId the ID of the group to get
	 * @return the UserGroup, or <em>null</em> if not found
	 */
	UserGroup getUserGroup(Long groupId);
	
	/**
	 * Find a set of UserGroup objects, optionally filtered by a search criteria and optionally
	 * sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are {@link UserGroup} property names.
	 * If no {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code name}, in an ascending manner.</p>
	 * 
	 * @param criteria an optional search criteria
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return set of groups, or an empty set if none found
	 */
	List<Match> findUserGroups(ObjectCriteria<UserGroupFilter> criteria, 
			List<SortDescriptor> sortDescriptors);
	
	/**
	 * Get the complete set of user constraints.
	 * 
	 * @param userId the user ID to get the constraints for
	 * @return the constraints, never <em>null</em>
	 */
	Set<Constraint> getUserConstraints(Long userId);
	
	/**
	 * Get the complete set of user constraints for a specific program.
	 * 
	 * @param userId the user ID to get the constraints for
	 * @return the constraints, never <em>null</em>
	 */
	Set<Constraint> getUserProgramConstraints(Long userId, Long programId);

}
