/* ==================================================================
 * UserAlertSecurityAspect.java - 22/09/2026 8:10:27 am
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

package net.solarnetwork.central.user.aop;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.biz.UserAlertBiz;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.domain.UserAlert;

/**
 * Security enforcing AOP aspect for {@link UserAlertBiz}.
 *
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class UserAlertSecurityAspect extends AuthorizationSupport {

	private final UserAlertDao userAlertDao;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param userAlertDao
	 *        the user alert DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserAlertSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao,
			UserAlertDao userAlertDao) {
		super(nodeOwnershipDao);
		this.userAlertDao = requireNonNullArgument(userAlertDao, "userAlertDao");
	}

	/**
	 * Match methods that read alerts for a user.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserAlertBiz.*ForUser(..)) && args(userId)")
	public void readForUser(Long userId) {
	}

	/**
	 * Match methods that read alerts for a node.
	 *
	 * @param nodeId
	 *        the node ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserAlertBiz.*ForNode(..)) && args(nodeId)")
	public void readForNode(Long nodeId) {
	}

	/**
	 * Match methods that save an alert.
	 *
	 * @param alert
	 *        the alert
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserAlertBiz.saveAlert(..)) && args(alert)")
	public void saveAlert(UserAlert alert) {
	}

	/**
	 * Match methods that read an alert.
	 *
	 * @param alertId
	 *        the alert ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserAlertBiz.alertSituation(..)) && args(alertId)")
	public void readAlert(Long alertId) {
	}

	/**
	 * Match methods that modify an alert.
	 *
	 * @param alertId
	 *        the alert ID
	 */
	@Pointcut("""
			(execution(* net.solarnetwork.central.user.biz.UserAlertBiz.deleteAlert(..))
			|| execution(* net.solarnetwork.central.user.biz.UserAlertBiz.updateSituationStatus(..)))
			&& args(alertId,..)
			""")
	public void modifyAlert(Long alertId) {
	}

	/**
	 * Require read access to a user.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Before(value = "readForUser(userId)", argNames = "userId")
	public void readForUserAccessCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	/**
	 * Require write access to a node.
	 *
	 * <p>
	 * Write access is required because read access would allow access to the
	 * alerts of public nodes.
	 * </p>
	 *
	 * @param nodeId
	 *        the node ID
	 */
	@Before(value = "readForNode(nodeId)", argNames = "nodeId")
	public void readForNodeAccessCheck(Long nodeId) {
		requireNodeWriteAccess(nodeId);
	}

	/**
	 * Require write access to the alert's user and node, and the user of any
	 * existing alert being updated.
	 *
	 * @param alert
	 *        the alert to save
	 */
	@Before(value = "saveAlert(alert)", argNames = "alert")
	public void saveAlertAccessCheck(UserAlert alert) {
		if ( alert == null ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}
		requireUserWriteAccess(alert.getUserId());
		if ( alert.getNodeId() != null ) {
			requireNodeWriteAccess(alert.getNodeId());
		}
		if ( alert.getId() != null ) {
			requireUserWriteAccess(requireAlert(alert.getId()).getUserId());
		}
	}

	/**
	 * Require read access to the user of an alert.
	 *
	 * @param alertId
	 *        the alert ID
	 */
	@Before(value = "readAlert(alertId)", argNames = "alertId")
	public void readAlertAccessCheck(Long alertId) {
		requireUserReadAccess(requireAlert(alertId).getUserId());
	}

	/**
	 * Require write access to the user of an alert.
	 *
	 * @param alertId
	 *        the alert ID
	 */
	@Before(value = "modifyAlert(alertId)", argNames = "alertId")
	public void modifyAlertAccessCheck(Long alertId) {
		requireUserWriteAccess(requireAlert(alertId).getUserId());
	}

	private UserAlert requireAlert(Long alertId) {
		final UserAlert alert = (alertId != null ? userAlertDao.get(alertId) : null);
		if ( alert == null ) {
			log.warn("Access DENIED to alert {}; not found", alertId);
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, alertId);
		}
		return alert;
	}

}
