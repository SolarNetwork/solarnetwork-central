/* ==================================================================
 * DatumExportDestinationService.java - 5/03/2018 5:34:02 PM
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

package net.solarnetwork.central.datum.export.biz;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * API for datum export destination services.
 * 
 * <p>
 * This API defines a service provider API that supports accepting datum export
 * data, such as FTP, S3, etc. The {@link #getId()} value should be a globally
 * unique identifier, such as a reverse domain package name for the
 * implementation class of the service.
 * </p>
 * 
 * <p>
 * It is expected that by implementing {@link SettingSpecifierProvider} any
 * {@link net.solarnetwork.settings.KeyedSettingSpecifier} returned by
 * {@link SettingSpecifierProvider#getSettingSpecifiers()} defines a
 * configurable runtime property that can be used to pass required configuration
 * to the service in the form of property maps in the various methods defined in
 * this interface.
 * </p>
 * 
 * @author matt
 * @version 2.0
 * @since 1.23
 */
public interface DatumExportDestinationService extends DatumExportService {

	/**
	 * Create a new {@link OutputStream} suitable for writing export data to
	 * based on a specific configuration.
	 * 
	 * @param config
	 *        the configuration in use
	 * @param resources
	 *        the resources to export
	 * @param runtimeProperties
	 *        the runtime properties to use
	 * @param progressListener
	 *        an optional listener for progress updates on the export process
	 * @throws IOException
	 *         if the export fails for any reason from an IO error
	 * @throw RemoteServiceException if a remote access error occurs
	 */
	void export(Configuration config, Iterable<DatumExportResource> resources,
			Map<String, ?> runtimeProperties, ProgressListener<DatumExportService> progressListener)
			throws IOException;

}
