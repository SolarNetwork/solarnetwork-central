/* ==================================================================
 * UserSecretAccessDao.java - 23/03/2025 3:58:58 pm
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

import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.function.BiFunction;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.user.domain.UserKeyPair;
import net.solarnetwork.central.user.domain.UserSecret;
import net.solarnetwork.util.ObjectUtils;

/**
 * Read-only DAO API for user secret data.
 * 
 * @author matt
 * @version 1.1
 */
public interface UserSecretAccessDao {

	/**
	 * Get a specific user secret.
	 * 
	 * @param userId
	 *        the ID of the user to save the secret for
	 * @param topicId
	 *        the topic ID
	 * @param key
	 *        a unique name for the secret
	 * @return the secret, or {@code null} if not available
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	UserSecret getUserSecret(Long userId, String topicId, String key);

	/**
	 * Decrypt a user secret value.
	 * 
	 * <p>
	 * This will look up an associated {@link UserKeyPair} for the secret's
	 * {@code topicId} and use that to decrypt the secret's value.
	 * </p>
	 * 
	 * @param secret
	 *        the secret to decrypt the value for
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws IllegalStateException
	 *         if the user key data cannot be decoded, or the secret value
	 *         cannot be encrypted
	 */
	byte[] decryptSecretValue(UserSecret secret);

	/**
	 * Create a decryptor function for a specific topic.
	 * 
	 * @param <K>
	 *        the user related key type
	 * @param dao
	 *        the DAO to use (may be {@code null}
	 * @param topic
	 *        the topic
	 * @return the decryptor function
	 * @throws IllegalArgumentException
	 *         if {@code topic} is {@code null}
	 * @since 1.1
	 */
	static <K extends UserIdRelated> BiFunction<K, String, String> userSecretDecryptorFunction(
			final UserSecretAccessDao dao, final String topic) {
		ObjectUtils.requireNonNullArgument(topic, "topic");
		return (id, key) -> {
			if ( dao == null ) {
				return null;
			}
			var secret = dao.getUserSecret(id.getUserId(), topic, key);
			if ( secret == null ) {
				return null;
			}
			var data = dao.decryptSecretValue(secret);
			if ( data == null ) {
				return null;
			}
			return new String(data, UTF_8);
		};
	}

	/**
	 * Create a decryptor function for a specific topic.
	 * 
	 * @param <K>
	 *        the user related key type
	 * @param topic
	 *        the topic
	 * @return the decryptor function
	 * @since 1.1
	 */
	default <K extends UserIdRelated> BiFunction<K, String, String> decryptorFunction(
			final String topic) {
		return userSecretDecryptorFunction(this, topic);
	}

}
