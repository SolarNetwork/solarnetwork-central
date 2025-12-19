/* ==================================================================
 * UserSecurityAspect.java - 13/12/2025 9:30:49 am
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * Security enforcing AOP aspect for {@link UserBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
@Component
public class UserSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	/**
	 * Match get list of user nodes for user ID.
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.getUserNodes(..))")
	public void listUserNodesForUserId() {
	}

	/**
	 * Match get list of archived user nodes for user ID.
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.getArchivedUserNodes(..))")
	public void listArchivedUserNodesForUserId() {
	}

	/**
	 * Match get specific user node.
	 *
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.getUserNode(..)) && args(userId,nodeId)")
	public void getUserNode(Long userId, Long nodeId) {
	}

	/**
	 * Match save user node.
	 *
	 * @param userNode
	 *        the node to save
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.saveUserNode(..)) && args(userNode)")
	public void saveUserNode(UserNode userNode) {
	}

	/**
	 * Match update user node archived status.
	 *
	 * @param userId
	 *        the user ID
	 * @param nodeIds
	 *        the node IDs to update
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.updateUserNodeArchivedStatus(..)) && args(userId,nodeIds,..)")
	public void updateUserNodeArchivedStatus(Long userId, Long[] nodeIds) {
	}

	/**
	 * Match get list of pending user node confirmations.
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.getPendingUserNodeConfirmations(..))")
	public void getPendingUserNodeConfirmationsForUserId() {
	}

	/**
	 * Match getting a specific pending user node confirmation.
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.getPendingUserNodeConfirmation(..))")
	public void getPendingUserNodeConfirmation() {
	}

	/**
	 * Match getting a user node certificate.
	 * 
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 */
	@Pointcut("execution(* net.solarnetwork.central.user.biz.UserBiz.getUserNodeCertificate(..)) && args(userId,nodeId)")
	public void getUserNodeCertificate(Long userId, Long nodeId) {
	}

	/**
	 * Enforce node ID policy restrictions when requesting the available user
	 * nodes.
	 *
	 * @param pjp
	 *        the join point
	 * @return the allowed results
	 * @throws Throwable
	 *         if anything goes wrong
	 */
	@Around("listUserNodesForUserId() || listArchivedUserNodesForUserId()")
	public List<UserNode> listUserNodesForUserIdAccessCheck(ProceedingJoinPoint pjp) throws Throwable {
		@SuppressWarnings("unchecked")
		List<UserNode> result = (List<UserNode>) pjp.proceed();
		return restrictNodesToPolicy(result, getActiveSecurityPolicy());
	}

	private List<UserNode> restrictNodesToPolicy(List<UserNode> nodes, SecurityPolicy policy) {
		if ( nodes == null || nodes.isEmpty() || policy == null || policy.getNodeIds() == null
				|| policy.getNodeIds().isEmpty() ) {
			return nodes;
		}
		final Set<Long> policyNodeIds = policy.getNodeIds();
		final List<UserNode> result = new ArrayList<>(nodes.size());
		for ( UserNode node : nodes ) {
			if ( policyNodeIds.contains(node.getNode().getId()) ) {
				result.add(node);
			}
		}
		return result;
	}

	/**
	 * Enforce node ID policy restrictions when requesting the pending user node
	 * confirmations.
	 *
	 * @param pjp
	 *        the join point
	 * @return the allowed results
	 * @throws Throwable
	 *         if anything goes wrong
	 */
	@Around("getPendingUserNodeConfirmationsForUserId()")
	public List<UserNodeConfirmation> listPendingUserNodeConfirmationsForUserIdAccessCheck(
			ProceedingJoinPoint pjp) throws Throwable {
		@SuppressWarnings("unchecked")
		List<UserNodeConfirmation> result = (List<UserNodeConfirmation>) pjp.proceed();
		return restrictNodeConfirmationsToPolicy(result, getActiveSecurityPolicy());
	}

	private List<UserNodeConfirmation> restrictNodeConfirmationsToPolicy(
			List<UserNodeConfirmation> nodes, SecurityPolicy policy) {
		if ( nodes == null || nodes.isEmpty() || policy == null || policy.getNodeIds() == null
				|| policy.getNodeIds().isEmpty() ) {
			return nodes;
		}
		final Set<Long> policyNodeIds = policy.getNodeIds();
		final List<UserNodeConfirmation> result = new ArrayList<>(nodes.size());
		for ( UserNodeConfirmation node : nodes ) {
			if ( node.getNodeId() != null && policyNodeIds.contains(node.getNodeId()) ) {
				result.add(node);
			}
		}
		return result;
	}

	/**
	 * Enforce node ID policy restrictions when getting a user node (or related
	 * entity).
	 * 
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 */
	@Before(value = "getUserNode(userId, nodeId) || getUserNodeCertificate(userId, nodeId)",
			argNames = "userId,nodeId")
	public void getUserNodeAccessCheck(Long userId, Long nodeId) {
		requireNodeReadAccess(nodeId);
	}

	/**
	 * Enforce node ID policy restrictions when saving a user node.
	 * 
	 * @param userNode
	 *        the user node
	 */
	@Before("saveUserNode(userNode)")
	public void saveUserNodeAccessCheck(UserNode userNode) {
		if ( userNode == null || userNode.getNode() == null || userNode.getNode().getId() == null ) {
			return;
		}
		requireNodeReadAccess(userNode.getNode().getId());
	}

	/**
	 * Enforce node ID policy restrictions when updating user node archive
	 * status.
	 * 
	 * @param userId
	 *        the user ID
	 * @param nodeIds
	 *        the node IDs
	 */
	@Before(value = "updateUserNodeArchivedStatus(userId,nodeIds)", argNames = "userId,nodeIds")
	public void updateUserNodeArchivedStatusAccessCheck(Long userId, Long[] nodeIds) {
		if ( nodeIds == null || nodeIds.length < 1 ) {
			return;
		}
		for ( Long nodeId : nodeIds ) {
			requireNodeWriteAccess(nodeId);
		}
	}

	/**
	 * Enforce node ID policy restrictions when requesting a pending user node
	 * confirmation.
	 *
	 * @param pjp
	 *        the join point
	 * @return the allowed result
	 * @throws Throwable
	 *         if anything goes wrong
	 */
	@Around("getPendingUserNodeConfirmation()")
	public UserNodeConfirmation getPendingUserNodeConfirmationAccessCheck(ProceedingJoinPoint pjp)
			throws Throwable {
		UserNodeConfirmation result = (UserNodeConfirmation) pjp.proceed();
		if ( result != null && result.getId() != null ) {
			requireNodeReadAccess(result.getNodeId());
		}
		return result;
	}

}
