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
import java.time.Instant;
import java.util.Arrays;
import net.solarnetwork.central.dao.BasicUserEntity;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.util.ObjectUtils;

/**
 * A user-managed "secret".
 * 
 * @author matt
 * @version 1.0
 */
public class UserSecretEntity extends BasicUserEntity<UserSecretEntity, UserStringStringCompositePK> {

	private static final long serialVersionUID = 1894357432310854707L;

	private final byte[] secret;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @param secret
	 *        the secret value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserSecretEntity(UserStringStringCompositePK id, Instant created, byte[] secret) {
		super(id, created);
		byte[] secretCopy = new byte[requireNonNullArgument(secret, "secret").length];
		System.arraycopy(secret, 0, secretCopy, 0, secret.length);
		this.secret = secretCopy;
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param groupId
	 *        the group ID
	 * @param key
	 *        the key
	 * @param created
	 *        the creation date
	 * @param secret
	 *        the secret value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserSecretEntity(Long userId, String groupId, String key, Instant created, byte[] secret) {
		this(new UserStringStringCompositePK(userId, groupId, key), created, secret);
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
	 * @param created
	 *        the creation date
	 * @param secretValue
	 *        the secret value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserSecretEntity(UserStringStringCompositePK id, Instant created, String secretValue) {
		this(id, created,
				ObjectUtils.requireNonNullArgument(secretValue, "secretValue").getBytes(UTF_8));
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
	 * @param groupId
	 *        the group ID
	 * @param key
	 *        the key
	 * @param created
	 *        the creation date
	 * @param secretValue
	 *        the secret value
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserSecretEntity(Long userId, String groupId, String key, Instant created,
			String secretValue) {
		this(new UserStringStringCompositePK(userId, groupId, key), created, secretValue);
	}

	@Override
	public UserSecretEntity copyWithId(UserStringStringCompositePK id) {
		return new UserSecretEntity(id, getCreated(), secret);
	}

	@Override
	public void copyTo(UserSecretEntity entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSameAs(UserSecretEntity other) {
		return Arrays.equals(secret, other.secret);
	}

	/**
	 * Get the group ID.
	 * 
	 * @return the group ID
	 */
	public String getGroupId() {
		var pk = getId();
		return (pk != null ? pk.getGroupId() : null);
	}

	/**
	 * Get the key.
	 * 
	 * @return the key
	 */
	public String getKey() {
		var pk = getId();
		return (pk != null ? pk.getEntityId() : null);
	}

	/**
	 * Get the raw secret value.
	 * 
	 * @return the secret value
	 */
	public byte[] secret() {
		byte[] secretCopy = new byte[secret.length];
		System.arraycopy(secret, 0, secretCopy, 0, secret.length);
		return secretCopy;
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
