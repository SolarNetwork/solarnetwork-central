/* ==================================================================
 * StreamCriteria.java - 27/10/2020 9:38:25 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao;

import java.util.UUID;

/**
 * Search criteria for streams.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface StreamCriteria {

	/**
	 * Get the first datum stream ID.
	 * 
	 * <p>
	 * This returns the first available datum stream ID from the
	 * {@link #getStreamIds()} array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the datum stream ID, or {@literal null} if not available
	 */
	UUID getStreamId();

	/**
	 * Get the datum stream IDs to filter by.
	 * 
	 * @return the dateum stream IDs
	 */
	UUID[] getStreamIds();

}
