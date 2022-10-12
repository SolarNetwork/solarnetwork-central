/* ==================================================================
 * OAuthClientSettings.java - 27/08/2022 7:08:40 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth client settings.
 * 
 * @param tokenUrl
 *        the OAuth token URL
 * @param clientId
 *        the client ID to use
 * @param clientSecret
 *        the client secret, which may be {@literal null} if stored in some
 *        other context
 * @author matt
 * @version 1.0
 */
public record OAuthClientSettings(String tokenUrl, String clientId, String clientSecret) {

	/**
	 * Get the settings as a Map.
	 * 
	 * <p>
	 * The various constants defined in {@link ExternalSystemServiceProperties}
	 * are used as the map keys.
	 * </p>
	 * 
	 * @return the settings as a Map
	 */
	public Map<String, String> asMap() {
		var map = new LinkedHashMap<String, String>();
		if ( tokenUrl != null ) {
			map.put(ExternalSystemServiceProperties.OAUTH_TOKEN_URL, tokenUrl);
		}
		if ( clientId != null ) {
			map.put(ExternalSystemServiceProperties.OAUTH_CLIENT_ID, clientId);
		}
		if ( clientSecret != null ) {
			map.put(ExternalSystemServiceProperties.OAUTH_CLIENT_SECRET, clientSecret);
		}
		return map;
	}

}
