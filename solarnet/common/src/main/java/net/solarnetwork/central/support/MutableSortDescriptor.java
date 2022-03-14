/* ==================================================================
 * MutableSortDescriptor.java - Dec 3, 2013 6:58:21 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import java.io.Serializable;
import net.solarnetwork.central.domain.SortDescriptor;

/**
 * Mutable implementation of {@link SortDescriptor}.
 * 
 * <p>
 * The {@code descending} property defaults to <em>false</em>.
 * </p>
 * 
 * @author matt
 * @version 1.2
 */
public class MutableSortDescriptor implements SortDescriptor, Serializable {

	private static final long serialVersionUID = 4099205361786294490L;

	private String sortKey;
	private boolean descending = false;

	@Override
	public String getSortKey() {
		return sortKey;
	}

	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	@Override
	public boolean isDescending() {
		return descending;
	}

	public void setDescending(boolean descending) {
		this.descending = descending;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.1
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (descending ? 1231 : 1237);
		result = prime * result + ((sortKey == null) ? 0 : sortKey.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.1
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof MutableSortDescriptor) ) {
			return false;
		}
		MutableSortDescriptor other = (MutableSortDescriptor) obj;
		if ( descending != other.descending ) {
			return false;
		}
		if ( sortKey == null ) {
			if ( other.sortKey != null ) {
				return false;
			}
		} else if ( !sortKey.equals(other.sortKey) ) {
			return false;
		}
		return true;
	}

}
