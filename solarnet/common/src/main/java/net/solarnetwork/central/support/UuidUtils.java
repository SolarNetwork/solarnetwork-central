/* ==================================================================
 * UuidUtils.java - 4/08/2022 9:44:31 am
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

package net.solarnetwork.central.support;

import java.time.Instant;
import java.util.UUID;

/**
 * Utility functions for UUIDs.
 * 
 * @author matt
 * @version 1.0
 */
public final class UuidUtils {

	private UuidUtils() {
		// not available
	}

	/**
	 * Extract the timestamp out of a UUID.
	 * 
	 * <p>
	 * Only UUID versions 1 and 7 are supported.
	 * </p>
	 * 
	 * @param uuid
	 *        the UUID to extract the timestamp from
	 * @return the timestamp, or {@literal null} if unable to extract a
	 *         timestamp
	 */
	public static Instant extractTimestamp(UUID uuid) {
		if ( uuid.version() == 7 ) {
			// timestamp is highest 48 bits of UUID
			return Instant.ofEpochMilli((uuid.getMostSignificantBits() >> 16) & 0xFFFFFFFFFFFFL);
		} else if ( uuid.version() == 1 ) {
			return Instant.ofEpochMilli(uuid.timestamp());
		}
		return null;
	}

	/**
	 * Generate a UUID v7 "boundary" value that encodes a given timestamp.
	 * 
	 * @param ts
	 *        the timestamp to encode
	 * @return the UUID
	 */
	public static UUID createUuidV7Boundary(Instant ts) {
		return TimeBasedV7UuidGenerator.createBoundary(ts);
	}
}