/* ==================================================================
 * AppSettingDao.java - 10/11/2021 8:20:08 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao;

import java.util.Collection;
import net.solarnetwork.central.domain.AppSetting;
import net.solarnetwork.central.domain.KeyTypePK;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for internal application settings.
 * 
 * @author matt
 * @version 1.0
 */
public interface AppSettingDao extends GenericDao<AppSetting, KeyTypePK> {

	/**
	 * Delete all settings with a given key.
	 * 
	 * @param key
	 *        the key of the settings to delete
	 * @return the number of settings deleted
	 */
	int deleteAll(String key);

	/**
	 * Transactionally lock a specific setting in the database.
	 * 
	 * @param key
	 *        the key of the setting to lock
	 * @param type
	 *        the type of the setting to lock
	 * @return the locked setting
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	AppSetting lockForUpdate(String key, String type);

	/**
	 * Transactionally lock a set of settings in the database.
	 * 
	 * @param key
	 *        the key of the setting to lock
	 * @return the locked settings
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	Collection<AppSetting> lockForUpdate(String key);

}
