/* ==================================================================
 * UserDao.java - Dec 11, 2009 8:38:07 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.user.dao;

import java.util.Set;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserFilter;
import net.solarnetwork.central.user.domain.UserFilterMatch;

/**
 * DAO API for User objects.
 * 
 * @author matt
 * @version 1.1
 */
public interface UserDao
		extends GenericDao<User, Long>, FilterableDao<UserFilterMatch, Long, UserFilter> {

	/**
	 * Get a user by their email.
	 * 
	 * @param email
	 *        the email address to lookup
	 * @return the found User, or {@literal null} if not found
	 */
	User getUserByEmail(String email);

	/**
	 * Get the set of roles a user belongs to.
	 * 
	 * <p>
	 * Roles represent granted user authorizations.
	 * </p>
	 * 
	 * @return the user roles
	 */
	Set<String> getUserRoles(User user);

	/**
	 * Store the set of roles a user belongs to.
	 * 
	 * <p>
	 * This will completely replace any existing roles the user may already
	 * belong to.
	 * </p>
	 * 
	 * @param user
	 *        the user to store the roles for
	 * @param roles
	 *        the set of roles
	 */
	void storeUserRoles(User user, Set<String> roles);

	/**
	 * Set a single property value on the billing data of a user.
	 * 
	 * <p>
	 * Note this method can only update billing data. If the billing data is
	 * {@literal null} for the given user, the property will <b>not</b> be
	 * added.
	 * </p>
	 * 
	 * @param userId
	 *        the ID if the user to update
	 * @param name
	 *        the property name to insert or update
	 * @param value
	 *        the value to store
	 * @return the number of updated rows
	 */
	int storeBillingDataProperty(Long userId, String name, Object value);

}
