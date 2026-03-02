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

import static java.util.Collections.emptyList;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.central.user.dao.BasicUserAuthTokenFilter.filterForIdentifier;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.cache.Cache;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.user.biz.NodeOwnershipBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.dao.BasicUserAuthTokenFilter;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.dao.UserAuthTokenFilter;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeCertificateDao;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.central.user.domain.UserNodeTransfer;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * DAO-based implementation of {@link UserBiz}.
 *
 * @author matt
 * @version 3.0
 */
public class DaoUserBiz implements UserBiz, NodeOwnershipBiz {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final UserDao userDao;
	private final UserAlertDao userAlertDao;
	private final UserNodeDao userNodeDao;
	private final UserNodeConfirmationDao userNodeConfirmationDao;
	private final UserNodeCertificateDao userNodeCertificateDao;
	private final UserAuthTokenDao userAuthTokenDao;
	private final SolarLocationDao solarLocationDao;
	private final SolarNodeDao solarNodeDao;

	private @Nullable Cache<UserStringCompositePK, UserAuthToken> userAuthTokenCache;

	/**
	 * Constructor.
	 * 
	 * @param userDao
	 *        the user DAO
	 * @param userNodeDao
	 *        the user node DAO
	 * @param userNodeConfirmationDao
	 *        the user node confirmation DAO
	 * @param userNodeCertificateDao
	 *        the user node certificate DAO
	 * @param solarNodeDao
	 *        the SolarNode DAO
	 * @param solarLocationDao
	 *        the location DAO
	 * @param userAuthTokenDao
	 *        the user token DAO
	 * @param userAlertDao
	 *        the user alert DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoUserBiz(UserDao userDao, UserNodeDao userNodeDao,
			UserNodeConfirmationDao userNodeConfirmationDao,
			UserNodeCertificateDao userNodeCertificateDao, SolarNodeDao solarNodeDao,
			SolarLocationDao solarLocationDao, UserAuthTokenDao userAuthTokenDao,
			UserAlertDao userAlertDao) {
		super();
		this.userDao = requireNonNullArgument(userDao, "userDao");
		this.userNodeDao = requireNonNullArgument(userNodeDao, "userNodeDao");
		this.userNodeConfirmationDao = requireNonNullArgument(userNodeConfirmationDao,
				"userNodeConfirmationDao");
		this.userNodeCertificateDao = requireNonNullArgument(userNodeCertificateDao,
				"userNodeCertificateDao");
		this.solarNodeDao = requireNonNullArgument(solarNodeDao, "solarNodeDao");
		this.solarLocationDao = requireNonNullArgument(solarLocationDao, "solarLocationDao");
		this.userAuthTokenDao = requireNonNullArgument(userAuthTokenDao, "userAuthTokenDao");
		this.userAlertDao = requireNonNullArgument(userAlertDao, "userAlertDao");
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public @Nullable User getUser(Long id) {
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
		if ( !result.getUserId().equals(userId) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
		}
		return result;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public UserNode saveUserNode(UserNode entry) throws AuthorizationException {
		assert entry != null;
		assert entry.getNode() != null;
		assert entry.getUser() != null;
		if ( (entry.getNode().getId() == null) || (entry.getUser().getId() == null) ) {
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
			if ( norm.getCountry() != null && norm.getTimeZoneId() != null ) {
				SolarLocation locEntity = solarLocationDao.getSolarLocationForLocation(norm);
				if ( locEntity == null ) {
					log.debug("Saving new SolarLocation {}", norm);
					locEntity = nonnull(solarLocationDao.get(solarLocationDao.save(norm)), "Location");
				}
				if ( !nonnull(locEntity.getId(), "Location ID").equals(node.getLocationId()) ) {
					log.debug("Updating node {} location from {} to {}", node.getId(),
							node.getLocationId(), locEntity.getId());
					node.setLocationId(locEntity.getId());
					solarNodeDao.save(node);
				}
			}
		}

		userNodeDao.save(entity);

		return entity;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateUserNodeArchivedStatus(Long userId, Long[] nodeIds, boolean archived)
			throws AuthorizationException {
		userNodeDao.updateUserNodeArchivedStatus(userId, nodeIds, archived);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNode> getArchivedUserNodes(Long userId) throws AuthorizationException {
		return userNodeDao.findArchivedUserNodesForUser(userId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNodeConfirmation> getPendingUserNodeConfirmations(Long userId) {
		User user = userDao.get(userId);
		return (user != null ? userNodeConfirmationDao.findPendingConfirmationsForUser(user)
				: emptyList());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public @Nullable UserNodeConfirmation getPendingUserNodeConfirmation(
			final Long userNodeConfirmationId) {
		return userNodeConfirmationDao.get(userNodeConfirmationId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public @Nullable UserNodeCertificate getUserNodeCertificate(Long userId, Long nodeId) {
		assert userId != null;
		assert nodeId != null;
		return userNodeCertificateDao.get(new UserNodePK(userId, nodeId));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public UserAuthToken generateUserAuthToken(Long userId, SecurityTokenType type,
			SecurityPolicy policy) {
		assert userId != null;
		assert type != null;
		SecureRandom rng;
		try {
			rng = SecureRandom.getInstance("SHA1PRNG");
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException("Unable to generate auth token", e);
		}
		final int randomLength = 24 + rng.nextInt(8);
		final String secretString = UserBizConstants.generateRandomToken(rng, randomLength);
		final int maxAttempts = 50;
		for ( int i = maxAttempts; i > 0; i-- ) {
			String tok = UserBizConstants.generateRandomAuthToken(rng);
			// verify token doesn't already exist
			if ( userAuthTokenDao.get(tok) == null ) {
				UserAuthToken authToken = new UserAuthToken(tok, userId, secretString, type);

				// verify user account has access to requested node IDs
				Set<Long> nodeIds = (policy == null ? null : policy.getNodeIds());
				if ( nodeIds != null ) {
					for ( Long nodeId : nodeIds ) {
						UserNode userNode = userNodeDao.get(nodeId);
						if ( userNode == null ) {
							throw new AuthorizationException(Reason.UNKNOWN_OBJECT, nodeId);
						}
						if ( !userNode.getUserId().equals(userId) ) {
							throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
						}
					}
				}

				if ( policy != null ) {
					BasicSecurityPolicy.Builder policyBuilder = new BasicSecurityPolicy.Builder()
							.withPolicy(policy);
					authToken.setPolicy(policyBuilder.build());
				}

				userAuthTokenDao.save(authToken);
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
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<UserAuthToken, String> listUserAuthTokensForUser(Long userId,
			@Nullable UserAuthTokenFilter filter) {
		requireNonNullArgument(userId, "userId");

		final Cache<UserStringCompositePK, UserAuthToken> cache = getUserAuthTokenCache();

		UserStringCompositePK cacheKey = null;
		if ( cache != null && filter != null && filter.hasIdentifierCriteria()
				&& filterForIdentifier(filter).equals(filter) ) {
			cacheKey = new UserStringCompositePK(userId,
					nonnull(filter.getIdentifier(), "Filter identifier"));
			var result = cache.get(cacheKey);
			if ( result != null ) {
				return new BasicFilterResults<>(List.of(result));
			}
		}

		BasicUserAuthTokenFilter f = new BasicUserAuthTokenFilter(filter);
		f.setUserId(userId);

		var result = userAuthTokenDao.findFiltered(f);

		if ( cache != null && cacheKey != null && result.getReturnedResultCount() == 1 ) {
			cache.put(cacheKey, result.getResults().iterator().next());
		}

		return result;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
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
	@Transactional(propagation = Propagation.REQUIRED)
	public UserAuthToken updateUserAuthTokenStatus(Long userId, String tokenId,
			SecurityTokenStatus newStatus) {
		assert userId != null;
		final UserAuthToken token = requireNonNullObject(userAuthTokenDao.get(tokenId), tokenId);
		if ( !userId.equals(token.getUserId()) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, tokenId);
		}
		if ( token.getStatus() != newStatus ) {
			token.setStatus(newStatus);
			userAuthTokenDao.save(token);
		}
		return token;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public UserAuthToken updateUserAuthTokenPolicy(Long userId, String tokenId, SecurityPolicy newPolicy,
			boolean replace) {
		assert userId != null;
		final UserAuthToken token = requireNonNullObject(userAuthTokenDao.get(tokenId), tokenId);
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
			userAuthTokenDao.save(token);
		}
		return token;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public UserAuthToken updateUserAuthTokenInfo(Long userId, String tokenId, UserAuthToken info) {
		assert userId != null;
		final UserAuthToken token = requireNonNullObject(userAuthTokenDao.get(tokenId), tokenId);
		if ( !userId.equals(token.getUserId()) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, tokenId);
		}
		if ( token.isInfoDifferent(info) ) {
			token.setName(info.getName());
			token.setDescription(info.getDescription());
			userAuthTokenDao.save(token);
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
	public @Nullable UserNodeTransfer getNodeOwnershipTransfer(Long userId, Long nodeId) {
		return userNodeDao.getUserNodeTransfer(new UserNodePK(userId, nodeId));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void requestNodeOwnershipTransfer(Long userId, Long nodeId, String newOwnerEmail)
			throws AuthorizationException {
		UserNodeTransfer xfer = new UserNodeTransfer();
		xfer.setUserId(userId);
		xfer.setNodeId(nodeId);
		xfer.setEmail(newOwnerEmail);
		userNodeDao.storeUserNodeTransfer(xfer);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void cancelNodeOwnershipTransfer(Long userId, Long nodeId) throws AuthorizationException {
		UserNodeTransfer xfer = userNodeDao.getUserNodeTransfer(new UserNodePK(userId, nodeId));
		if ( xfer != null ) {
			userNodeDao.deleteUserNodeTransfer(xfer);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public UserNodeTransfer confirmNodeOwnershipTransfer(Long userId, Long nodeId, boolean accept)
			throws AuthorizationException {
		final UserNodePK pk = new UserNodePK(userId, nodeId);
		final UserNodeTransfer xfer = requireNonNullObject(userNodeDao.getUserNodeTransfer(pk), pk);
		if ( accept ) {
			UserNode userNode = requireNonNullObject(userNodeDao.get(nodeId), nodeId);
			User recipient = requireNonNullObject(
					userDao.getUserByEmail(nonnull(xfer.getEmail(), "Transfer email")), xfer.getEmail());

			// at this point, we can delete the transfer request
			userNodeDao.deleteUserNodeTransfer(xfer);

			// remove any node alerts associated with this node
			int deletedAlertCount = userAlertDao.deleteAllAlertsForNode(userId, nodeId);
			log.debug("Deleted {} alerts associated with node {} for ownership transfer",
					deletedAlertCount, nodeId);

			// clean up auth tokens associated with node: if token contains just this node id, delete it
			// but if it contains other node IDs, just remove this node ID from it
			for ( UserAuthToken token : userAuthTokenDao.findUserAuthTokensForUser(userId) ) {
				if ( token.getNodeIds() != null && token.getNodeIds().contains(nodeId) ) {
					if ( token.getNodeIds().size() == 1 ) {
						// only this node ID associated, so delete token
						log.debug("Deleting UserAuthToken {} for node ownership transfer",
								token.getId());
						userAuthTokenDao.delete(token);
					} else {
						// other node IDs associated, so remove the node ID from this token
						log.debug(
								"Removing node ID {} from UserAuthToken {} for node ownership transfer",
								nodeId, token.getId());
						Set<Long> nodeIds = new LinkedHashSet<>(token.getNodeIds()); // get mutable set
						nodeIds.remove(nodeId);
						BasicSecurityPolicy.Builder secPolicyBuilder = new BasicSecurityPolicy.Builder()
								.withPolicy(token.getPolicy()).withNodeIds(nodeIds);
						token.setPolicy(secPolicyBuilder.build());

						userAuthTokenDao.save(token);
					}
				}
			}

			// and now, transfer ownership
			if ( !userNode.getUserId().equals(recipient.getId()) ) {
				userNode.setUser(recipient);
				userNodeDao.save(userNode);
			}
		} else {
			// rejecting
			cancelNodeOwnershipTransfer(userId, nodeId);
		}
		return xfer;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public @Nullable Snws2AuthorizationBuilder createSnws2AuthorizationBuilder(Long userId,
			String tokenId, Instant signingDate) {
		assert userId != null;
		UserAuthToken token = userAuthTokenDao.get(tokenId);
		if ( token == null ) {
			return null;
		}
		if ( !userId.equals(token.getUserId()) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, tokenId);
		}
		return userAuthTokenDao.createSnws2AuthorizationBuilder(tokenId, signingDate);
	}

	/**
	 * Get the token cache.
	 * 
	 * @return the cache
	 */
	public @Nullable Cache<UserStringCompositePK, UserAuthToken> getUserAuthTokenCache() {
		return userAuthTokenCache;
	}

	/**
	 * Set the token cache.
	 * 
	 * @param userAuthTokenCache
	 *        the cache to set
	 */
	public void setUserAuthTokenCache(
			@Nullable Cache<UserStringCompositePK, UserAuthToken> userAuthTokenCache) {
		this.userAuthTokenCache = userAuthTokenCache;
	}

}
