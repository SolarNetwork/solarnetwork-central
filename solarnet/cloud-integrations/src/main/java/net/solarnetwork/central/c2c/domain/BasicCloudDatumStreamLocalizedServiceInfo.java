/* ==================================================================
 * BasicCloudDatumStreamLocalizedServiceInfo.java - 23/10/2024 8:55:21 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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
import net.solarnetwork.util.IntRange;

/**
 * Basic implementation of {@link CloudDatumStreamLocalizedServiceInfo}.
 *
 * @author matt
 * @version 1.0
 */
public class BasicCloudDatumStreamLocalizedServiceInfo extends BasicConfigurableLocalizedServiceInfo
		implements CloudDatumStreamLocalizedServiceInfo {

	private final boolean requiresPolling;
	private final Iterable<String> supportedPlaceholders;
	private final Iterable<Integer> supportedDataValueWildcardIdentifierLevels;
	private final IntRange dataValueIdentifierLevelsSourceIdRange;

	/**
	 * Copy constructor from another {@link LocalizedServiceInfo} instance.
	 *
	 * @param info
	 *        the info to copy
	 * @param settings
	 *        the settings
	 * @param requiresPolling
	 *        the polling requirement
	 * @param supportedPlaceholders
	 *        the supported placeholder keys
	 * @param supportedDataValueWildcardIdentifierLevels
	 *        the supported data value wildcard levels
	 * @param dataValueIdentifierLevelsSourceIdRange
	 *        the data value identifier levels source ID range
	 */
	public BasicCloudDatumStreamLocalizedServiceInfo(LocalizedServiceInfo info,
			List<SettingSpecifier> settings, boolean requiresPolling,
			Iterable<String> supportedPlaceholders,
			Iterable<Integer> supportedDataValueWildcardIdentifierLevels,
			IntRange dataValueIdentifierLevelsSourceIdRange) {
		super(info, settings);
		this.requiresPolling = requiresPolling;
		this.supportedPlaceholders = supportedPlaceholders;
		this.supportedDataValueWildcardIdentifierLevels = supportedDataValueWildcardIdentifierLevels;
		this.dataValueIdentifierLevelsSourceIdRange = dataValueIdentifierLevelsSourceIdRange;
	}

	@Override
	public boolean isRequiresPolling() {
		return requiresPolling;
	}

	@Override
	public final Iterable<String> getSupportedPlaceholders() {
		return supportedPlaceholders;
	}

	@Override
	public final Iterable<Integer> getSupportedDataValueWildcardIdentifierLevels() {
		return supportedDataValueWildcardIdentifierLevels;
	}

	@Override
	public final IntRange getDataValueIdentifierLevelsSourceIdRange() {
		return dataValueIdentifierLevelsSourceIdRange;
	}

}
