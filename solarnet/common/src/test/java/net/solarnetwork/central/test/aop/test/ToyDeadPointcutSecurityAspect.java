/* ==================================================================
 * ToyDeadPointcutSecurityAspect.java - 22/09/2026 8:39:25 am
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

package net.solarnetwork.central.test.aop.test;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationSupport;

/**
 * Security aspect for {@link ToyBiz} with pointcuts that match nothing.
 *
 * @author matt
 * @version 1.0
 */
@Aspect
public class ToyDeadPointcutSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public ToyDeadPointcutSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	@Pointcut("execution(* net.solarnetwork.central.test.aop.test.ToyBiz.userThing(..)) && args(userId)")
	public void readUser(Long userId) {
	}

	/**
	 * A pointcut for a method that does not exist.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.test.aop.test.ToyBiz.missingThing(..)) && args(userId)")
	public void readMissing(Long userId) {
	}

	/**
	 * A pointcut whose arguments do not match the method arguments.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.test.aop.test.ToyBiz.publicThing(..)) && args(userId)")
	public void readWrongArgs(Long userId) {
	}

	@Before(value = "readUser(userId) || readMissing(userId) || readWrongArgs(userId)",
			argNames = "userId")
	public void readUserAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

}
