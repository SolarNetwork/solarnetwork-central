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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

	private static final long serialVersionUID = 3442524641711956994L;

	private final Long nodeId;
	private final Long userId;
	private final String country;
	private final ZoneId zone;
	private final boolean requiresAuthorization;
	private final boolean archived;

	/**
	 * Create a new ownership instance.
	 * 
	 * <p>
	 * The zone will be set to {@literal UTC}. The {@code requiresAuthorization}
	 * and {@code archived} properties will be set to {@literal false}.
	 * </p>
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param userId
	 *        the user ID
	 * @return the new ownership
	 * @throws IllegalArgumentException
	 *         if {@code nodeId} or {@code userId} is {@literal null}
	 */
	public static BasicSolarNodeOwnership ownershipFor(Long nodeId, Long userId) {
		return new BasicSolarNodeOwnership(nodeId, userId, null, ZoneOffset.UTC, false, false);
	}

	/**
	 * Create a new private ownership instance.
	 * 
	 * <p>
	 * The {@code requiresAuthorization} property will be set to
	 * {@literal true}. The {@code archived} property will be set to
	 * {@literal false}.
	 * </p>
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param userId
	 *        the user ID
	 * @param country
	 *        the country
	 * @param timeZoneId
	 *        the time zone ID
	 * @return the new ownership
	 * @throws IllegalArgumentException
	 *         if {@code nodeId} or {@code userId} is {@literal null}
	 */
	public static BasicSolarNodeOwnership ownershipFor(Long nodeId, Long userId, String country,
			String timeZoneId) {
		return new BasicSolarNodeOwnership(nodeId, userId, country,
				(timeZoneId != null ? ZoneId.of(timeZoneId) : ZoneOffset.UTC), false, false);
	}

	/**
	 * Create a new private ownership instance.
	 * 
	 * <p>
	 * The zone will be set to {@literal UTC}. The {@code requiresAuthorization}
	 * property will be set to {@literal true}. The {@code archived} property
	 * will be set to {@literal false}.
	 * </p>
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param userId
	 *        the user ID
	 * @return the new ownership
	 * @throws IllegalArgumentException
	 *         if {@code nodeId} or {@code userId} is {@literal null}
	 */
	public static BasicSolarNodeOwnership privateOwnershipFor(Long nodeId, Long userId) {
		return new BasicSolarNodeOwnership(nodeId, userId, null, ZoneOffset.UTC, true, false);
	}

	/**
	 * Create a new private ownership instance.
	 * 
	 * <p>
	 * The {@code requiresAuthorization} property will be set to
	 * {@literal true}. The {@code archived} property will be set to
	 * {@literal false}.
	 * </p>
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param userId
	 *        the user ID
	 * @param country
	 *        the country
	 * @param timeZoneId
	 *        the time zone ID
	 * @return the new ownership
	 * @throws IllegalArgumentException
	 *         if {@code nodeId} or {@code userId} is {@literal null}
	 */
	public static BasicSolarNodeOwnership privateOwnershipFor(Long nodeId, Long userId, String country,
			String timeZoneId) {
		return new BasicSolarNodeOwnership(nodeId, userId, country,
				(timeZoneId != null ? ZoneId.of(timeZoneId) : ZoneOffset.UTC), true, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param userId
	 *        the owner user ID
	 * @param country
	 *        the country code
	 * @param zone
	 *        the zone, or {@literal null} to use {@literal UTC}
	 * @param requiresAuthorization
	 *        {@literal true} if authorization is required to access the node's
	 *        data
	 * @param archived
	 *        {@literal true} if the node has been archived
	 * @throws IllegalArgumentException
	 *         if {@code nodeId} or {@code userId} is {@literal null}
	 */
	public BasicSolarNodeOwnership(Long nodeId, Long userId, String country, ZoneId zone,
			boolean requiresAuthorization, boolean archived) {
		super();
		this.nodeId = requireNonNullArgument(nodeId, "nodeId");
		this.userId = requireNonNullArgument(userId, "userId");
		this.country = country;
		this.zone = (zone != null ? zone : ZoneOffset.UTC);
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
				&& Objects.equals(country, other.getCountry())
				&& Objects.equals(zone, other.getZone())
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
		StringBuilder buf = new StringBuilder();
		buf.append("SolarNodeOwnership{");
		if ( nodeId != null ) {
			buf.append("nodeId=");
			buf.append(nodeId);
			buf.append(", ");
		}
		if ( userId != null ) {
			buf.append("userId=");
			buf.append(userId);
			buf.append(", ");
		}
		if ( country != null ) {
			buf.append("country=");
			buf.append(country);
			buf.append(", ");
		}
		buf.append("zone=").append(zone.getId()).append(", ");
		buf.append(requiresAuthorization ? "private" : "public");
		if ( archived ) {
			buf.append(", archived");
		}
		buf.append("}");
		return buf.toString();
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
	public String getCountry() {
		return country;
	}

	@Override
	public ZoneId getZone() {
		return zone;
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
