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
import java.util.Objects;
import net.solarnetwork.domain.Identity;

/**
 * Base implementation of a String-based
 * {@link net.solarnetwork.domain.Identity}.
 *
 * @author matt
 * @version 2.0
 */
public abstract class BaseStringIdentity implements Cloneable, Serializable, Identity<String> {

	@Serial
	private static final long serialVersionUID = -2979855366308936650L;

	private String id = null;

	@Override
	public BaseStringIdentity clone() {
		try {
			return (BaseStringIdentity) super.clone();
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
	 * Test if two BaseStringEntity objects have the same {@link #getId()}
	 * value.
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
		BaseStringIdentity other = (BaseStringIdentity) obj;
		return Objects.equals(id, other.getId());
	}

	/**
	 * Get the ID.
	 * 
	 * @return the id
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * Set the ID.
	 * 
	 * @param id
	 *        the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

}
