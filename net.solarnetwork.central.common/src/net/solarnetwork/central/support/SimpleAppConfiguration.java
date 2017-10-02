/* ==================================================================
 * SimpleAppConfiguration.java - 2/10/2017 10:28:05 AM
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.central.domain.AppConfiguration;

/**
 * Basic immutable implementation of {@link AppConfiguration}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleAppConfiguration implements AppConfiguration {

	private final Map<String, String> serviceUrls;

	/**
	 * Default constructor.
	 */
	public SimpleAppConfiguration() {
		this(null);
	}

	/**
	 * Constructor.
	 * 
	 * @param serviceUrls
	 *        the service URLs to expose
	 */
	public SimpleAppConfiguration(Map<String, String> serviceUrls) {
		super();
		if ( serviceUrls == null || serviceUrls.isEmpty() ) {
			this.serviceUrls = Collections.emptyMap();
		} else {
			this.serviceUrls = Collections
					.unmodifiableMap(new LinkedHashMap<String, String>(serviceUrls));
		}
	}

	@Override
	public Map<String, String> getServiceUrls() {
		return serviceUrls;
	}

}
