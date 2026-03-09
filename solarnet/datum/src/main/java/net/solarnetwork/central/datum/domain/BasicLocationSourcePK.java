/* ==================================================================
 * BasicLocationSourcePK.java - 25/03/2020 10:19:04 am
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

package net.solarnetwork.central.datum.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.BasePK;

/**
 * Basic primary key composed of a location ID and source ID.
 *
 * @author matt
 * @version 1.0
 * @since 2.2
 */
@SuppressWarnings("MissingImplementsComparable")
public class BasicLocationSourcePK extends BasePK implements Serializable, Cloneable {

	@Serial
	private static final long serialVersionUID = 5041677194630502340L;

	private final Long locationId;
	private final String sourceId;

	/**
	 * Constructor.
	 *
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BasicLocationSourcePK(Long locationId, String sourceId) {
		super();
		this.locationId = requireNonNullArgument(locationId, "locationId");
		this.sourceId = requireNonNullArgument(sourceId, "sourceId");
	}

	@Override
	public BasicLocationSourcePK clone() {
		return (BasicLocationSourcePK) super.clone();
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("n=");
		buf.append(locationId);
		buf.append(";s=");
		buf.append(sourceId);
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		if ( !buf.isEmpty() ) {
			buf.append(", ");
		}
		buf.append("locationId=");
		buf.append(locationId);
		buf.append(", sourceId=");
		buf.append(sourceId);
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
		if ( (obj == null) || !(obj instanceof BasicLocationSourcePK other) ) {
			return false;
		}
		return Objects.equals(locationId, other.locationId) && Objects.equals(sourceId, other.sourceId);
	}

	/**
	 * Compare two {@code BasicLocationSourcePK} objects.
	 *
	 * <p>
	 * Keys are ordered based on:
	 * </p>
	 *
	 * <ol>
	 * <li>locationId</li>
	 * <li>sourceId</li>
	 * </ol>
	 *
	 * {@code null} values will be sorted before non-{@code null} values.
	 *
	 * @param o
	 *        the object to compare to
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or l to, or greater than the specified
	 *         object.
	 */
	public int compareTo(@Nullable BasicLocationSourcePK o) {
		if ( o == null ) {
			return 1;
		}
		int comparison = locationId.compareTo(o.locationId);
		if ( comparison != 0 ) {
			return comparison;
		}
		return sourceId.compareToIgnoreCase(o.sourceId);
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
	 * Get the source ID.
	 *
	 * @return the source ID
	 */
	public final String getSourceId() {
		return sourceId;
	}

}
