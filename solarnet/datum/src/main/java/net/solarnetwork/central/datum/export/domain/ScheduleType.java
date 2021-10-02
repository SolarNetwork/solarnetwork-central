/* ==================================================================
 * ScheduleType.java - 5/03/2018 8:36:59 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.domain;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;

/**
 * Enumeration of export job schedule options.
 * 
 * @author matt
 * @version 1.1
 * @since 1.23
 */
public enum ScheduleType {

	Hourly('h'),

	Daily('d'),

	Weekly('w'),

	Monthly('m'),

	Adhoc('a');

	private final char key;

	private ScheduleType(char key) {
		this.key = key;
	}

	/**
	 * Get the key value.
	 * 
	 * @return the key value
	 */
	public char getKey() {
		return key;
	}

	/**
	 * Get an enum for a key value.
	 * 
	 * @param key
	 *        the key of the enum to get
	 * @return the enum with the given key
	 * @throws IllegalArgumentException
	 *         if {@code key} is not supported
	 */
	public static ScheduleType forKey(char key) {
		for ( ScheduleType type : ScheduleType.values() ) {
			if ( type.key == key ) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unsupported key: " + key);
	}

	/**
	 * Get a {@link ChronoUnit} for a given schedule.
	 * 
	 * <p>
	 * The returned property will be the appropriate property for rounding on
	 * given this schedule type.
	 * </p>
	 * 
	 * @return the property, never {@literal null}
	 */
	public ChronoUnit dateTimeProperty() {
		switch (this) {
			case Hourly:
				return ChronoUnit.HOURS;

			case Weekly:
				return ChronoUnit.WEEKS;

			case Monthly:
				return ChronoUnit.MONTHS;

			default:
				return ChronoUnit.DAYS;
		}
	}

	/**
	 * Get a {@link DurationFieldType} for this schedule.
	 * 
	 * @return the field type, never {@literal null}
	 */
	public DurationFieldType durationFieldType() {
		DurationFieldType type;
		switch (this) {
			case Hourly:
				type = DurationFieldType.hours();
				break;

			case Weekly:
				type = DurationFieldType.weeks();
				break;

			case Monthly:
				type = DurationFieldType.months();
				break;

			default:
				type = DurationFieldType.days();
		}
		return type;
	}

	/**
	 * Get an appropriate "export date" for a given date.
	 * 
	 * <p>
	 * The returned date can be used as the <em>starting</em> date of an export
	 * task executing at {@code date}. It will be a copy of {@code date} that
	 * has been rounded by flooring based on this schedule type.
	 * </p>
	 * 
	 * <p>
	 * Note for the {@code Adhoc} type {@code date} will be returned, or the
	 * current time if {@literal null}.
	 * </p>
	 * 
	 * @param date
	 *        the date to get an export date for, or {@literal null} for the
	 *        current date
	 * @return the export date
	 */
	public ZonedDateTime exportDate(ZonedDateTime date) {
		ZonedDateTime exportDate = (date != null ? date : ZonedDateTime.now());
		if ( this != Adhoc ) {
			ChronoUnit dateProperty = dateTimeProperty();
			if ( dateProperty == ChronoUnit.MONTHS ) {
				exportDate = exportDate.with(TemporalAdjusters.firstDayOfMonth())
						.truncatedTo(ChronoUnit.DAYS);
			} else if ( dateProperty == ChronoUnit.WEEKS ) {
				exportDate = exportDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
						.truncatedTo(ChronoUnit.DAYS);
			} else {
				exportDate = exportDate.truncatedTo(dateProperty);
			}
		}
		return exportDate;
	}

	/**
	 * Get the "next" export date for a given date.
	 * 
	 * @param date
	 *        the date to get the "next" export date for, or {@literal null} for
	 *        the current date
	 * @return the "next" export date
	 * @see #offsetExportDate(DateTime, int)
	 */
	public ZonedDateTime nextExportDate(ZonedDateTime date) {
		return offsetExportDate(date, 1);
	}

	/**
	 * Get the "previous" export date for a given date.
	 * 
	 * @param date
	 *        the date to get the "previous" export date for, or {@literal null}
	 *        for the current date
	 * @return the "previous" export date
	 * @see #offsetExportDate(DateTime, int)
	 */
	public ZonedDateTime previousExportDate(ZonedDateTime date) {
		return offsetExportDate(date, -1);
	}

	/**
	 * Get an offset export date from a given date.
	 * 
	 * <p>
	 * Note for the {@code Adhoc} type {@link #exportDate(DateTime)} will be
	 * returned, with no offset applied.
	 * </p>
	 * 
	 * @param date
	 *        the date to get the offset export date for, or {@literal null} for
	 *        the current date
	 * @param offset
	 *        the schedule period offset
	 * @return the offset export date
	 */
	public ZonedDateTime offsetExportDate(ZonedDateTime date, int offset) {
		ZonedDateTime exportDate = exportDate(date);
		if ( this == Adhoc ) {
			return exportDate;
		}
		ChronoUnit unit = dateTimeProperty();
		return exportDate.plus(offset, unit);
	}

}
