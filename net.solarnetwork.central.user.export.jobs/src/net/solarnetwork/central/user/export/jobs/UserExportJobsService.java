/* ==================================================================
 * UserExportJobsService.java - 19/04/2018 6:45:21 AM
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

package net.solarnetwork.central.user.export.jobs;

import org.joda.time.DateTime;
import net.solarnetwork.central.datum.export.domain.ScheduleType;

/**
 * Jobs service API to separate core functionality out of job classes.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserExportJobsService {

	/**
	 * Query for user export configurations that need to be submitted for
	 * execution, and submit them.
	 * 
	 * @param date
	 *        the date to query for (typically the current time)
	 * @param scheduleType
	 *        the type of schedule to look for
	 * @return the count of configurations found
	 */
	public int createExportExecutionTasks(DateTime date, ScheduleType scheduleType);

}
