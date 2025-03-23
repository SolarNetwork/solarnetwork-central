/* ==================================================================
 * DefaultUserSecretAccessDao.java - 23/03/2025 4:01:26 pm
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

package net.solarnetwork.central.user.dao;

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.security.KeyPair;
import javax.cache.Cache;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.RsaAlgorithm;
import org.springframework.security.crypto.encrypt.RsaSecretEncryptor;
import net.solarnetwork.central.biz.SecretsBiz;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.user.domain.UserKeyPairEntity;
import net.solarnetwork.central.user.domain.UserSecret;
import net.solarnetwork.central.user.domain.UserSecretEntity;

/**
 * Default implementation of {@link UserSecretAccessDao} that delegates.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultUserSecretAccessDao implements UserSecretAccessDao {

	private final SecretsBiz secretsBiz;
	private final UserKeyPairEntityDao keyPairDao;
	private final UserSecretEntityDao secretDao;
	private final String secretEncryptionSalt;

	private Cache<UserStringStringCompositePK, UserSecretEntity> secretCache;
	private Cache<UserStringCompositePK, KeyPair> keyPairCache;

	/**
	 * Constructor.
	 * 
	 * @param secretsBiz
	 *        the secrets service to use
	 * @param keyPairDao
	 *        the key pair DAO to use
	 * @param secretDao
	 *        the secret DAO to use
	 * @param secretEncryptionSalt
	 *        the salt used for secret encryption
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DefaultUserSecretAccessDao(SecretsBiz secretsBiz, UserKeyPairEntityDao keyPairDao,
			UserSecretEntityDao secretDao, String secretEncryptionSalt) {
		super();
		this.secretsBiz = requireNonNullArgument(secretsBiz, "secretsBiz");
		this.keyPairDao = requireNonNullArgument(keyPairDao, "keyPairDao");
		this.secretDao = requireNonNullArgument(secretDao, "secretDao");
		this.secretEncryptionSalt = requireNonNullArgument(secretEncryptionSalt, "secretEncryptionSalt");
	}

	@Override
	public UserSecret getUserSecret(Long userId, String topicId, String key) {
		final var secretId = new UserStringStringCompositePK(requireNonNullArgument(userId, "id"),
				requireNonNullArgument(topicId, "topicId"), requireNonNullArgument(key, "key"));

		final var cache = getSecretCache();

		UserSecretEntity result = null;

		if ( cache != null ) {
			result = cache.get(secretId);
		}
		if ( result == null ) {
			result = secretDao.get(secretId);
			if ( result != null && cache != null ) {
				cache.put(secretId, result);
			}
		}

		return result;
	}

	@Override
	public byte[] decryptSecretValue(UserSecret secret) {
		requireNonNullArgument(secret, "secret");

		final var cache = getKeyPairCache();

		final var keyPairId = new UserStringCompositePK(secret.getUserId(), secret.getTopicId());

		KeyPair keyPair = null;
		if ( cache != null ) {
			keyPair = cache.get(keyPairId);
		}
		if ( keyPair == null ) {
			// lookup KeyPairEntity using key derived from topic ID
			UserKeyPairEntity keyPairEntity = requireNonNullObject(keyPairDao.get(keyPairId), keyPairId);

			// lookup key pair password from SecretsBiz
			var keyPairPasswordKey = keyPairEntity.secretsBizKey();
			var keyPairPassword = requireNonNullObject(secretsBiz.getSecret(keyPairPasswordKey),
					keyPairPasswordKey);
			keyPair = keyPairEntity.keyPair(keyPairPassword);
			if ( cache != null ) {
				cache.put(keyPairId, keyPair);
			}
		}

		var encryptor = encryptor(keyPair);

		return encryptor.decrypt(secret.secret());
	}

	private BytesEncryptor encryptor(KeyPair keyPair) {
		return new RsaSecretEncryptor(keyPair, RsaAlgorithm.DEFAULT, secretEncryptionSalt, true);
	}

	/**
	 * Get the cache to use for secrets.
	 * 
	 * @return the cache
	 */
	public Cache<UserStringStringCompositePK, UserSecretEntity> getSecretCache() {
		return secretCache;
	}

	/**
	 * Set the cache to use for secrets.
	 * 
	 * @param secretCache
	 *        the cache to set
	 */
	public void setSecretCache(Cache<UserStringStringCompositePK, UserSecretEntity> secretCache) {
		this.secretCache = secretCache;
	}

	/**
	 * Get a cache to use for user key pairs.
	 * 
	 * @return the cache
	 */
	public Cache<UserStringCompositePK, KeyPair> getKeyPairCache() {
		return keyPairCache;
	}

	/**
	 * Set a cache to use for user key pairs.
	 * 
	 * @param keyPairCache
	 *        the cache to set
	 */
	public void setKeyPairCache(Cache<UserStringCompositePK, KeyPair> keyPairCache) {
		this.keyPairCache = keyPairCache;
	}

}
