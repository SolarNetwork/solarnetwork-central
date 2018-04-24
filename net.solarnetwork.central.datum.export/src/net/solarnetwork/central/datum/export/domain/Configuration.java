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

import java.util.LinkedHashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;

/**
 * A complete configuration for a scheduled export job.
 * 
 * @author matt
 * @version 1.0
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

	/** A runtime property for a {@code DateTime} object. */
	String PROP_DATE_TIME = "ts";

	/** A runtime property for a filename extension. */
	String PROP_FILENAME_EXTENSION = "ext";

	/**
	 * Get runtime properties to use for an export at a specific time.
	 * 
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
	default Map<String, Object> getRuntimeProperties(DateTime exportTime,
			DateTimeFormatter dateFormatter, DatumExportOutputFormatService outputFormatService) {
		Map<String, Object> result = new LinkedHashMap<String, Object>(8);
		DateTime ts = exportTime != null ? exportTime : new DateTime();
		result.put(PROP_DATE_TIME, ts);

		String date = (dateFormatter != null ? dateFormatter.print(ts)
				: ISODateTimeFormat.basicDateTimeNoMillis().withZoneUTC().print(ts));
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

		return result;
	}

}
