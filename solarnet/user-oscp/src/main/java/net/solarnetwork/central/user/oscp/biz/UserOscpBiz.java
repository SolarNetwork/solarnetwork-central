/* ==================================================================
 * UserOscpBiz.java - 15/08/2022 10:33:25 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.oscp.biz;

import java.util.Collection;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupSettings;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.UserSettings;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.oscp.domain.AssetConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityGroupConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityGroupSettingsInput;
import net.solarnetwork.central.user.oscp.domain.CapacityOptimizerConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityProviderConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.UserSettingsInput;

/**
 * Service API for SolarUser OSCP support.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserOscpBiz {

	/**
	 * Get the user settings.
	 * 
	 * @param userId
	 *        the ID of the user to get the settings for
	 * @return the settings, or {@literal null} if no settings exist
	 */
	UserSettings settingsForUser(Long userId);

	/**
	 * Get a capacity provider configuration for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get the configuration for
	 * @param entityId
	 *        the ID of the configuration to get
	 * @return the configuration; never {@literal null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if a
	 *         configuration with the given IDs does not exist
	 */
	CapacityProviderConfiguration capacityProviderForUser(Long userId, Long entityId);

	/**
	 * Get a capacity optimizer configuration for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get the configuration for
	 * @param entityId
	 *        the ID of the configuration to get
	 * @return the configuration; never {@literal null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if a
	 *         configuration with the given IDs does not exist
	 */
	CapacityOptimizerConfiguration capacityOptimizerForUser(Long userId, Long entityId);

	/**
	 * Get a capacity group configuration for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get the configuration for
	 * @param entityId
	 *        the ID of the configuration to get
	 * @return the configuration; never {@literal null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if a
	 *         configuration with the given IDs does not exist
	 */
	CapacityGroupConfiguration capacityGroupForUser(Long userId, Long entityId);

	/**
	 * Get the capacity group settings for a given group.
	 * 
	 * @param userId
	 *        the ID of the user to get the settings for
	 * @param entityId
	 *        the ID of the group to get
	 * @return the settings, or {@literal null} if no settings exist
	 */
	CapacityGroupSettings capacityGroupSettingsForUser(Long userId, Long entityId);

	/**
	 * Get an asset configuration for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get the configuration for
	 * @param entityId
	 *        the ID of the configuration to get
	 * @return the configuration; never {@literal null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if a
	 *         configuration with the given IDs does not exist
	 */
	AssetConfiguration assetForUser(Long userId, Long entityId);

	/**
	 * Delete a user settings for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to delete the configuration for
	 */
	void deleteUserSettings(Long userId);

	/**
	 * Delete a capacity provider configuration for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get the configuration for
	 * @param entityId
	 *        the ID of the configuration to get
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if a
	 *         configuration with the given IDs does not exist
	 */
	void deleteCapacityProvider(Long userId, Long entityId);

	/**
	 * Get a capacity optimizer configuration for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to delete the configuration for
	 * @param entityId
	 *        the ID of the configuration to delete
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if a
	 *         configuration with the given IDs does not exist
	 */
	void deleteCapacityOptimizer(Long userId, Long entityId);

	/**
	 * Delete a capacity group configuration for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to delete the configuration for
	 * @param entityId
	 *        the ID of the configuration to delete
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if a
	 *         configuration with the given IDs does not exist
	 */
	void deleteCapacityGroup(Long userId, Long entityId);

	/**
	 * Delete a capacity group settings for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to delete the configuration for
	 * @param entityId
	 *        the ID of the configuration to delete
	 */
	void deleteCapacityGroupSettings(Long userId, Long entityId);

	/**
	 * Delete an asset configuration for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to delete the configuration for
	 * @param entityId
	 *        the ID of the configuration to delete
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if a
	 *         configuration with the given IDs does not exist
	 */
	void deleteAsset(Long userId, Long entityId);

	/**
	 * List the available capacity provider configurations for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @return all available configurations; never {@literal null}
	 */
	Collection<CapacityProviderConfiguration> capacityProvidersForUser(Long userId);

	/**
	 * List the available capacity optimizer configurations for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @return all available configurations; never {@literal null}
	 */
	Collection<CapacityOptimizerConfiguration> capacityOptimizersForUser(Long userId);

	/**
	 * List the available capacity group configurations for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @return all available configurations; never {@literal null}
	 */
	Collection<CapacityGroupConfiguration> capacityGroupsForUser(Long userId);

	/**
	 * List the available capacity group settings for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get settings for
	 * @return all available settings; never {@literal null}
	 */
	Collection<CapacityGroupSettings> capacityGroupSettingsForUser(Long userId);

	/**
	 * List the available asset configurations for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @return all available configurations; never {@literal null}
	 */
	Collection<AssetConfiguration> assetsForUser(Long userId);

	/**
	 * List the available asset configurations for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @return all available configurations; never {@literal null}
	 */
	Collection<AssetConfiguration> assetsForUserCapacityGroup(Long userId, Long groupId);

	/**
	 * Create a new capacity provider configuration.
	 * 
	 * @param userId
	 *        the user ID to create the configuration for
	 * @param input
	 *        the configuration input
	 * @return the configuration entity
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if
	 *         {@code userId} does not exist
	 */
	CapacityProviderConfiguration createCapacityProvider(Long userId,
			CapacityProviderConfigurationInput input) throws AuthorizationException;

	/**
	 * Create a new capacity optimizer configuration.
	 * 
	 * @param userId
	 *        the user ID to create the configuration for
	 * @param input
	 *        the configuration input
	 * @return the configuration entity
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if
	 *         {@code userId} does not exist
	 */
	CapacityOptimizerConfiguration createCapacityOptimizer(Long userId,
			CapacityOptimizerConfigurationInput input) throws AuthorizationException;

	/**
	 * Create a new capacity group configuration.
	 * 
	 * @param userId
	 *        the user ID to create the configuration for
	 * @param input
	 *        the configuration input
	 * @return the configuration entity
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if
	 *         {@code userId} does not exist
	 */
	CapacityGroupConfiguration createCapacityGroup(Long userId, CapacityGroupConfigurationInput input)
			throws AuthorizationException;

	/**
	 * Create a new asset configuration.
	 * 
	 * @param userId
	 *        the user ID to create the configuration for
	 * @param input
	 *        the configuration input
	 * @return the configuration entity
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if
	 *         {@code userId} does not exist
	 */
	AssetConfiguration createAsset(Long userId, AssetConfigurationInput input)
			throws AuthorizationException;

	/**
	 * Update or create a user settings.
	 * 
	 * @param userId
	 *        the user ID to update the settings for
	 * @param input
	 *        the settings input
	 * @return the updated settings entity
	 */
	UserSettings updateUserSettings(Long userId, UserSettingsInput input) throws AuthorizationException;

	/**
	 * Update an existing capacity provider configuration.
	 * 
	 * @param userId
	 *        the user ID to update the configuration for
	 * @param entityId
	 *        the entity ID to update the configuration for
	 * @param input
	 *        the configuration input
	 * @return the updated configuration entity
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if an
	 *         entity matching {@code userId} and {@code entityId} does not
	 *         exist
	 */
	CapacityProviderConfiguration updateCapacityProvider(Long userId, Long entityId,
			CapacityProviderConfigurationInput input) throws AuthorizationException;

	/**
	 * Update an existing capacity optimizer configuration.
	 * 
	 * @param userId
	 *        the user ID to update the configuration for
	 * @param entityId
	 *        the entity ID to update the configuration for
	 * @param input
	 *        the configuration input
	 * @return the updated configuration entity
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if an
	 *         entity matching {@code userId} and {@code entityId} does not
	 *         exist
	 */
	CapacityOptimizerConfiguration updateCapacityOptimizer(Long userId, Long entityId,
			CapacityOptimizerConfigurationInput input) throws AuthorizationException;

	/**
	 * Update an existing capacity group configuration.
	 * 
	 * @param userId
	 *        the user ID to update the configuration for
	 * @param entityId
	 *        the entity ID to update the configuration for
	 * @param input
	 *        the configuration input
	 * @return the updated configuration entity
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if an
	 *         entity matching {@code userId} and {@code entityId} does not
	 *         exist
	 */
	CapacityGroupConfiguration updateCapacityGroup(Long userId, Long entityId,
			CapacityGroupConfigurationInput input) throws AuthorizationException;

	/**
	 * Update or create a capacity group settings.
	 * 
	 * @param userId
	 *        the user ID to update the settings for
	 * @param entityId
	 *        the entity ID to update the settings for
	 * @param input
	 *        the settings input
	 * @return the updated settings entity
	 */
	CapacityGroupSettings updateCapacityGroupSettings(Long userId, Long entityId,
			CapacityGroupSettingsInput input) throws AuthorizationException;

	/**
	 * Update an existing asset configuration.
	 * 
	 * @param userId
	 *        the user ID to update the configuration for
	 * @param entityId
	 *        the entity ID to update the configuration for
	 * @param input
	 *        the configuration input
	 * @return the updated configuration entity
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if an
	 *         entity matching {@code userId} and {@code entityId} does not
	 *         exist
	 */
	AssetConfiguration updateAsset(Long userId, Long entityId, AssetConfigurationInput input)
			throws AuthorizationException;

}
