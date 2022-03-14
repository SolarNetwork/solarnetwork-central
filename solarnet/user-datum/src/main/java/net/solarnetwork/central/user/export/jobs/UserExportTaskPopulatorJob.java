/* ==================================================================
 * UserExportTaskPopulatorJob.java - 18/04/2018 7:45:34 AM
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to find export tasks that need to be submitted for execution.
 * 
 * @author matt
 * @version 2.0
 */
public class UserExportTaskPopulatorJob extends JobSupport {

	private final ScheduleType scheduleType;
	private final UserExportJobsService jobsService;

	/**
	 * Constructor.
	 * 
	 * @param scheduleType
	 *        the schedule type
	 * @param jobsService
	 *        the helper
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserExportTaskPopulatorJob(ScheduleType scheduleType, UserExportJobsService jobsService) {
		super();
		this.scheduleType = requireNonNullArgument(scheduleType, "scheduleType");
		this.jobsService = requireNonNullArgument(jobsService, "jobsService");
		setGroupId("UserExport");
	}

	@Override
	public void run() {
		int count = jobsService.createExportExecutionTasks(Instant.now(), scheduleType);
		log.info("Found {} {} user export configurations for execution", count, scheduleType);
	}

}
