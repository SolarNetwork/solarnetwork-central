/* ==================================================================
 * NodeInformation.java - Aug 8, 2010 9:23:16 PM
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
 * $Revision$
 * ==================================================================
 */

package net.solarnetwork.central.domain;

/**
 * General node information.
 * 
 * @author matt
 * @version 1.0
 */
public interface NodeInformation extends NodeIdentity {

	/**
	 * Get a theoretical maximum power generation capacity.
	 * 
	 * @return generation capacity watts
	 */
	Long getGenerationCapacityWatts();
	
	/**
	 * Get a theoretical maximum power storage capacity.
	 * 
	 * @return storage capacity in watt hours
	 */
	Long getStorageCapacityWattHours();
	
	/**
	 * Get the location of the node.
	 * 
	 * @return location
	 */
	Location getLocation();

}
