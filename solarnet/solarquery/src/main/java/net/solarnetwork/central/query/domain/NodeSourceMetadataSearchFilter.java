/* ==================================================================
 * NodeSourceMetadataFilter.java - 7/02/2025 2:24:29 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.domain;

import net.solarnetwork.central.domain.DateRangeFilter;
import net.solarnetwork.central.domain.MetadataFilter;

/**
 * Search filter for node/source metadata.
 * 
 * <p>
 * This class is used for API documentation only.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface NodeSourceMetadataSearchFilter
		extends NodeSourceSearchFilter, DateRangeFilter, MetadataFilter {

	/**
	 * Get the node IDs.
	 * 
	 * @return the node IDs
	 */
	Long[] getNodeIds();

	/**
	 * Get the flag to include node IDs in the result.
	 * 
	 * @return {@code true} to include node IDs in the result, otherwise just
	 *         include source IDs
	 */
	Boolean getWithNodeIds();

}
