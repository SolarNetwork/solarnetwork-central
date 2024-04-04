/* ==================================================================
 * UserInstructionInputSecurityAspect.java - 29/03/2024 9:37:06 am
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

package net.solarnetwork.central.user.inin.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.inin.biz.UserInstructionInputBiz;

/**
 * Security enforcing AOP aspect for {@link UserInstructionInputBiz}.
 *
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class UserInstructionInputSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserInstructionInputSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match methods like {@code *ForUser(userId, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.inin.biz.UserInstructionInputBiz.*ForUser(..)) && args(userId,..)")
	public void readForUserId(Long userId) {
	}

	/**
	 * Match methods like {@code *ForId(userKey, ...)}.
	 *
	 * @param userKey
	 *        the user key
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.inin.biz.UserInstructionInputBiz.*ForId(..)) && args(userKey,..)")
	public void readForUserKey(UserIdRelated userKey) {
	}

	/**
	 * Match methods like {@code *ForUser(userKey, ...)}.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.inin.biz.UserInstructionInputBiz.*Configuration(..)) && args(userKey,..)")
	public void updateConfigurationForUserKey(UserIdRelated userKey) {
	}

	/**
	 * Match preview transform.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.inin.biz.UserInstructionInputBiz.previewTransform(..)) && args(userKey,..)")
	public void previewTransform(UserIdRelated userKey) {
	}

	@Before("readForUserId(userId)")
	public void userIdReadAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before("readForUserKey(userKey)")
	public void userKeyReadAccessCheck(UserIdRelated userKey) {
		requireUserReadAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Before("previewTransform(userKey)")
	public void previewAccessCheck(UserIdRelated userKey) {
		requireUserReadAccess(userKey != null ? userKey.getUserId() : null);
	}

	@Before("updateConfigurationForUserKey(userKey)")
	public void userKeyWriteAccessCheck(UserIdRelated userKey) {
		requireUserWriteAccess(userKey != null ? userKey.getUserId() : null);
	}

}
