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

package net.solarnetwork.central.datum.domain.export;

/**
 * A complete configuration for a scheduled export job.
 * 
 * @author matt
 * @version 1.0
 */
public interface Configuration {

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

}
