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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskFilter;
import net.solarnetwork.central.c2c.dao.CloudIntegrationsFilter;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPollTaskEntityInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPropertyConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudIntegrationsConfigurationInput;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.Datum;

/**
 * Service API for SolarUser cloud integrations support.
 *
 * @author matt
 * @version 1.1
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
	 * Get a specific {@link CloudDatumStreamService} based on its service
	 * identifier.
	 *
	 * @return the datum stream service, or {@literal null} if not available
	 */
	CloudDatumStreamService datumStreamService(String identifier);

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
	<C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> FilterResults<C, K> listConfigurationsForUser(
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
	 * Save a list of cloud datum stream property configurations.
	 *
	 * <p>
	 * This method will first <b>delete</b> all configurations for the given
	 * {@code groupId}, then <b>insert</b> the given configurations, assigning
	 * {@code index} key component values based on list order.
	 * </p>
	 * </p>
	 *
	 * @param datumStreamId
	 *        the datum stream ID of the configurations to delete
	 * @param inputs
	 *        the configuration inputs to save
	 * @return the saved configurations
	 */
	List<CloudDatumStreamPropertyConfiguration> replaceDatumStreamPropertyConfiguration(
			UserLongCompositePK datumStreamId, List<CloudDatumStreamPropertyConfigurationInput> inputs);

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
	<C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> void updateConfigurationEnabled(
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
	 * @param locale
	 *        the desired locale for any error messages
	 * @return the validation result, never {@literal null}
	 */
	Result<Void> validateIntegrationConfigurationForId(UserLongCompositePK id, Locale locale);

	/**
	 * List data values.
	 *
	 * @param id
	 *        the ID of the {@link CloudDatumStreamConfiguration} to get the
	 *        data values for
	 * @param filters
	 *        an optional set of search filters to limit the data value groups
	 *        to; the available key values come from the identifiers returned by
	 *        {@link CloudDatumStreamService#dataValueFilters(Locale)}
	 * @return the available values, never {@literal null}
	 *
	 */
	Iterable<CloudDataValue> listDatumStreamDataValues(UserLongCompositePK id, Map<String, ?> filters);

	/**
	 * Get the latest available datum from a datum stream.
	 *
	 * @param id
	 *        the ID of the {@link CloudDatumStreamConfiguration} to get the
	 *        datum for
	 * @return the result, never {@literal null}
	 */
	Datum latestDatumStreamDatumForId(UserLongCompositePK id);

	/**
	 * List datum matching search criteria for a datum stream configuration.
	 *
	 * @param id
	 *        the ID of the {@link CloudDatumStreamConfiguration} to get the
	 *        datum for
	 * @param filter
	 *        the search criteria
	 * @return the result, never {@literal null}
	 */
	CloudDatumStreamQueryResult listDatumStreamDatum(UserLongCompositePK id,
			CloudDatumStreamQueryFilter filter);

	/**
	 * Get a list of all available cloud datum stream poll tasks for a given
	 * user.
	 *
	 * @param userId
	 *        the user ID to get entities for
	 * @param filter
	 *        an optional filter
	 * @return the available entities, never {@literal null}
	 */
	FilterResults<CloudDatumStreamPollTaskEntity, UserLongCompositePK> listDatumStreamPollTasksForUser(
			Long userId, CloudDatumStreamPollTaskFilter filter);

	/**
	 * Update the state of a datum stream poll task.
	 *
	 * @param id
	 *        the ID of the task to update the state of
	 * @param desiredState
	 *        the state to update the task to
	 * @param expectedStates
	 *        a set of states that must include the task's current state in
	 *        order to change it to {@code desiredState}, or {@literal null} if
	 *        the current state of the task does not matter
	 * @return the resulting task, or {@literal null} if no such task exists
	 */
	CloudDatumStreamPollTaskEntity updateDatumStreamPollTaskState(UserLongCompositePK id,
			BasicClaimableJobState desiredState, BasicClaimableJobState... expectedStates);

	/**
	 * Save a datum stream poll task.
	 *
	 * @param id
	 *        the ID of the {@link CloudDatumStreamConfiguration}
	 * @param input
	 *        the info to save
	 * @param expectedStates
	 *        a set of states that must include the task's current state in
	 *        order to change it to the info's given state, or {@literal null}
	 *        if the current state of the task does not matter
	 * @return the resulting task
	 */
	CloudDatumStreamPollTaskEntity saveDatumStreamPollTask(UserLongCompositePK id,
			CloudDatumStreamPollTaskEntityInput input, BasicClaimableJobState... expectedStates);

	/**
	 * Delete a specific datum stream poll task.
	 *
	 * @param id
	 *        the primary key of the entity to delete
	 */
	void deleteDatumStreamPollTask(UserLongCompositePK id);

}
