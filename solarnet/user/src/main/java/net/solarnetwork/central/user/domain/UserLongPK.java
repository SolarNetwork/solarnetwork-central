/* ==================================================================
 * UserLongPK.java - 3/06/2020 3:58:50 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import java.io.Serial;
import net.solarnetwork.central.domain.CompositeKey;
import net.solarnetwork.central.domain.CompositeKey2;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;

/**
 * Primary key based on a user ID and another {@code Long} ID.
 * 
 * @author matt
 * @version 1.1
 * @since 2.2
 */
public class UserLongPK implements UserRelatedCompositeKey<UserLongPK>, CompositeKey2<Long, Long> {

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

	@Serial
	private static final long serialVersionUID = -4475927214213411061L;

	private Long id;
	private Long userId;

	/**
	 * Default constructor.
	 */
	public UserLongPK() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param userId
	 *        the user ID
	 * @param id
	 *        the ID
	 */
	public UserLongPK(Long userId, Long id) {
		super();
		this.id = id;
		this.userId = userId;
	}

	/**
	 * Compare two {@code UserLongPK} objects. Keys are ordered based on:
	 * 
	 * <ol>
	 * <li>userId</li>
	 * <li>id</li>
	 * </ol>
	 * 
	 * {@literal null} values will be sorted before non-{@literal null} values.
	 */
	@Override
	public int compareTo(UserLongPK o) {
		if ( o == null ) {
			return 1;
		}
		if ( o.userId == null ) {
			return 1;
		} else if ( userId == null ) {
			return -1;
		}
		int comparison = userId.compareTo(o.userId);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.id == null ) {
			return 1;
		} else if ( id == null ) {
			return -1;
		}
		return id.compareTo(o.id);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserLongPK{");
		if ( userId != null ) {
			builder.append("userId=");
			builder.append(userId);
			builder.append(", ");
		}
		if ( id != null ) {
			builder.append("id=");
			builder.append(id);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		UserLongPK other = (UserLongPK) obj;
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		} else if ( !id.equals(other.id) ) {
			return false;
		}
		if ( userId == null ) {
			if ( other.userId != null ) {
				return false;
			}
		} else if ( !userId.equals(other.userId) ) {
			return false;
		}
		return true;
	}

	@Override
	protected UserLongPK clone() {
		try {
			return (UserLongPK) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// shouldn't get here
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
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
	public UserLongPK createKey(CompositeKey template, Object... components) {
		Object v1 = (components != null && components.length > 0 ? components[0]
				: template != null ? template.keyComponent(0) : null);
		Object v2 = (components != null && components.length > 1 ? components[1]
				: template != null ? template.keyComponent(1) : null);
		Long k1 = (v1 != null ? keyComponentValue(0, v1) : UNASSIGNED_USER_ID);
		Long k2 = (v2 != null ? keyComponentValue(1, v2) : UNASSIGNED_ENTITY_ID);
		return new UserLongPK(k1, k2);
	}

	@Override
	public final Long keyComponent1() {
		return userId;
	}

	@Override
	public final Long keyComponent2() {
		return id;
	}

	@Override
	public final boolean keyComponentIsAssigned(int index) {
		return switch (index) {
			case 0 -> userId != null && userId != UNASSIGNED_USER_ID;
			case 1 -> id != null && id != UNASSIGNED_ENTITY_ID;
			default -> CompositeKey2.super.keyComponentIsAssigned(index);
		};
	}

	/**
	 * Get the ID.
	 * 
	 * @return the ID
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Set the ID.
	 * 
	 * @param id
	 *        the ID to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Set the uesr ID.
	 * 
	 * @param userId
	 *        the user ID to set
	 */
	public void setUserId(Long userId) {
		this.userId = userId;
	}

}
