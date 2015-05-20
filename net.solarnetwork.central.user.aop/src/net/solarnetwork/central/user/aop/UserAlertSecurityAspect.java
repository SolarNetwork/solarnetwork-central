/* ==================================================================
 * UserAlertSecurityAspect.java - 19/05/2015 8:02:08 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

import net.solarnetwork.central.user.biz.UserAlertBiz;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.support.AuthorizationSupport;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Security enforcing AOP aspect for {@link UserAlertBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
public class UserAlertSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        The {@link UserNodeDao} to use.
	 */
	public UserAlertSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*UserAlertBiz.userAlertsForUser(..)) && args(userId)")
	public void findAlertsForUser(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*UserAlertBiz.saveAlert(..)) && args(alert)")
	public void saveAlert(UserAlert alert) {
	}

	@Before("findAlertsForUser(userId)")
	public void checkViewAlertsForUser(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before("saveAlert(alert)")
	public void checkViewAlertsForUser(UserAlert alert) {
		requireUserWriteAccess(alert.getUserId());
	}

}
