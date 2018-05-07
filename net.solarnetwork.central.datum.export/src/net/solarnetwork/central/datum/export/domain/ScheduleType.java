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

import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;

/**
 * Enumeration of export job schedule options.
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public enum ScheduleType {

	Hourly('h'),

	Daily('d'),

	Weekly('w'),

	Monthly('m');

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
	 * Get a {@link DateTime.Property} for a given date.
	 * 
	 * <p>
	 * The returned property will be the appropriate property for rounding on
	 * given this schedule type.
	 * </p>
	 * 
	 * @param date
	 *        the date, or {@literal null} for the current date
	 * @return the property, never {@literal null}
	 */
	public DateTime.Property dateTimeProperty(DateTime date) {
		DateTime exportDate = (date != null ? date : new DateTime());
		DateTime.Property dateProperty;
		switch (this) {
			case Hourly:
				dateProperty = exportDate.hourOfDay();
				break;

			case Weekly:
				dateProperty = exportDate.weekOfWeekyear();
				break;

			case Monthly:
				dateProperty = exportDate.monthOfYear();
				break;

			default:
				dateProperty = exportDate.dayOfMonth();
		}
		return dateProperty;
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
	 * @param date
	 *        the date to get an export date for, or {@literal null} for the
	 *        current date
	 * @return the export date
	 */
	public DateTime exportDate(DateTime date) {
		DateTime exportDate = (date != null ? date : new DateTime());
		DateTime.Property dateProperty = dateTimeProperty(exportDate);
		exportDate = dateProperty.roundFloorCopy();
		return exportDate;
	}

	/**
	 * Get the "next" export date for a given date.
	 * 
	 * @param date
	 *        the date to get the "next" export date for, or {@literal null} for
	 *        the current date
	 * @return the "next" export date
	 */
	public DateTime nextExportDate(DateTime date) {
		return offsetExportDate(date, 1);
	}

	/**
	 * Get the "previous" export date for a given date.
	 * 
	 * @param date
	 *        the date to get the "previous" export date for, or {@literal null}
	 *        for the current date
	 * @return the "previous" export date
	 */
	public DateTime previousExportDate(DateTime date) {
		return offsetExportDate(date, -1);
	}

	/**
	 * Get an offset export date from a given date.
	 * 
	 * @param date
	 *        the date to get the offset export date for, or {@literal null} for
	 *        the current date
	 * @param offset
	 *        the schedule period offset
	 * @return the offset export date
	 */
	public DateTime offsetExportDate(DateTime date, int offset) {
		DateTime exportDate = exportDate(date);
		DurationFieldType fieldType = durationFieldType();
		return exportDate.withFieldAdded(fieldType, offset);
	}

}
