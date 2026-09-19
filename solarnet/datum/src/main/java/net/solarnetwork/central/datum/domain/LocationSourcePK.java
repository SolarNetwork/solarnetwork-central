/* ==================================================================
 * LocationSourcePK.java - Oct 17, 2014 3:03:16 PM
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

package net.solarnetwork.central.datum.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Primary key based on a location ID and source ID.
 *
 * @author matt
 * @version 1.1
 */
public class LocationSourcePK
		implements Serializable, Cloneable, Comparable<LocationSourcePK>, ObjectSourcePK {

	@Serial
	private static final long serialVersionUID = 2535992672383477286L;

	private final Long locationId;
	private final String sourceId;

	/**
	 * Construct with values.
	 *
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public LocationSourcePK(Long locationId, String sourceId) {
		super();
		this.locationId = requireNonNullArgument(locationId, "locationId");
		this.sourceId = requireNonNullArgument(sourceId, "sourceId");
	}

	/**
	 * Compare two {@code LocationSourcePK} objects. Keys are ordered based on:
	 *
	 * <ol>
	 * <li>locationId</li>
	 * <li>sourceId</li>
	 * </ol>
	 *
	 * <em>Null</em> values will be sorted before non-{@code null} values.
	 */
	@Override
	public int compareTo(@Nullable LocationSourcePK o) {
		if ( o == null ) {
			return 1;
		}
		int comparison = locationId.compareTo(o.locationId);
		if ( comparison != 0 ) {
			return comparison;
		}
		return sourceId.compareToIgnoreCase(o.sourceId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LocationSourcePK{locationId=");
		builder.append(locationId);
		builder.append(", sourceId=");
		builder.append(sourceId);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(locationId, sourceId);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof LocationSourcePK other) ) {
			return false;
		}
		if ( !locationId.equals(other.locationId) ) {
			return false;
		}
		return sourceId.equals(other.sourceId);
	}

	@Override
	public LocationSourcePK clone() {
		try {
			return (LocationSourcePK) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// shouldn't get here
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the location ID.
	 *
	 * @return the location ID
	 */
	public final Long getLocationId() {
		return locationId;
	}

	/**
	 * Get the object ID.
	 *
	 * <p>
	 * This method is an alias for {@link #getLocationId()}.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public final Long getObjectId() {
		return getLocationId();
	}

	@Override
	public final String getSourceId() {
		return sourceId;
	}

}
