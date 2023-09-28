/* ==================================================================
 * ExternalSystemServiceProperties.java - 26/08/2022 4:17:22 pm
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

/**
 * Constants and helpers for OSCP external system service property handling.
 * 
 * @author matt
 * @version 1.2
 */
public interface ExternalSystemServiceProperties {

	/**
	 * A service property that configures an OAuth token URL to use for outgoing
	 * system requests.
	 */
	String OAUTH_TOKEN_URL = "oauth-token-url";

	/**
	 * A service property that configures an OAuth client ID to use for outgoing
	 * system requests.
	 */
	String OAUTH_CLIENT_ID = "oauth-client-id";

	/**
	 * A service property that configures an OAuth client secret to use for
	 * outgoing system requests.
	 */
	String OAUTH_CLIENT_SECRET = "oauth-client-secret";

	/**
	 * A service property that configures asset measurement messages to be used
	 * in place of group measurements.
	 * 
	 * <p>
	 * The value should be a boolean, with {@literal true} to enable the group
	 * asset measurement mode.
	 * </p>
	 */
	String ASSET_MEAESUREMENT = "group-asset-measurement";

	/**
	 * A service property that configures additional HTTP header values to be
	 * included in all OSCP requests.
	 * 
	 * <p>
	 * The value associated with this key should be a simple map of string
	 * key/value pairs.
	 * </p>
	 */
	String EXTRA_HTTP_HEADERS = "http-headers";

	/**
	 * A service property that can be used to define custom URL paths for OSCP
	 * action messages to be sent to, instead of the URL paths defined in the
	 * OSCP standard.
	 * 
	 * <p>
	 * The value associated with this key should be a simple map of string
	 * key/value pairs. The supported keys are:
	 * </p>
	 * 
	 * <ul>
	 * <li><code>AdjustGroupCapacityForecast</code></li>
	 * <li><code>GroupCapacityComplianceError</code></li>
	 * <li><code>Heartbeat</code></li>
	 * <li><code>HandshakeAcknowledge</code></li>
	 * <li><code>Register</code></li>
	 * <li><code>UpdateGroupCapacityForecast</code></li>
	 * </ul>
	 */
	String URL_PATHS = "url-paths";

	/**
	 * A service property for a "combined asset" identifier.
	 * 
	 * <p>
	 * If non-{@literal null} then all assets with a capacity group should be
	 * reported as a single, combined asset with this ID.
	 * </p>
	 * 
	 * @since 1.1
	 */
	String COMBINED_ASSET_ID = "group-combined-asset-id";

	/**
	 * An extra HTTP header for a SolarNetwork source ID value.
	 * 
	 * @since 1.2
	 */
	String SOURCE_ID_HEADER = "x-sn-source-id";
}
