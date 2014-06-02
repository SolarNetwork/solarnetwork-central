/* ===================================================================
 * Aggregation.java
 * 
 * Created Dec 1, 2009 4:10:14 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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
 * ===================================================================
 */

package net.solarnetwork.central.domain;

/**
 * An aggregation level enumeration.
 * 
 * @version 1.2
 */
public enum Aggregation {

	/** Minute level aggregation. */
	Minute(60),

	/** Hour level aggregation. */
	Hour(3600),

	/** Aggregate by hour of the day, e.g. compare 12-1pm across multiple days. */
	HourOfDay(3600),

	/** Aggregate by hour of the day per season. */
	SeasonalHourOfDay(3600),

	/** Day level aggregation. */
	Day(86400),

	/**
	 * Aggregate by day of the week, e.g. compare Mondays against Tuesdays
	 * across multiple weeks.
	 */
	DayOfWeek(86400),

	/** Aggregate by day of the week per season. */
	SeasonalDayOfWeek(86400),

	/** Week level aggregation. */
	Week(604800),

	/**
	 * Aggregate by week of the year, e.g. compare Week 1's against Week 2's
	 * across multiple years.
	 */
	WeekOfYear(604800),

	/** Month level aggregation. */
	Month(2419200);

	private final Integer level;

	private Aggregation(int level) {
		this.level = level;
	}

	/**
	 * Compare the level of this to another.
	 * 
	 * @param other
	 *        the other
	 * @return -1 if this level less than other level, 0 if levels are equal, or
	 *         1 if this level is greater than other level
	 */
	public int compareLevel(Aggregation other) {
		return this.level.compareTo(other.level);
	}

	/**
	 * Get the number of seconds the aggregation level represents.
	 * 
	 * <p>
	 * For aggregation levels higher than {@link #Day} the number of seconds are
	 * approximate, based on the following standardized periods:
	 * </p>
	 * 
	 * <ul>
	 * <li><b>Week</b> - 7 days</li>
	 * <li><b>Month</b> - 4 weeks (28 days)</li>
	 * </ul>
	 * 
	 * @return the aggregation level
	 */
	public Integer getLevel() {
		return level;
	}

}
