/* ==================================================================
 * ExpireDatumJob.java - 10/07/2018 7:15:11 AM
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

import java.util.List;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.expire.dao.UserDataConfigurationDao;
import net.solarnetwork.central.user.expire.domain.UserDataConfiguration;

/**
 * Job to delete datum for user defined expire policies.
 * 
 * @author matt
 * @version 1.0
 */
public class ExpireDatumJob extends JobSupport {

	private final UserDataConfigurationDao configDao;

	public ExpireDatumJob(EventAdmin eventAdmin, UserDataConfigurationDao configDao) {
		super(eventAdmin);
		this.configDao = configDao;
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		List<UserDataConfiguration> configs = configDao.getAll(null);
		if ( configs == null ) {
			return true;
		}
		for ( UserDataConfiguration config : configs ) {
			if ( !config.isActive() ) {
				continue;
			}
			long start = System.currentTimeMillis();
			long count = configDao.deleteExpiredDataForConfiguration(config);
			if ( count > 0 && log.isInfoEnabled() ) {
				log.info("Deleted {} datum in {}s for user {} older than {} days matching policy {}",
						count, (System.currentTimeMillis() - start) / 1000, config.getUserId(),
						config.getExpireDays(), config.getFilterJson());
			}
		}
		return true;
	}
}
