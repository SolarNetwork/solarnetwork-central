/* ==================================================================
 * UserFilter.java - 4/02/2019 7:33:16 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

import net.solarnetwork.central.domain.Filter;

/**
 * Filter API for user related data.
 * 
 * @author matt
 * @version 1.0
 * @since 1.35
 */
public interface UserFilter extends Filter {

	/**
	 * Get the first user ID. This returns the first available user ID from the
	 * {@link #getUserIds()} array, or {@literal null} if not available.
	 * 
	 * @return the first user ID, or {@literal null} if not available
	 */
	public Long getUserId();

	/**
	 * Get an array of user IDs.
	 * 
	 * @return array of user IDs (may be {@literal null})
	 */
	public Long[] getUserIds();

}
