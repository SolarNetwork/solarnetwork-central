/* ==================================================================
 * UserModifiableEnabledStatusDao.java - 8/08/2023 8:22:16 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao;

/**
 * DAO API for updating user-modifiable enabled status on entities.
 * 
 * @param <F>
 *        the filter type
 * @author matt
 * @version 1.0
 */
public interface UserModifiableEnabledStatusDao<F> {

	/**
	 * Update the enabled status of server controls, optionally filtered.
	 * 
	 * @param userId
	 *        the user ID to update configurations for
	 * @param filter
	 *        an optional filter
	 * @param enabled
	 *        the enabled status to set
	 * @return the number of entities updated
	 */
	int updateEnabledStatus(Long userId, F filter, boolean enabled);

}
