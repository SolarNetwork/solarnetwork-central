/* ==================================================================
 * OscpWebUtils.java - 11/08/2022 1:28:12 pm
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

package net.solarnetwork.central.oscp.web;

/**
 * Web-related utilities for OSCP.
 * 
 * @author matt
 * @version 1.0
 */
public final class OscpWebUtils {

	/** The HTTP header for a message correlation ID. */
	public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

	/** The HTTP header for an error message. */
	public static final String ERROR_MESSAGE_HEADER = "X-Error-Message";

	/**
	 * The {@code Authorization} HTTP header scheme for OSCP token
	 * authentication.
	 */
	public static final String OSCP_TOKEN_AUTHORIZATION_SCHEME = "Token";

	/** The URL path to the Flexibility Provider API. */
	public static final String FLEXIBILITY_PROVIDER_URL_PATH = "/oscp/fp";

	/**
	 * The URL path to the register endpoint (constant across protocol
	 * versions).
	 */
	public static final String REGISTER_URL_PATH = "/register";

	/** URL paths for OSCP 2.0. */
	public static final class UrlPaths_20 {

		/** The URL path for the version 2.0 API. */
		public static final String V20_URL_PATH = "/2.0";

		/** The URL path to the handshake endpoint. */
		public static final String HANDSHAKE_URL_PATH = "/handshake";

		/** The URL path to the handshake acknowledge endpoint. */
		public static final String HANDSHAKE_ACK_URL_PATH = "/handshake_acknowledge";

		/** The URL path to the heartbeat endpoint. */
		public static final String HEARTBEAT_URL_PATH = "/heartbeat";

		/** The URL path to the update group capacity forecast endpoint. */
		public static final String UPDATE_GROUP_CAPACITY_FORECAST_URL_PATH = "/update_group_capacity_forecast";

		/** The URL path to the adjust group capacity forecast endpoint. */
		public static final String ADJUST_GROUP_CAPACITY_FORECAST_URL_PATH = "/adjust_group_capacity_forecast";

		/** The URL path to the group capacity compliance error endpoint. */
		public static final String GROUP_CAPACITY_COMPLIANCE_ERROR_URL_PATH = "/group_capacity_compliance_error";

		/** The URL path to the update group measurements endpoint. */
		public static final String UPDATE_GROUP_MEASUREMENTS_URL_PATH = "/update_group_measurements";

		/** The URL path to the update asset measurements endpoint. */
		public static final String UPDATE_ASSET_MEASUREMENTS_URL_PATH = "/update_asset_measurements";

		/**
		 * Create a path to the Flexibility Provider API.
		 * 
		 * @param path
		 *        the path, relative to the Flexibility Provider base path
		 * @return the URL path
		 */
		public static String fpUrlPath(String path) {
			return FLEXIBILITY_PROVIDER_URL_PATH + path;
		}

		private UrlPaths_20() {
			// not available
		}

	}

	private OscpWebUtils() {
		// not available
	}

}
