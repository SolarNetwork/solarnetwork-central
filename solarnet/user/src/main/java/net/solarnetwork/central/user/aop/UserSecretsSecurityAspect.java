/* ==================================================================
 * UserSecretSecurityAspect.java - 23/03/2025 11:34:06 am
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
import net.solarnetwork.central.user.biz.UserSecretBiz;

/**
 * Security enforcing AOP aspect for {@link UserSecretBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class UserSecretsSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserSecretsSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match get-style read methods given a user ID.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserSecretBiz.list*(..)) && args(userId,..)")
	public void listForUserId(Long userId) {
	}

	/**
	 * Match delete methods given a user ID.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserSecretBiz.delete*(..)) && args(userId,..)")
	public void deleteForUserId(Long userId) {
	}

	/**
	 * Match save methods given a user ID.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserSecretBiz.save*(..)) && args(userId,..)")
	public void saveForUserId(Long userId) {
	}

	/**
	 * Match decrypt method given a user ID related entity.
	 *
	 * @param entity
	 *        the user related entity
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserSecretBiz.decryptSecretValue(..)) && args(entity)")
	public void decryptSecret(UserIdRelated entity) {
	}

	@Before(value = "listForUserId(userId)")
	public void userIdReadAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = """
			   deleteForUserId(userId)
			|| saveForUserId(userId)
			""")
	public void userIdWriteAccessCheck(Long userId) {
		requireUserWriteAccess(userId);
	}

	@Before(value = "decryptSecret(entity)")
	public void entityReadAccessCheck(UserIdRelated entity) {
		requireUserReadAccess(entity != null ? entity.getUserId() : null);
	}

}
