/* ==================================================================
 * DaoUserOcppBiz.java - 29/02/2020 4:48:40 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.ocpp.biz.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.dao.UserSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.user.dao.UserRelatedEntity;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.service.PasswordEncoder;

/**
 * DAO-based implementation of {@link UserOcppBiz}.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoUserOcppBiz implements UserOcppBiz {

	private final CentralSystemUserDao systemUserDao;
	private final CentralChargePointDao chargePointDao;
	private final CentralChargePointConnectorDao connectorDao;
	private final CentralAuthorizationDao authorizationDao;
	private final CentralChargeSessionDao chargeSessionDao;
	private final UserSettingsDao userSettingsDao;
	private final ChargePointSettingsDao chargePointSettingsDao;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Constructor.
	 * 
	 * @param systemUserDao
	 *        the system user DAO
	 * @param chargePointDao
	 *        the charge point DAO
	 * @param connectorDao
	 *        the connector DAO
	 * @param authorizationDao
	 *        the authorization DAO
	 * @param chargeSessionDao
	 *        the session DAO
	 * @param userSettingsDao
	 *        the user settings DAo
	 * @param chargePointSettingsDao
	 *        the charge point settings DAO
	 * @param passwordEncoder
	 *        the system user password encoder to use
	 * @throws IllegalArgumentException
	 *         if any parmeter is {@literal null}
	 */
	public DaoUserOcppBiz(CentralSystemUserDao systemUserDao, CentralChargePointDao chargePointDao,
			CentralChargePointConnectorDao connectorDao, CentralAuthorizationDao authorizationDao,
			CentralChargeSessionDao chargeSessionDao, UserSettingsDao userSettingsDao,
			ChargePointSettingsDao chargePointSettingsDao, PasswordEncoder passwordEncoder) {
		super();
		this.systemUserDao = requireNonNullArgument(systemUserDao, "systemUserDao");
		this.chargePointDao = requireNonNullArgument(chargePointDao, "chargePointDao");
		this.connectorDao = requireNonNullArgument(connectorDao, "connectorDao");
		this.authorizationDao = requireNonNullArgument(authorizationDao, "authorizationDao");
		this.chargeSessionDao = requireNonNullArgument(chargeSessionDao, "chargeSessionDao");
		this.userSettingsDao = requireNonNullArgument(userSettingsDao, "userSettingsDao");
		this.chargePointSettingsDao = requireNonNullArgument(chargePointSettingsDao,
				"chargePointSettingsDao");
		this.passwordEncoder = requireNonNullArgument(passwordEncoder, "passwordEncoder");
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<CentralSystemUser> systemUsersForUser(Long userId) {
		return systemUserDao.findAllForOwner(userId);
	}

	private static String generateRandomToken(int byteCount) {
		SecureRandom rng;
		try {
			rng = SecureRandom.getInstance("SHA1PRNG");
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException("Unable to generate password.", e);
		}
		return generateRandomToken(rng, byteCount);
	}

	private static String generateRandomToken(SecureRandom rng, int byteCount) {
		byte[] randomBytes = new byte[byteCount];
		rng.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	/**
	 * Validate a result user-related entity has the same owner as an input
	 * entity.
	 * 
	 * <p>
	 * The DAO update methods might only update a matching user ID, and then
	 * return the (unchanged) record without having actually updated anything.
	 * </p>
	 * 
	 * @param in
	 *        the entity that was passed in
	 * @param out
	 *        the entity that will be retuned
	 * @throws AuthorizationException
	 *         if {@code in} has no user ID, or its user ID does not match that
	 *         in {@code out}
	 */
	private static void verifyUserRelatedEntityResult(UserRelatedEntity<?> in,
			UserRelatedEntity<?> out) {
		if ( in == null || out == null ) {
			return;
		} else if ( in.getUserId() == null ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}
		if ( !in.getUserId().equals(out.getUserId()) ) {
			throw new AuthorizationException(in.getUserId().toString(), Reason.ACCESS_DENIED);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CentralSystemUser saveSystemUser(CentralSystemUser systemUser) {
		if ( systemUser == null ) {
			return null;
		}
		String generatedPassword = null;
		if ( systemUser.getPassword() == null && systemUser.getId() == null ) {
			// generate new password
			generatedPassword = generateRandomToken(16);
			systemUser.setPassword(passwordEncoder.encode(generatedPassword));

		} else if ( systemUser.getPassword() != null ) {
			// encrypt password
			systemUser.setPassword(passwordEncoder.encode(systemUser.getPassword()));
		}
		CentralSystemUser result = (CentralSystemUser) systemUserDao.get(systemUserDao.save(systemUser));
		verifyUserRelatedEntityResult(systemUser, result);
		if ( generatedPassword != null ) {
			// return password to caller
			result.setPassword(generatedPassword);
		}
		return result;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public CentralSystemUser systemUserForUser(Long userId, String username) {
		return systemUserDao.getForUsername(userId, username);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public CentralSystemUser systemUserForUser(Long userId, Long id) {
		return systemUserDao.get(userId, id);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteUserSystemUser(Long userId, Long id) {
		systemUserDao.delete(userId, id);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<CentralAuthorization> authorizationsForUser(Long userId) {
		return authorizationDao.findAllForOwner(userId);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public CentralAuthorization authorizationForUser(Long userId, Long id) {
		return authorizationDao.get(userId, id);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteUserAuthorization(Long userId, Long id) {
		authorizationDao.delete(userId, id);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CentralAuthorization saveAuthorization(CentralAuthorization authorization) {
		if ( authorization == null ) {
			return null;
		}
		CentralAuthorization result = (CentralAuthorization) authorizationDao
				.get(authorizationDao.save(authorization));
		verifyUserRelatedEntityResult(authorization, result);
		return result;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<CentralChargePoint> chargePointsForUser(Long userId) {
		return chargePointDao.findAllForOwner(userId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CentralChargePoint saveChargePoint(CentralChargePoint chargePoint) {
		if ( chargePoint == null ) {
			return null;
		}
		CentralChargePoint result = (CentralChargePoint) chargePointDao
				.get(chargePointDao.save(chargePoint));
		verifyUserRelatedEntityResult(chargePoint, result);
		return result;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public CentralChargePoint chargePointForUser(Long userId, Long id) {
		return chargePointDao.get(userId, id);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteUserChargePoint(Long userId, Long id) {
		chargePointDao.delete(userId, id);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public CentralChargePointConnector chargePointConnectorForUser(Long userId,
			ChargePointConnectorKey id) {
		return connectorDao.get(userId, id);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteUserChargePointConnector(Long userId, ChargePointConnectorKey id) {
		connectorDao.delete(userId, id);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<CentralChargePointConnector> chargePointConnectorsForUser(Long userId) {
		return connectorDao.findAllForOwner(userId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CentralChargePointConnector saveChargePointConnector(CentralChargePointConnector entity) {
		return (CentralChargePointConnector) connectorDao.get(connectorDao.save(entity));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public ChargePointSettings chargePointSettingsForUser(Long userId, Long chargePointId) {
		return chargePointSettingsDao.get(userId, chargePointId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteUserChargePointSettings(Long userId, Long chargePointId) {
		chargePointSettingsDao.delete(userId, chargePointId);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<ChargePointSettings> chargePointSettingsForUser(Long userId) {
		return chargePointSettingsDao.findAllForOwner(userId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChargePointSettings saveChargePointSettings(ChargePointSettings settings) {
		ChargePointSettings result = chargePointSettingsDao.get(chargePointSettingsDao.save(settings));
		verifyUserRelatedEntityResult(settings, result);
		return result;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public UserSettings settingsForUser(Long userId) {
		return userSettingsDao.get(userId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteUserSettings(Long userId) {
		userSettingsDao.delete(userId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserSettings saveSettings(UserSettings settings) {
		return userSettingsDao.get(userSettingsDao.save(settings));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public ChargeSession chargeSessionForUser(Long userId, UUID sessionId) {
		return chargeSessionDao.get(sessionId, userId);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<ChargeSession> incompleteChargeSessionsForChargePoint(Long userId,
			Long chargePointId) {
		return chargeSessionDao.getIncompleteChargeSessionsForUserForChargePoint(userId, chargePointId);
	}

}