/* ==================================================================
 * BasicCloudControlLocalizedServiceInfo.java - 3/11/2025 4:58:49 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.domain;

import java.util.List;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicConfigurableLocalizedServiceInfo;

/**
 * Basic implementation of {@link CloudControlLocalizedServiceInfo}.
 *
 * @author matt
 * @version 1.0
 */
public class BasicCloudControlLocalizedServiceInfo extends BasicConfigurableLocalizedServiceInfo
		implements CloudControlLocalizedServiceInfo {

	private final boolean dataValuesRequireDatumStream;

	/**
	 * Copy constructor from another {@link LocalizedServiceInfo} instance.
	 *
	 * @param info
	 *        the info to copy
	 * @param settings
	 *        the settings
	 * @param dataValuesRequireDatumStream
	 *        the data values datum stream requirement
	 */
	public BasicCloudControlLocalizedServiceInfo(LocalizedServiceInfo info,
			List<SettingSpecifier> settings, boolean dataValuesRequireDatumStream) {
		super(info, settings);
		this.dataValuesRequireDatumStream = dataValuesRequireDatumStream;
	}

	@Override
	public final boolean isDataValuesRequireDatumStream() {
		return dataValuesRequireDatumStream;
	}

}
