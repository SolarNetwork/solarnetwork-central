/* ==================================================================
 * NodeGroupInformation.java - Apr 30, 2011 10:48:21 AM
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

/**
 * General node group information.
 * 
 * @author matt
 * @version $Revision$
 */
public interface NodeGroupInformation extends NodeGroupIdentity {

	/**
	 * Get the name of the group.
	 * 
	 * @return the group name
	 */
	String getName();
	
	/**
	 * Get the location of the node.
	 * 
	 * @return location
	 */
	Location getLocation();

	/**
	 * Get a theoretical maximum power generation capacity of all group members combined.
	 * 
	 * @return generation capacity watts
	 */
	Long getGenerationCapacityWatts();
	
	/**
	 * Get a theoretical maximum power storage capacity of all group members combined.
	 * 
	 * @return storage capacity in watt hours
	 */
	Long getStorageCapacityWattHours();
	
}
