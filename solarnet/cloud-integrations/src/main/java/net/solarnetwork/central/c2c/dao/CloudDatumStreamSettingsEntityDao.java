/* ==================================================================
 * CloudDatumStreamSettingsEntityDao.java - 28/10/2024 7:21:50 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.dao;

import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettingsEntity;
import net.solarnetwork.central.c2c.domain.UserSettingsEntity;
import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterableDao;

/**
 * DAO API for {@link CloudDatumStreamSettingsEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudDatumStreamSettingsEntityDao
		extends GenericCompositeKey2Dao<CloudDatumStreamSettingsEntity, UserLongCompositePK, Long, Long>,
		FilterableDao<CloudDatumStreamSettingsEntity, UserLongCompositePK, CloudDatumStreamSettingsFilter> {

	/**
	 * Get settings resolved using {@link UserSettingsEntity} defaults if a
	 * datum stream setting does not exist.
	 *
	 * @param userId
	 *        the owner user ID
	 * @param datumStreamId
	 *        the {@link CloudDatumStreamConfiguration} ID to resolve settings
	 *        for
	 * @return the settings, or {@code defaultSettings} if no datum stream or
	 *         user settings exist
	 */
	CloudDatumStreamSettings resolveSettings(Long userId, Long datumStreamId,
			CloudDatumStreamSettings defaultSettings);
}
