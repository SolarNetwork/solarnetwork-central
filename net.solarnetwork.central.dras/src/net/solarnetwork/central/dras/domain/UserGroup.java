/* ==================================================================
 * UserGroup.java - Jun 2, 2011 5:54:42 PM
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

import java.io.Serializable;

import net.solarnetwork.central.domain.SolarNodeGroup;

/**
 * A user group.
 * 
 * @author matt
 * @version $Revision$
 */
public class UserGroup extends SolarNodeGroup implements Member, Match, Cloneable, Serializable {

	private static final long serialVersionUID = 2454180095596592691L;

	private Boolean enabled;

	/**
	 * Default constructor.
	 */
	public UserGroup() {
		super();
	}
	
	/**
	 * Construct with ID.
	 * @param id the ID
	 */
	public UserGroup(Long id) {
		super();
		setId(id);
	}
	
	public Boolean getEnabled() {
		return enabled;
	}
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	
}
