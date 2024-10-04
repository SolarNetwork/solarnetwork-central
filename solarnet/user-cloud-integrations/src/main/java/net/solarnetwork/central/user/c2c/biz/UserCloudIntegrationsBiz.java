/* ==================================================================
 * UserCloudIntegrationsBiz.java - 30/09/2024 11:08:18 am
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

package net.solarnetwork.central.user.c2c.biz;

import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudIntegrationsFilter;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.central.user.c2c.domain.CloudIntegrationsConfigurationInput;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

/**
 * Service API for SolarUser cloud integrations support.
 *
 * @author matt
 * @version 1.0
 */
public interface UserCloudIntegrationsBiz {

	/**
	 * Get a list of all available {@link CloudIntegrationService}
	 * implementations.
	 *
	 * @return the integration services, never {@literal null}
	 */
	Iterable<CloudIntegrationService> availableIntegrationServices();

	/**
	 * Get a specific {@link CloudIntegrationService} based on its service
	 * identifier.
	 *
	 * @return the integration service, or {@literal null} if not available
	 */
	CloudIntegrationService integrationService(String identifier);

	/**
	 * Get a list of all available cloud integration configurations for a given
	 * user.
	 *
	 * @param <C>
	 *        the configuration type
	 * @param userId
	 *        the user ID to get configurations for
	 * @param filter
	 *        an optional filter
	 * @param configurationClass
	 *        the desired configuration type
	 * @return the available configurations, never {@literal null}
	 */
	<C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> FilterResults<C, K> configurationsForUser(
			Long userId, CloudIntegrationsFilter filter, Class<C> configurationClass);

	/**
	 * Get a specific configuration kind for a given ID.
	 *
	 * @param <C>
	 *        the configuration type
	 * @param <K>
	 *        the primary key type
	 * @param id
	 *        the primary key of the configuration to get
	 * @param configurationClass
	 *        the configuration type to get
	 * @return the configuration, or {@literal null} if not available
	 */
	<C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> C configurationForId(
			K id, Class<C> configurationClass);

	/**
	 * Save a cloud integration configuration for a user.
	 *
	 * @param <T>
	 *        the configuration input type
	 * @param <C>
	 *        the configuration type
	 * @param <K>
	 *        the primary key type
	 * @param id
	 *        the ID of the configuration to save; at a minimum the user ID
	 *        component must be provided
	 * @param input
	 *        the configuration input to save
	 * @return the saved configuration
	 */
	<T extends CloudIntegrationsConfigurationInput<C, K>, C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> C saveConfiguration(
			K id, T input);

	/**
	 * Update the enabled status of configurations, optionally filtered.
	 *
	 * @param <C>
	 *        the configuration type
	 * @param <K>
	 * @param id
	 *        the ID of the configuration to save; at a minimum the user ID
	 *        component must be provided
	 * @param enabled
	 *        the enabled status to set
	 * @param configurationClass
	 *        the configuration type to get
	 */
	<C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> void enableConfiguration(
			K id, boolean enabled, Class<C> configurationClass);

	/**
	 * Delete a specific cloud integration configuration.
	 *
	 * @param <C>
	 *        the configuration type
	 * @param <K>
	 *        the primary key type
	 * @param id
	 *        the primary key of the configuration to delete
	 * @param configurationClass
	 *        the type of the configuration to delete
	 */
	<C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> void deleteConfiguration(
			K id, Class<C> configurationClass);

	/**
	 * Validate a {@link CloudIntegrationConfiguration} is configured
	 * appropriately.
	 *
	 * @param id
	 *        the ID of the configuration to validate
	 * @return the validation result, never {@literal null}
	 */
	Result<Void> validateIntegrationConfigurationForId(UserLongCompositePK id);

}
