/* ==================================================================
 * UserNodeInfo.java - 2 Aug 2026 6:52:12 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.domain.Unique;

/**
 * A basic user and node information record.
 * 
 * <p>
 * This record implements {@link Unique} based on just the {@code nodeId}.
 * Equality is similary based only on {@code nodeId}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "userId", "nodeId", "name", "idAndName", "description", "locationId", "timeZone",
		"requiresAuthorization", "created" })
public record UserNodeInfo(
// @formatter:off
		  Long nodeId
		, Long userId
		, @Nullable String name
		, @Nullable String description
		, Instant created
		, boolean requiresAuthorization
		, Long locationId
		, ZoneId timeZone
		// @formatter:on
) implements UserIdRelated, Unique<Long>, Serializable {

	/**
	 * Convert a {@link UserNode} into a {@link UserNodeInfo}.
	 * 
	 * @param userNode
	 *        the user node
	 * @return the info
	 */
	public static UserNodeInfo forUserNode(final UserNode userNode) {
		final SolarLocation nodeLocation = nonnull(userNode.getNodeLocation(), "Node location");
		ZoneId zone = null;
		try {
			zone = ZoneId.of(nodeLocation.getTimeZoneId());
		} catch ( Exception e ) {
			// ignore and continue
		}
		if ( zone == null ) {
			zone = ZoneOffset.UTC;
		}
		return new UserNodeInfo(userNode.id(), userNode.getUserId(), userNode.getName(),
				userNode.getDescription(), nonnull(userNode.getCreated(), "Created"),
				userNode.isRequiresAuthorization(), nodeLocation.id(), zone);
	}

	@JsonIgnore
	@Override
	public @Nullable Long getId() {
		return nodeId;
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof UserNodeInfo other) ) {
			return false;
		}
		return Objects.equals(nodeId, other.nodeId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeId);
	}

	/**
	 * Get the node ID and associated name, if available, as a string.
	 * 
	 * <p>
	 * If no name is available, this method returns just the node ID.
	 * </p>
	 *
	 * @return The node ID and name as a string.
	 */
	@JsonProperty
	public final String idAndName() {
		StringBuilder buf = new StringBuilder();
		buf.append(nodeId);
		if ( name != null && !name.isEmpty() ) {
			buf.append(" - ").append(name);
		}
		return buf.toString();
	}

}
