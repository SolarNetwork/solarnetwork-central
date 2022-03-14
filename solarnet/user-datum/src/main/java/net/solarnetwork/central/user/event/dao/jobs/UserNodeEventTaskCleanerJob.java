/* ==================================================================
 * UserNodeEventTaskCleanerJob.java - 8/06/2020 2:23:54 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.dao.jobs;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.event.dao.UserNodeEventTaskDao;
import net.solarnetwork.util.ObjectUtils;

/**
 * Job to periodically delete completed/abandoned user node event tasks.
 * 
 * @author matt
 * @version 2.0
 */
public class UserNodeEventTaskCleanerJob extends JobSupport {

	/** The default value for the {@code minimumAgeMinutes} property. */
	public static final int DEFAULT_MINIMUM_AGE_MINUTES = 720;

	private final UserNodeEventTaskDao taskDao;
	private int minimumAgeMinutes = DEFAULT_MINIMUM_AGE_MINUTES;

	/**
	 * Constructor.
	 * 
	 * @param taskDao
	 *        the task DAO to use
	 */
	public UserNodeEventTaskCleanerJob(UserNodeEventTaskDao taskDao) {
		super();
		this.taskDao = ObjectUtils.requireNonNullArgument(taskDao, "taskDao");
		setGroupId("UserNodeEvent");
	}

	@Override
	public void run() {
		Instant date = Instant.now().minus(minimumAgeMinutes, ChronoUnit.MINUTES);
		long result = taskDao.purgeCompletedTasks(date);
		log.info("Purged {} completed/abandoned user node event tasks older than {} minutes", result,
				minimumAgeMinutes);
	}

	/**
	 * Set the minimum age tasks must be to be considered for deletion.
	 * 
	 * @param minimumAgeMinutes
	 *        the minimum age since completion or abandonment, in minutes;
	 *        defaults to {@link #DEFAULT_MINIMUM_AGE_MINUTES}
	 */
	public void setMinimumAgeMinutes(int minimumAgeMinutes) {
		this.minimumAgeMinutes = minimumAgeMinutes;
	}

}
