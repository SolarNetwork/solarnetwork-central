/* ==================================================================
 * UserNode.java - Jan 29, 2010 11:46:09 AM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import net.solarnetwork.central.domain.BaseEntity;
import net.solarnetwork.central.domain.SolarNode;

/**
 * A solar node with user details.
 * 
 * <p>This object augments a {@link SolarNode} with additional
 * information that nodes themselves are not concerned with, but users
 * are. This allows the {@link SolarNode} object to remain lightweight.</p>
 * 
 * @author matt
 * @version $Id$
 */
public class UserNode extends BaseEntity {

	private static final long serialVersionUID = -2623340433468823234L;

	private String description;
	private String name;
	
	private User user;
	private SolarNode node;
	
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the user
	 */
	public User getUser() {
		return user;
	}
	
	/**
	 * @param user the user to set
	 */
	public void setUser(User user) {
		this.user = user;
	}
	
	/**
	 * @return the node
	 */
	public SolarNode getNode() {
		return node;
	}
	
	/**
	 * @param node the node to set
	 */
	public void setNode(SolarNode node) {
		this.node = node;
	}

}
