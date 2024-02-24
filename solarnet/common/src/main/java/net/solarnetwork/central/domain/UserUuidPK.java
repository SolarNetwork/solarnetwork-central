/* ==================================================================
 * UserUuidPK.java - 1/08/2022 10:20:13 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import com.fasterxml.uuid.UUIDComparator;

/**
 * Immutable primary key for user-related entities using a UUID primary key.
 * 
 * @author matt
 * @version 1.1
 */
public final class UserUuidPK extends BasePK implements Serializable, Cloneable, Comparable<UserUuidPK>,
		CompositeKey2<Long, UUID>, UserIdRelated {

	private static final long serialVersionUID = 417842772182618447L;

	/**
	 * A special "not a value" instance to be used for generated UUID values yet
	 * to be generated.
	 */
	public static final UUID UNASSIGNED_UUID_ID = UUID
			.fromString("00000000-0000-7000-b000-000000000000");

	/**
	 * Create a new instance using the "unassigned" UUID value.
	 * 
	 * @param userId
	 *        the ID of the user to use
	 * @return the new key instance
	 */
	public static UserUuidPK unassignedUuidKey(Long userId) {
		return new UserUuidPK(userId, UNASSIGNED_UUID_ID);
	}

	private final Long userId;
	private final UUID uuid;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param uuid
	 *        the UUID
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserUuidPK(Long userId, UUID uuid) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.uuid = requireNonNullArgument(uuid, "uuid");
	}

	@Override
	public int compareTo(UserUuidPK o) {
		if ( o == null ) {
			return 1;
		}

		int comparison = userId.compareTo(o.userId);
		if ( comparison != 0 ) {
			return comparison;
		}

		// NOTE: JDK UUID ordering not suitable here, see UUIDComparator for more info
		return UUIDComparator.staticCompare(uuid, o.uuid);
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("u=").append(userId);
		buf.append(";i=").append(uuid);
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		buf.append("userId=").append(userId);
		buf.append(", uuid=").append(uuid);
	}

	@Override
	protected UserUuidPK clone() {
		return (UserUuidPK) super.clone();
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, uuid);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof UserUuidPK) ) {
			return false;
		}
		UserUuidPK other = (UserUuidPK) obj;
		return Objects.equals(userId, other.userId) && Objects.equals(uuid, other.uuid);
	}

	/**
	 * Get the user ID.
	 * 
	 * @return the user ID
	 */
	@Override
	public final Long getUserId() {
		return userId;
	}

	/**
	 * Get the UUID.
	 * 
	 * @return the UUID
	 */
	public final UUID getUuid() {
		return uuid;
	}

	@Override
	public final Long keyComponent1() {
		return getUserId();
	}

	@Override
	public final UUID keyComponent2() {
		return getUuid();
	}

	@Override
	public final boolean keyComponentIsAssigned(int index) {
		if ( index == 1 ) {
			return (uuid != null && uuid != UNASSIGNED_UUID_ID);
		}
		return CompositeKey2.super.keyComponentIsAssigned(index);
	}

	/**
	 * Test if the UUID is assigned.
	 * 
	 * @return {@literal true} if the entity ID value is assigned,
	 *         {@literal false} if it is considered "not a value"
	 */
	public final boolean uuidIsAssigned() {
		return keyComponentIsAssigned(1);
	}

}
