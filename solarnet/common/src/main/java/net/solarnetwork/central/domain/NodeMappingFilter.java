/* ==================================================================
 * NodeMappingFilter.java - 25/05/2018 11:51:24 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import java.util.Map;
import java.util.Set;

/**
 * Extension of {@link Filter} for mapping node IDs into virtual IDs.
 * 
 * @author matt
 * @version 1.0
 * @since 1.39
 */
public interface NodeMappingFilter {

	/**
	 * Get a map whose keys represent virtual node ID values for the associated
	 * value's set of real node IDs.
	 * 
	 * <p>
	 * This mapping provides a way to request a set of node IDs be treated as a
	 * single logical virtual node ID. For example a set of node IDs for data
	 * collected from different buildings could be treated as a single virtual
	 * site node ID.
	 * <p>
	 * 
	 * @return the mapping of virtual node IDs to the set of real node IDs that
	 *         should be mapped to them
	 */
	Map<Long, Set<Long>> getNodeIdMappings();

}
