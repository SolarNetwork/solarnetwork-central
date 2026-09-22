/* ==================================================================
 * ToyTargetInvokingSecurityAspect.java - 22/09/2026 2:19:25 pm
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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationSupport;

/**
 * Security aspect for {@link ToyBiz} that invokes the target before checking
 * access.
 *
 * @author matt
 * @version 1.0
 */
@Aspect
public class ToyTargetInvokingSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public ToyTargetInvokingSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Check access only after invoking the target method.
	 *
	 * @param pjp
	 *        the join point
	 * @param userId
	 *        the user ID
	 * @return the result
	 * @throws Throwable
	 *         if any error occurs
	 */
	@Around(value = "execution(* net.solarnetwork.central.test.aop.test.ToyBiz.userThing(..)) && args(userId)",
			argNames = "pjp,userId")
	public Object readUserAccessCheck(ProceedingJoinPoint pjp, Long userId) throws Throwable {
		final Object result = pjp.proceed();
		requireUserReadAccess(userId);
		return result;
	}

	/**
	 * Look up data on the target before checking access.
	 *
	 * @param biz
	 *        the target
	 * @param nodeId
	 *        the node ID
	 */
	@Before(value = "execution(* net.solarnetwork.central.test.aop.test.ToyBiz.nodeThing(..)) && args(nodeId) && target(biz)",
			argNames = "biz,nodeId")
	public void readNodeAccessCheck(ToyBiz biz, Long nodeId) {
		biz.publicThing();
		requireNodeReadAccess(nodeId);
	}

}
