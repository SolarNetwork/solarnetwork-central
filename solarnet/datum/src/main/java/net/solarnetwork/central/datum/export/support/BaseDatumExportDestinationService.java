/* ==================================================================
 * BaseDatumExportDestinationService.java - 11/04/2018 12:19:19 PM
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

package net.solarnetwork.central.datum.export.support;

import net.solarnetwork.central.datum.export.biz.DatumExportDestinationService;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;

/**
 * Base class to support implementations of
 * {@link DatumExportDestinationService}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public abstract class BaseDatumExportDestinationService
		extends BaseSettingsSpecifierLocalizedServiceInfoProvider<String>
		implements DatumExportDestinationService {

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the {@link Identity#getId()} to use
	 */
	public BaseDatumExportDestinationService(String id) {
		super(id);
	}

}
