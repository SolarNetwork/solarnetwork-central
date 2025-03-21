/* ==================================================================
 * UserSecretBiz.java - 22/03/2025 8:32:59 am
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

package net.solarnetwork.central.user.biz;

import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.user.dao.UserSecretFilter;
import net.solarnetwork.central.user.domain.UserSecret;
import net.solarnetwork.central.user.domain.UserSecretInput;
import net.solarnetwork.dao.FilterResults;

/**
 * Service API for user secrets.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserSecretBiz {

	/**
	 * Save a user secret.
	 *
	 * @param userId
	 *        the ID of the user to save the secret for
	 * @param topicId
	 *        the topic ID
	 * @param key
	 *        the key
	 * @param input
	 *        the secret to save
	 * @return the saved settings
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	UserSecret saveUserSecret(Long userId, String topicId, String key, UserSecretInput input);

	/**
	 * Delete a user secret.
	 *
	 * @param userId
	 *        the ID of the user to delete secret for
	 * @param topicId
	 *        the topic ID, or {@code null} for all topic IDs
	 * @param key
	 *        the key, or {@code null} for all keys
	 * @throws IllegalArgumentException
	 *         if {@code userId} is {@code null}
	 */
	void deleteUserSecret(Long userId, String topicId, String key);

	/**
	 * Get a list of all available secrets for a given user.
	 *
	 * @param userId
	 *        the user ID to get configurations for
	 * @param filter
	 *        an optional filter
	 * @return the available secrets, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code userId} is {@code null}
	 */
	FilterResults<? extends UserSecret, UserStringStringCompositePK> listSecretsForUser(Long userId,
			UserSecretFilter filter);

}
