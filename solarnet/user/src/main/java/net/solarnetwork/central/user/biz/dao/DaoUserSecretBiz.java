/* ==================================================================
 * DaoUserSecretBiz.java - 22/03/2025 9:13:34 am
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

package net.solarnetwork.central.user.biz.dao;

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.security.KeyPair;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.RsaAlgorithm;
import org.springframework.security.crypto.encrypt.RsaSecretEncryptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.biz.SecretsBiz;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.security.RsaKeyHelper;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.biz.UserSecretBiz;
import net.solarnetwork.central.user.dao.BasicUserSecretFilter;
import net.solarnetwork.central.user.dao.UserKeyPairEntityDao;
import net.solarnetwork.central.user.dao.UserKeyPairFilter;
import net.solarnetwork.central.user.dao.UserSecretEntityDao;
import net.solarnetwork.central.user.dao.UserSecretFilter;
import net.solarnetwork.central.user.domain.UserKeyPair;
import net.solarnetwork.central.user.domain.UserKeyPairEntity;
import net.solarnetwork.central.user.domain.UserKeyPairInput;
import net.solarnetwork.central.user.domain.UserSecret;
import net.solarnetwork.central.user.domain.UserSecretEntity;
import net.solarnetwork.central.user.domain.UserSecretInput;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.service.CertificateService;

/**
 * DAO implementation of {@link UserSecretBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserSecretBiz implements UserSecretBiz {

	private final InstantSource clock;
	private final SecretsBiz secretsBiz;
	private final CertificateService certificateService;
	private final UserKeyPairEntityDao keyPairDao;
	private final UserSecretEntityDao secretDao;
	private final String secretEncryptionSalt;

	private final HmacUtils keyPairPasswordHmac;

	private Validator validator;

	/**
	 * Constructor.
	 * 
	 * @param clock
	 *        the clock to use
	 * @param secretsBiz
	 *        the secrets service to use
	 * @param certificateService
	 *        the certificate service to use
	 * @param keyPairDao
	 *        the key pair DAO to use
	 * @param secretDao
	 *        the secret DAO to use
	 * @param keyPairPasswordHmacKey
	 *        the HMAC key to use when hashing user-supplied key pair passwords
	 * @param secretEncryptionSalt
	 *        the salt used for secret encryption
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoUserSecretBiz(InstantSource clock, SecretsBiz secretsBiz,
			CertificateService certificateService, UserKeyPairEntityDao keyPairDao,
			UserSecretEntityDao secretDao, String keyPairPasswordHmacKey, String secretEncryptionSalt) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.secretsBiz = requireNonNullArgument(secretsBiz, "secretsBiz");
		this.certificateService = requireNonNullArgument(certificateService, "certificateService");
		this.keyPairDao = requireNonNullArgument(keyPairDao, "keyPairDao");
		this.secretDao = requireNonNullArgument(secretDao, "secretDao");
		this.keyPairPasswordHmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
				requireNonNullArgument(keyPairPasswordHmacKey, "keyPairPasswordHmacKey"));
		this.secretEncryptionSalt = requireNonNullArgument(secretEncryptionSalt, "secretEncryptionSalt");
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public UserKeyPair saveUserKeyPair(Long userId, UserKeyPairInput input) {
		requireNonNullArgument(userId, "id");

		validateInput(requireNonNullArgument(input, "input"));

		Instant now = clock.instant();

		var keyPair = RsaKeyHelper.parseKeyPair(input.getKeyPem());

		// use hash of provided password, so we know it is of a known length
		var passwordHash = keyPairPasswordHmac.hmac(input.getPassword());
		var password = Base64.getUrlEncoder().encodeToString(passwordHash);

		UserKeyPairEntity entity = UserKeyPairEntity.withKeyPair(userId, input.getKey(), now, now,
				keyPair, password, certificateService);

		secretsBiz.putSecret(entity.secretsBizKey(), password);

		var id = keyPairDao.create(userId, entity);
		return (id != null ? keyPairDao.get(id) : null);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void deleteUserKeyPair(Long userId, String key) {
		requireNonNullArgument(userId, "id");
		requireNonNullArgument(key, "key");

		var pk = keyPairDao.entityKey(new UserStringCompositePK(userId, key));

		secretsBiz.deleteSecret(pk.secretsBizKey());

		keyPairDao.delete(pk);
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	@Override
	public FilterResults<? extends UserKeyPair, UserStringCompositePK> listKeyPairsForUser(Long userId,
			UserKeyPairFilter filter) {
		BasicUserSecretFilter f = new BasicUserSecretFilter(filter);
		f.setUserId(userId);
		return keyPairDao.findFiltered(f);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public UserSecret saveUserSecret(Long userId, UserSecretInput input) {
		requireNonNullArgument(userId, "id");

		validateInput(requireNonNullArgument(input, "input"));

		Instant now = clock.instant();

		// lookup KeyPairEntity using key derived from topic ID
		var keyPairId = new UserStringCompositePK(userId, input.getTopicId());
		UserKeyPairEntity keyPair = requireNonNullObject(keyPairDao.get(keyPairId), keyPairId);

		// lookup key pair password from SecretsBiz
		var keyPairPasswordKey = keyPair.secretsBizKey();
		var keyPairPassword = requireNonNullObject(secretsBiz.getSecret(keyPairPasswordKey),
				keyPairPasswordKey);

		var encryptor = encryptor(keyPair, keyPairPassword);

		UserSecretEntity entity = new UserSecretEntity(userId, input.getTopicId(), input.getKey(), now,
				now, encryptor.encrypt(input.getSecret()));
		var id = secretDao.create(userId, input.getTopicId(), entity);
		return (id != null ? secretDao.get(id) : null);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void deleteUserSecret(Long userId, String topicId, String key) {
		requireNonNullArgument(userId, "id");

		var pk = secretDao.entityKey(new UserStringStringCompositePK(userId,
				topicId != null ? topicId : UserStringStringCompositePK.UNASSIGNED_GROUP_ID,
				key != null ? key : UserStringStringCompositePK.UNASSIGNED_ENTITY_ID));

		secretDao.delete(pk);
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	@Override
	public FilterResults<UserSecretEntity, UserStringStringCompositePK> listSecretsForUser(Long userId,
			UserSecretFilter filter) {
		BasicUserSecretFilter f = new BasicUserSecretFilter(filter);
		f.setUserId(userId);
		return secretDao.findFiltered(f);
	}

	private BytesEncryptor encryptor(UserKeyPairEntity keyPair, String keyPairPassword) {
		return encryptor(keyPair.keyPair(keyPairPassword));
	}

	private BytesEncryptor encryptor(KeyPair keyPair) {
		return new RsaSecretEncryptor(keyPair, RsaAlgorithm.DEFAULT, secretEncryptionSalt, true);
	}

	private void validateInput(final Object input) {
		validateInput(input, getValidator());
	}

	private static void validateInput(final Object input, final Validator v) {
		if ( input == null || v == null ) {
			return;
		}
		var violations = v.validate(input);
		if ( violations == null || violations.isEmpty() ) {
			return;
		}
		BindingResult errors = ExceptionUtils
				.toBindingResult(new ConstraintViolationException(violations), v);
		if ( errors.hasErrors() ) {
			throw new ValidationException(errors);
		}
	}

	/**
	 * Get the validator.
	 *
	 * @return the validator
	 */
	public Validator getValidator() {
		return validator;
	}

	/**
	 * Set the validator.
	 *
	 * @param validator
	 *        the validator to set
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

}
