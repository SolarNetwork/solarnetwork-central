/* ==================================================================
 * OscpCapacityProviderWebUtils.java - 23/08/2022 10:41:27 am
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

package net.solarnetwork.oscp.sim.cp.web;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Web-related utilities for OSCP.
 * 
 * @author matt
 * @version 1.0
 */
public final class OscpCapacityProviderWebUtils {

	private OscpCapacityProviderWebUtils() {
		//  not available
	}

	/**
	 * Generate a new token.
	 * 
	 * @return the new token
	 */
	public static String generateToken() {
		SecureRandom rng;
		try {
			rng = SecureRandom.getInstanceStrong();
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException("Cannot generate new token value", e);
		}
		byte[] bytes = new byte[48];
		rng.nextBytes(bytes);
		return Base64.getUrlEncoder().encodeToString(bytes);
	}

}
