/* ==================================================================
 * UserMetadataSecurityAspect.java - 5/04/2024 10:58:35 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.biz.UserMetadataBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;

/**
 * Security AOP support for {@link UserMetadataBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class UserMetadataSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param nodeOwnershipDao
	 *        the ownership DAO to use
	 */
	public UserMetadataSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	@Pointcut("execution(* net.solarnetwork.central.biz.UserMetadataBiz.addGeneralNode*(..)) && args(userId,..)")
	public void modifyMetadata(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.biz.UserMetadataBiz.findUserMetadata(..)) && args(filter,..)")
	public void listMetadata(UserMetadataFilter filter) {
	}

	/**
	 * Check access to modify metadata.
	 * 
	 * @param userId
	 *        the user ID
	 */
	@Before("modifyMetadata(userId)")
	public void modifyMetadataCheck(Long userId) {
		requireUserWriteAccess(userId);
	}

	/**
	 * Check access to modify metadata.
	 * 
	 * @param userId
	 *        the user ID
	 */
	@Before("listMetadata(filter)")
	public void listMetadataCheck(UserMetadataFilter filter) {
		if ( filter == null || !filter.hasUserCriteria() ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		Long[] ids = filter.getUserIds();
		if ( ids != null ) {
			for ( Long userId : ids ) {
				requireUserReadAccess(userId);
			}
		}
	}

}
