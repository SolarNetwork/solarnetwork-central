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

import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.biz.UserAlertBiz;
import net.solarnetwork.central.user.dao.UserAlertDao;
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
 * @version 1.1
 */
@Aspect
public class UserAlertSecurityAspect extends AuthorizationSupport {

	private final UserAlertDao userAlertDao;

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        The {@link UserNodeDao} to use.
	 * @param userAlertDao
	 *        The {@link UserAlertDao} to use.
	 */
	public UserAlertSecurityAspect(UserNodeDao userNodeDao, UserAlertDao userAlertDao) {
		super(userNodeDao);
		this.userAlertDao = userAlertDao;
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*UserAlertBiz.userAlertsForUser(..)) && args(userId)")
	public void findAlertsForUser(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*UserAlertBiz.alertSituationCountForUser(..)) && args(userId)")
	public void getAlertSituationCountForUser(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*UserAlertBiz.alertSituationsForUser(..)) && args(userId)")
	public void getAlertSituationsForUser(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*UserAlertBiz.alertSituationsForNode(..)) && args(nodeId)")
	public void getAlertSituationsForNode(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*UserAlertBiz.saveAlert(..)) && args(alert)")
	public void saveAlert(UserAlert alert) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*UserAlertBiz.alertSituation(..)) && args(alertId)")
	public void getAlert(Long alertId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*UserAlertBiz.deleteAlert(..)) && args(alertId)")
	public void deleteAlert(Long alertId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*UserAlertBiz.updateSituationStatus(..)) && args(alertId, ..)")
	public void updateSituationStatus(Long alertId) {
	}

	@Before("findAlertsForUser(userId) || getAlertSituationCountForUser(userId) || getAlertSituationsForUser(userId)")
	public void checkViewAlertsForUser(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before("saveAlert(alert)")
	public void checkSaveAlert(UserAlert alert) {
		requireUserWriteAccess(alert.getUserId());
		if ( alert.getId() != null ) {
			// check userID not being changed
			UserAlert entity = userAlertDao.get(alert.getId());
			if ( entity == null ) {
				throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT,
						alert.getId());
			}
			requireUserWriteAccess(entity.getUserId());
		}
	}

	@Before("getAlert(alertId)")
	public void checkGetAlert(Long alertId) {
		// check userID of existing alert
		UserAlert entity = userAlertDao.get(alertId);
		if ( entity == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, alertId);
		}
		requireUserReadAccess(entity.getUserId());
	}

	@Before("updateSituationStatus(alertId) || deleteAlert(alertId)")
	public void checkUpdateAlertProperties(Long alertId) {
		// check userID of existing alert
		UserAlert entity = userAlertDao.get(alertId);
		if ( entity == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, alertId);
		}
		requireUserWriteAccess(entity.getUserId());
	}

	@Before("getAlertSituationsForNode(nodeId)")
	public void checkGetForNode(Long nodeId) {
		// require WRITE access here because read access not sufficient for alerts: we want owners or user tokens only
		requireNodeWriteAccess(nodeId);
	}
}
