/* ==================================================================
 * DatumHttpFunctions.java - 13/03/2025 2:56:00 pm
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

package net.solarnetwork.central.datum.domain;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * API for datum-related HTTP helper functions.
 *
 * @author matt
 * @version 1.0
 */
public interface DatumHttpFunctions {

	/**
	 * Encode the given username and password into an HTTP Basic Authentication
	 * header value using the {@linkplain StandardCharsets#ISO_8859_1
	 * ISO-8859-1} character set.
	 *
	 * @param username
	 *        the username
	 * @param password
	 *        the password
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 */
	default String httpBasic(String username, String password) {
		return "Basic " + Base64.getEncoder().encodeToString(
				"%s:%s".formatted(username, password).getBytes(StandardCharsets.ISO_8859_1));
	}

}
