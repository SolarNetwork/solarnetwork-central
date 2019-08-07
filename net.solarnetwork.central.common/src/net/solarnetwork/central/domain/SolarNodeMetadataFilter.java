/* ==================================================================
 * SolarNodeMetadataFilter.java - 11/11/2016 11:02:18 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

/**
 * Filter API for {@link SolarNodeMetadata}.
 * 
 * @author matt
 * @version 1.1
 * @since 1.32
 */
public interface SolarNodeMetadataFilter extends Filter, MetadataFilter {

	/**
	 * Get the first node ID.
	 * 
	 * <p>
	 * This returns the first available node ID from the {@link #getNodeIds()}
	 * array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the node ID, or {@literal null} if not available
	 */
	Long getNodeId();

	/**
	 * Get an array of node IDs.
	 * 
	 * @return array of node IDs (may be {@literal null})
	 */
	Long[] getNodeIds();

	/**
	 * Get the first tag.
	 * 
	 * <p>
	 * This returns the first available tag from the {@link #getTags()} array,
	 * or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the first tag, or {@literal null} if not available
	 */
	String getTag();

	/**
	 * Get an array of tags.
	 * 
	 * @return array of tags (may be {@literal null})
	 */
	String[] getTags();

}
