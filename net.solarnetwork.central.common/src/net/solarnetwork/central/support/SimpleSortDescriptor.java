/* ==================================================================
 * SimpleSortDescriptor.java - Jun 10, 2011 7:09:23 PM
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

package net.solarnetwork.central.support;

import net.solarnetwork.central.domain.SortDescriptor;

/**
 * Implementation of {@link SortDescriptor}.
 * 
 * @author matt
 * @version 1.2
 */
public class SimpleSortDescriptor implements SortDescriptor {

	private final String sortKey;
	private final boolean descending;

	/**
	 * Construct with a sort key.
	 * 
	 * <p>
	 * Ascending order will be used.
	 * </p>
	 * 
	 * @param sortKey
	 *        the sort key
	 */
	public SimpleSortDescriptor(String sortKey) {
		this(sortKey, true);
	}

	public SimpleSortDescriptor(String sortKey, boolean descending) {
		super();
		this.sortKey = sortKey;
		this.descending = descending;
	}

	@Override
	public String getSortKey() {
		return sortKey;
	}

	@Override
	public boolean isDescending() {
		return descending;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @sine 1.2
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
	 * @sine 1.2
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof SimpleSortDescriptor) ) {
			return false;
		}
		SimpleSortDescriptor other = (SimpleSortDescriptor) obj;
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
