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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.expire.dao.ExpireUserDataConfigurationDao;
import net.solarnetwork.central.user.expire.domain.ExpireUserDataConfiguration;

/**
 * Job to delete datum for user defined expire policies.
 * 
 * @author matt
 * @version 2.0
 */
public class ExpireDatumJob extends JobSupport {

	private final ExpireUserDataConfigurationDao configDao;

	/**
	 * Constructor.
	 * 
	 * @param configDao
	 *        the DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ExpireDatumJob(ExpireUserDataConfigurationDao configDao) {
		super();
		this.configDao = requireNonNullArgument(configDao, "configDao");
		setGroupId("UserExpire");
	}

	@Override
	public void run() {
		List<ExpireUserDataConfiguration> configs = configDao.getAll(null);
		if ( configs == null ) {
			return;
		}
		for ( ExpireUserDataConfiguration config : configs ) {
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
	}
}
