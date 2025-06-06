/* ==================================================================
 * BaseIdentity.java - Jun 2, 2011 8:39:42 PM
 *
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

import java.io.Serial;
import java.io.Serializable;

/**
 * Base implementation of a Long-based {@link net.solarnetwork.domain.Identity}.
 *
 * @author matt
 * @version 1.1
 */
public abstract class BaseIdentity
		implements Cloneable, Serializable, net.solarnetwork.domain.Identity<Long> {

	@Serial
	private static final long serialVersionUID = -5979349641482303093L;

	private Long id = null;

	@Override
	public BaseIdentity clone() {
		try {
			return (BaseIdentity) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should never get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/**
	 * Test if two BaseEntity objects have the same {@link #getId()} value.
	 */
	@SuppressWarnings("EqualsGetClass")
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( (obj == null) || (getClass() != obj.getClass()) ) {
			return false;
		}
		BaseIdentity other = (BaseIdentity) obj;
		if ( id == null ) {
			return other.id == null;
		}
		return id.equals(other.id);
	}

	/**
	 * Compare based on the primary key, with {@literal null} values ordered
	 * before non-null values.
	 */
	@Override
	public int compareTo(Long o) {
		if ( id == null && o == null ) {
			return 0;
		}
		if ( id == null ) {
			return -1;
		}
		if ( o == null ) {
			return 1;
		}
		return id.compareTo(o);
	}

	/**
	 * Get the ID.
	 * 
	 * @return the id
	 */
	@Override
	public Long getId() {
		return id;
	}

	/**
	 * Set the ID.
	 * 
	 * @param id
	 *        the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

}
