/* ==================================================================
 * CapacityGroupSettingsDao.java - 10/10/2022 8:38:28 am
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
import net.solarnetwork.central.oscp.domain.CapacityGroupSettings;
import net.solarnetwork.central.oscp.domain.DatumPublishSettings;
import net.solarnetwork.central.oscp.domain.UserSettings;

/**
 * DAO API for {@link CapacityGroupSettings}.
 * 
 * @author matt
 * @version 1.0
 */
public interface CapacityGroupSettingsDao
		extends GenericCompositeKey2Dao<CapacityGroupSettings, UserLongCompositePK, Long, Long> {

	/**
	 * Resolve datum publish settings using {@link UserSettings} defaults if
	 * capacity group settings do not exist for the given ID.
	 * 
	 * @param userId
	 *        the user ID
	 * @param groupId
	 *        the capacity group ID to resolve settings for
	 * @return the settings, or {@literal null} if no capacity group or user
	 *         settings exist
	 */
	DatumPublishSettings resolveDatumPublishSettings(Long userId, Long groupId);

	/**
	 * Resolve datum publish settings using {@link UserSettings} defaults if
	 * capacity group settings do not exist for the given ID.
	 * 
	 * @param userId
	 *        the user ID
	 * @param groupIdentifier
	 *        the capacity group identifier to resolve settings for
	 * @return the settings, or {@literal null} if no capacity group or user
	 *         settings exist
	 */
	DatumPublishSettings resolveDatumPublishSettings(Long userId, String groupIdentifier);

}
