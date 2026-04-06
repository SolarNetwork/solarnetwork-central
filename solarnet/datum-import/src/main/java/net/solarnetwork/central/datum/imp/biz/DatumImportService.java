/* ==================================================================
 * DatumImportService.java - 6/11/2018 4:43:23 PM
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

import net.solarnetwork.domain.Identity;
import net.solarnetwork.service.LocalizedServiceInfoProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * A service used with datum import.
 * 
 * <p>
 * This API is generally not used directly, but extended into more specific
 * APIs. This API provides a common foundation for dynamic services that can be
 * discovered at runtime and exposed via a UI that understands the
 * {@code net.solarnetwork.settings} framework.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public interface DatumImportService
		extends Identity<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

}
