/* ==================================================================
 * DaoUserBiz.java - Dec 12, 2012 2:38:53 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.biz.dao;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.user.biz.NodeOwnershipBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeCertificateDao;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserAuthTokenStatus;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.central.user.domain.UserNodeTransfer;

/**
 * DAO-based implementation of {@link UserBiz}.
 * 
 * @author matt
 * @version 1.2
 */
public class DaoUserBiz implements UserBiz, NodeOwnershipBiz {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private UserDao userDao;
	private UserAlertDao userAlertDao;
	private UserNodeDao userNodeDao;
	private UserNodeConfirmationDao userNodeConfirmationDao;
	private UserNodeCertificateDao userNodeCertificateDao;
	private UserAuthTokenDao userAuthTokenDao;
	private SolarLocationDao solarLocationDao;
	private SolarNodeDao solarNodeDao;

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public User getUser(Long id) {
		return userDao.get(id);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNode> getUserNodes(Long userId) {
		return userNodeDao.findUserNodesAndCertificatesForUser(userId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public UserNode getUserNode(Long userId, Long nodeId) throws AuthorizationException {
		assert userId != null;
		assert nodeId != null;
		UserNode result = userNodeDao.get(nodeId);
		if ( result == null ) {
			throw new AuthorizationException(nodeId.toString(), Reason.UNKNOWN_OBJECT);
		}
		if ( result.getUser().getId().equals(userId) == false ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
		}
		return result;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserNode saveUserNode(UserNode entry) throws AuthorizationException {
		assert entry != null;
		assert entry.getNode() != null;
		assert entry.getUser() != null;
		if ( entry.getNode().getId() == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, null);
		}
		if ( entry.getUser().getId() == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, null);
		}
		UserNode entity = userNodeDao.get(entry.getNode().getId());
		if ( entity == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, entry.getNode().getId());
		}
		if ( entry.getName() != null ) {
			entity.setName(entry.getName());
		}
		if ( entry.getDescription() != null ) {
			entity.setDescription(entry.getDescription());
		}
		entity.setRequiresAuthorization(entry.isRequiresAuthorization());

		// Maintain the node's location as well; see if the location matches exactly one in the DB,
		// and if so assign that location (if not already assigned). If no location matches, create
		// a new location and assign that.
		if ( entry.getNodeLocation() != null ) {
			SolarNode node = entity.getNode();
			SolarLocation norm = SolarLocation.normalizedLocation(entry.getNodeLocation());
			SolarLocation locEntity = solarLocationDao.getSolarLocationForLocation(norm);
			if ( locEntity == null ) {
				log.debug("Saving new SolarLocation {}", locEntity);
				locEntity = solarLocationDao.get(solarLocationDao.store(norm));
			}
			if ( locEntity.getId().equals(node.getLocationId()) == false ) {
				log.debug("Updating node {} location from {} to {}", node.getId(), node.getLocationId(),
						locEntity.getId());
				node.setLocationId(locEntity.getId());
				solarNodeDao.store(node);
			}
		}

		userNodeDao.store(entity);

		return entity;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNodeConfirmation> getPendingUserNodeConfirmations(Long userId) {
		User user = userDao.get(userId);
		return userNodeConfirmationDao.findPendingConfirmationsForUser(user);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public UserNodeConfirmation getPendingUserNodeConfirmation(final Long userNodeConfirmationId) {
		return userNodeConfirmationDao.get(userNodeConfirmationId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public UserNodeCertificate getUserNodeCertificate(Long userId, Long nodeId) {
		assert userId != null;
		assert nodeId != null;
		return userNodeCertificateDao.get(new UserNodePK(userId, nodeId));
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserAuthToken generateUserAuthToken(final Long userId, final UserAuthTokenType type,
			final Set<Long> nodeIds) {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder().withNodeIds(nodeIds).build();
		return generateUserAuthToken(userId, type, policy);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserAuthToken generateUserAuthToken(Long userId, UserAuthTokenType type,
			SecurityPolicy policy) {
		assert userId != null;
		assert type != null;
		SecureRandom rng;
		try {
			rng = SecureRandom.getInstance("SHA1PRNG");
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException("Unable to generate auth token", e);
		}
		final int randomLength = 16 + rng.nextInt(8);
		final String secretString = UserBizConstants.generateRandomToken(rng, randomLength);
		final int maxAttempts = 50;
		for ( int i = maxAttempts; i > 0; i-- ) {
			String tok = UserBizConstants.generateRandomAuthToken(rng);
			// verify token doesn't already exist
			if ( userAuthTokenDao.get(tok) == null ) {
				UserAuthToken authToken = new UserAuthToken(tok, userId, secretString, type);

				BasicSecurityPolicy.Builder policyBuilder = new BasicSecurityPolicy.Builder()
						.withPolicy(policy);

				// verify user account has access to requested node IDs
				Set<Long> nodeIds = (policy == null ? null : policy.getNodeIds());
				if ( nodeIds != null ) {
					for ( Long nodeId : nodeIds ) {
						UserNode userNode = userNodeDao.get(nodeId);
						if ( userNode == null ) {
							throw new AuthorizationException(Reason.UNKNOWN_OBJECT, nodeId);
						}
						if ( userNode.getUser().getId().equals(userId) == false ) {
							throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
						}
					}
				}

				authToken.setPolicy(policyBuilder.build());

				userAuthTokenDao.store(authToken);
				return authToken;
			}
		}
		log.error("Failed to generate unique token after {} attempts", maxAttempts);
		throw new RuntimeException("Failed to generate unique token");
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserAuthToken> getAllUserAuthTokens(Long userId) {
		assert userId != null;
		return userAuthTokenDao.findUserAuthTokensForUser(userId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteUserAuthToken(Long userId, String tokenId) {
		assert userId != null;
		UserAuthToken token = userAuthTokenDao.get(tokenId);
		if ( token == null ) {
			return;
		}
		if ( !userId.equals(token.getUserId()) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, tokenId);
		}
		userAuthTokenDao.delete(token);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserAuthToken updateUserAuthTokenStatus(Long userId, String tokenId,
			UserAuthTokenStatus newStatus) {
		assert userId != null;
		UserAuthToken token = userAuthTokenDao.get(tokenId);
		if ( token == null ) {
			return null;
		}
		if ( !userId.equals(token.getUserId()) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, tokenId);
		}
		if ( token.getStatus() != newStatus ) {
			token.setStatus(newStatus);
			userAuthTokenDao.store(token);
		}
		return token;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserAuthToken updateUserAuthTokenPolicy(Long userId, String tokenId, SecurityPolicy newPolicy,
			boolean replace) {
		assert userId != null;
		UserAuthToken token = userAuthTokenDao.get(tokenId);
		if ( token == null ) {
			return null;
		}
		if ( !userId.equals(token.getUserId()) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, tokenId);
		}
		BasicSecurityPolicy.Builder policyBuilder = new BasicSecurityPolicy.Builder();
		if ( replace ) {
			policyBuilder = policyBuilder.withPolicy(newPolicy);
		} else {
			policyBuilder = policyBuilder.withPolicy(token.getPolicy()).withMergedPolicy(newPolicy);
		}
		BasicSecurityPolicy newBasicPolicy = policyBuilder.build();
		if ( !newBasicPolicy.equals(token.getPolicy()) ) {
			token.setPolicy(newBasicPolicy);
			userAuthTokenDao.store(token);
		}
		return token;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNodeTransfer> pendingNodeOwnershipTransfersForEmail(String email) {
		return userNodeDao.findUserNodeTransferRequestsForEmail(email);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public UserNodeTransfer getNodeOwnershipTransfer(Long userId, Long nodeId) {
		return userNodeDao.getUserNodeTransfer(new UserNodePK(userId, nodeId));
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void requestNodeOwnershipTransfer(Long userId, Long nodeId, String newOwnerEmail)
			throws AuthorizationException {
		UserNodeTransfer xfer = new UserNodeTransfer();
		xfer.setUserId(userId);
		xfer.setNodeId(nodeId);
		xfer.setEmail(newOwnerEmail);
		userNodeDao.storeUserNodeTransfer(xfer);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void cancelNodeOwnershipTransfer(Long userId, Long nodeId) throws AuthorizationException {
		UserNodeTransfer xfer = userNodeDao.getUserNodeTransfer(new UserNodePK(userId, nodeId));
		if ( xfer != null ) {
			userNodeDao.deleteUserNodeTrasnfer(xfer);
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserNodeTransfer confirmNodeOwnershipTransfer(Long userId, Long nodeId, boolean accept)
			throws AuthorizationException {
		UserNodePK pk = new UserNodePK(userId, nodeId);
		UserNodeTransfer xfer = userNodeDao.getUserNodeTransfer(pk);
		if ( accept ) {
			if ( xfer == null ) {
				throw new AuthorizationException(Reason.UNKNOWN_OBJECT, pk);
			}
			UserNode userNode = userNodeDao.get(nodeId);
			if ( userNode == null ) {
				throw new AuthorizationException(Reason.UNKNOWN_OBJECT, nodeId);
			}
			User recipient = userDao.getUserByEmail(xfer.getEmail());
			if ( recipient == null ) {
				throw new AuthorizationException(Reason.UNKNOWN_OBJECT, xfer.getEmail());
			}

			// at this point, we can delete the transfer request
			userNodeDao.deleteUserNodeTrasnfer(xfer);

			// remove any node alerts associated with this node
			int deletedAlertCount = userAlertDao.deleteAllAlertsForNode(userId, nodeId);
			log.debug("Deleted {} alerts associated with node {} for ownership transfer",
					deletedAlertCount, nodeId);

			// clean up auth tokens associated with node: if token contains just this node id, delete it
			// but if it contains other node IDs, just remove this node ID from it
			for ( UserAuthToken token : userAuthTokenDao.findUserAuthTokensForUser(userId) ) {
				if ( token.getNodeIds() != null && token.getNodeIds().contains(nodeId) ) {
					token.getNodeIds().remove(nodeId);
					if ( token.getNodeIds().size() == 0 ) {
						// only node ID associated, so delete token
						log.debug("Deleting UserAuthToken {} for node ownership transfer",
								token.getId());
						userAuthTokenDao.delete(token);
					} else {
						// other node IDs associated, so remove this token
						log.debug(
								"Removing node ID {} from UserAuthToken {} for node ownership transfer",
								nodeId, token.getId());
						userAuthTokenDao.store(token);
					}
				}
			}

			// and now, transfer ownership
			if ( recipient.getId().equals(userNode.getUser().getId()) == false ) {
				userNode.setUser(recipient);
				userNodeDao.store(userNode);
			}
		} else {
			// rejecting
			cancelNodeOwnershipTransfer(userId, nodeId);
		}
		return xfer;
	}

	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}

	public void setUserNodeDao(UserNodeDao userNodeDao) {
		this.userNodeDao = userNodeDao;
	}

	public void setUserNodeConfirmationDao(UserNodeConfirmationDao userNodeConfirmationDao) {
		this.userNodeConfirmationDao = userNodeConfirmationDao;
	}

	public void setUserNodeCertificateDao(UserNodeCertificateDao userNodeCertificateDao) {
		this.userNodeCertificateDao = userNodeCertificateDao;
	}

	public void setUserAuthTokenDao(UserAuthTokenDao userAuthTokenDao) {
		this.userAuthTokenDao = userAuthTokenDao;
	}

	public void setSolarLocationDao(SolarLocationDao solarLocationDao) {
		this.solarLocationDao = solarLocationDao;
	}

	public void setSolarNodeDao(SolarNodeDao solarNodeDao) {
		this.solarNodeDao = solarNodeDao;
	}

	public void setUserAlertDao(UserAlertDao userAlertDao) {
		this.userAlertDao = userAlertDao;
	}

}
