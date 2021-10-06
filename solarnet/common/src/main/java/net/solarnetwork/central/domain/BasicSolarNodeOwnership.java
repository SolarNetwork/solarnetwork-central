/* ==================================================================
 * BasicSolarNodeOwnership.java - 6/10/2021 8:50:17 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import java.io.Serializable;
import java.util.Objects;
import net.solarnetwork.domain.Differentiable;

/**
 * Basic implementation of {@link SolarNodeOwnership}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicSolarNodeOwnership
		implements Serializable, SolarNodeOwnership, Differentiable<SolarNodeOwnership> {

	private static final long serialVersionUID = -1018957806985468904L;

	private final Long nodeId;
	private final Long userId;
	private final boolean requiresAuthorization;
	private final boolean archived;

	/**
	 * Create a new ownership instance.
	 * 
	 * <p>
	 * The {@code requiresAuthorization} and {@code archived} properties will be
	 * set to {@literal false}.
	 * </p>
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param userId
	 *        the user ID
	 * @return the new ownership
	 * @throws NullPointerException
	 *         if {@code nodeId} or {@code userId} is {@literal null}
	 */
	public static BasicSolarNodeOwnership ownershipFor(Long nodeId, Long userId) {
		return new BasicSolarNodeOwnership(nodeId, userId, false, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param userId
	 *        the owner user ID
	 * @param requiresAuthorization
	 *        {@literal true} if authorization is required to access the node's
	 *        data
	 * @param archived
	 *        {@literal true} if the node has been archived
	 * @throws NullPointerException
	 *         if {@code nodeId} or {@code userId} is {@literal null}
	 */
	public BasicSolarNodeOwnership(Long nodeId, Long userId, boolean requiresAuthorization,
			boolean archived) {
		super();
		this.nodeId = Objects.requireNonNull(nodeId, "The nodeId argument must not be null.");
		this.userId = Objects.requireNonNull(userId, "The userId argument must not be null.");
		this.requiresAuthorization = requiresAuthorization;
		this.archived = archived;
	}

	/**
	 * Test if the properties of another object are the same as in this
	 * instance.
	 * 
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(SolarNodeOwnership other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(nodeId, other.getNodeId())
				&& Objects.equals(userId, other.getUserId())
				&& Objects.equals(requiresAuthorization, other.isRequiresAuthorization())
				&& Objects.equals(archived, other.isArchived());
		// @formatter:on
	}

	@Override
	public boolean differsFrom(SolarNodeOwnership other) {
		return !isSameAs(other);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SolarNodeOwnership{");
		if ( nodeId != null ) {
			builder.append("nodeId=");
			builder.append(nodeId);
			builder.append(", ");
		}
		if ( userId != null ) {
			builder.append("userId=");
			builder.append(userId);
			builder.append(", ");
		}
		builder.append(requiresAuthorization ? "private" : "public");
		if ( archived ) {
			builder.append(",archived");
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeId);
	}

	/**
	 * Compare node ownership.
	 * 
	 * <p>
	 * This compares <b>only</b> by node ID.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof BasicSolarNodeOwnership) ) {
			return false;
		}
		BasicSolarNodeOwnership other = (BasicSolarNodeOwnership) obj;
		return Objects.equals(nodeId, other.nodeId);
	}

	@Override
	public Long getNodeId() {
		return nodeId;
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	@Override
	public boolean isRequiresAuthorization() {
		return requiresAuthorization;
	}

	@Override
	public boolean isArchived() {
		return archived;
	}

}
