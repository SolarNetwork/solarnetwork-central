/* ==================================================================
 * BasicLocationSourceDatePK.java - 25/03/2020 10:21:33 am
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

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Basic primary key composed of a location ID and source ID and date.
 *
 * @author matt
 * @version 2.0
 * @since 2.2
 */
@SuppressWarnings("MissingImplementsComparable")
public class BasicLocationSourceDatePK extends BasicLocationSourcePK implements Serializable, Cloneable {

	@Serial
	private static final long serialVersionUID = 8552882329808995996L;

	private Instant created;

	/**
	 * Default constructor.
	 */
	public BasicLocationSourceDatePK() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @param created
	 *        the creation date
	 */
	public BasicLocationSourceDatePK(Long locationId, String sourceId, Instant created) {
		super();
		setLocationId(locationId);
		setSourceId(sourceId);
		setCreated(created);
	}

	@Override
	public BasicLocationSourceDatePK clone() {
		return (BasicLocationSourceDatePK) super.clone();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(created);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) || !(obj instanceof BasicLocationSourceDatePK other) ) {
			return false;
		}
		if ( created == null ) {
			return other.created == null;
		}
		return created.equals(other.created);
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		super.populateIdValue(buf);
		buf.append(";c=");
		if ( created != null ) {
			buf.append(created);
		}
	}

	/**
	 * Compare two {@code GeneralLocationDautumPK} objects.
	 *
	 * <p>
	 * Keys are ordered based on:
	 * </p>
	 *
	 * <ol>
	 * <li>locationId</li>
	 * <li>sourceId</li>
	 * <li>created</li>
	 * </ol>
	 *
	 * {@literal null} values will be sorted before non-{@literal null} values.
	 *
	 * @param o
	 *        the object to compare to
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or l to, or greater than the specified
	 *         object.
	 */
	public int compareTo(BasicLocationSourceDatePK o) {
		int comparison = super.compareTo(o);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.created == null ) {
			return 1;
		} else if ( created == null ) {
			return -1;
		}
		return created.compareTo(o.created);
	}

	/**
	 * Populate a string builder with a friendly string value.
	 *
	 * <p>
	 * This method is called from {@link #toString()}. Extending classes can add
	 * more data as necessary. The buffer will be initially empty when invoked.
	 * </p>
	 *
	 * @param buf
	 *        the buffer to populate
	 */
	@Override
	protected void populateStringValue(StringBuilder buf) {
		super.populateStringValue(buf);
		if ( created != null ) {
			if ( !buf.isEmpty() ) {
				buf.append(", ");
			}
			buf.append("created=");
			buf.append(created);
		}
	}

	public Instant getCreated() {
		return created;
	}

	public void setCreated(Instant created) {
		this.created = created;
	}

}
