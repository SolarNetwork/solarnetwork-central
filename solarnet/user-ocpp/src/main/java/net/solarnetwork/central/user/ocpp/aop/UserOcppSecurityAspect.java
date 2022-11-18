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
import org.springframework.stereotype.Component;
import net.solarnetwork.central.common.dao.UserCriteria;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.dao.UserNodeRelatedEntity;
import net.solarnetwork.central.user.dao.UserRelatedEntity;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;

/**
 * Security enforcing AOP aspect for {@link UserOcppBiz}.
 * 
 * @author matt
 * @version 2.1
 */
@Aspect
@Component
public class UserOcppSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserOcppSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match methods like {@code *ForUser(userId)}.
	 * 
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.ocpp.biz.UserOcppBiz.*ForUser(..)) && args(userId,..)")
	public void readForUser(Long userId) {
	}

	/**
	 * Match methods like {@code save*(entity)}.
	 * 
	 * @param entity
	 *        the entity
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.ocpp.biz.UserOcppBiz.save*(..)) && args(entity,..)")
	public void saveUserRelatedEntity(UserRelatedEntity<?> entity) {
	}

	/**
	 * Match methods like {@code save*(entity)}.
	 * 
	 * @param entity
	 *        the entity
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.ocpp.biz.UserOcppBiz.save*(..)) && args(entity,..)")
	public void saveUserNodeRelatedEntity(UserNodeRelatedEntity<?> entity) {
	}

	/**
	 * Match methods like {@code deleteUser*(entity)}.
	 * 
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.ocpp.biz.UserOcppBiz.deleteUser*(..)) && args(userId,..)")
	public void deleteUserRelatedEntityById(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.ocpp.biz.UserOcppBiz.findFiltered*(..)) && args(filter,..)")
	public void filter(UserCriteria filter) {
	}

	@Before("readForUser(userId)")
	public void userReadAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before("saveUserRelatedEntity(entity)")
	public void userWriteAccessCheck(UserRelatedEntity<?> entity) {
		if ( entity == null || entity.getUserId() == null ) {
			throw new IllegalArgumentException("The entity's userId parameter must not be null.");
		}
		requireUserWriteAccess(entity.getUserId());
	}

	@Before("saveUserNodeRelatedEntity(entity)")
	public void userNodeWriteAccessCheck(UserNodeRelatedEntity<?> entity) {
		if ( entity == null || entity.getNodeId() == null ) {
			throw new IllegalArgumentException("The entity's nodeId parameter must not be null.");
		}
		requireNodeWriteAccess(entity.getNodeId());
	}

	@Before("deleteUserRelatedEntityById(userId)")
	public void userIdWriteAccessCheck(Long userId) {
		if ( userId == null ) {
			throw new IllegalArgumentException("The userId parameter must not be null.");
		}
		requireUserWriteAccess(userId);
	}

	/**
	 * Verify user read access on a query filter.
	 * 
	 * @param filter
	 */
	@Before("filter(filter)")
	public void filterAccessCheck(UserCriteria filter) {
		if ( !filter.hasUserCriteria() ) {
			throw new IllegalArgumentException("The userId filter criteria must not be null.");
		}
		for ( Long userId : filter.getUserIds() ) {
			requireUserReadAccess(userId);
		}
	}

}
