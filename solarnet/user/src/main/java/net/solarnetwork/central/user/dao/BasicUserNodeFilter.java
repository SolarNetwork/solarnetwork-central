/* ==================================================================
 * BasicUserNodeFilter.java - 2 Aug 2026 10:00:01 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao;

import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.NameCriteria;
import net.solarnetwork.dao.OptimizedQueryCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of {@link UserNodeFilter}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicUserNodeFilter extends BasicCoreCriteria implements UserNodeFilter {

	private String @Nullable [] names;
	private boolean withoutTotalResultsCount = true;

	/**
	 * Constructor.
	 */
	public BasicUserNodeFilter() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param criteria
	 *        the criteria to copy
	 */
	public BasicUserNodeFilter(@Nullable PaginationCriteria criteria) {
		super(criteria);
	}

	@Override
	public void copyFrom(@Nullable PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria == null ) {
			return;
		}
		if ( criteria instanceof BasicUserNodeFilter c ) {
			setNames(c.names);
			setWithoutTotalResultsCount(c.withoutTotalResultsCount);
		} else {
			if ( criteria instanceof NameCriteria c ) {
				setNames(c.getNames());
			}
			if ( criteria instanceof OptimizedQueryCriteria c ) {
				setWithoutTotalResultsCount(c.isWithoutTotalResultsCount());
			}
		}
	}

	@Override
	public BasicUserNodeFilter clone() {
		return (BasicUserNodeFilter) super.clone();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(names);
		result = prime * result + Boolean.hashCode(withoutTotalResultsCount);
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) || !(obj instanceof BasicUserNodeFilter other) ) {
			return false;
		}
		// @formatter:off
		return Arrays.equals(names, other.names)
				&& withoutTotalResultsCount == other.withoutTotalResultsCount
				;
		// @formatter:on
	}

	/**
	 * Set the name.
	 * 
	 * @param name
	 *        the name to set
	 */
	public final void setName(@Nullable String name) {
		setNames(name != null ? new String[] { name } : null);
	}

	@Override
	public final String @Nullable [] getNames() {
		return names;
	}

	/**
	 * Set the names.
	 * 
	 * @param names
	 *        the names to set
	 */
	public final void setNames(String @Nullable [] names) {
		this.names = names;
	}

	@Override
	public final boolean isWithoutTotalResultsCount() {
		return withoutTotalResultsCount;
	}

	/**
	 * Toggle the total results count flag.
	 *
	 * @param withoutTotalResultsCount
	 *        the value to set
	 */
	public final void setWithoutTotalResultsCount(boolean withoutTotalResultsCount) {
		this.withoutTotalResultsCount = withoutTotalResultsCount;
	}

}
