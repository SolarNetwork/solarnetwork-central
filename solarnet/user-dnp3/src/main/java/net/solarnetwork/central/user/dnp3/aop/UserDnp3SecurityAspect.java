/* ==================================================================
 * UserDnp3SecurityAspect.java - 7/08/2023 10:03:12 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dnp3.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.dnp3.biz.UserDnp3Biz;

/**
 * Security enforcing AOP aspect for {@link UserDnp3Biz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class UserDnp3SecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserDnp3SecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match methods like {@code *ForUser(userId, ...)}.
	 * 
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.dnp3.biz.UserDnp3Biz.*ForUser*(..)) && args(userId,..)")
	public void readForUser(Long userId) {
	}

	/**
	 * Match methods like {@code create*(userId, ...)}.
	 * 
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.dnp3.biz.UserDnp3Biz.save*(..)) && args(userId,..)")
	public void saveUserRelatedEntity(Long userId) {
	}

	@Before("readForUser(userId)")
	public void userReadAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before("saveUserRelatedEntity(userId)")
	public void userWriteAccessCheck(Long userId) {
		requireUserWriteAccess(userId);
	}

}
