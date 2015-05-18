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

package net.solarnetwork.central.user.alerts;

import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.dao.UserAlertSituationDao;
import org.joda.time.DateTime;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Job to periodically clean out old, resolved user alert situations.
 * 
 * @author matt
 * @version 1.0
 */
public class UserAlertSituationCleaner extends JobSupport {

	/** The default value for the {@code daysOlder} property. */
	public static final int DEFAULT_DAYS_OLDER = 30;

	private final UserAlertSituationDao dao;
	private int daysOlder = DEFAULT_DAYS_OLDER;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        The {@link EventAdmin} to use.
	 * @param dao
	 *        The {@link UserAlertSituationDao} to use.
	 */
	public UserAlertSituationCleaner(EventAdmin eventAdmin, UserAlertSituationDao userAlertSituationDao) {
		super(eventAdmin);
		this.dao = userAlertSituationDao;
	}

	/**
	 * Purge completed situations by calling
	 * {@link UserAlertSituationDao#purgeResolvedSituations(DateTime)}.
	 */
	@Override
	protected boolean handleJob(Event job) throws Exception {
		DateTime date = new DateTime().minusDays(daysOlder);
		long result = dao.purgeResolvedSituations(date);
		log.info("Purged {} user alert situations older than {} ({} days ago)", result, date, daysOlder);
		return true;
	}

	public int getDaysOlder() {
		return daysOlder;
	}

	public void setDaysOlder(int daysOlder) {
		this.daysOlder = daysOlder;
	}

}
