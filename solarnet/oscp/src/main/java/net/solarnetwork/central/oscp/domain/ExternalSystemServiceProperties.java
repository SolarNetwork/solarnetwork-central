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
 * @version 1.0
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
	 */
	String ASSET_MEAESUREMENT = "group-asset-measurement";
}