/* ==================================================================
 * Configuration.java - 5/03/2018 8:31:01 PM
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

import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.IsoFields;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;

/**
 * A complete configuration for a scheduled export job.
 *
 * @author matt
 * @version 1.2
 * @since 1.23
 */
public interface Configuration {

	/**
	 * Get a name for this configuration.
	 *
	 * @return a configuration name
	 */
	String getName();

	/**
	 * Get the configuration of what data to export.
	 *
	 * @return the data configuration
	 */
	DataConfiguration getDataConfiguration();

	/**
	 * Get the configuration of the output format of the exported data.
	 *
	 * @return the output configuration
	 */
	OutputConfiguration getOutputConfiguration();

	/**
	 * Get the configuration for the destination of the exported data.
	 *
	 * @return the destination configuration
	 */
	DestinationConfiguration getDestinationConfiguration();

	/**
	 * Get the schedule at which to export the data.
	 *
	 * @return the desired export schedule
	 */
	ScheduleType getSchedule();

	/**
	 * Get the desired time zone for the export.
	 *
	 * @return the time zone
	 */
	String getTimeZoneId();

	/**
	 * Get the minimum number of hours offset before the scheduled export should
	 * run.
	 *
	 * <p>
	 * When configuring an hourly export, for example, a delay of this many
	 * hours is added before exporting the data, to give some leeway to data
	 * that might be posted more slowly.
	 * </p>
	 *
	 * @return an hour delay offset, or {@literal 0} for no delay
	 */
	int getHourDelayOffset();

	/** A runtime property for a formatted date string. */
	String PROP_DATE = "date";

	/** A runtime property for a {@code ZonedDateTime} object. */
	String PROP_DATE_TIME = "ts";

	/** A runtime property for a filename extension. */
	String PROP_FILENAME_EXTENSION = "ext";

	/**
	 * A runtime property for the configuration name, normalized with
	 * {@link #PROP_NAME_SANITIZER}.
	 *
	 * @since 1.1
	 */
	String PROP_JOB_NAME = "jobName";

	/**
	 * A regular expression to remove unfriendly characters from file names
	 * (like the {@link #PROP_JOB_NAME} parameter).
	 *
	 * @since 1.1
	 */
	Pattern PROP_NAME_SANITIZER = Pattern.compile("(?U)[^\\w\\._-]+");

	/**
	 * A runtime property for the job export process time, as a millisecond Unix
	 * epoch integer.
	 *
	 * @since 1.1
	 */
	String PROP_CURRENT_TIME = "now";

	/**
	 * A runtime property for the export job/task ID.
	 *
	 * @since 1.2
	 */
	String PROP_EXPORT_ID = "id";

	/**
	 * A runtime property for the configuration name.
	 *
	 * @since 1.2
	 */
	String PROP_NAME = "name";

	// @formatter:off
	/** A formatter for week, in {@literal YYYY'W'WWD} form. */
    DateTimeFormatter WEEK_DATE = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(IsoFields.WEEK_BASED_YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral("W")
                .appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 2)
                .appendValue(DAY_OF_WEEK, 1)
                .toFormatter();
    // @formatter:on

	/**
	 * Get runtime properties to use for an export at a specific time.
	 *
	 * @param request
	 *        the export request
	 * @param exportTime
	 *        the time of the export
	 * @param dateFormatter
	 *        a formatter to use; if not provided an ISO timestamp formatter
	 *        will be used
	 * @param outputFormatService
	 *        the output format service to determine the filename extension to
	 *        use
	 * @return the properties, never {@literal null}
	 */
	default Map<String, Object> createRuntimeProperties(DatumExportRequest request,
			DateTimeFormatter dateFormatter, DatumExportOutputFormatService outputFormatService) {
		Map<String, Object> result = new LinkedHashMap<String, Object>(8);

		if ( request != null && request.getId() != null ) {
			result.put(PROP_EXPORT_ID, request.getId());
		}

		ZoneId zone = (getTimeZoneId() != null ? ZoneId.of(getTimeZoneId()) : ZoneOffset.UTC);
		ZonedDateTime ts = (request != null && request.getExportDate() != null ? request.getExportDate()
				: Instant.now()).atZone(zone);
		result.put(PROP_DATE_TIME, ts);

		DateTimeFormatter fmt = (dateFormatter != null ? dateFormatter
				: createDateTimeFormatterForSchedule()).withZone(zone);
		String date = fmt.format(ts);
		result.put(PROP_DATE, date);

		String ext = null;
		if ( outputFormatService != null ) {
			ext = outputFormatService.getExportFilenameExtension();
		}
		if ( ext != null ) {
			OutputConfiguration outpConfig = getOutputConfiguration();
			if ( outpConfig != null ) {
				OutputCompressionType compressType = outpConfig.getCompressionType();
				if ( compressType != null ) {
					String compressExt = compressType.getFilenameExtension();
					if ( !compressExt.isEmpty() ) {
						ext += "." + compressExt;
					}
				}
			}
			result.put(PROP_FILENAME_EXTENSION, ext);
		}

		if ( getName() != null ) {
			result.put(PROP_NAME, getName());
			result.put(PROP_JOB_NAME, PROP_NAME_SANITIZER.matcher(getName()).replaceAll("_"));
		}

		result.put(PROP_CURRENT_TIME, System.currentTimeMillis());

		return result;
	}

	/**
	 * Get a default {@link DateTimeFormatter} based on the configured schedule
	 * type in the time zone specified by {@link #getTimeZoneId()}.
	 *
	 * <p>
	 * If no time zone is available, {@literal UTC} will be used.
	 * </p>
	 *
	 * @return the formatter, never {@literal null}
	 */
	default DateTimeFormatter createDateTimeFormatterForSchedule() {
		ScheduleType schedule = getSchedule();
		if ( schedule == null ) {
			schedule = ScheduleType.Daily;
		}
		ZoneId zone = (getTimeZoneId() != null ? ZoneId.of(getTimeZoneId()) : ZoneOffset.UTC);
		switch (schedule) {
			case Hourly:
				return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(zone);

			case Weekly:
				return WEEK_DATE.withZone(zone);

			case Monthly:
				return DateTimeFormatter.ofPattern("yyyy-MM").withZone(zone);

			default:
				return DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zone);

		}
	}

}
