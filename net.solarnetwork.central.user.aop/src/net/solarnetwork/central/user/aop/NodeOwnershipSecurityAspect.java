/* ==================================================================
 * NodeOwnershipSecurityAspect.java - Apr 22, 2015 6:38:24 AM
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
import net.solarnetwork.central.user.biz.NodeOwnershipBiz;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.central.user.domain.UserNodeTransfer;
import net.solarnetwork.central.user.support.AuthorizationSupport;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Security enforcing AOP aspect for {@link NodeOwnershipBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
public class NodeOwnershipSecurityAspect extends AuthorizationSupport {

	private final UserDao userDao;

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        The {@link UserNodeDao} to use.
	 */
	public NodeOwnershipSecurityAspect(UserNodeDao userNodeDao, UserDao userDao) {
		super(userNodeDao);
		this.userDao = userDao;
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*NodeOwnershipBiz.pending*(..)) && args(email)")
	public void pendingRequestsForEmail(String email) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*NodeOwnershipBiz.requestNodeOwnershipTransfer(..)) && args(userId,nodeId,..)")
	public void requestTransfer(Long userId, Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*NodeOwnershipBiz.cancel*(..)) && args(userId,nodeId,..)")
	public void cancelTransferRequest(Long userId, Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.biz.*NodeOwnershipBiz.confirm*(..)) && args(userId,nodeId,..)")
	public void confirmTransferRequest(Long userId, Long nodeId) {
	}

	@Before("pendingRequestsForEmail(email)")
	public void checkPendingRequestsForEmail(String email) {
		User recipient = userDao.getUserByEmail(email);
		if ( recipient == null ) {
			log.warn("Access DENIED to transfer recipient {}; not found", email);
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, email);
		}
		requireUserReadAccess(recipient.getId());
	}

	@Before("requestTransfer(userId, nodeId) || cancelTransferRequest(userId, nodeId)")
	public void checkUserNodeRequestOrCancelTransferRequest(Long userId, Long nodeId) {
		// the active user must have write-access to the given node
		requireNodeWriteAccess(nodeId);
	}

	@Before("confirmTransferRequest(userId, nodeId)")
	public void checkUserNodeConfirmTransferAccess(Long userId, Long nodeId) {
		// the active user must be the recipient of the transfer request
		final UserNodePK userNodePK = new UserNodePK(userId, nodeId);
		UserNodeTransfer xfer = getUserNodeDao().getUserNodeTransfer(userNodePK);
		if ( xfer == null ) {
			log.warn("Access DENIED to transfer {}; not found", userNodePK);
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, userNodePK);
		}
		User recipient = userDao.getUserByEmail(xfer.getEmail());
		if ( recipient == null ) {
			log.warn("Access DENIED to transfer recipient {}; not found", xfer.getEmail());
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, nodeId);
		}
		requireUserWriteAccess(recipient.getId());
	}
}
