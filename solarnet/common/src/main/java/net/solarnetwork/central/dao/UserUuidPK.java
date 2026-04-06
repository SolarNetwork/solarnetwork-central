/* ==================================================================
 * UserUuidPK.java - 10/11/2018 7:23:06 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Primary key based on a user ID and a UUID.
 *
 * @author matt
 * @version 1.1
 * @since 2.0
 */
public class UserUuidPK implements Serializable, Cloneable, Comparable<UserUuidPK> {

	@Serial
	private static final long serialVersionUID = -235030587630636014L;

	private @Nullable UUID id;
	private @Nullable Long userId;

	/**
	 * Default constructor.
	 */
	public UserUuidPK() {
		super();
	}

	/**
	 * Construct with values.
	 *
	 * @param userId
	 *        the user ID
	 * @param id
	 *        the UUID
	 */
	public UserUuidPK(@Nullable Long userId, @Nullable UUID id) {
		super();
		this.id = id;
		this.userId = userId;
	}

	/**
	 * Compare two {@code UserUuidPK} objects. Keys are ordered based on:
	 *
	 * <ol>
	 * <li>userId</li>
	 * <li>id</li>
	 * </ol>
	 *
	 * {@code null} values will be sorted before non-{@code null} values.
	 */
	@Override
	public int compareTo(@Nullable UserUuidPK o) {
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
		builder.append("UserUuidPK{");
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
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof UserUuidPK other) ) {
			return false;
		}
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		} else if ( !id.equals(other.id) ) {
			return false;
		}
		if ( userId == null ) {
			return other.userId == null;
		}
		return userId.equals(other.userId);
	}

	@Override
	public UserUuidPK clone() {
		try {
			return (UserUuidPK) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// shouldn't get here
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the ID.
	 * 
	 * @return the ID
	 */
	public final @Nullable UUID getId() {
		return id;
	}

	/**
	 * Test if an ID is available.
	 * 
	 * @return {@code true} if an {@code id} value is available
	 * @since 1.1
	 */
	public boolean hasId() {
		return id != null;
	}

	/**
	 * Get the ID.
	 * 
	 * <p>
	 * This is a nullability shortcut, for example after {@link #hasId()}
	 * returns {@code true}.
	 * </p>
	 * 
	 * @return the ID (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	public final UUID id() {
		return id;
	}

	/**
	 * Set the ID.
	 * 
	 * @param id
	 *        the ID to set
	 */
	public final void setId(@Nullable UUID id) {
		this.id = id;
	}

	/**
	 * Get the user ID.
	 * 
	 * @return the user ID to set
	 */
	public final @Nullable Long getUserId() {
		return userId;
	}

	/**
	 * Set the user ID.
	 * 
	 * @param userId
	 *        the user ID to set
	 */
	public final void setUserId(@Nullable Long userId) {
		this.userId = userId;
	}

}
