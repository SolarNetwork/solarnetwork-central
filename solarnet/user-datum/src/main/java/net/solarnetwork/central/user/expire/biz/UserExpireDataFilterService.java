/* ==================================================================
 * DatumExportDataFilterService.java - 16/04/2018 7:24:24 PM
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

package net.solarnetwork.central.user.expire.biz;

import net.solarnetwork.domain.Identity;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.service.LocalizedServiceInfoProvider;

/**
 * API for datum expire data filter services.
 * 
 * <p>
 * This API defines a service provider API that supports accepting datum expire
 * data. The {@link #getId()} value should be a globally unique identifier, such
 * as a reverse domain package name for the implementation class of the service.
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
 * @version 1.0
 */
public interface UserExpireDataFilterService
		extends Identity<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

}
