/* ==================================================================
 * UserLongCompositePK.java - 11/08/2022 9:50:38 am
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
import java.io.Serial;
import java.util.Objects;

/**
 * Immutable primary key for user-related entities using a Long entity key.
 *
 * @author matt
 * @version 1.2
 */
public final class UserLongCompositePK extends BasePK
		implements UserRelatedCompositeKey<UserLongCompositePK>, CompositeKey2<Long, Long> {

	@Serial
	private static final long serialVersionUID = 2537083574768869025L;

	/**
	 * A special "not a value" instance to be used for generated user ID values
	 * yet to be generated.
	 */
	public static final Long UNASSIGNED_USER_ID = Long.MIN_VALUE;

	/**
	 * A special "not a value" instance to be used for generated entity ID
	 * values yet to be generated.
	 */
	public static final Long UNASSIGNED_ENTITY_ID = Long.MIN_VALUE;

	/**
	 * Create a new instance using the "unassigned" entity ID value.
	 *
	 * @param userId
	 *        the ID of the user to use
	 * @return the new key instance
	 */
	public static UserLongCompositePK unassignedEntityIdKey(Long userId) {
		return new UserLongCompositePK(userId, UNASSIGNED_ENTITY_ID);
	}

	private final Long userId;
	private final Long entityId;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param entityId
	 *        the entity ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserLongCompositePK(Long userId, Long entityId) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.entityId = requireNonNullArgument(entityId, "entityId");
	}

	@Override
	public int compareTo(UserLongCompositePK o) {
		if ( o == null ) {
			return 1;
		}

		int comparison = userId.compareTo(o.userId);
		if ( comparison != 0 ) {
			return comparison;
		}

		return entityId.compareTo(o.entityId);
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("u=").append(userId);
		buf.append(";i=").append(entityId);
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		buf.append("userId=").append(userId);
		buf.append(", entityId=").append(entityId);
	}

	@Override
	public UserLongCompositePK clone() {
		return (UserLongCompositePK) super.clone();
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, entityId);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof UserLongCompositePK other) ) {
			return false;
		}
		return Objects.equals(userId, other.userId) && Objects.equals(entityId, other.entityId);
	}

	/**
	 * Get the entity ID.
	 *
	 * @return the entity ID
	 */
	public Long getEntityId() {
		return entityId;
	}

	@Override
	public Long keyComponent1() {
		return userId;
	}

	@Override
	public Long keyComponent2() {
		return entityId;
	}

	@SuppressWarnings({ "BoxedPrimitiveEquality", "ReferenceEquality" })
	@Override
	public boolean keyComponentIsAssigned(int index) {
		return switch (index) {
			case 0 -> userId != UNASSIGNED_USER_ID;
			case 1 -> entityId != UNASSIGNED_ENTITY_ID;
			default -> CompositeKey2.super.keyComponentIsAssigned(index);
		};
	}

	/**
	 * Test if the entity ID is assigned.
	 *
	 * @return {@literal true} if the entity ID value is assigned,
	 *         {@literal false} if it is considered "not a value"
	 */
	public boolean entityIdIsAssigned() {
		return keyComponentIsAssigned(1);
	}

	@SuppressWarnings({ "unchecked", "TypeParameterUnusedInFormals" })
	@Override
	public <T> T keyComponentValue(int index, Object val) {
		try {
			if ( index == 0 || index == 1 ) {
				return switch (val) {
					case null -> (T) UNASSIGNED_ENTITY_ID;
					case Long n -> (T) n;
					case Number n -> (T) Long.valueOf(n.longValue());
					default -> (T) Long.valueOf(val.toString());
				};
			}
		} catch ( NumberFormatException e ) {
			throw new IllegalArgumentException(
					"Key component %d does not support value %s.".formatted(index, val));
		}
		throw new IllegalArgumentException("Key component %d out of range.".formatted(index));
	}

	@Override
	public UserLongCompositePK createKey(CompositeKey template, Object... components) {
		Object v1 = (components != null && components.length > 0 ? components[0]
				: template != null ? template.keyComponent(0) : null);
		Object v2 = (components != null && components.length > 1 ? components[1]
				: template != null ? template.keyComponent(1) : null);
		Long k1 = (v1 != null ? keyComponentValue(0, v1) : UNASSIGNED_USER_ID);
		Long k2 = (v2 != null ? keyComponentValue(1, v2) : UNASSIGNED_ENTITY_ID);
		return new UserLongCompositePK(k1, k2);
	}

}
