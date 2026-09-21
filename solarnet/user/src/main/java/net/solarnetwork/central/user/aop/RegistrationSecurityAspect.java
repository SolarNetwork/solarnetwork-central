/* ==================================================================
 * RegistrationSecurityAspect.java - 15/12/2025 12:12:30 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.domain.NewNodeRequest;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;

/**
 * Security enforcing AOP aspect for {@link RegistrationBiz}.
 *
 * @author matt
 * @version 1.1
 */
@Aspect
@Component
public class RegistrationSecurityAspect extends AuthorizationSupport {

	private final UserNodeConfirmationDao userNodeConfirmationDao;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param userNodeConfirmationDao
	 *        the user node confirmation DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 1.1
	 */
	public RegistrationSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao,
			UserNodeConfirmationDao userNodeConfirmationDao) {
		super(nodeOwnershipDao);
		this.userNodeConfirmationDao = requireNonNullArgument(userNodeConfirmationDao,
				"userNodeConfirmationDao");
	}

	/**
	 * Match creating a node manually.
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.RegistrationBiz.createNodeManually(..))")
	public void createNodeManually() {
	}

	/**
	 * Match creating a node, either manually or via an association.
	 *
	 * @param request
	 *        the new node request
	 * @since 1.1
	 */
	@Pointcut("""
			(execution(* net.solarnetwork.central.user.biz.RegistrationBiz.createNodeManually(..))
			|| execution(* net.solarnetwork.central.user.biz.RegistrationBiz.createNodeAssociation(..)))
			&& args(request)
			""")
	public void createNode(NewNodeRequest request) {
	}

	/**
	 * Match reading a node association.
	 *
	 * @param userNodeConfirmationId
	 *        the confirmation ID
	 * @since 1.1
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.RegistrationBiz.getNodeAssociation(..)) && args(userNodeConfirmationId)")
	public void getNodeAssociation(Long userNodeConfirmationId) {
	}

	/**
	 * Match cancelling a node association.
	 *
	 * @param userNodeConfirmationId
	 *        the confirmation ID
	 * @since 1.1
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.RegistrationBiz.cancelNodeAssociation(..)) && args(userNodeConfirmationId)")
	public void cancelNodeAssociation(Long userNodeConfirmationId) {
	}

	/**
	 * Match renewing a node certificate.
	 *
	 * @param userNode
	 *        the node to renew the certificate for
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.RegistrationBiz.renewNodeCertificate(..)) && args(userNode,..)")
	public void renewCertificate(UserNode userNode) {
	}

	/**
	 * Match updating a user.
	 *
	 * @param user
	 *        the user to update
	 * @since 1.1
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.RegistrationBiz.updateUser(..)) && args(user)")
	public void updateUser(User user) {
	}

	/**
	 * Match getting a pending node certificate renewal.
	 *
	 * @param userNode
	 *        the node to get the pending certificate renewal for
	 * @since 1.1
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.RegistrationBiz.getPendingNodeCertificateRenewal(..)) && args(userNode,..)")
	public void getPendingCertificateRenewal(UserNode userNode) {
	}

	/**
	 * Enforce an unrestricted policy.
	 */
	@Before(value = "createNodeManually()")
	public void requireUnrestrictedPolicyAccessCheck() {
		requireUnrestrictedSecurityPolicy();
	}

	/**
	 * Require write access to the user a node is being created for.
	 *
	 * @param request
	 *        the new node request
	 * @since 1.1
	 */
	@Before(value = "createNode(request)", argNames = "request")
	public void createNodeAccessCheck(NewNodeRequest request) {
		requireUserWriteAccess(request != null ? request.getUserId() : null);
	}

	/**
	 * Require read access to the user of a node association.
	 *
	 * @param userNodeConfirmationId
	 *        the confirmation ID
	 * @since 1.1
	 */
	@Before(value = "getNodeAssociation(userNodeConfirmationId)", argNames = "userNodeConfirmationId")
	public void getNodeAssociationAccessCheck(Long userNodeConfirmationId) {
		requireUserReadAccess(requireConfirmation(userNodeConfirmationId).getUserId());
	}

	/**
	 * Require write access to the user of a node association.
	 *
	 * @param userNodeConfirmationId
	 *        the confirmation ID
	 * @since 1.1
	 */
	@Before(value = "cancelNodeAssociation(userNodeConfirmationId)",
			argNames = "userNodeConfirmationId")
	public void cancelNodeAssociationAccessCheck(Long userNodeConfirmationId) {
		requireUserWriteAccess(requireConfirmation(userNodeConfirmationId).getUserId());
	}

	/**
	 * Require write access to a node.
	 *
	 * @param userNode
	 *        the node
	 */
	@Before(value = "renewCertificate(userNode) || getPendingCertificateRenewal(userNode)",
			argNames = "userNode")
	public void requireUserNodeWriteAccess(UserNode userNode) {
		final Long nodeId = userNode != null && userNode.getNode() != null ? userNode.getNode().getId()
				: null;
		if ( nodeId != null ) {
			requireNodeWriteAccess(nodeId);
		}
	}

	/**
	 * Require write access to the user being updated.
	 *
	 * @param user
	 *        the user to update
	 * @since 1.1
	 */
	@Before(value = "updateUser(user)", argNames = "user")
	public void updateUserAccessCheck(User user) {
		requireUserWriteAccess(user != null ? user.getId() : null);
	}

	private UserNodeConfirmation requireConfirmation(Long userNodeConfirmationId) {
		final UserNodeConfirmation conf = (userNodeConfirmationId != null
				? userNodeConfirmationDao.get(userNodeConfirmationId)
				: null);
		if ( conf == null ) {
			log.warn("Access DENIED to node association {}; not found", userNodeConfirmationId);
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, userNodeConfirmationId);
		}
		return conf;
	}

}
