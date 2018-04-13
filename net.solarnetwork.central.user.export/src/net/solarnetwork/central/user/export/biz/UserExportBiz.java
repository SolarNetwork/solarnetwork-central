/* ==================================================================
 * UserExportBiz.java - 22/03/2018 8:33:56 PM
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

package net.solarnetwork.central.user.export.biz;

import java.util.List;
import java.util.Locale;
import net.solarnetwork.central.datum.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserIdentifiableConfiguration;
import net.solarnetwork.domain.LocalizedServiceInfo;

/**
 * Service API for user export feature.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserExportBiz {

	/**
	 * Get a list of all available output format services.
	 * 
	 * @return the available services, never {@literal null}
	 */
	Iterable<DatumExportOutputFormatService> availableOutputFormatServices();

	/**
	 * Get a list of all available destination services.
	 * 
	 * @return the available services, never {@literal null}
	 */
	Iterable<DatumExportDestinationService> availableDestinationServices();

	/**
	 * Get a localized list of all available compression type information.
	 * 
	 * @return the compression type info
	 */
	Iterable<LocalizedServiceInfo> availableOutputCompressionTypes(Locale locale);

	/**
	 * Get a saved datum export configuration.
	 * 
	 * @param id
	 *        the ID of the configuration to get
	 * @return the configuration, or {@literal null} if not available
	 */
	UserDatumExportConfiguration datumExportConfiguration(Long id);

	/**
	 * Save a configuration for a user.
	 * 
	 * @param configuration
	 *        the configuration to save
	 * @return the primary key of the saved configuration
	 */
	Long saveDatumExportConfiguration(UserDatumExportConfiguration configuration);

	/**
	 * Get a list of all available datum export configurations for a given user.
	 * 
	 * @param userId
	 *        the user ID to get configurations for
	 * @return the available configurations, never {@literal null}
	 */
	List<UserDatumExportConfiguration> datumExportsForUser(Long userId);

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
