/* ==================================================================
 * LocationSourceRange.java - 27/02/2019 7:48:10 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao.mybatis;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalDateTime;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Helper DTO for location and source date ranges.
 * 
 * @author matt
 * @version 1.0
 * @since 3.5
 */
public class LocationSourceRange {

	private Long locationId;
	private String sourceId;
	private DateTime startDate;
	private DateTime endDate;
	private String timeZoneId;
	private Integer timeZoneOffset;
	private LocalDateTime localStartDate;
	private LocalDateTime localEndDate;
	private Aggregation aggregation;

	/**
	 * Create a new range.
	 * 
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param aggregation
	 *        the aggregation
	 * @return the new range
	 */
	public static LocationSourceRange range(DateTime start, DateTime end, Aggregation aggregation) {
		LocationSourceRange r = new LocationSourceRange();
		r.setStartDate(start);
		r.setEndDate(end);
		r.setAggregation(aggregation);
		return r;
	}

	/**
	 * Create a new local range.
	 * 
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param aggregation
	 *        the aggregation
	 * @return the new range
	 */
	public static LocationSourceRange range(LocalDateTime start, LocalDateTime end,
			Aggregation aggregation) {
		LocationSourceRange r = new LocationSourceRange();
		r.setLocalStartDate(start);
		r.setLocalEndDate(end);
		r.setAggregation(aggregation);
		return r;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LocationSourceRange{");
		if ( locationId != null ) {
			builder.append("locationId=");
			builder.append(locationId);
			builder.append(", ");
		}
		if ( sourceId != null ) {
			builder.append("sourceId=");
			builder.append(sourceId);
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
	public Interval getInterval() {
		if ( startDate == null || endDate == null ) {
			return null;
		}
		long d1 = startDate.getMillis();
		long d2 = endDate.getMillis();
		DateTimeZone tz = null;
		if ( timeZoneId != null ) {
			tz = DateTimeZone.forID(timeZoneId);
		}
		return new Interval(d1 < d2 ? d1 : d2, d2 > d1 ? d2 : d1, tz);
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public DateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(DateTime startDate) {
		this.startDate = startDate;
	}

	public DateTime getEndDate() {
		return endDate;
	}

	public void setEndDate(DateTime endDate) {
		this.endDate = endDate;
	}

	public String getTimeZoneId() {
		return timeZoneId;
	}

	public void setTimeZoneId(String timeZone) {
		this.timeZoneId = timeZone;
	}

	public Integer getTimeZoneOffset() {
		return timeZoneOffset;
	}

	public void setTimeZoneOffset(Integer timeZoneOffset) {
		this.timeZoneOffset = timeZoneOffset;
	}

	/**
	 * Get the local start date.
	 * 
	 * @return the start date
	 */
	public LocalDateTime getLocalStartDate() {
		return localStartDate;
	}

	/**
	 * Set the local start date.
	 * 
	 * @param localStartDate
	 *        the start date
	 */
	public void setLocalStartDate(LocalDateTime localStartDate) {
		this.localStartDate = localStartDate;
	}

	/**
	 * Get the local end date.
	 * 
	 * @return the end date
	 */
	public LocalDateTime getLocalEndDate() {
		return localEndDate;
	}

	/**
	 * Get the local end date.
	 * 
	 * @param localEndDate
	 *        the end date
	 */
	public void setLocalEndDate(LocalDateTime localEndDate) {
		this.localEndDate = localEndDate;
	}

	/**
	 * Get the aggregation.
	 * 
	 * @return the aggregation
	 */
	public Aggregation getAggregation() {
		return aggregation;
	}

	/**
	 * Set the aggregation.
	 * 
	 * @param aggregation
	 *        the aggregation to set
	 */
	public void setAggregation(Aggregation aggregation) {
		this.aggregation = aggregation;
	}

}