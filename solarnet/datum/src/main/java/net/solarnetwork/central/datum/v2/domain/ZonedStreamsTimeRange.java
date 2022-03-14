/* ==================================================================
 * ZonedStreamsTimeRange.java - 14/11/2020 7:39:16 am
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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

/**
 * An absolute date range for a specific time zone and set of stream IDs.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class ZonedStreamsTimeRange {

	private final Instant startDate;
	private final Instant endDate;
	private final String timeZoneId;
	private final UUID[] streamIds;

	/**
	 * Constructor.
	 * 
	 * @param startDate
	 *        the start date
	 * @param endDate
	 *        the end date
	 * @param timeZoneId
	 *        the time zone ID
	 * @param streamIds
	 *        the stream IDs
	 */
	public ZonedStreamsTimeRange(Instant startDate, Instant endDate, String timeZoneId,
			UUID... streamIds) {
		super();
		this.startDate = startDate;
		this.endDate = endDate;
		this.timeZoneId = timeZoneId;
		this.streamIds = streamIds;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ZonedStreamsTimeRange{");
		if ( timeZoneId != null ) {
			builder.append("timeZoneId=");
			builder.append(timeZoneId);
			builder.append(", ");
		}
		if ( startDate != null ) {
			builder.append("startDate=");
			builder.append(startDate);
			builder.append(", ");
		}
		if ( endDate != null ) {
			builder.append("endDate=");
			builder.append(endDate);
			builder.append(", ");
		}
		if ( streamIds != null ) {
			builder.append("streamIds=");
			builder.append(Arrays.toString(streamIds));
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the start date.
	 * 
	 * @return the start date
	 */
	public Instant getStartDate() {
		return startDate;
	}

	/**
	 * Get the end date.
	 * 
	 * @return the end date
	 */
	public Instant getEndDate() {
		return endDate;
	}

	/**
	 * Get the time zone ID.
	 * 
	 * @return the time zone ID
	 */
	public String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Get a {@link ZoneId} for the configured time zone ID.
	 * 
	 * @return the zone, or {@literal null} if one cannot be determined
	 */
	public ZoneId zoneId() {
		try {
			return ZoneId.of(timeZoneId);
		} catch ( DateTimeException | NullPointerException e ) {
			// ignore
			return null;
		}
	}

	/**
	 * Get the stream IDs.
	 * 
	 * @return the stream IDs
	 */
	public UUID[] getStreamIds() {
		return streamIds;
	}

}
