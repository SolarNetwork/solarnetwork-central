/* ==================================================================
 * DatumImportInputFormatService.java - 6/11/2018 4:48:31 PM
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

package net.solarnetwork.central.datum.imp.biz;

import java.io.Closeable;
import java.io.IOException;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.InputConfiguration;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * API for datum import format services.
 * 
 * <p>
 * This API defines a service provider API that supports reading and parsing a
 * stream of datum objects from some other form, such as JSON, XML, or CSV.
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
 */
public interface DatumImportInputFormatService extends DatumImportService {

	/**
	 * Get an appropriate filename extension to use for this input format.
	 * 
	 * @return the input extension
	 */
	String getInputFilenameExtension();

	/**
	 * Get an appropriate content type to use for this input format.
	 * 
	 * @return the input content type
	 */
	String getInputContentType();

	/**
	 * A runtime context object for decoding datum from the input format
	 * supported by this service.
	 */
	interface ImportContext extends Closeable, Iterable<GeneralNodeDatum> {

	}

	/**
	 * Create an import context for decoding the data with.
	 * 
	 * @param config
	 *        the configuration to create the context for
	 * @param resource
	 *        the resource to decode
	 * @param progressListener
	 *        the progress listener
	 * @return the context
	 * @throws IOException
	 *         if an IO error occurs
	 * @throws IllegalArgumentException
	 *         if {@code config} is not valid for any reason
	 */
	ImportContext createImportContext(InputConfiguration config, DatumImportResource resource,
			ProgressListener<DatumImportService> progressListener) throws IOException;

}
