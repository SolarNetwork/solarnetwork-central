/* ==================================================================
 * FilterSecurityAspect.java - Jun 23, 2011 6:40:11 PM
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

import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dras.dao.UserAwareFilter;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.support.EventCriteria;
import net.solarnetwork.central.dras.support.ParticipantCriteria;
import net.solarnetwork.central.dras.support.ProgramCriteria;
import net.solarnetwork.central.dras.support.UserCriteria;
import net.solarnetwork.central.dras.support.UserGroupCriteria;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Security enforcer for ProgramBiz and ProgramAdminBiz.
 * 
 * @author matt
 * @version $Revision$
 */
@Aspect
public class FilterSecurityAspect extends SecurityAspectSupport {

	/** The program admin role. */
	public static final String PROGRAM_ADMIN_ROLE = "ROLE_PROGRAM_ADMIN";

	/** The event admin role. */
	public static final String EVENT_ADMIN_ROLE = "ROLE_OPERATOR";

	/** The user admin role. */
	public static final String USER_ADMIN_ROLE = "ROLE_USER";

	/**
	 * Constructor.
	 * 
	 * @param userDao the UserDao
	 */
	public FilterSecurityAspect(UserDao userDao) {
		super(userDao);
	}
	
	/**
	 * Match Biz methods that accept a UserAwareFilter argument.
	 * 
	 * @param criteria the search criteria
	 */
	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.dras.biz.*.*(..)) && args(criteria,..)")
	public void searchMethod(ObjectCriteria<? extends UserAwareFilter> criteria) {}

	/**
	 * Limit programs to current user unless user has {@link #PROGRAM_ADMIN_ROLE} role.
	 * 
	 * @param criteria the criteria to filter
	 */
	@Before("searchMethod(criteria)")
	public void enforceProgramSearchFilter(ObjectCriteria<? extends UserAwareFilter> criteria) {
		final String[] adminRole;
		if ( criteria instanceof EventCriteria ) {
			adminRole = new String[] {EVENT_ADMIN_ROLE};
		} else if ( criteria instanceof ParticipantCriteria ) {
			adminRole = new String[] {EVENT_ADMIN_ROLE, PROGRAM_ADMIN_ROLE};
		} else if ( criteria instanceof ProgramCriteria ) {
			adminRole = new String[] {PROGRAM_ADMIN_ROLE};
		} else if ( criteria instanceof UserCriteria || criteria instanceof UserGroupCriteria ) {
			adminRole = new String[] {USER_ADMIN_ROLE};
		} else {
			throw new IllegalArgumentException("Criteria type [" 
					+criteria.getClass().getSimpleName() +" not supported.");
		}
		enforceFilterUser(criteria.getSimpleFilter(), adminRole);
	}
	
}
