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

import static net.solarnetwork.central.user.biz.dao.UserBizConstants.generateRandomAuthToken;
import static net.solarnetwork.central.user.biz.dao.UserBizConstants.getUnconfirmedEmail;
import static net.solarnetwork.central.user.biz.dao.UserBizConstants.isUnconfirmedEmail;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.PasswordEncoder;
import net.solarnetwork.central.user.biz.UserBiz;
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
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO-based implementation of {@link UserBiz}.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
@Service("daoUserBiz")
public class DaoUserBiz implements UserBiz {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private UserDao userDao;

	@Autowired
	private UserNodeDao userNodeDao;

	@Autowired
	private UserNodeConfirmationDao userNodeConfirmationDao;

	@Autowired
	private UserNodeCertificateDao userNodeCertificateDao;

	@Autowired
	private UserAuthTokenDao userAuthTokenDao;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public User logonUser(String email, String password) throws AuthorizationException {
		if ( email == null ) {
			throw new AuthorizationException(email, AuthorizationException.Reason.UNKNOWN_EMAIL);
		}

		if ( log.isInfoEnabled() ) {
			log.info("Login attempt: " + email);
		}

		// do not allow logon attempt of unconfirmed email
		if ( isUnconfirmedEmail(email) ) {
			throw new AuthorizationException(email, AuthorizationException.Reason.UNKNOWN_EMAIL);
		}

		User entity = userDao.getUserByEmail(email);
		if ( entity == null ) {
			// first check if user waiting confirmation still
			String unconfirmedEmail = getUnconfirmedEmail(email);
			entity = userDao.getUserByEmail(unconfirmedEmail);
			if ( entity != null ) {
				throw new AuthorizationException(email,
						AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED);
			}
			throw new AuthorizationException(email, AuthorizationException.Reason.UNKNOWN_EMAIL);
		}

		if ( !passwordEncoder.matches(password, entity.getPassword()) ) {
			throw new AuthorizationException(email, AuthorizationException.Reason.BAD_PASSWORD);
		}

		if ( log.isInfoEnabled() ) {
			log.info("Login successful: " + email);
		}

		// populate roles
		entity.setRoles(userDao.getUserRoles(entity));

		return entity;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public User getUser(Long id) {
		return userDao.get(id);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public User getUser(String email) {
		return userDao.getUserByEmail(email);
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
		if ( entry.getName() != null ) {
			entity.setName(entry.getName());
		}
		if ( entry.getDescription() != null ) {
			entity.setDescription(entry.getDescription());
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
	public UserNodeCertificate getUserNodeCertificate(Long certId) {
		assert certId != null;
		return userNodeCertificateDao.get(certId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserAuthToken generateUserAuthToken(Long userId) {
		assert userId != null;
		SecureRandom rnd;
		try {
			rnd = SecureRandom.getInstance("SHA1PRNG");
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException("Unable to generate auth token", e);
		}
		int randomLength = 8 + rnd.nextInt(8);
		byte[] secret = new byte[randomLength];
		rnd.nextBytes(secret);
		String secretString = new String(Hex.encodeHex(secret));
		final int maxAttempts = 50;
		for ( int i = maxAttempts; i > 0; i-- ) {
			String tok = generateRandomAuthToken();
			// verify token doesn't already exist
			if ( userAuthTokenDao.get(tok) == null ) {
				UserAuthToken authToken = new UserAuthToken(tok, userId, secretString,
						UserAuthTokenType.User);
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

	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

}
