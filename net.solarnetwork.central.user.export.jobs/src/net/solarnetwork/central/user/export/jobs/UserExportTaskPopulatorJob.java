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

import java.util.List;
import org.joda.time.DateTime;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;

/**
 * Job to find export tasks that need to be submitted for execution.
 * 
 * @author matt
 * @version 1.0
 */
public class UserExportTaskPopulatorJob extends JobSupport {

	private final ScheduleType scheduleType;
	private final UserDatumExportConfigurationDao configurationDao;
	private final UserDatumExportTaskInfoDao taskDao;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the event admin
	 * @param scheduleType
	 *        the schedule type
	 * @param configurationDao
	 *        the configuration DAO
	 * @param tasskDao
	 *        the task DAO
	 */
	public UserExportTaskPopulatorJob(EventAdmin eventAdmin, ScheduleType scheduleType,
			UserDatumExportConfigurationDao configurationDao, UserDatumExportTaskInfoDao taskDao) {
		super(eventAdmin);
		this.scheduleType = scheduleType;
		this.configurationDao = configurationDao;
		this.taskDao = taskDao;
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		// NOTE the userId is passed in as null to find for all users; in time as the number of users grows
		// we could enhance this job and the DAO API to restrict the execution to a range of user IDs
		// so different jobs could run in parallel without overlapping
		List<UserDatumExportConfiguration> configs = configurationDao.findForExecution(new DateTime(),
				scheduleType);
		for ( UserDatumExportConfiguration config : configs ) {
			try {
				UserDatumExportTaskInfo task = new UserDatumExportTaskInfo();
				task.setConfig(new BasicConfiguration(config));
				taskDao.store(task);
			} catch ( Exception e ) {
				log.error("Error submiting user {} export task for config {}", config.getUserId(),
						config.getId());
			}
		}
		return true;
	}

}
