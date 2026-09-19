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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamIdentity;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * A date/time interval associated with an object datum ID.
 *
 * @author matt
 * @version 2.1
 */
public class DatumDateInterval extends DateInterval {

	private final BasicObjectDatumStreamIdentity objectDatumId;

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
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public static DatumDateInterval streamInterval(Instant start, Instant end, String timeZoneId,
			ObjectDatumKind kind, UUID streamId, Long objectId, String sourceId) {
		return new DatumDateInterval(start, end, timeZoneId,
				new BasicObjectDatumStreamIdentity(streamId, kind, objectId, sourceId));
	}

	/**
	 * Create a new stream interval.
	 *
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param zone
	 *        the time zone
	 * @param kind
	 *        the object kind
	 * @param streamId
	 *        the stream ID
	 * @param objectId
	 *        the stream object ID
	 * @param sourceId
	 *        the stream source ID
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 2.1
	 */
	public static DatumDateInterval streamInterval(Instant start, Instant end, ZoneId zone,
			ObjectDatumKind kind, UUID streamId, Long objectId, String sourceId) {
		return new DatumDateInterval(start, end, zone,
				new BasicObjectDatumStreamIdentity(streamId, kind, objectId, sourceId));
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
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DatumDateInterval(Instant start, Instant end, ZoneId zone,
			BasicObjectDatumStreamIdentity objectId) {
		super(start, end, zone);
		this.objectDatumId = requireNonNullArgument(objectId, "objectId");
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
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DatumDateInterval(Instant start, Instant end, String timeZoneId,
			BasicObjectDatumStreamIdentity objectId) {
		this(start, end, ZoneId.of(timeZoneId), objectId);
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

	/**
	 * Get the object ID.
	 *
	 * @return the objectId
	 */
	public final BasicObjectDatumStreamIdentity getObjectDatumId() {
		return objectDatumId;
	}

}
