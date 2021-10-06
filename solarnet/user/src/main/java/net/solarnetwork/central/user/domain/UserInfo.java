/* ==================================================================
 * UserInfo.java - 21/08/2017 11:33:54 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import java.util.Map;

/**
 * API for user information.
 * 
 * @author matt
 * @version 1.0
 * @since 1.25
 */
public interface UserInfo {

	/**
	 * Get the unique ID of the user.
	 * 
	 * @return the ID
	 */
	Long getId();

	/**
	 * Get the full name.
	 * 
	 * @return the name
	 */
	String getName();

	/**
	 * Get the email.
	 * 
	 * @return the email
	 */
	String getEmail();

	/**
	 * Get the enabled flag.
	 * 
	 * @return the enabled flag
	 */
	Boolean getEnabled();

	/**
	 * Get the user's location ID.
	 * 
	 * @return the location ID, or {@literal null} if not available
	 */
	Long getLocationId();

	/**
	 * Get the internal data.
	 * 
	 * <p>
	 * This data object is arbitrary information needed for internal use, for
	 * example metadata like external billing account IDs to integrate with an
	 * external billing system.
	 * </p>
	 * 
	 * @return the internal data
	 */
	Map<String, Object> getInternalData();

}
