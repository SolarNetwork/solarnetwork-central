/* ==================================================================
 * UserNodeRelatedEntity.java - 1/03/2020 7:16:44 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodePK;

/**
 * API for an entity associated with a {@link UserNode}.
 * 
 * @param <K>
 *        the primary key type
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public interface UserNodeRelatedEntity<K> extends UserRelatedEntity<K> {

	/**
	 * Get node ID this entity relates to.
	 * 
	 * @return the node ID
	 */
	Long getNodeId();

	/**
	 * Get the user and node key that this entity relates to.
	 * 
	 * @return the user and node key
	 */
	default UserNodePK getUserNodeId() {
		return new UserNodePK(getUserId(), getNodeId());
	}

}
