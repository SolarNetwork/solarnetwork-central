/* ==================================================================
 * UserExpireBiz.java - 9/07/2018 10:16:09 AM
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

package net.solarnetwork.central.user.expire.biz;

import java.util.List;
import net.solarnetwork.central.user.domain.UserIdentifiableConfiguration;

/**
 * Service API for user datum expire feature.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserExpireBiz {

	/**
	 * Get a specific configuration kind for a given user.
	 * 
	 * @param userId
	 *        the user ID to get configurations for
	 * @param configurationClass
	 *        the configuration type to get
	 * @param id
	 *        the primary key of the configuration to get
	 * @return the configuration, or {@literal null} if not available
	 */
	<T extends UserIdentifiableConfiguration> T configurationForUser(Long userId,
			Class<T> configurationClass, Long id);

	/**
	 * Save a configuration for a user.
	 * 
	 * @param configuration
	 *        the configuration to save
	 * @return the primary key of the saved configuration
	 */
	Long saveConfiguration(UserIdentifiableConfiguration configuration);

	/**
	 * Delete a specific configuration.
	 * 
	 * @param configuration
	 *        the configuration to delete
	 */
	void deleteConfiguration(UserIdentifiableConfiguration configuration);

	/**
	 * Get a list of all available data export configurations for a given user.
	 * 
	 * @param userId
	 *        the user ID to get configurations for
	 * @return the available configurations, never {@literal null}
	 */
	<T extends UserIdentifiableConfiguration> List<T> configurationsForUser(Long userId,
			Class<T> configurationClass);

}
