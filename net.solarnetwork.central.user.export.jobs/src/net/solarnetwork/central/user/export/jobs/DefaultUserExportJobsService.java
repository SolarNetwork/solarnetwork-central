/* ==================================================================
 * DefaultUserExportJobsService.java - 19/04/2018 6:37:47 AM
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
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.user.export.biz.UserExportTaskBiz;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;

/**
 * Helper class for user export jobs.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultUserExportJobsService implements UserExportJobsService {

	private final UserDatumExportConfigurationDao configurationDao;
	private final UserExportTaskBiz taskBiz;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param configurationDao
	 *        the configuration DAO
	 * @param taskDao
	 *        the task DAO
	 * @param taskBiz
	 *        the task service
	 */
	public DefaultUserExportJobsService(UserDatumExportConfigurationDao configurationDao,
			UserExportTaskBiz taskBiz) {
		super();
		this.configurationDao = configurationDao;
		this.taskBiz = taskBiz;
	}

	@Override
	public int createExportExecutionTasks(DateTime date, ScheduleType scheduleType) {
		// NOTE in time as the number of users grows we could enhance this job and the DAO API to restrict 
		// the execution to a range of user IDs so different jobs could run in parallel without overlapping
		if ( scheduleType == null ) {
			throw new IllegalArgumentException("scheduleType must be provided");
		}
		List<UserDatumExportConfiguration> configs = configurationDao.findForExecution(date,
				scheduleType);
		for ( UserDatumExportConfiguration config : configs ) {

			DateTimeZone userTz = (config.getTimeZoneId() != null
					? DateTimeZone.forID(config.getTimeZoneId())
					: DateTimeZone.UTC);

			DateTime maxExportDate = scheduleType.exportDate(date.withZone(userTz));
			while ( scheduleType.nextExportDate(maxExportDate).isAfterNow() ) {
				maxExportDate = scheduleType.previousExportDate(maxExportDate);
			}

			DateTime currExportDate = scheduleType
					.exportDate(config.getMinimumExportDate().withZone(userTz));
			if ( currExportDate == null ) {
				currExportDate = maxExportDate;
			}
			try {
				while ( !currExportDate.isAfter(maxExportDate) ) {
					UserDatumExportTaskInfo task = taskBiz.submitDatumExportConfiguration(config,
							currExportDate);
					if ( task != null ) {
						log.info("Submitted user datum export task {}", task);
					}
					currExportDate = scheduleType.nextExportDate(currExportDate);
				}
			} catch ( Exception e ) {
				log.error("Error submiting user {} export task for config {}", config.getUserId(),
						config.getId(), e);
			}
		}

		return configs.size();
	}

}
