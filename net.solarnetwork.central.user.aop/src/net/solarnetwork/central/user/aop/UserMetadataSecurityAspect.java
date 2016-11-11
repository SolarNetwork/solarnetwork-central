/* ==================================================================
 * UserMetadataSecurityAspect.java - 11/11/2016 5:09:03 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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
 */

package net.solarnetwork.central.user.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.biz.UserMetadataBiz;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserMetadataFilter;
import net.solarnetwork.central.user.support.AuthorizationSupport;

/**
 * Security enforcing AOP aspect for {@link UserMetadataBiz}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.7
 */
@Aspect
public class UserMetadataSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao to use
	 */
	public UserMetadataSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.UserMetadata*.addUser*(..)) && args(userId,..)")
	public void addMetadata(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.UserMetadata*.storeUser*(..)) && args(userId,..)")
	public void storeMetadata(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.UserMetadata*.removeUser*(..)) && args(userId)")
	public void removeMetadata(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.UserMetadata*.findUser*(..)) && args(filter,..)")
	public void findMetadata(UserMetadataFilter filter) {
	}

	/**
	 * Check access to modifying user metadata.
	 * 
	 * @param userId
	 *        the ID of the user to verify
	 */
	@Before("addMetadata(userId) || storeMetadata(userId) || removeMetadata(userId)")
	public void updateMetadataCheck(Long userId) {
		requireUserWriteAccess(userId);
	}

	/**
	 * Check access to reading user metadata.
	 * 
	 * @param filter
	 *        the filter to verify
	 */
	@Before("findMetadata(filter)")
	public void readMetadataCheck(UserMetadataFilter filter) {
		Long[] userIds = (filter == null ? null : filter.getUserIds());
		if ( userIds == null ) {
			log.warn("Access DENIED to metadata without user ID filter");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		for ( Long userId : userIds ) {
			requireUserReadAccess(userId);
		}
	}

}
