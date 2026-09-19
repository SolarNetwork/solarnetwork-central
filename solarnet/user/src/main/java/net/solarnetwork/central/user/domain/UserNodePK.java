/* ==================================================================
 * UserNodePK.java - Oct 3, 2014 6:47:25 AM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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
import java.io.Serializable;
import org.jspecify.annotations.Nullable;

/**
 * Primary key based on a user ID and node ID.
 *
 * @author matt
 * @version 1.0
 */
public class UserNodePK implements Serializable, Cloneable, Comparable<UserNodePK> {

	@Serial
	private static final long serialVersionUID = -2661140310545544324L;

	private @Nullable Long nodeId;
	private @Nullable Long userId;

	/**
	 * Default constructor.
	 */
	public UserNodePK() {
		super();
	}

	/**
	 * Construct with values.
	 *
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 */
	public UserNodePK(@Nullable Long userId, @Nullable Long nodeId) {
		super();
		this.nodeId = nodeId;
		this.userId = userId;
	}

	/**
	 * Compare two {@code UserNodePK} objects. Keys are ordered based on:
	 *
	 * <ol>
	 * <li>userId</li>
	 * <li>nodeId</li>
	 * </ol>
	 *
	 * <em>Null</em> values will be sorted before non-{@code null} values.
	 */
	@Override
	public int compareTo(@Nullable UserNodePK o) {
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
		if ( o.nodeId == null ) {
			return 1;
		} else if ( nodeId == null ) {
			return -1;
		}
		return nodeId.compareTo(o.nodeId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserNodePK{");
		if ( userId != null ) {
			builder.append("userId=");
			builder.append(userId);
			builder.append(", ");
		}
		if ( nodeId != null ) {
			builder.append("nodeId=");
			builder.append(nodeId);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof UserNodePK other) ) {
			return false;
		}
		if ( nodeId == null ) {
			if ( other.nodeId != null ) {
				return false;
			}
		} else if ( !nodeId.equals(other.nodeId) ) {
			return false;
		}
		if ( userId == null ) {
			return other.userId == null;
		}
		return userId.equals(other.userId);
	}

	@Override
	public UserNodePK clone() {
		try {
			return (UserNodePK) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// shouldn't get here
			throw new RuntimeException(e);
		}
	}

	public final @Nullable Long getNodeId() {
		return nodeId;
	}

	public final void setNodeId(@Nullable Long nodeId) {
		this.nodeId = nodeId;
	}

	public final @Nullable Long getUserId() {
		return userId;
	}

	public final void setUserId(@Nullable Long userId) {
		this.userId = userId;
	}

}
