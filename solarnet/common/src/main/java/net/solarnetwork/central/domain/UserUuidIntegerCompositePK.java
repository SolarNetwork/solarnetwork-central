/* ==================================================================
 * UserUuidIntegerCompositePK.java - 20/02/2024 6:11:36 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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
import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

/**
 * Basic implementation of a Long, UUID, Integer composite key.
 *
 * @author matt
 * @version 1.1
 */
public class UserUuidIntegerCompositePK extends BasePK implements
		UserRelatedCompositeKey<UserUuidIntegerCompositePK>, CompositeKey3<Long, UUID, Integer> {

	@Serial
	private static final long serialVersionUID = -7492906234566578908L;

	/**
	 * A special "not a value" instance to be used for generated user ID values
	 * yet to be generated.
	 */
	public static final Long UNASSIGNED_USER_ID = Long.MIN_VALUE;

	/**
	 * A special "not a value" instance to be used for generated group ID values
	 * yet to be generated.
	 */
	public static final UUID UNASSIGNED_GROUP_ID = new UUID(0L, 0L);

	/**
	 * A special "not a value" instance to be used for generated entity ID
	 * values yet to be generated.
	 */
	public static final Integer UNASSIGNED_ENTITY_ID = Integer.MIN_VALUE;

	/**
	 * Create a new instance using the "unassigned" entity ID value.
	 *
	 * @param userId
	 *        the user ID to use
	 * @param groupId
	 *        the ID of the group to use
	 * @return the new key instance
	 */
	public static UserUuidIntegerCompositePK unassignedEntityIdKey(Long userId, UUID groupId) {
		return new UserUuidIntegerCompositePK(userId, groupId, UNASSIGNED_ENTITY_ID);
	}

	private final Long userId;
	private final UUID groupId;
	private final Integer entityId;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param groupId
	 *        the user ID
	 * @param entityId
	 *        the entity ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserUuidIntegerCompositePK(Long userId, UUID groupId, Integer entityId) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.groupId = requireNonNullArgument(groupId, "groupId");
		this.entityId = requireNonNullArgument(entityId, "entityId");
	}

	@Override
	public int compareTo(UserUuidIntegerCompositePK o) {
		if ( o == null ) {
			return 1;
		}

		int comparison = userId.compareTo(o.userId);
		if ( comparison != 0 ) {
			return comparison;
		}

		comparison = groupId.compareTo(o.groupId);
		if ( comparison != 0 ) {
			return comparison;
		}

		return entityId.compareTo(o.entityId);
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("u=").append(userId);
		buf.append(";g=").append(groupId);
		buf.append(";i=").append(entityId);
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		buf.append("userId=").append(userId);
		buf.append(", groupId=").append(groupId);
		buf.append(", entityId=").append(entityId);
	}

	@Override
	public UserUuidIntegerCompositePK clone() {
		return (UserUuidIntegerCompositePK) super.clone();
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, groupId, entityId);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof UserUuidIntegerCompositePK other) ) {
			return false;
		}
		return Objects.equals(userId, other.userId) && Objects.equals(groupId, other.groupId)
				&& Objects.equals(entityId, other.entityId);
	}

	/**
	 * Get the group ID.
	 *
	 * @return the user ID
	 */
	public final UUID getGroupId() {
		return groupId;
	}

	/**
	 * Get the entity ID.
	 *
	 * @return the entity ID
	 */
	public final Integer getEntityId() {
		return entityId;
	}

	@Override
	public final Long keyComponent1() {
		return userId;
	}

	@Override
	public final UUID keyComponent2() {
		return groupId;
	}

	@Override
	public final Integer keyComponent3() {
		return entityId;
	}

	@SuppressWarnings({ "BoxedPrimitiveEquality", "ReferenceEquality" })
	@Override
	public final boolean keyComponentIsAssigned(int index) {
		return switch (index) {
			case 0 -> userId != UNASSIGNED_USER_ID;
			case 1 -> groupId != UNASSIGNED_GROUP_ID;
			case 2 -> entityId != UNASSIGNED_ENTITY_ID;
			default -> CompositeKey3.super.keyComponentIsAssigned(index);
		};
	}

	/**
	 * Test if the group ID is assigned.
	 *
	 * @return {@literal true} if the group ID value is assigned,
	 *         {@literal false} if it is considered "not a value"
	 */
	public final boolean groupIdIsAssigned() {
		return keyComponentIsAssigned(1);
	}

	/**
	 * Test if the entity ID is assigned.
	 *
	 * @return {@literal true} if the entity ID value is assigned,
	 *         {@literal false} if it is considered "not a value"
	 */
	public final boolean entityIdIsAssigned() {
		return keyComponentIsAssigned(2);
	}

	@SuppressWarnings({ "unchecked", "TypeParameterUnusedInFormals" })
	@Override
	public <T> T keyComponentValue(int index, Object val) {
		try {
			if ( index == 0 ) {
				return switch (val) {
					case null -> (T) UNASSIGNED_USER_ID;
					case Long n -> (T) n;
					case Number n -> (T) Long.valueOf(n.longValue());
					default -> (T) Long.valueOf(val.toString());
				};
			} else if ( index == 1 ) {
				return switch (val) {
					case null -> (T) UNASSIGNED_GROUP_ID;
					case UUID u -> (T) u;
					default -> (T) UUID.fromString(val.toString());
				};
			} else if ( index == 2 ) {
				return switch (val) {
					case null -> (T) UNASSIGNED_ENTITY_ID;
					case Integer n -> (T) n;
					case Number n -> (T) Integer.valueOf(n.intValue());
					default -> (T) Integer.valueOf(val.toString());
				};
			}
		} catch ( NumberFormatException e ) {
			throw new IllegalArgumentException(
					"Key component %d does not support value %s.".formatted(index, val));
		}
		throw new IllegalArgumentException("Key component %d out of range.".formatted(index));
	}

	@Override
	public UserUuidIntegerCompositePK createKey(CompositeKey template, Object... components) {
		Object v1 = (components != null && components.length > 0 ? components[0]
				: template != null ? template.keyComponent(0) : null);
		Object v2 = (components != null && components.length > 1 ? components[1]
				: template != null ? template.keyComponent(1) : null);
		Object v3 = (components != null && components.length > 2 ? components[2]
				: template != null ? template.keyComponent(2) : null);
		Long k1 = (v1 != null ? keyComponentValue(0, v1) : UNASSIGNED_USER_ID);
		UUID k2 = (v2 != null ? keyComponentValue(1, v2) : UNASSIGNED_GROUP_ID);
		Integer k3 = (v3 != null ? keyComponentValue(2, v3) : UNASSIGNED_ENTITY_ID);
		return new UserUuidIntegerCompositePK(k1, k2, k3);
	}

}
