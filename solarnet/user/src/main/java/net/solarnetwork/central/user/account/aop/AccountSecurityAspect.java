/* ==================================================================
 * AccountSecurityAspect.java - 27 Sept 2026 8:44:42 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.account.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;

/**
 * Security enforcing AOP aspect for {@link UserAccountBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class AccountSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO to use
	 */
	public AccountSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	@Pointcut("execution(* net.solarnetwork.central.user.account.biz.UserAccountBiz.create*(..)) && args(userId, ..)")
	public void forUserWriteAccess(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.account.biz.UserAccountBiz.*ForUser(..)) && args(userId, ..)")
	public void forUserReadAccess(Long userId) {
	}

	@Before(value = """
			forUserReadAccess(userId)
			""", argNames = "userId")
	public void checkForUserReadAccess(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = """
			forUserWriteAccess(userId)
			""", argNames = "userId")
	public void checkForUserWriteAccess(Long userId) {
		requireUserWriteAccess(userId);
	}

}
