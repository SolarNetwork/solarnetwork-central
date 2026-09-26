/* ==================================================================
 * UserAccountBiz.java - 26 Sept 2026 3:01:44 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.account.biz;

import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.account.domain.SnAccount;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;

/**
 * Service API for SolarUser account management.
 *
 * @author matt
 * @version 1.0
 */
public interface UserAccountBiz {

	/**
	 * Create a new account.
	 *
	 * @param userId
	 *        the user ID to create the account for
	 * @param input
	 *        the account input
	 * @return the account entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if
	 *         {@code userId} does not exist, or
	 *         {@link AuthorizationException.Reason#REGISTRATION_ALREADY_CONFIRMED}
	 *         if an account already exists for the given user
	 * @throws ValidationException
	 *         if the input is not valid
	 */
	SnAccount<?, ?, ?> createAccount(Long userId, SnAccountCreationInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException;

	/**
	 * Get an account.
	 * 
	 * @param userId
	 *        the user ID to get the account for
	 * @return the account entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws AuthorizationException
	 *         with , or
	 *         {@link AuthorizationException.Reason#REGISTRATION_NOT_CONFIRMED}
	 *         if the user has not registered for an account or
	 *         {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if an
	 *         account does not exist for the given user
	 */
	SnAccount<?, ?, ?> getAccountForUser(Long userId)
			throws IllegalArgumentException, AuthorizationException;

}
