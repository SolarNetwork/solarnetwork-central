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
 * Basic implementation of a Long and Integer composite key.
 * 
 * @author matt
 * @version 1.1
 */
public final class LongIntegerCompositePK extends BasePK implements Serializable, Cloneable,
		Comparable<LongIntegerCompositePK>, CompositeKey2<Long, Integer> {

	private static final long serialVersionUID = 8423043492448548574L;

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
	 * @param groupId
	 *        the ID of the group to use
	 * @return the new key instance
	 */
	public static LongIntegerCompositePK unassignedEntityIdKey(Long groupId) {
		return new LongIntegerCompositePK(groupId, UNASSIGNED_ENTITY_ID);
	}

	private final Long groupId;
	private final Integer entityId;

	/**
	 * Constructor.
	 * 
	 * @param groupId
	 *        the user ID
	 * @param entityId
	 *        the entity ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LongIntegerCompositePK(Long groupId, Integer entityId) {
		super();
		this.groupId = requireNonNullArgument(groupId, "groupId");
		this.entityId = requireNonNullArgument(entityId, "entityId");
	}

	@Override
	public int compareTo(LongIntegerCompositePK o) {
		if ( o == null ) {
			return 1;
		}

		int comparison = groupId.compareTo(o.groupId);
		if ( comparison != 0 ) {
			return comparison;
		}

		return entityId.compareTo(o.entityId);
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("u=").append(groupId);
		buf.append(";i=").append(entityId);
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		buf.append("groupId=").append(groupId);
		buf.append(", entityId=").append(entityId);
	}

	@Override
	protected UserLongCompositePK clone() {
		return (UserLongCompositePK) super.clone();
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, entityId);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof LongIntegerCompositePK) ) {
			return false;
		}
		LongIntegerCompositePK other = (LongIntegerCompositePK) obj;
		return Objects.equals(groupId, other.groupId) && Objects.equals(entityId, other.entityId);
	}

	/**
	 * Get a short identifier string.
	 * 
	 * <p>
	 * The format of the returned string is {@code (groupId,entityId)}.
	 * </p>
	 * 
	 * @return the identifier
	 */
	public String ident() {
		return String.format("(%d,%d)", groupId, entityId);
	}

	/**
	 * Get the user ID.
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
		return getGroupId();
	}

	@Override
	public final Integer keyComponent2() {
		return getEntityId();
	}

	@Override
	public final boolean keyComponentIsAssigned(int index) {
		if ( index == 0 ) {
			return groupId != UNASSIGNED_GROUP_ID;
		} else if ( index == 1 ) {
			return entityId != UNASSIGNED_ENTITY_ID;
		}
		return CompositeKey2.super.keyComponentIsAssigned(index);
	}

	/**
	 * Test if the group ID is assigned.
	 * 
	 * @return {@literal true} if the group ID value is assigned,
	 *         {@literal false} if it is considered "not a value"
	 */
	public final boolean groupIdIsAssigned() {
		return keyComponentIsAssigned(0);
	}

	/**
	 * Test if the entity ID is assigned.
	 * 
	 * @return {@literal true} if the entity ID value is assigned,
	 *         {@literal false} if it is considered "not a value"
	 */
	public final boolean entityIdIsAssigned() {
		return keyComponentIsAssigned(1);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T keyComponentValue(int index, Object val) {
		try {
			if ( index == 0 ) {
				if ( val == null ) {
					return (T) UNASSIGNED_GROUP_ID;
				} else if ( val instanceof Long n ) {
					return (T) n;
				} else if ( val instanceof Number n ) {
					return (T) Long.valueOf(n.longValue());
				} else {
					return (T) Long.valueOf(val.toString());
				}
			} else if ( index == 1 ) {
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
	public LongIntegerCompositePK createKey(CompositeKey template, Object... components) {
		Object v1 = (components != null && components.length > 0 ? components[0]
				: template != null ? template.keyComponent(0) : null);
		Object v2 = (components != null && components.length > 1 ? components[1]
				: template != null ? template.keyComponent(1) : null);
		Long k1 = (v1 != null ? keyComponentValue(0, v1) : UNASSIGNED_GROUP_ID);
		Integer k2 = (v2 != null ? keyComponentValue(1, v2) : UNASSIGNED_ENTITY_ID);
		return new LongIntegerCompositePK(k1, k2);
	}

}
