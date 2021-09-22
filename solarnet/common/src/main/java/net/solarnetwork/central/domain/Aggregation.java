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
 * @version 1.6
 */
public enum Aggregation {

	/**
	 * No aggregation.
	 * 
	 * @since 1.6
	 */
	None(0, "0"),

	/** Minute level aggregation. */
	Minute(60, "m"),

	/** Five minute level aggregation. */
	FiveMinute(60 * 5, "5m"),

	/** Ten minute level aggregation. */
	TenMinute(60 * 10, "10m"),

	/** Fifteen minute level aggregation. */
	FifteenMinute(60 * 15, "15m"),

	/**
	 * Thirty minute level aggregation.
	 * 
	 * @since 1.5
	 */
	ThirtyMinute(60 * 30, "30m"),

	/** Hour level aggregation. */
	Hour(3600, "h"),

	/**
	 * Aggregate by hour of the day, e.g. compare 12-1pm across multiple days.
	 */
	HourOfDay(3600, "hd"),

	/** Aggregate by hour of the day per season. */
	SeasonalHourOfDay(3600, "shd"),

	/** Day level aggregation. */
	Day(86400, "d"),

	/**
	 * Aggregate by day of the week, e.g. compare Mondays against Tuesdays
	 * across multiple weeks.
	 */
	DayOfWeek(86400, "wd"),

	/** Aggregate by day of the week per season. */
	SeasonalDayOfWeek(86400, "swd"),

	/** Week level aggregation. */
	Week(604800, "w"),

	/**
	 * Aggregate by week of the year, e.g. compare Week 1's against Week 2's
	 * across multiple years.
	 */
	WeekOfYear(604800, "yw"),

	/** Month level aggregation. */
	Month(2419200, "M"),

	/**
	 * Year level aggregation.
	 * 
	 * @since 1.6
	 */
	Year(31536000, "y"),

	/**
	 * Aggregate all values into a single total result.
	 * 
	 * @since 1.4
	 */
	RunningTotal(Integer.MAX_VALUE, "rt");

	private final Integer level;
	private final String key;

	private Aggregation(int level, String key) {
		this.level = level;
		this.key = key;
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
	 * <li><b>Year</b> - 365 days</li>
	 * </ul>
	 * 
	 * @return the aggregation level
	 */
	public Integer getLevel() {
		return level;
	}

	/**
	 * Get a key value.
	 * 
	 * @return the key
	 * @since 1.6
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get an enum instance for a key value.
	 * 
	 * @param key
	 *        the key value; if {@literal null} or empty then {@link #None} will
	 *        be returned
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code key} is not supported
	 * @since 1.6
	 */
	public static Aggregation forKey(String key) {
		if ( key == null || key.isEmpty() ) {
			return None;
		}
		try {
			// try name() value first for convenience
			return Aggregation.valueOf(key);
		} catch ( IllegalArgumentException e ) {
			for ( Aggregation a : Aggregation.values() ) {
				if ( a.key.equals(key) ) {
					return a;
				}
			}
		}
		throw new IllegalArgumentException("Invalid Aggregation value [" + key + "]");
	}

}
