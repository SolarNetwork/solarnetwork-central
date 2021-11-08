/* ==================================================================
 * DatumDeleteJobInfoCleanerJob.java - 13/11/2018 4:19:46 PM
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

package net.solarnetwork.central.user.expire.jobs;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteJobBiz;

/**
 * Delete old job records for completed or abandoned jobs.
 * 
 * @author matt
 * @version 3.0
 */
public class DatumDeleteJobInfoCleanerJob extends JobSupport {

	/** The default value for the {@code minimumAgeMinutes} property. */
	public static final int DEFAULT_MINIMUM_AGE_MINUTES = 720;

	private final UserDatumDeleteJobBiz datumDeleteBiz;
	private int minimumAgeMinutes = DEFAULT_MINIMUM_AGE_MINUTES;

	/**
	 * Constructor.
	 * 
	 * @param datumDeleteBiz
	 *        the service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DatumDeleteJobInfoCleanerJob(UserDatumDeleteJobBiz datumDeleteBiz) {
		super();
		this.datumDeleteBiz = requireNonNullArgument(datumDeleteBiz, "datumDeleteBiz");
		setGroupId("UserExpire");
		setMinimumAgeMinutes(DEFAULT_MINIMUM_AGE_MINUTES);
	}

	@Override
	public void run() {
		Instant date = Instant.now().minus(minimumAgeMinutes, ChronoUnit.MINUTES);
		long result = datumDeleteBiz.purgeOldJobs(date);
		log.info("Purged {} completed/abandoned datum delete tasks older than {} minutes", result,
				minimumAgeMinutes);
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
