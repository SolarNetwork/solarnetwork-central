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

import java.io.Serializable;
import java.util.Objects;
import net.solarnetwork.central.domain.BasePK;

/**
 * Basic primary key composed of a location ID and source ID.
 * 
 * @author matt
 * @version 1.0
 * @since 2.2
 */
public class BasicLocationSourcePK extends BasePK implements Serializable, Cloneable {

	private static final long serialVersionUID = 5041677194630502340L;

	private Long locationId;
	private String sourceId;

	/**
	 * Default constructor.
	 */
	public BasicLocationSourcePK() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 */
	public BasicLocationSourcePK(Long locationId, String sourceId) {
		super();
		this.locationId = locationId;
		this.sourceId = sourceId;
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("n=");
		if ( locationId != null ) {
			buf.append(locationId);
		}
		buf.append(";s=");
		if ( sourceId != null ) {
			buf.append(sourceId);
		}
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		if ( locationId != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("locationId=");
			buf.append(locationId);
		}
		if ( sourceId != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("sourceId=");
			buf.append(sourceId);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(locationId, sourceId);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof BasicLocationSourcePK) ) {
			return false;
		}
		BasicLocationSourcePK other = (BasicLocationSourcePK) obj;
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
	 * {@literal null} values will be sorted before non-{@literal null} values.
	 * 
	 * @param o
	 *        the object to compare to
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 */
	public int compareTo(BasicLocationSourcePK o) {
		if ( o == null ) {
			return 1;
		}
		Long oLocationId = o.getLocationId();
		Long locationId = getLocationId();
		if ( oLocationId == null ) {
			return 1;
		} else if ( locationId == null ) {
			return -1;
		}
		int comparison = locationId.compareTo(oLocationId);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.sourceId == null ) {
			return 1;
		} else if ( sourceId == null ) {
			return -1;
		}
		return sourceId.compareToIgnoreCase(o.sourceId);
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
