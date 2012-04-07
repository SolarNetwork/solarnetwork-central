/* ==================================================================
 * UserFilter.java - Jun 8, 2011 4:16:00 PM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.dao;

import java.util.List;

/**
 * Filter for User.
 * 
 * @author matt
 * @version $Revision$
 */
public interface UserFilter extends LocationBoxFilter {

	/**
	 * The username to filter on.
	 * 
	 * @return the username
	 */
	String getUsername();
	
	/**
	 * Like {@link #getUsername()} but exact match only.
	 * @return unique ID
	 */
	String getUniqueId();
	
	/**
	 * A set of roles to filter by.
	 * @return the roles
	 */
	List<String> getRoles();
	
	/**
	 * The UserGroup to filter by, that is only return users that are a
	 * member of any group in this list.
	 * 
	 * @return the UserGroup IDs
	 */
	List<Long> getUserGroups();
	
	/**
	 * Filter by the <em>enabled</em> flag.
	 * 
	 * @return boolean
	 */
	Boolean getEnabled();
	
}
