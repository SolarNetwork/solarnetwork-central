/* ==================================================================
 * SolarNodeFilterMatch.java - 28/05/2018 2:46:59 PM
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

/**
 * API for a {@link SolarNode} search or filter match result.
 * 
 * @author matt
 * @version 1.1
 * @since 1.40
 */
public interface SolarNodeFilterMatch extends NodeIdentity, FilterMatch<Long> {

	/**
	 * Get the metadata JSON.
	 * 
	 * @return the metadata
	 * @since 1.1
	 */
	String getMetaJson();

}
