/* ==================================================================
 * UserRelatedEntity.java - 25/03/2018 1:55:08 PM
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

package net.solarnetwork.central.dao;

import net.solarnetwork.dao.Entity;

/**
 * API for an entity associated with a user ID.
 * 
 * @param <PK>
 *        the primary key type
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public interface UserRelatedEntity<PK> extends Entity<PK> {

	/**
	 * Get the user ID this entity relates to.
	 * 
	 * @return the user ID
	 */
	Long getUserId();

}
