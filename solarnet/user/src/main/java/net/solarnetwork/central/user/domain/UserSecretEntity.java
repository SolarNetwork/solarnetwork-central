/* ==================================================================
 * UserSecretEntity.java - 21/03/2025 4:45:34 pm
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

package net.solarnetwork.central.user.domain;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BasicUserEntity;
import net.solarnetwork.central.domain.UserStringStringCompositePK;

/**
 * A user-managed "secret".
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "userId", "topicId", "key", "created", "modified" })
@JsonIgnoreProperties({ "id" })
public class UserSecretEntity extends BasicUserEntity<UserSecretEntity, UserStringStringCompositePK>
		implements UserSecret {

	@Serial
	private static final long serialVersionUID = 1894357432310854707L;

	private final byte[] secret;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param secret
	 *        the secret value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserSecretEntity(UserStringStringCompositePK id, byte[] secret) {
		super(id);
		this.secret = Arrays.copyOf(secret, requireNonNullArgument(secret, "secret").length);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @param modified
	 *        the modification date
	 * @param secret
	 *        the secret value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserSecretEntity(UserStringStringCompositePK id, Instant created, Instant modified,
			byte[] secret) {
		super(id, created, modified);
		this.secret = Arrays.copyOf(secret, requireNonNullArgument(secret, "secret").length);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param topicId
	 *        the topic ID
	 * @param key
	 *        the key
	 * @param created
	 *        the creation date
	 * @param modified
	 *        the modification date
	 * @param secret
	 *        the secret value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserSecretEntity(Long userId, String topicId, String key, Instant created, Instant modified,
			byte[] secret) {
		this(new UserStringStringCompositePK(userId, topicId, key), created, modified, secret);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The secret value will be encoded as UTF-8 bytes.
	 * </p>
	 *
	 * @param id
	 *        the primary key
	 * @param secretValue
	 *        the secret value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserSecretEntity(UserStringStringCompositePK id, String secretValue) {
		this(id, requireNonNullArgument(secretValue, "secretValue").getBytes(UTF_8));
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The secret value will be encoded as UTF-8 bytes.
	 * </p>
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param secretValue
	 *        the secret value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserSecretEntity(UserStringStringCompositePK id, Instant created, Instant modified,
			String secretValue) {
		this(id, created, modified, requireNonNullArgument(secretValue, "secretValue").getBytes(UTF_8));
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The secret value will be encoded as UTF-8 bytes.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID
	 * @param topicId
	 *        the topic ID
	 * @param key
	 *        the key
	 * @param created
	 *        the creation date
	 * @param secretValue
	 *        the secret value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserSecretEntity(Long userId, String topicId, String key, Instant created, Instant modified,
			String secretValue) {
		this(userId, topicId, key, created, modified,
				requireNonNullArgument(secretValue, "secretValue").getBytes(UTF_8));
	}

	@Override
	public UserSecretEntity copyWithId(UserStringStringCompositePK id) {
		return new UserSecretEntity(id, getCreated(), getModified(), secret);
	}

	@Override
	public boolean isSameAs(UserSecretEntity other) {
		return Arrays.equals(secret, other.secret);
	}

	@Override
	public String getTopicId() {
		var pk = getId();
		return (pk != null ? pk.getGroupId() : null);
	}

	/**
	 * Get the key.
	 * 
	 * @return the key
	 */
	@Override
	public String getKey() {
		var pk = getId();
		return (pk != null ? pk.getEntityId() : null);
	}

	@Override
	public byte[] secret() {
		return Arrays.copyOf(secret, secret.length);
	}

	/**
	 * Get the secret as a string.
	 * 
	 * <p>
	 * This will decode the raw secret value as UTF-8 bytes.
	 * </p>
	 * 
	 * @return the secret value as a string
	 */
	public String secretValue() {
		return new String(secret, UTF_8);
	}
}
