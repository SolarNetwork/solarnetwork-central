/* ==================================================================
 * UserEventSecurityAspect.java - 23/09/2026 9:12:04 am
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

package net.solarnetwork.central.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.biz.UserEventBiz;
import net.solarnetwork.central.common.dao.UserEventFilter;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;

/**
 * Security AOP support for {@link UserEventBiz}.
 *
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class UserEventSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the ownership DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserEventSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match methods like {@code find*(filter, ...)}.
	 *
	 * @param filter
	 *        the filter
	 */
	@Pointcut("execution(* net.solarnetwork.central.biz.UserEventBiz.find*(..)) && args(filter,..)")
	public void findEvents(UserEventFilter filter) {
	}

	/**
	 * Check access to query user events.
	 *
	 * <p>
	 * Events have unstructured data, so a security policy cannot be applied to
	 * them: an unrestricted policy is required.
	 * </p>
	 *
	 * @param filter
	 *        the filter to check
	 */
	@Before(value = "findEvents(filter)", argNames = "filter")
	public void findEventsCheck(@Nullable UserEventFilter filter) {
		if ( filter == null || !filter.hasUserCriteria() ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		for ( Long userId : filter.userIds() ) {
			requireUserReadAccess(userId);
		}
		requireUnrestrictedSecurityPolicy();
	}

}
