/* ==================================================================
 * ExpireUserDataConfigurationDao.java - 9/07/2018 10:12:13 AM
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

package net.solarnetwork.central.user.expire.dao;

import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.user.dao.UserConfigurationDao;
import net.solarnetwork.central.user.dao.UserRelatedGenericDao;
import net.solarnetwork.central.user.expire.domain.ExpireUserDataConfiguration;

/**
 * DAO API for {@link ExpireUserDataConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface ExpireUserDataConfigurationDao
		extends UserRelatedGenericDao<ExpireUserDataConfiguration, Long>,
		UserConfigurationDao<ExpireUserDataConfiguration, Long> {

	/**
	 * Count all expired data for a given configuration.
	 * 
	 * <p>
	 * These results show the number of records that <i>would</i> be deleted by
	 * calling
	 * {@link #deleteExpiredDataForConfiguration(ExpireUserDataConfiguration)}.
	 * </p>
	 * 
	 * @param config
	 *        the configuration to delete expired data for
	 * @return the count of expired data deleted
	 */
	DatumRecordCounts countExpiredDataForConfiguration(ExpireUserDataConfiguration config);

	/**
	 * Delete all expired data for a given configuration.
	 * 
	 * @param config
	 *        the configuration to delete expired data for
	 * @return the count of expired data deleted
	 */
	long deleteExpiredDataForConfiguration(ExpireUserDataConfiguration config);

}
