/* ==================================================================
 * UserNodeInstructionTaskDao.java - 10/11/2025 3:44:57 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao;

import net.solarnetwork.central.common.dao.ClaimableTaskDao;
import net.solarnetwork.central.common.dao.FilterableDeleteDao;
import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.dao.FilterableDao;

/**
 * DAO API for {@link UserNodeInstructionTaskEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public interface UserNodeInstructionTaskDao extends
		GenericCompositeKey2Dao<UserNodeInstructionTaskEntity, UserLongCompositePK, Long, Long>,
		FilterableDao<UserNodeInstructionTaskEntity, UserLongCompositePK, UserNodeInstructionTaskFilter>,
		FilterableDeleteDao<UserNodeInstructionTaskFilter>,
		ClaimableTaskDao<UserNodeInstructionTaskEntity, UserLongCompositePK> {

}
