/* ==================================================================
 * BasicObjectIdentity.java - 11/04/2018 6:59:05 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

/**
 * A basic, immutable implementation of {@link Identity}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.38
 */
public class BasicObjectIdentity<PK extends Comparable<PK>> implements Identity<PK> {

	private final PK id;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID to use
	 */
	public BasicObjectIdentity(PK id) {
		super();
		this.id = id;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
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
	 * Test if two {@code BasicObjectIdentity} objects have the same
	 * {@link #getId()} value.
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		@SuppressWarnings("unchecked")
		BasicObjectIdentity<PK> other = (BasicObjectIdentity<PK>) obj;
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		} else if ( !id.equals(other.id) ) {
			return false;
		}
		return true;
	}

	/**
	 * Compare based on the {@code id}, with {@literal null} values ordered
	 * before non-{@literal null} values.
	 */
	@Override
	public int compareTo(PK o) {
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

	@Override
	public PK getId() {
		return id;
	}

}
