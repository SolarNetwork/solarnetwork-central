/* ==================================================================
 * UuidTimeDecoder.java - 4/08/2022 11:33:26 am
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

package net.solarnetwork.central.biz;

import java.time.Instant;
import java.util.UUID;

/**
 * API for a service that knows how to deal with time-based UUID values.
 * 
 * @author matt
 * @version 1.0
 */
public interface UuidTimestampDecoder {

	/**
	 * Decode the timestamp from a UUID.
	 * 
	 * @param uuid
	 *        the UUID to decode the timestamp from
	 * @return the timestamp, or {@literal null} if unable to decode one
	 */
	Instant decodeTimestamp(UUID uuid);

	/**
	 * Create a timestamp "boundary" value.
	 * 
	 * <p>
	 * The returned UUID will have only a time component encoded in it (and the
	 * version/variant bits). All other bits will be set to {@literal 0}.
	 * </p>
	 * 
	 * @param ts
	 *        the timestamp to encode
	 * @return the UUID
	 */
	UUID createTimestampBoundary(Instant ts);

}
