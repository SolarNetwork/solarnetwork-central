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

package net.solarnetwork.central.datum.biz;

import net.solarnetwork.domain.Identity;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.support.LocalizedServiceInfoProvider;

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
public interface DatumExportOutputFormatService
		extends Identity<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

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

}
