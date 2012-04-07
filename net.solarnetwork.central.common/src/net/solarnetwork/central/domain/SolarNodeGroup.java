/* ==================================================================
 * SolarNodeGroup.java - Apr 30, 2011 10:42:39 AM
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

package net.solarnetwork.central.domain;

import java.io.Serializable;

import org.joda.time.DateTime;

/**
 * Domain object for a group of nodes.
 * 
 * <p>Groups are assigned a {@link Location} which is meant to give the group
 * a broadly-defined location There is no actual restriction that nodes within
 * the group are physically within the group location's boundaries, but in 
 * practice this will often be the case.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class SolarNodeGroup extends BaseEntity implements Cloneable, Serializable, NodeGroupIdentity {

	private static final long serialVersionUID = 1843734913796373879L;

	private Long locationId = null;
	private String name;

	/**
	 * Default constructor.
	 */
	public SolarNodeGroup() {
		super();
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param id the ID
	 * @param locationId the location ID
	 * @param name the name
	 */
	public SolarNodeGroup(Long id, Long locationId, String name) {
		super();
		setId(id);
		setCreated(new DateTime());
		setLocationId(locationId);
		setName(name);
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
	 * @return the locationId
	 */
	public Long getLocationId() {
		return locationId;
	}

	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}
	
}
