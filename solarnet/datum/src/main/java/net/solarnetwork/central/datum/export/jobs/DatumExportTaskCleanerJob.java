/* ==================================================================
 * DatumExportTaskCleanerJob.java - 28/04/2018 7:32:38 AM
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

package net.solarnetwork.central.datum.export.jobs;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.solarnetwork.central.datum.export.dao.DatumExportTaskInfoDao;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to delete datum export tasks that have completed processing.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumExportTaskCleanerJob extends JobSupport {

	/** The default value for the {@code minimumAgeMinutes} property. */
	public static final int DEFAULT_MINIMUM_AGE_MINUTES = 720;

	private final DatumExportTaskInfoDao taskDao;

	private int minimumAgeMinutes = DEFAULT_MINIMUM_AGE_MINUTES;

	/**
	 * Constructor.
	 * 
	 * @param taskDao
	 *        the DAO to use
	 */
	public DatumExportTaskCleanerJob(DatumExportTaskInfoDao taskDao) {
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		setGroupId("DatumExport");
		setMinimumAgeMinutes(DEFAULT_MINIMUM_AGE_MINUTES);
	}

	@Override
	public void run() {
		Instant date = Instant.now().minus(minimumAgeMinutes, ChronoUnit.MINUTES);
		long result = taskDao.purgeCompletedTasks(date);
		log.info("Purged {} completed datum export tasks older than {} minutes", result,
				minimumAgeMinutes);
	}

	/**
	 * Set the minimum age completed jobs must be to be considered for deletion.
	 * 
	 * @param minimumAgeMinutes
	 *        the minimum age since completion, in minutes; defaults to
	 *        {@link #DEFAULT_MINIMUM_AGE_MINUTES}
	 */
	public void setMinimumAgeMinutes(int minimumAgeMinutes) {
		this.minimumAgeMinutes = minimumAgeMinutes;
	}

}
