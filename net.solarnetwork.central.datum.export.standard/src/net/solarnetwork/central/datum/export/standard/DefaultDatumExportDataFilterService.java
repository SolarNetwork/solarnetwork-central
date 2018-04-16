/* ==================================================================
 * DefaultDatumExportDataFilterService.java - 16/04/2018 7:26:28 PM
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

package net.solarnetwork.central.datum.export.standard;

import java.util.Collections;
import java.util.List;
import net.solarnetwork.central.datum.biz.DatumExportDataFilterService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;

/**
 * Default implementation of {@link DatumExportDataFilterService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultDatumExportDataFilterService
		extends BaseSettingsSpecifierLocalizedServiceInfoProvider<String>
		implements DatumExportDataFilterService {

	/**
	 * Constructor.
	 */
	public DefaultDatumExportDataFilterService() {
		super("net.solarnetwork.central.datum.export.standard.DefaultDatumExportDataFilterService");
	}

	@Override
	public String getDisplayName() {
		return "Data Filter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

}
