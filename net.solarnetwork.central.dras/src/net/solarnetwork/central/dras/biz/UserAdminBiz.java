/* ==================================================================
 * UserAdminBiz.java - Jun 8, 2011 4:05:34 PM
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

import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserGroup;
import net.solarnetwork.central.dras.support.MembershipCommand;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * User administrator API.
 * 
 * @author matt
 * @version $Revision$
 */
@PreAuthorize("hasRole('ROLE_USER_ADMIN')")
public interface UserAdminBiz {

	/**
	 * Create or update a User.
	 * 
	 * <p>If the {@code template} {@link User#getId()} value is present,
	 * that persisted User will be updated. Otherwise a new User will be
	 * persisted.</p>
	 * 
	 * @param template the user data
	 * @param roles the roles to assign to the user (may be <em>null</em>)
	 * @param programs the IDs of the programs to assign the user to (may be <em>null</em>)
	 * @return the persisted User
	 */
	User storeUser(User template, Set<String> roles, Set<Long> programs);
	
	
	/**
	 * Create or update a UserGroup.
	 * 
	 * <p>If the {@code template} {@link User#getId()} value is present,
	 * that persisted UserGroup will be updated. Otherwise a new UserGroup will be
	 * persisted.</p>
	 * 
	 * <p>This method does <b>not</b> modify an existing group's member set.</p>
	 * 
	 * @param template the user data
	 * @param roles the roles to assign to the user (may be <em>null</em>)
	 * @param programs the IDs of the programs to assign the user to (may be <em>null</em>)
	 * @return the persisted UserGroup
	 */
	UserGroup storeUserGroup(UserGroup template);
	
	/**
	 * Manage the users of a user group.
	 * 
	 * @param membership the members to assign
	 * @param userIds the set of User IDs to assign as members to the group
	 * @return the EffectiveCollection
	 */
	EffectiveCollection<UserGroup, Member> assignUserGroupMembers(
			MembershipCommand membership);
	
	/**
	 * Store constraints for a specific user.
	 * 
	 * <p>These constraints serve as defaults for all participants owned
	 * by the given user, in all programs.</p>
	 * 
	 * @param userId the user ID to save the constraints with
	 * @param constraints the list of constraints
	 * @return the EffectiveCollection
	 */
	EffectiveCollection<User, Constraint> storeUserConstraints(Long userId, 
			List<Constraint> constraints);
	
	/**
	 * Store constraints for a user within a specific program.
	 * 
	 * <p>These constraints serve as defaults for all participants owned
	 * by the given user, in the given program only.</p>
	 * 
	 * @param programId the program ID to save the constraints with
	 * @param constraints the list of constraints
	 * @return the EffectiveCollection
	 */
	EffectiveCollection<User, Constraint> storeUserProgramConstraints(Long userId, 
			Long programId, List<Constraint> constraints);
	
}
