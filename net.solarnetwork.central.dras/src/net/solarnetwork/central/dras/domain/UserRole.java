/* ==================================================================
 * UserRole.java - Jun 5, 2011 11:39:10 AM
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

package net.solarnetwork.central.dras.domain;

import net.solarnetwork.central.domain.BaseStringIdentity;

/**
 * A user role.
 * 
 * @author matt
 * @version $Revision$
 */
public class UserRole extends BaseStringIdentity {

	private static final long serialVersionUID = 6009666381728995154L;

	private String description;
	
	/**
	 * Default constructor.
	 */
	public UserRole() {
		super();
	}
	
	/**
	 * Construct with a role name (ID).
	 * 
	 * @param rolename the role name
	 */
	public UserRole(String roleName) {
		super();
		setId(roleName);
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

}
