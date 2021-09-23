/* ==================================================================
 * DatumDateInterval.java - 29/11/2020 8:58:25 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * A date/time interval associated with an object datum ID.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumDateInterval extends DateInterval {

	private final ObjectDatumId objectDatumId;

	/**
	 * Create a new stream interval.
	 * 
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param timeZoneId
	 *        the time zone ID
	 * @param kind
	 *        the object kind
	 * @param streamId
	 *        the stream ID
	 * @param objectId
	 *        the stream object ID
	 * @param sourceId
	 *        the stream source ID
	 * @return the new instance
	 */
	public static DatumDateInterval streamInterval(Instant start, Instant end, String timeZoneId,
			ObjectDatumKind kind, UUID streamId, Long objectId, String sourceId) {
		return new DatumDateInterval(start, end, timeZoneId,
				new ObjectDatumId(kind, streamId, objectId, sourceId, null, null));
	}

	/**
	 * Constructor.
	 * 
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param zone
	 *        the time zone
	 * @param objectId
	 *        the object ID
	 */
	public DatumDateInterval(Instant start, Instant end, ZoneId zone, ObjectDatumId objectId) {
		super(start, end, zone);
		this.objectDatumId = objectId;
	}

	/**
	 * Constructor.
	 * 
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param timeZoneId
	 *        the time zone ID
	 * @param objectId
	 *        the object ID
	 */
	public DatumDateInterval(Instant start, Instant end, String timeZoneId, ObjectDatumId objectId) {
		super(start, end, timeZoneId);
		this.objectDatumId = objectId;
	}

	/**
	 * Get the object ID.
	 * 
	 * @return the objectId
	 */
	public ObjectDatumId getObjectDatumId() {
		return objectDatumId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DatumDateInterval{");
		if ( objectDatumId != null ) {
			builder.append("objectDatumId=");
			builder.append(objectDatumId);
			builder.append(", ");
		}
		if ( getStart() != null ) {
			builder.append("start=");
			builder.append(getStart());
			builder.append(", ");
		}
		if ( getEnd() != null ) {
			builder.append("end=");
			builder.append(getEnd());
			builder.append(", ");
		}
		if ( getZone() != null ) {
			builder.append("zone=");
			builder.append(getZone());
		}
		builder.append("}");
		return builder.toString();
	}

}
