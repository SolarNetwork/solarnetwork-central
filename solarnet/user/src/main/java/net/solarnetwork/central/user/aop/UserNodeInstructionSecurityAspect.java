/* ==================================================================
 * UserNodeInstructionSecurityAspect.java - 16/11/2025 3:06:34 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.biz.UserNodeInstructionBiz;

/**
 * Security enforcing AOP aspect for {@link UserNodeInstructionBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class UserNodeInstructionSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserNodeInstructionSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match read methods given a user ID.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserNodeInstructionBiz.*ForUser(..)) && args(userId,..)")
	public void readForUserId(Long userId) {
	}

	/**
	 * Match update methods given an entity.
	 *
	 * @param userKey
	 *        the user key
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserNodeInstructionBiz.update*(..)) && args(userKey,..)")
	public void updateEntityForUserKey(UserIdRelated userKey) {
	}

	/**
	 * Match save methods given an entity.
	 *
	 * @param userKey
	 *        the user key
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserNodeInstructionBiz.save*(..)) && args(userKey,..)")
	public void saveEntityForUserKey(UserIdRelated userKey) {
	}

	/**
	 * Match delete methods given an entity.
	 *
	 * @param userKey
	 *        the user key
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserNodeInstructionBiz.delete*(..)) && args(userKey,..)")
	public void deleteEntityForUserKey(UserIdRelated userKey) {
	}

	@Before(value = "readForUserId(userId)", argNames = "userId")
	public void userIdReadAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = "saveEntityForUserKey(userKey)", argNames = "userKey")
	public void saveEntityAccessCheck(UserIdRelated userKey) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Before(value = "updateEntityForUserKey(userKey)", argNames = "userKey")
	public void updateEntityAccessCheck(UserIdRelated userKey) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Before(value = "deleteEntityForUserKey(userKey)", argNames = "userKey")
	public void deleteEntityAccessCheck(UserIdRelated userKey) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
	}

}
