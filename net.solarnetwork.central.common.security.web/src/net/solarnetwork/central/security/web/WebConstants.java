/* ==================================================================
 * WebConstants.java - Nov 27, 2012 2:23:07 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security.web;

/**
 * Global web constants for the central SolarNetwork applications.
 * 
 * @author matt
 * @version 1.1
 */
public final class WebConstants {

	/** The prefix used for all custom HTTP headers. */
	public static final String HEADER_PREFIX = "X-SN-";

	/** An error message header. */
	public static final String HEADER_ERROR_MESSAGE = HEADER_PREFIX + "ErrorMessage";

	/**
	 * A date header, e.g. for clients that don't have direct access to standard
	 * HTTP Date header.
	 */
	public static final String HEADER_DATE = HEADER_PREFIX + "Date";

	/**
	 * The hex-encoded SHA256 value of an empty string.
	 * 
	 * @since 1.1
	 */
	public static final String EMPTY_STRING_SHA256_HEX = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

	// can't construct me
	private WebConstants() {
		super();
	}

}
