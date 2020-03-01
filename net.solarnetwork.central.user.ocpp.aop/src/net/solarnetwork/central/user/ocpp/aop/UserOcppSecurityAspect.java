/* ==================================================================
 * UserOcppSecurityAspect.java - 29/02/2020 8:19:43 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.ocpp.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.dao.UserRelatedEntity;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;
import net.solarnetwork.central.user.support.AuthorizationSupport;

/**
 * Security enforcing AOP aspect for {@link UserOcppBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
public class UserOcppSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao
	 */
	public UserOcppSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
	}

	/**
	 * Match methods like {@code *ForUser(userId)}.
	 * 
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.ocpp.biz.UserOcppBiz.*ForUser(..)) && args(userId,..)")
	public void readForUser(Long userId) {
	}

	/**
	 * Match methods like {@code save*(entity)}.
	 * 
	 * @param entity
	 *        the entity
	 */
	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.ocpp.biz.UserOcppBiz.save*(..)) && args(entity,..)")
	public void saveUserRelatedEntity(UserRelatedEntity<?> entity) {
	}

	@Before("readForUser(userId)")
	public void userReadAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before("saveUserRelatedEntity(entity)")
	public void userReadAccessCheck(UserRelatedEntity<?> entity) {
		if ( entity == null || entity.getUserId() == null ) {
			throw new IllegalArgumentException("The entity's userId parameter must not be null.");
		}
		requireUserWriteAccess(entity.getUserId());
	}

}
