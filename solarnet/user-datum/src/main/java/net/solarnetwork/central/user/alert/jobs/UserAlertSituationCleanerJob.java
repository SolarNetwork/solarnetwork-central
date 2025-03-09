/* ==================================================================
 * UserAlertSituationCleaner.java - 19/05/2015 7:21:05 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.alert.jobs;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.dao.UserAlertSituationDao;

/**
 * Job to periodically clean out old, resolved user alert situations.
 * 
 * @author matt
 * @version 1.1
 */
public class UserAlertSituationCleanerJob extends JobSupport {

	/** The default value for the {@code daysOlder} property. */
	public static final int DEFAULT_DAYS_OLDER = 30;

	private final InstantSource clock;
	private final UserAlertSituationDao dao;
	private int daysOlder = DEFAULT_DAYS_OLDER;

	/**
	 * Constructor.
	 * 
	 * @param clock
	 *        the clock to use
	 * @param userAlertSituationDao
	 *        The {@link UserAlertSituationDao} to use.
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserAlertSituationCleanerJob(InstantSource clock,
			UserAlertSituationDao userAlertSituationDao) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.dao = requireNonNullArgument(userAlertSituationDao, "userAlertSituationDao");
		setGroupId("UserAlert");
	}

	@Override
	public void run() {
		Instant date = clock.instant().truncatedTo(ChronoUnit.MINUTES).minus(daysOlder, ChronoUnit.DAYS);
		long result = dao.purgeResolvedSituations(date);
		log.info("Purged {} user alert situations older than {} ({} days ago)", result, date, daysOlder);
	}

	/**
	 * Get the "days older" value.
	 * 
	 * @return the minimum number of days older to purge
	 */
	public int getDaysOlder() {
		return daysOlder;
	}

	/**
	 * Set the "days older" value.
	 * 
	 * @param daysOlder
	 *        the minimum number of days older to purge
	 */
	public void setDaysOlder(int daysOlder) {
		this.daysOlder = daysOlder;
	}

}
