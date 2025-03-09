/* ==================================================================
 * BasicCloudIntegrationLocalizedServiceInfo.java - 10/03/2025 11:40:30 am
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

import java.net.URI;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicConfigurableLocalizedServiceInfo;

/**
 * Localized service information for cloud integration services.
 *
 * @author matt
 * @version 1.0
 */
public class BasicCloudIntegrationLocalizedServiceInfo extends BasicConfigurableLocalizedServiceInfo
		implements CloudIntegrationLocalizedServiceInfo {

	private final Map<String, URI> wellKnownUrls;

	/**
	 * Copy constructor from another {@link LocalizedServiceInfo} instance.
	 *
	 * @param info
	 *        the info to copy
	 * @param settings
	 *        the settings
	 * @param wellKnownUrls
	 *        the well-known URL mapping
	 */
	public BasicCloudIntegrationLocalizedServiceInfo(LocalizedServiceInfo info,
			List<SettingSpecifier> settings, Map<String, URI> wellKnownUrls) {
		super(info, settings);
		this.wellKnownUrls = wellKnownUrls;
	}

	@Override
	public Map<String, URI> getWellKnownUrls() {
		return wellKnownUrls;
	}

}
