/* ==================================================================
 * DatumExportOutputFormatService.java - 5/03/2018 8:24:26 PM
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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.util.ProgressListener;

/**
 * API for datum export destination services.
 * 
 * <p>
 * This API defines a service provider API that supports converting datum data
 * into some other form, such as JSON, XML, or CSV.
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
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public interface DatumExportOutputFormatService extends DatumExportService {

	/**
	 * Get an appropriate filename extension to use for this export format.
	 * 
	 * @return the extension
	 */
	String getExportFilenameExtension();

	/**
	 * Get an appropriate content type to use for this export format.
	 * 
	 * @return the export content type
	 */
	String getExportContentType();

	/**
	 * A runtime context object for encoding datum into the output format
	 * supported by this service.
	 */
	interface ExportContext extends Closeable, Flushable {

		/**
		 * Called at the start of the export process, to initialize any
		 * necessary resources or write any header information.
		 * 
		 * @param estimatedResultCount
		 *        an estimate on the total number of results to be exported, or
		 *        if less than {@literal 0} the count is not known
		 * @return the output stream to write to
		 * @throws IOException
		 *         if an IO error occurs
		 */
		void start(long estimatedResultCount) throws IOException;

		/**
		 * Append datum match data to the output stream started via
		 * {@link #start()}.
		 * 
		 * @param iterable
		 *        the data to encode
		 * @param progressListener
		 *        a progress listener
		 * @throws IOException
		 *         if an IO error occurs
		 */
		void appendDatumMatch(Iterable<? extends GeneralNodeDatumFilterMatch> iterable,
				ProgressListener<DatumExportService> progressListener) throws IOException;

		/**
		 * Called at the end of the export process, to clean up any necessary
		 * resources or write any footer information.
		 * 
		 * <p>
		 * <b>Note<b> that the resources returned by this method should be
		 * considered temporary, and only support reading their contents
		 * <b>once</b>. If the calling code needs to repeatedly read the
		 * contents of a returned resource, it must copy the resource to another
		 * location first.
		 * </p>
		 * 
		 * @return the resource(s) to upload to the export destination
		 * @throws IOException
		 *         if an IO error occurs
		 */
		Iterable<DatumExportResource> finish() throws IOException;

	}

	/**
	 * Create an export context for encoding the data with.
	 * 
	 * @param config
	 *        the configuration to create the context for
	 * @return the context
	 */
	ExportContext createExportContext(OutputConfiguration config);

}
