/* ==================================================================
 * BasicAppConfigurationBiz.java - 2/10/2017 10:26:36 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import java.util.Map;
import net.solarnetwork.central.biz.AppConfigurationBiz;
import net.solarnetwork.central.domain.AppConfiguration;
import net.solarnetwork.util.StringUtils;

/**
 * Basic implementation of {@link AppConfigurationBiz}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.35
 */
public class BasicAppConfigurationBiz implements AppConfigurationBiz {

	private AppConfiguration appConfiguration = new SimpleAppConfiguration();

	@Override
	public AppConfiguration getAppConfiguration() {
		return appConfiguration;
	}

	/**
	 * Set the service URLs to use.
	 * 
	 * @param serviceUrls
	 *        the URLs to use
	 */
	public void setServiceUrls(Map<String, String> serviceUrls) {
		appConfiguration = new SimpleAppConfiguration(serviceUrls);
	}

	/**
	 * Set the serviice URLs to use via a string map.
	 * 
	 * @param mapping
	 *        the comma-delimited string mapping of service URLs
	 */
	public void setServiceUrlMapping(String mapping) {
		setServiceUrls(StringUtils.commaDelimitedStringToMap(mapping));
	}

}
