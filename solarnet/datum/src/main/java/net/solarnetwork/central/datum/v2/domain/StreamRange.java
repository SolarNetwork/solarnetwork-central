/* ==================================================================
 * StreamRange.java - 2/12/2020 12:46:40 pm
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Helper DTO for node and source date ranges.
 *
 * @author matt
 * @version 1.1
 * @since 2.8
 */
public class StreamRange {

	private final UUID streamId;
	private @Nullable Instant startDate;
	private @Nullable Instant endDate;
	private @Nullable String timeZoneId;
	private @Nullable Integer timeZoneOffset;
	private @Nullable LocalDateTime localStartDate;
	private @Nullable LocalDateTime localEndDate;
	private @Nullable Aggregation aggregation;

	/**
	 * Constructor.
	 *
	 * @param streamId
	 *        the stream ID
	 * @throws IllegalArgumentException
	 *         if {@code streamId} is {@code null}
	 */
	public StreamRange(UUID streamId) {
		super();
		this.streamId = requireNonNullArgument(streamId, "streamId");
	}

	/**
	 * Create a new range.
	 *
	 * @param streamId
	 *        the stream ID
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param aggregation
	 *        the aggregation
	 * @return the new range
	 * @throws IllegalArgumentException
	 *         if {@code streamId} is {@code null}
	 */
	public static StreamRange range(UUID streamId, @Nullable Instant start, @Nullable Instant end,
			@Nullable Aggregation aggregation) {
		StreamRange r = new StreamRange(streamId);
		r.setStartDate(start);
		r.setEndDate(end);
		r.setAggregation(aggregation);
		return r;
	}

	/**
	 * Create a new local range.
	 *
	 * @param streamId
	 *        the stream ID
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param aggregation
	 *        the aggregation
	 * @return the new range
	 */
	public static StreamRange range(UUID streamId, @Nullable LocalDateTime start,
			@Nullable LocalDateTime end, @Nullable Aggregation aggregation) {
		StreamRange r = new StreamRange(streamId);
		r.setLocalStartDate(start);
		r.setLocalEndDate(end);
		r.setAggregation(aggregation);
		return r;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StreamRange{");
		if ( streamId != null ) {
			builder.append("streamId=");
			builder.append(streamId);
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
		if ( localStartDate != null ) {
			builder.append("localStartDate=");
			builder.append(localStartDate);
			builder.append(", ");
		}
		if ( localEndDate != null ) {
			builder.append("localEndDate=");
			builder.append(localEndDate);
			builder.append(", ");
		}
		if ( timeZoneId != null ) {
			builder.append("timeZoneId=");
			builder.append(timeZoneId);
			builder.append(", ");
		}
		if ( timeZoneOffset != null ) {
			builder.append("timeZoneOffset=");
			builder.append(timeZoneOffset);
			builder.append(", ");
		}
		if ( aggregation != null ) {
			builder.append("aggregation=");
			builder.append(aggregation);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get an interval out of the start/end date range.
	 *
	 * @return the interval
	 */
	public final @Nullable DateInterval getInterval() {
		if ( startDate == null || endDate == null ) {
			return null;
		}
		ZoneId tz = null;
		if ( timeZoneId != null ) {
			tz = ZoneId.of(timeZoneId);
		}
		return new DateInterval(startDate.isBefore(endDate) ? startDate : endDate,
				endDate.isAfter(startDate) ? endDate : startDate, tz);
	}

	/**
	 * Get the stream ID.
	 *
	 * @return the stream ID (never {@code null}
	 */
	public final UUID getStreamId() {
		return streamId;
	}

	/**
	 * Get the start date.
	 *
	 * @return the start date
	 */
	public final @Nullable Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 *
	 * @param startDate
	 *        the date to set
	 */
	public final void setStartDate(@Nullable Instant startDate) {
		this.startDate = startDate;
	}

	/**
	 * Get the end date.
	 *
	 * @return the end date
	 */
	public final @Nullable Instant getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date.
	 *
	 * @param endDate
	 *        the date to set
	 */
	public final void setEndDate(@Nullable Instant endDate) {
		this.endDate = endDate;
	}

	/**
	 * Get the time zone ID.
	 *
	 * @return the time zone ID
	 */
	public final @Nullable String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Set the time zone ID.
	 *
	 * @param timeZone
	 *        the time zone ID
	 */
	public final void setTimeZoneId(@Nullable String timeZone) {
		this.timeZoneId = timeZone;
	}

	/**
	 * Get the time zone offset.
	 *
	 * @return the time zone offset
	 */
	public final @Nullable Integer getTimeZoneOffset() {
		return timeZoneOffset;
	}

	/**
	 * Set the time zone offset.
	 *
	 * @param timeZoneOffset
	 *        the offset to set
	 */
	public final void setTimeZoneOffset(@Nullable Integer timeZoneOffset) {
		this.timeZoneOffset = timeZoneOffset;
	}

	/**
	 * Get the local start date.
	 *
	 * @return the start date
	 */
	public final @Nullable LocalDateTime getLocalStartDate() {
		return localStartDate;
	}

	/**
	 * Set the local start date.
	 *
	 * @param localStartDate
	 *        the start date
	 */
	public final void setLocalStartDate(@Nullable LocalDateTime localStartDate) {
		this.localStartDate = localStartDate;
	}

	/**
	 * Get the local end date.
	 *
	 * @return the end date
	 */
	public final @Nullable LocalDateTime getLocalEndDate() {
		return localEndDate;
	}

	/**
	 * Get the local end date.
	 *
	 * @param localEndDate
	 *        the end date
	 */
	public final void setLocalEndDate(@Nullable LocalDateTime localEndDate) {
		this.localEndDate = localEndDate;
	}

	/**
	 * Get the aggregation.
	 *
	 * @return the aggregation
	 */
	public final @Nullable Aggregation getAggregation() {
		return aggregation;
	}

	/**
	 * Set the aggregation.
	 *
	 * @param aggregation
	 *        the aggregation to set
	 */
	public final void setAggregation(@Nullable Aggregation aggregation) {
		this.aggregation = aggregation;
	}

}
