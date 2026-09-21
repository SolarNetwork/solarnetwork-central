/* ==================================================================
 * NodeOwnershipSecurityAspect.java - 22/09/2026 7:16:14 am
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
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.biz.NodeOwnershipBiz;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.central.user.domain.UserNodeTransfer;

/**
 * Security enforcing AOP aspect for {@link NodeOwnershipBiz}.
 *
 * <p>
 * Only the owner of a node can request, view, or cancel an ownership transfer
 * of that node, and only the recipient of a transfer request can confirm (or
 * reject) it.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class NodeOwnershipSecurityAspect extends AuthorizationSupport {

	private final UserDao userDao;
	private final UserNodeDao userNodeDao;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param userDao
	 *        the user DAO
	 * @param userNodeDao
	 *        the user node DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public NodeOwnershipSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao, UserDao userDao,
			UserNodeDao userNodeDao) {
		super(nodeOwnershipDao);
		this.userDao = requireNonNullArgument(userDao, "userDao");
		this.userNodeDao = requireNonNullArgument(userNodeDao, "userNodeDao");
	}

	/**
	 * Match methods that list pending transfer requests for a recipient email.
	 *
	 * @param email
	 *        the recipient email
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.NodeOwnershipBiz.pending*(..)) && args(email)")
	public void pendingTransfersForEmail(String email) {
	}

	/**
	 * Match methods that request a transfer.
	 *
	 * @param userId
	 *        the node owner user ID
	 * @param nodeId
	 *        the node ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.NodeOwnershipBiz.requestNodeOwnershipTransfer(..)) && args(userId,nodeId,..)")
	public void requestTransfer(Long userId, Long nodeId) {
	}

	/**
	 * Match methods that get a transfer.
	 *
	 * @param userId
	 *        the node owner user ID
	 * @param nodeId
	 *        the node ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.NodeOwnershipBiz.getNodeOwnershipTransfer(..)) && args(userId,nodeId)")
	public void getTransfer(Long userId, Long nodeId) {
	}

	/**
	 * Match methods that cancel a transfer.
	 *
	 * @param userId
	 *        the node owner user ID
	 * @param nodeId
	 *        the node ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.NodeOwnershipBiz.cancel*(..)) && args(userId,nodeId,..)")
	public void cancelTransfer(Long userId, Long nodeId) {
	}

	/**
	 * Match methods that confirm (or reject) a transfer.
	 *
	 * @param userId
	 *        the node owner user ID
	 * @param nodeId
	 *        the node ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.NodeOwnershipBiz.confirm*(..)) && args(userId,nodeId,..)")
	public void confirmTransfer(Long userId, Long nodeId) {
	}

	/**
	 * Require the active user to be the user with a given email.
	 *
	 * @param email
	 *        the transfer recipient email
	 */
	@Before(value = "pendingTransfersForEmail(email)", argNames = "email")
	public void pendingTransfersAccessCheck(String email) {
		final User recipient = userForEmail(email);
		if ( recipient == null ) {
			log.warn("Access DENIED to transfers for recipient [{}]; not found", email);
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, email);
		}
		requireUserReadAccess(recipient.getId());
	}

	/**
	 * Require the active user to be the owner of the node being transferred.
	 *
	 * @param userId
	 *        the node owner user ID
	 * @param nodeId
	 *        the node ID
	 */
	@Before(value = "requestTransfer(userId, nodeId) || getTransfer(userId, nodeId) || cancelTransfer(userId, nodeId)",
			argNames = "userId,nodeId")
	public void ownerTransferAccessCheck(Long userId, Long nodeId) {
		requireUserWriteAccess(userId);
		requireNodeWriteAccess(nodeId);
	}

	/**
	 * Require the active user to be the recipient of the transfer.
	 *
	 * @param userId
	 *        the node owner user ID
	 * @param nodeId
	 *        the node ID
	 */
	@Before(value = "confirmTransfer(userId, nodeId)", argNames = "userId,nodeId")
	public void confirmTransferAccessCheck(Long userId, Long nodeId) {
		final UserNodePK pk = new UserNodePK(userId, nodeId);
		final UserNodeTransfer xfer = userNodeDao.getUserNodeTransfer(pk);
		if ( xfer == null ) {
			log.warn("Access DENIED to transfer {}; not found", pk);
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, pk);
		}
		final User recipient = userForEmail(xfer.getEmail());
		if ( recipient == null ) {
			log.warn("Access DENIED to transfer {}; recipient [{}] not found", pk, xfer.getEmail());
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, pk);
		}
		requireUserWriteAccess(recipient.getId());
	}

	private @Nullable User userForEmail(@Nullable String email) {
		return (email != null ? userDao.getUserByEmail(email) : null);
	}

}
