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

import org.joda.time.DateTime;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.datum.export.dao.DatumExportTaskInfoDao;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to delete datum export tasks that have completed processing.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumExportTaskCleanerJob extends JobSupport {

	/** The default value for the {@code minimumAgeMinutes} property. */
	public static final int DEFAULT_MINIMUM_AGE_MINUTES = 720;

	private final DatumExportTaskInfoDao taskDao;

	private int minimumAgeMinutes = DEFAULT_MINIMUM_AGE_MINUTES;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param taskDao
	 *        the DAO to use
	 */
	public DatumExportTaskCleanerJob(EventAdmin eventAdmin, DatumExportTaskInfoDao taskDao) {
		super(eventAdmin);
		this.taskDao = taskDao;
		setJobGroup("DatumExport");
		setMinimumAgeMinutes(DEFAULT_MINIMUM_AGE_MINUTES);
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		DateTime date = new DateTime().minusMinutes(minimumAgeMinutes);
		long result = taskDao.purgeCompletedTasks(date);
		log.info("Purged {} completed datum export tasks older than {} minutes", result,
				minimumAgeMinutes);
		return true;
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
