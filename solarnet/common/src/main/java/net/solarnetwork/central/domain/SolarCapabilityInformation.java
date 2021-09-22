/* ==================================================================
 * SolarCapabilityInformation.java - Jun 8, 2011 2:39:08 PM
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
 * API for capability information for some identifiable object, such as a node,
 * node group, etc.
 * 
 * <p>
 * This API is meant to be used for both atomic measurements and aggregated
 * measurements, depending on the context it is used in.
 * </p>
 * 
 * @param <PK>
 *        the identity type
 * @author matt
 * @version 1.1
 */
public interface SolarCapabilityInformation<PK> extends net.solarnetwork.domain.Identity<PK> {

	/**
	 * Get the name of the object.
	 * 
	 * @return the name
	 */
	String getName();

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
