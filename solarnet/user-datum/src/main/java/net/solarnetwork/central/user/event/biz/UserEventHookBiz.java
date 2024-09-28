/* ==================================================================
 * UserEventHookBiz.java - 11/06/2020 8:03:28 am
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

package net.solarnetwork.central.user.event.biz;

import java.util.List;
import java.util.Locale;
import net.solarnetwork.central.dao.UserLongIdentifiableConfigurationEntity;
import net.solarnetwork.central.datum.biz.DatumAppEventProducer;
import net.solarnetwork.central.domain.CompositeKey2;
import net.solarnetwork.domain.LocalizedServiceInfo;

/**
 * API for user node event hook tasks.
 * 
 * @author matt
 * @version 1.2
 */
public interface UserEventHookBiz {

	/**
	 * Get all available datum event producers.
	 * 
	 * @return the producers, never {@literal null}
	 * @since 1.1
	 */
	Iterable<DatumAppEventProducer> availableDatumEventProducers();

	/**
	 * Get all available node event hook services.
	 * 
	 * @return the services, never {@literal null}
	 * @since 1.1
	 */
	Iterable<UserNodeEventHookService> availableNodeEventHookServices();

	/**
	 * Get all available datum event topics.
	 * 
	 * @param locale
	 *        the desired locale
	 * @return the topics as service info, never {@literal null}
	 * @since 1.1
	 */
	Iterable<LocalizedServiceInfo> availableDatumEventTopics(Locale locale);

	/**
	 * Get a specific configuration kind for a given user.
	 * 
	 * @param <T>
	 *        the configuration type
	 * @param userId
	 *        the user ID to get configurations for
	 * @param configurationClass
	 *        the configuration type to get
	 * @param id
	 *        the primary key of the configuration to get
	 * @return the configuration, or {@literal null} if not available
	 */
	<T extends UserLongIdentifiableConfigurationEntity<?>> T configurationForUser(Long userId,
			Class<T> configurationClass, Long id);

	/**
	 * Save a configuration for a user.
	 * 
	 * @param configuration
	 *        the configuration to save
	 * @return the primary key of the saved configuration
	 */
	CompositeKey2<Long, Long> saveConfiguration(
			UserLongIdentifiableConfigurationEntity<?> configuration);

	/**
	 * Delete a specific configuration.
	 * 
	 * @param configuration
	 *        the configuration to delete
	 */
	void deleteConfiguration(UserLongIdentifiableConfigurationEntity<?> configuration);

	/**
	 * Get a list of all available configurations for a given user.
	 * 
	 * @param <T>
	 *        the configuration type
	 * @param userId
	 *        the user ID to get configurations for
	 * @param configurationClass
	 *        the desired configuration type
	 * @return the available configurations, never {@literal null}
	 */
	<T extends UserLongIdentifiableConfigurationEntity<?>> List<T> configurationsForUser(Long userId,
			Class<T> configurationClass);

}
