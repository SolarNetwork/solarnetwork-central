/* ==================================================================
 * BasicCloudConfigurationTopicLocalizedServiceInfo.java - 8/12/2025 5:54:00 am
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
import net.solarnetwork.util.ObjectUtils;

/**
 * Basic implementation of {@link CloudConfigurationTopicLocalizedServiceInfo}.
 *
 * @author matt
 * @version 1.0
 */
public class BasicCloudConfigurationTopicLocalizedServiceInfo extends
		BasicConfigurableLocalizedServiceInfo implements CloudConfigurationTopicLocalizedServiceInfo {

	private final String topic;

	/**
	 * Copy constructor from another {@link LocalizedServiceInfo} instance.
	 *
	 * @param info
	 *        the info to copy
	 * @param settings
	 *        the settings
	 * @param topic
	 *        the topic
	 * @throws IllegalArgumentException
	 *         if {@code topic} is {@code null}
	 */
	public BasicCloudConfigurationTopicLocalizedServiceInfo(LocalizedServiceInfo info,
			List<SettingSpecifier> settings, String topic) {
		super(info, settings);
		this.topic = ObjectUtils.requireNonNullArgument(topic, "topic");
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
