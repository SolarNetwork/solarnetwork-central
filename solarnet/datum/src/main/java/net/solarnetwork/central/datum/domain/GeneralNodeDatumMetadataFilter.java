/* ==================================================================
 * GeneralNodeDatumMetadataFilter.java - Oct 3, 2014 10:21:42 AM
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

/**
 * Filter API for {@link GeneralNodeDatumMetadata}.
 * 
 * @author matt
 * @version 1.1
 */
public interface GeneralNodeDatumMetadataFilter extends GeneralDatumMetadataFilter {

	/**
	 * Get the first node ID. This returns the first available node ID from the
	 * {@link #getNodeIds()} array, or {@literal null} if not available.
	 * 
	 * @return the node ID, or {@literal null} if not available
	 */
	public Long getNodeId();

	/**
	 * Get an array of node IDs.
	 * 
	 * @return array of node IDs (may be {@literal null})
	 */
	public Long[] getNodeIds();

}
