/* ==================================================================
 * UserFluxBiz.java - 25/06/2024 8:03:04 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.flux.biz;

import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.flux.dao.UserFluxAggregatePublishConfigurationFilter;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfigurationInput;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfigurationInput;
import net.solarnetwork.dao.FilterResults;

/**
 * Service API for SolarUser SolarFlux support.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserFluxBiz {

	/**
	 * Save the default aggregate publish configuration for a user.
	 *
	 * @param userId
	 *        the ID of the user to save the configuration for
	 * @param input
	 *        the configuration input to save
	 * @return the saved configuration
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	UserFluxDefaultAggregatePublishConfiguration saveDefaultAggregatePublishConfiguration(Long userId,
			UserFluxDefaultAggregatePublishConfigurationInput input);

	/**
	 * Delete the default aggregate publish configuration for a user.
	 *
	 * @param userId
	 *        the ID of the user to delete the configuration for
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	void deleteDefaultAggregatePublishConfiguration(Long userId);

	/**
	 * Get a default aggregate publish configuration for a user.
	 *
	 * @param userId
	 *        the ID of the user to save the configuration for
	 * @param input
	 *        the configuration input to save
	 * @return the configuration, or a default one if no persisted configuration
	 *         available; never {@literal null}
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	UserFluxDefaultAggregatePublishConfiguration defaultAggregatePublishConfigurationForUser(
			Long userId);

	/**
	 * Save a datum input configuration for a user.
	 *
	 * @param id
	 *        the ID of the configuration to save; at a minimum the user ID
	 *        component must be provided
	 * @param input
	 *        the configuration input to save
	 * @return the saved configuration
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	UserFluxAggregatePublishConfiguration saveAggregatePublishConfiguration(UserLongCompositePK id,
			UserFluxAggregatePublishConfigurationInput input);

	/**
	 * Delete an aggregate publish configuration for a user.
	 *
	 * @param id
	 *        the ID of the configuration to delete; at a minimum the user ID
	 *        component must be provided; if the entity ID is not provided,
	 *        delete all configurations for the given user ID
	 */
	void deleteAggregatePublishConfiguration(UserLongCompositePK id);

	/**
	 * Get an aggregate publish configuration for a user.
	 *
	 * @param userId
	 *        the ID of the user to save the configuration for
	 * @param configurationId
	 *        the ID of the configuration to get
	 * @return the configuration, or {@literal null}
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	UserFluxAggregatePublishConfiguration aggregatePublishConfigurationForUser(Long userId,
			Long configurationId);

	/**
	 * List the available SolarFlux aggregate publish configurations for a user.
	 * 
	 * @param userId
	 *        the user ID to get configurations for
	 * @param filter
	 *        an optional filter
	 * @return the available configurations, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if the {@code userId} argument is {@literal null}
	 */
	FilterResults<UserFluxAggregatePublishConfiguration, UserLongCompositePK> aggregatePublishConfigurationsForUser(
			Long userId, UserFluxAggregatePublishConfigurationFilter filter);

}
