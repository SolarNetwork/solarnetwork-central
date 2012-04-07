/* ==================================================================
 * SecurityAspectSupport.java - Jun 23, 2011 6:49:25 PM
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

package net.solarnetwork.central.dras.aop;

import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.dao.UserAwareFilter;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.support.SimpleUserFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Support class for security-related aspects.
 * 
 * @author matt
 * @version $Revision$
 */
public class SecurityAspectSupport {

	private UserDao userDao;
	
	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	public SecurityAspectSupport(UserDao userDao) {
		super();
		this.userDao = userDao;
	}

	/**
	 * Get the ID of the acting user.
	 * 
	 * @return ID, or <em>null</em> if not available
	 */
	protected final Long getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if ( auth == null ) {
			log.info("No Authentication available, cannot tell current user ID");
			return null;
		}
		String currentUserName = auth.getName();
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setUniqueId(currentUserName);
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		if ( results.getReturnedResultCount() < 1 ) {
			log.debug("User not found for username {}", currentUserName);
			return null;
		}
		return results.getResults().iterator().next().getId();
	}
	
	/**
	 * Test if the current user has a specific role.
	 * 
	 * <p>If more than one role is provided, any role is allowed to match,
	 * i.e. the set of roles is treated as an "or" style match.</p>
	 * 
	 * @param role the roles to test for
	 * @return <em>true</em> if the current user has the role
	 */
	protected final boolean currentUserHasRole(final String... role) {
		// see if we return ALL programs, or just those for the current user
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		for ( GrantedAuthority ga : auth.getAuthorities() ) {
			for ( String r : role ) {
				if ( r.equalsIgnoreCase(ga.getAuthority()) ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Force the user ID in a {@link UserAwareFilter} to the current user ID
	 * unless the current user  has the specified role.
	 * 
	 * <p>If more than one role is provided, any role is allowed to match,
	 * i.e. the set of roles is treated as an "or" style match.</p>
	 * 
	 * @param filter the filter to modify
	 * @param adminRole the admin role
	 */
	protected void enforceFilterUser(final UserAwareFilter filter, final String... adminRole) {
		// see if we return ALL programs, or just those for the current user
		boolean admin = currentUserHasRole(adminRole);
		if ( !admin ) {
			// limit to just active user
			Long currUser = getCurrentUserId();
			if ( !currUser.equals(filter.getUserId()) ) {
				log.debug("Forcing userId to {} in filter {} because user does not have role {}", 
						new Object[] {currUser, filter, adminRole});
				filter.setUserId(currUser);
			}
		}
	}
	
	public UserDao getUserDao() {
		return userDao;
	}
	
}
