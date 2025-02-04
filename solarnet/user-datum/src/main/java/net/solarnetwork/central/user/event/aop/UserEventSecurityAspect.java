/* ==================================================================
 * UserEventSecurityAspect.java - 11/06/2020 9:46:50 am
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

package net.solarnetwork.central.user.event.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.security.AuthorizationSupport;

/**
 * Security enforcing AOP aspect for user event APIs.
 *
 * @author matt
 * @version 2.1
 */
@Aspect
@Component
public class UserEventSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserEventSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	@Pointcut("execution(* net.solarnetwork.central.user.event.biz.*Biz.*ForUser(..)) && args(userId,..)")
	public void actionForUser(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.event.biz.*Biz.save*(..)) && args(config,..)")
	public void saveConfiguration(UserIdRelated config) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.event.biz.*Biz.delete*(..)) && args(config,..)")
	public void deleteConfiguration(UserIdRelated config) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.event.biz.*Biz.*ForConfiguration(..)) && args(config,..)")
	public void actionForConfiguration(UserIdRelated config) {
	}

	@Before(value = "actionForUser(userId)", argNames = "userId")
	public void actionForUserCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = "saveConfiguration(config) || deleteConfiguration(config) || actionForConfiguration(config)",
			argNames = "config")
	public void saveConfigurationCheck(UserIdRelated config) {
		final Long userId = (config != null ? config.getUserId() : null);
		requireUserWriteAccess(userId);
	}

}
