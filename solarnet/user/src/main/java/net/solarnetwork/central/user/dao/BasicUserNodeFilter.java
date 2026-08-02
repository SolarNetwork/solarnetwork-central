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

import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of {@link UserNodeFilter}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicUserNodeFilter extends BasicCoreCriteria implements UserNodeFilter {

	private String @Nullable [] names;

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

	/**
	 * Set the name.
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(@Nullable String name) {
		setNames(name != null ? new String[] { name } : null);
	}

	@Override
	public String @Nullable [] getNames() {
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

}
