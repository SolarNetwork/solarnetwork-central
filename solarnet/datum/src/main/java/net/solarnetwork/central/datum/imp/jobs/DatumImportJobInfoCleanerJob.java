/* ==================================================================
 * DatumImportJobInfoCleanerJob.java - 13/11/2018 4:19:46 PM
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

package net.solarnetwork.central.datum.imp.jobs;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.datum.imp.biz.DatumImportJobBiz;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Delete old job status records for completed or abandoned jobs.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumImportJobInfoCleanerJob extends JobSupport {

	/** The default value for the {@code minimumAgeMinutes} property. */
	public static final int DEFAULT_MINIMUM_AGE_MINUTES = 720;

	private final DatumImportJobBiz importJobBiz;
	private int minimumAgeMinutes = DEFAULT_MINIMUM_AGE_MINUTES;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param importJobBiz
	 *        the service to use
	 */
	public DatumImportJobInfoCleanerJob(EventAdmin eventAdmin, DatumImportJobBiz importJobBiz) {
		super(eventAdmin);
		this.importJobBiz = importJobBiz;
		setJobGroup("DatumImport");
		setMinimumAgeMinutes(DEFAULT_MINIMUM_AGE_MINUTES);
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		Instant date = Instant.now().minus(minimumAgeMinutes, ChronoUnit.MINUTES);
		long result = importJobBiz.purgeOldJobs(date);
		log.info("Purged {} completed/abandoned datum import tasks older than {} minutes", result,
				minimumAgeMinutes);
		return true;
	}

	/**
	 * Set the minimum age jobs must be to be considered for deletion.
	 * 
	 * @param minimumAgeMinutes
	 *        the minimum age since completion or abandonment, in minutes;
	 *        defaults to {@link #DEFAULT_MINIMUM_AGE_MINUTES}
	 */
	public void setMinimumAgeMinutes(int minimumAgeMinutes) {
		this.minimumAgeMinutes = minimumAgeMinutes;
	}

}
