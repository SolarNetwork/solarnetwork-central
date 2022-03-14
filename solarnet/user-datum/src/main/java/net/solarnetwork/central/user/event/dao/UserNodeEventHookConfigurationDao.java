/* ==================================================================
 * UserNodeEventHookConfigurationDao.java - 3/06/2020 2:56:04 pm
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

package net.solarnetwork.central.user.event.dao;

import java.util.List;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for {@link UserNodeEventHookConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserNodeEventHookConfigurationDao
		extends GenericDao<UserNodeEventHookConfiguration, UserLongPK> {

	/**
	 * Get a set of all configuration entities for a user.
	 * 
	 * <p>
	 * The results will be ordered by name.
	 * </p>
	 * 
	 * @param userId
	 *        The ID of the user to get all configurations for.
	 * @return The found entities, or an empty list if none found.
	 */
	List<UserNodeEventHookConfiguration> findConfigurationsForUser(Long userId);

}
