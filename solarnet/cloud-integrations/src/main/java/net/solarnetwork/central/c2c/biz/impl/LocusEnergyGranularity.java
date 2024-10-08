/* ==================================================================
 * LocusEnergyGranularity.java - 9/10/2024 6:45:11 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.impl;

import java.time.Period;

/**
 * Enumeration of Locus Energy data granularity values.
 *
 * @author matt
 * @version 1.0
 */
public enum LocusEnergyGranularity {

	/** The latest available. */
	Latest("latest"),

	/** One minute granularity. */
	OneMinute("1min", Period.ofDays(5)),

	/** Five minute granularity. */
	FiveMinute("5min", Period.ofDays(31)),

	/** Fifteen minute granularity. */
	FifteenMinute("15min", Period.ofDays(31)),

	/** One hour granularity. */
	Hour("hourly", Period.ofDays(31)),

	/** One day granularity. */
	Day("daily", Period.ofYears(1)),

	/** One month granularity. */
	Month("monthly"),

	/** One year granularity. */
	Year("yearly"),

	;

	private final String key;
	private final Period constraint;

	private LocusEnergyGranularity(String key) {
		this(key, null);
	}

	private LocusEnergyGranularity(String key, Period constraint) {
		this.key = key;
		this.constraint = constraint;
	}

	/**
	 * Get the key.
	 *
	 * @return the key, never {@literal null}
	 */
	public final String getKey() {
		return key;
	}

	/**
	 * Get the query time range constraint.
	 *
	 * @return the maximum query time range, or {@literal null} if there is no
	 *         limit
	 */
	public final Period getConstraint() {
		return constraint;
	}

}
