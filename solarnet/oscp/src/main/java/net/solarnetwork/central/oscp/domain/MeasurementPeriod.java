/* ==================================================================
 * MeasurementPeriod.java - 14/08/2022 11:24:21 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import com.fasterxml.jackson.annotation.JsonCreator;
import net.solarnetwork.domain.CodedValue;

/**
 * An enumeration of possible measurement periods.
 * 
 * @author matt
 * @version 1.0
 */
public enum MeasurementPeriod implements CodedValue {

	/** Every minute. */
	Minute(60),

	/** Every 5 minutes. */
	FiveMinute(300),

	/** Every 10 minutes. */
	TenMinute(600),

	/** Every 15 minutes. */
	FifteenMinute(900),

	/** Every 20 minutes. */
	TwentyMinute(1200),

	/** Every 10 minutes. */
	ThirtyMinute(1800),

	/** Every hour. */
	Hour(3600),

	;

	private final int seconds;

	private MeasurementPeriod(int seconds) {
		this.seconds = seconds;
	}

	/**
	 * Get the number of seconds represented by this period.
	 * 
	 * @return the number of seconds
	 */
	@Override
	public int getCode() {
		return seconds;
	}

	/**
	 * Get the starting date for a period that contains a given date.
	 * 
	 * @param date
	 *        the date to get the starting period date for
	 * @return the starting period date
	 */
	public Instant periodStart(Instant date) {
		return date.truncatedTo(ChronoUnit.MINUTES)
				.minus((date.atZone(ZoneOffset.UTC).getMinute() * 60) % seconds, ChronoUnit.SECONDS);
	}

	/**
	 * Get the starting date for a period that ends at the start of the period
	 * that contains a given date.
	 * 
	 * @param date
	 *        the date to get the previous starting period date for
	 * @return the previous starting period date
	 */
	public Instant previousPeriodStart(Instant taskDate) {
		return periodStart(taskDate).minus(seconds, ChronoUnit.SECONDS);
	}

	/**
	 * Get the next measurement period start based on a given period start.
	 * 
	 * @param periodStart
	 *        the period start; no validation is performed to check if this is a
	 *        valid starting period date
	 * @return the period start offset by this period
	 */
	public Instant nextPeriodStart(Instant periodStart) {
		return periodStart.plusSeconds(seconds);
	}

	/**
	 * Create an enum instance from a code value.
	 * 
	 * @param code
	 *        the code value
	 * @return the enum instance
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid enum value
	 */
	public static MeasurementPeriod forCode(int code) {
		for ( MeasurementPeriod e : MeasurementPeriod.values() ) {
			if ( code == e.seconds ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Invalid MeasurementPeriod code [" + code + "]");
	}

	/**
	 * Create an enum instance from a string value.
	 * 
	 * @param value
	 *        the string representation; both enum names and code values are
	 *        supported
	 * @return the enum instance
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid enum value
	 */
	@JsonCreator
	public static MeasurementPeriod fromValue(String value) {
		if ( value != null && value.length() > 0 ) {
			final boolean coded = (value.length() == 1);
			final char code = value.charAt(0);
			for ( MeasurementPeriod e : MeasurementPeriod.values() ) {
				if ( coded && code == e.seconds ) {
					return e;
				} else if ( e.name().equalsIgnoreCase(value) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException("Invalid MeasurementPeriod value [" + value + "]");
	}

}
