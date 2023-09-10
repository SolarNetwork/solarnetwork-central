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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Utility functions for UUIDs.
 * 
 * @author matt
 * @version 1.1
 * @deprecated use {@code net.solarnetwork.util.UuidUtils} instead
 */
@Deprecated(since = "1.17")
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
		return extractTimestamp(uuid, false);
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
	 * @param assumeMicros
	 *        if {@literal true} then for version 7 UUIDs assume that bits 66-76
	 *        represent microseconds of the timestamp
	 * @return the timestamp, or {@literal null} if unable to extract a
	 *         timestamp
	 * @since 1.1
	 */
	public static Instant extractTimestamp(UUID uuid, boolean assumeMicros) {
		if ( uuid.version() == 7 ) {
			return extractTimestampV7(uuid, assumeMicros);
		} else if ( uuid.version() == 1 ) {
			return Instant.ofEpochMilli(uuid.timestamp());
		}
		return null;
	}

	/**
	 * Extract the timestamp out of a version 7 UUID.
	 * 
	 * @param uuid
	 *        the UUID to extract the timestamp from
	 * @param assumeMicros
	 *        if {@literal true} then assume that bits 66-76 represent
	 *        microseconds of the timestamp
	 * @return the timestamp, or {@literal null} if unable to extract a
	 *         timestamp
	 */
	public static Instant extractTimestampV7(UUID uuid, boolean assumeMicros) {
		if ( uuid.version() == 7 ) {
			// timestamp is highest 48 bits of UUID
			Instant inst = Instant.ofEpochMilli((uuid.getMostSignificantBits() >> 16) & 0xFFFFFFFFFFFFL);
			if ( assumeMicros ) {
				inst = inst.plus((uuid.getMostSignificantBits() & 0xFFF) >> 2, ChronoUnit.MICROS);
			}
			return inst;
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
