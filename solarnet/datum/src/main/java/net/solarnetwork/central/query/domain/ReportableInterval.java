/* ==================================================================
 * ReportableInterval.java - Aug 5, 2009 3:12:15 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * The overall interval range something can be reported on.
 * 
 * <p>
 * This represents the overall date range of available data within some query
 * set.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public final class ReportableInterval {

	private final ZonedDateTime startDate;
	private final ZonedDateTime endDate;
	private final ZoneId zone;

	/**
	 * Constructor.
	 * 
	 * @param startDate
	 *        the start date
	 * @param endDate
	 *        the end date
	 * @param zone
	 *        the node's time zone
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ReportableInterval(ZonedDateTime startDate, ZonedDateTime endDate, ZoneId zone) {
		super();
		if ( startDate == null ) {
			throw new IllegalArgumentException("The startDate argument must not be null.");
		}
		this.startDate = startDate;
		if ( endDate == null ) {
			throw new IllegalArgumentException("The endDate argument must not be null.");
		}
		this.endDate = endDate;
		if ( zone == null ) {
			throw new IllegalArgumentException("The zone argument must not be null.");
		}
		this.zone = zone;
	}

	/**
	 * Constructor.
	 * 
	 * @param startDate
	 *        the start date
	 * @param endDate
	 *        the end date
	 * @param zone
	 *        the node's time zone
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ReportableInterval(Instant startDate, Instant endDate, ZoneId zone) {
		this(startDate != null && zone != null ? startDate.atZone(zone) : null,
				endDate != null && zone != null ? endDate.atZone(zone) : null, zone);
	}

	/**
	 * Get a count of days this interval spans (inclusive).
	 * 
	 * <p>
	 * This is the complete number of calendar days the data is present in, so
	 * partial days are counted as full days. For example, the interval
	 * {@code 2008-08-11/2009-08-05} returns 360.
	 * </p>
	 * 
	 * @return count of days within the interval
	 */
	public long getDayCount() {
		return ChronoUnit.DAYS.between(startDate, endDate) + 1;
	}

	/**
	 * Get a count of months this interval spans (inclusive).
	 * 
	 * <p>
	 * This is the complete number of calendar months the data is present in, so
	 * partial months are counted as full months. For example, the interval
	 * {@code 2008-08-11/2009-08-05} returns 13.
	 * </p>
	 * 
	 * @return count of months within the interval
	 */
	public long getMonthCount() {
		return ChronoUnit.MONTHS.between(startDate.with(TemporalAdjusters.firstDayOfMonth()),
				endDate.with(TemporalAdjusters.firstDayOfMonth())) + 1;
	}

	/**
	 * Get a count of years this interval spans (inclusive).
	 * 
	 * <p>
	 * This is the complete number of calendar years the data is present in, so
	 * partial years are counted as full years. For example, the interval
	 * {@code 2008-08-11/2009-08-05} returns 2.
	 * </p>
	 * 
	 * @return count of months within the interval
	 */
	public long getYearCount() {
		return ChronoUnit.YEARS.between(startDate.with(TemporalAdjusters.firstDayOfYear()),
				endDate.with(TemporalAdjusters.firstDayOfYear())) + 1;
	}

	/**
	 * Get the start date.
	 * 
	 * @return the startDate
	 */
	public LocalDateTime getStartDate() {
		return startDate.toLocalDateTime();
	}

	/**
	 * Get the end date.
	 * 
	 * @return the endDate
	 */
	public LocalDateTime getEndDate() {
		return endDate.toLocalDateTime();
	}

	/**
	 * Get the end date, in milliseconds since the epoch.
	 * 
	 * @return the end date in milliseconds
	 */
	public Long getEndDateMillis() {
		return endDate.toInstant().toEpochMilli();
	}

	/**
	 * Get the start date, in milliseconds since the epoch.
	 * 
	 * @return the start date in milliseconds
	 */
	public Long getStartDateMillis() {
		return startDate.toInstant().toEpochMilli();
	}

	/**
	 * Get the time zone.
	 * 
	 * @return the zone
	 */
	public ZoneId getTimeZone() {
		return zone;
	}

}
