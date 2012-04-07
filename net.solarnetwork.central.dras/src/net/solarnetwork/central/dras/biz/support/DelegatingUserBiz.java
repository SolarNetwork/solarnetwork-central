/* ==================================================================
 * DelegatingUserBiz.java - Jun 24, 2011 4:38:52 PM
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

package net.solarnetwork.central.dras.biz.support;

import java.util.List;
import java.util.Set;

import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.dras.biz.UserBiz;
import net.solarnetwork.central.dras.dao.UserFilter;
import net.solarnetwork.central.dras.dao.UserGroupFilter;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserGroup;
import net.solarnetwork.central.dras.domain.UserRole;
import net.solarnetwork.central.dras.support.UserInformation;

/**
 * Delegating {@link UserBiz}, to support AOP with OSGi services.
 * 
 * @author matt
 * @version $Revision$
 */
public class DelegatingUserBiz implements UserBiz {

	private UserBiz delegate;

	/**
	 * Construct with delegate.
	 * 
	 * @param delegate the delegate
	 */
	public DelegatingUserBiz(UserBiz delegate) {
		this.delegate = delegate;
	}

	@Override
	public Set<UserRole> getAllUserRoles() {
		return delegate.getAllUserRoles();
	}

	@Override
	public User getUser(Long userId) {
		return delegate.getUser(userId);
	}

	@Override
	public Set<UserRole> getUserRoles(Long userId) {
		return delegate.getUserRoles(userId);
	}

	@Override
	public UserInformation getUserInfo(Long userId) {
		return delegate.getUserInfo(userId);
	}

	@Override
	public List<Match> findUsers(ObjectCriteria<UserFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		return delegate.findUsers(criteria, sortDescriptors);
	}

	@Override
	public UserGroup getUserGroup(Long groupId) {
		return delegate.getUserGroup(groupId);
	}

	@Override
	public List<Match> findUserGroups(ObjectCriteria<UserGroupFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		return delegate.findUserGroups(criteria, sortDescriptors);
	}

	@Override
	public Set<Constraint> getUserConstraints(Long userId) {
		return delegate.getUserConstraints(userId);
	}

	@Override
	public Set<Constraint> getUserProgramConstraints(Long userId, Long programId) {
		return delegate.getUserProgramConstraints(userId, programId);
	}

}
