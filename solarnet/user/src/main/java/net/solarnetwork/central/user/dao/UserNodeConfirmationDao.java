/* ==================================================================
 * UserNodeConfirmationDao.java - Sep 7, 2011 5:04:40 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

import java.util.List;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;

/**
 * DAO API for UserNodeConfirmation entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserNodeConfirmationDao extends GenericDao<UserNodeConfirmation, Long> {

	/**
	 * Find a list of all pending UserNodeConfirmation objects for a particular
	 * user.
	 * 
	 * @param user
	 *        the user to get all pending confirmations for
	 * @return list of {@link UserNodeConfirmation} objects, or an empty list if
	 *         none found
	 */
	List<UserNodeConfirmation> findPendingConfirmationsForUser(User user);

	/**
	 * Get a confirmation object for a given user ID and key.
	 * 
	 * @param userId
	 *        the user ID
	 * @param key
	 *        the confirmation key
	 * @return the found UserNodeConfirmation, or <em>null</em> if not found
	 */
	UserNodeConfirmation getConfirmationForKey(Long userId, String key);

}
