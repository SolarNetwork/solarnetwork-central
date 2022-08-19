/* ==================================================================
 * ExternalSystemConfigurationDao.java - 18/08/2022 8:32:15 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao;

import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.dao.FilterableDao;

/**
 * DAO API for external system configuration DAOs.
 * 
 * @author matt
 * @version 1.0
 */
public interface ExternalSystemConfigurationDao<C extends BaseOscpExternalSystemConfiguration<C>>
		extends GenericCompositeKey2Dao<C, UserLongCompositePK, Long, Long>,
		FilterableDao<C, UserLongCompositePK, ConfigurationFilter>, ExternalSystemAuthTokenDao {

	/**
	 * Get a persisted entity by its primary key, locking the row for updates
	 * within the current transaction.
	 * 
	 * @param id
	 *        the primary key to retrieve
	 * @return the domain object, or {@literal null} if not available
	 */
	C getForUpdate(UserLongCompositePK id);

	/**
	 * Save system settings for a given configuration.
	 * 
	 * <p>
	 * The configuration must exist prior to saving any settings for it.
	 * </p>
	 * 
	 * @param id
	 *        the primary key to save the settings for
	 * @param settings
	 *        the settings to save
	 */
	void saveSettings(UserLongCompositePK id, SystemSettings settings);

}
