/* ==================================================================
 * GeneralNodeDatumFilter.java - Aug 27, 2014 6:52:46 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import net.solarnetwork.central.domain.NodeMappingFilter;
import net.solarnetwork.central.domain.SourceMappingFilter;

/**
 * Filter API for {@link GeneralNodeDatum}.
 * 
 * @author matt
 * @version 1.4
 */
public interface GeneralNodeDatumFilter
		extends CommonFilter, CombiningFilter, NodeMappingFilter, SourceMappingFilter {

	/**
	 * Get the first node ID. This returns the first available node ID from the
	 * {@link #getNodeIds()} array, or <em>null</em> if not available.
	 * 
	 * @return the node ID, or <em>null</em> if not available
	 */
	Long getNodeId();

	/**
	 * Get an array of node IDs.
	 * 
	 * @return array of node IDs (may be <em>null</em>)
	 */
	Long[] getNodeIds();

	/**
	 * Get the first user ID. This returns the first available user ID from the
	 * {@link #getUserIds()} array, or <em>null</em> if not available.
	 * 
	 * @return the first user ID, or <em>null</em> if not available
	 * @since 1.2
	 */
	public Long getUserId();

	/**
	 * Get an array of user IDs.
	 * 
	 * @return array of user IDs (may be <em>null</em>)
	 * @since 1.2
	 */
	public Long[] getUserIds();

}
