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
import com.fasterxml.jackson.annotation.JsonCreator;

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

	LocusEnergyGranularity(String key) {
		this(key, null);
	}

	LocusEnergyGranularity(String key, Period constraint) {
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

	/**
	 * Get an enum instance for a name or key value.
	 *
	 * @param value
	 *        the enumeration name or key value, case-insensitve
	 * @return the enum; if {@code value} is {@literal null} or empty then
	 *         {@link #Latest} is returned
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static LocusEnergyGranularity fromValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return Latest;
		}
		for ( LocusEnergyGranularity e : LocusEnergyGranularity.values() ) {
			if ( value.equalsIgnoreCase(e.key) || value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown LocusEnergyGranularity value [" + value + "]");
	}

}
