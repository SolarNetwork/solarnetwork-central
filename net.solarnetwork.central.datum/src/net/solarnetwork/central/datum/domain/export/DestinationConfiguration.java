/* ==================================================================
 * DestinationConfiguration.java - 5/03/2018 5:23:53 PM
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

package net.solarnetwork.central.datum.domain.export;

import java.util.Map;
import net.solarnetwork.central.datum.biz.DatumExportDestinationService;

/**
 * A destination configuration object for a datum export.
 * 
 * <p>
 * This API defines where the data should be exported to.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface DestinationConfiguration {

	/**
	 * Get a name for this configuration.
	 * 
	 * <p>
	 * This is expected to be a user-supplied name.
	 * </p>
	 * 
	 * @return a configuration name
	 */
	String getName();

	/**
	 * Get a unique identifier for the export destination service to use.
	 * 
	 * <p>
	 * This value will correspond to some
	 * {@link DatumExportDestinationService#getId()} value.
	 * </p>
	 * 
	 * @return the service type identifier
	 */
	String getServiceIdentifier();

	/**
	 * Get a map of properties to pass to the
	 * {@link DatumExportDestinationService} in order to perform actions.
	 * 
	 * <p>
	 * It is expected this map would contain runtime configuration such as the
	 * credentials to use, host name, etc.
	 * </p>
	 * 
	 * @return the runtime properties to pass to the
	 *         {@link DatumExportDestinationService}
	 */
	Map<String, ?> getServiceProperties();

}
