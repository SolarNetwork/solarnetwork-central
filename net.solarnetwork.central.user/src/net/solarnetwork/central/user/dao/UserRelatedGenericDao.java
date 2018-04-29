/* ==================================================================
 * UserRelatedGenericDao.java - 17/04/2018 10:44:57 AM
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

package net.solarnetwork.central.user.dao;

import java.io.Serializable;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.user.domain.UserRelatedEntity;

/**
 * Extension of {@link GenericDao} to restrict actions based on the owner of the
 * entities.
 * 
 * <p>
 * The idea with this DAO is to add the user ID of each entity into all SQL
 * queries, so that querying and updates are always restricted to a specific
 * user. Often the user will be the currently authenticated actor, which will
 * have already been verified before reaching the DAO layer.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface UserRelatedGenericDao<T extends UserRelatedEntity<PK>, PK extends Serializable>
		extends GenericDao<T, PK> {

	/**
	 * Get a persisted domain object by its primary key and the owner's user ID.
	 * 
	 * @param id
	 *        the primary key to retrieve
	 * @param userId
	 *        the ID of the owner
	 * @return the domain object
	 */
	T get(PK id, Long userId);

}
