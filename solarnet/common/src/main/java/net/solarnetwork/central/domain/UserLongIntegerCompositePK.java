/* ==================================================================
 * LongIntegerCompositePK.java - 6/08/2023 9:57:28 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

/**
 * Basic implementation of a Long, Long, Integer composite key.
 * 
 * @author matt
 * @version 1.1
 */
public final class UserLongIntegerCompositePK extends BasePK implements Serializable, Cloneable,
		Comparable<UserLongIntegerCompositePK>, CompositeKey3<Long, Long, Integer>, UserIdRelated {

	private static final long serialVersionUID = -2993287060057007236L;

	/**
	 * A special "not a value" instance to be used for generated user ID values
	 * yet to be generated.
	 */
	public static final Long UNASSIGNED_USER_ID = Long.MIN_VALUE;

	/**
	 * A special "not a value" instance to be used for generated group ID values
	 * yet to be generated.
	 */
	public static final Long UNASSIGNED_GROUP_ID = Long.MIN_VALUE;

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
	public static UserLongIntegerCompositePK unassignedEntityIdKey(Long userId, Long groupId) {
		return new UserLongIntegerCompositePK(userId, groupId, UNASSIGNED_ENTITY_ID);
	}

	private final Long userId;
	private final Long groupId;
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
	public UserLongIntegerCompositePK(Long userId, Long groupId, Integer entityId) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.groupId = requireNonNullArgument(groupId, "groupId");
		this.entityId = requireNonNullArgument(entityId, "entityId");
	}

	@Override
	public int compareTo(UserLongIntegerCompositePK o) {
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
	protected UserLongCompositePK clone() {
		return (UserLongCompositePK) super.clone();
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
		if ( !(obj instanceof UserLongIntegerCompositePK) ) {
			return false;
		}
		UserLongIntegerCompositePK other = (UserLongIntegerCompositePK) obj;
		return Objects.equals(userId, other.userId) && Objects.equals(groupId, other.groupId)
				&& Objects.equals(entityId, other.entityId);
	}

	/**
	 * Get a short identifier string.
	 * 
	 * <p>
	 * The format of the returned string is {@code (userId,groupId,entityId)}.
	 * </p>
	 * 
	 * @return the identifier
	 */
	public String ident() {
		return String.format("(%d,%d,%d)", userId, groupId, entityId);
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
	 * Get the group ID.
	 * 
	 * @return the user ID
	 */
	public final Long getGroupId() {
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
		return getUserId();
	}

	@Override
	public final Long keyComponent2() {
		return getGroupId();
	}

	@Override
	public final Integer keyComponent3() {
		return getEntityId();
	}

	@Override
	public final boolean keyComponentIsAssigned(int index) {
		if ( index == 0 ) {
			return userId != UNASSIGNED_USER_ID;
		} else if ( index == 1 ) {
			return groupId != UNASSIGNED_GROUP_ID;
		} else if ( index == 2 ) {
			return entityId != UNASSIGNED_ENTITY_ID;
		}
		return CompositeKey3.super.keyComponentIsAssigned(index);
	}

	/**
	 * Test if the user ID is assigned.
	 * 
	 * @return {@literal true} if the user ID value is assigned,
	 *         {@literal false} if it is considered "not a value"
	 */
	public final boolean userIdIsAssigned() {
		return keyComponentIsAssigned(0);
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

	@SuppressWarnings("unchecked")
	@Override
	public <T> T keyComponentValue(int index, Object val) {
		try {
			if ( index == 0 || index == 1 ) {
				if ( val == null ) {
					return (T) UNASSIGNED_GROUP_ID;
				} else if ( val instanceof Long n ) {
					return (T) n;
				} else if ( val instanceof Number n ) {
					return (T) Long.valueOf(n.longValue());
				} else {
					return (T) Long.valueOf(val.toString());
				}
			} else if ( index == 2 ) {
				if ( val == null ) {
					return (T) UNASSIGNED_ENTITY_ID;
				} else if ( val instanceof Integer n ) {
					return (T) n;
				} else if ( val instanceof Number n ) {
					return (T) Integer.valueOf(n.intValue());
				} else {
					return (T) Integer.valueOf(val.toString());
				}
			}
		} catch ( NumberFormatException e ) {
			throw new IllegalArgumentException(
					"Key component %d does not support value %s.".formatted(index, val));
		}
		throw new IllegalArgumentException("Key component %d out of range.".formatted(index));
	}

	@Override
	public UserLongIntegerCompositePK createKey(CompositeKey template, Object... components) {
		Object v1 = (components != null && components.length > 0 ? components[0]
				: template != null ? template.keyComponent(0) : null);
		Object v2 = (components != null && components.length > 1 ? components[1]
				: template != null ? template.keyComponent(1) : null);
		Object v3 = (components != null && components.length > 2 ? components[2]
				: template != null ? template.keyComponent(2) : null);
		Long k1 = (v1 != null ? keyComponentValue(0, v1) : UNASSIGNED_USER_ID);
		Long k2 = (v2 != null ? keyComponentValue(1, v2) : UNASSIGNED_GROUP_ID);
		Integer k3 = (v3 != null ? keyComponentValue(2, v3) : UNASSIGNED_ENTITY_ID);
		return new UserLongIntegerCompositePK(k1, k2, k3);
	}

}
