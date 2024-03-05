/* ==================================================================
 * InputDataEntityDao.java - 5/03/2024 10:43:16 am
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

package net.solarnetwork.central.din.dao;

import net.solarnetwork.central.din.domain.InputDataEntity;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for {@link InputDataEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public interface InputDataEntityDao extends GenericDao<InputDataEntity, UserLongStringCompositePK> {

	/**
	 * Get the current data and update it to a new value.
	 *
	 * @param id
	 *        the ID to update
	 * @param data
	 *        the data to save
	 * @return the previous data, or {@literal null} if none available
	 */
	byte[] getAndPut(UserLongStringCompositePK id, byte[] data);

}
