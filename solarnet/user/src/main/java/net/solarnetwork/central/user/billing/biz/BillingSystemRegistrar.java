/* ==================================================================
 * BillingSystemRegistrar.java - 26 Sept 2026 7:33:08 pm
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

package net.solarnetwork.central.user.billing.biz;

import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.account.domain.SnAccount;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.domain.User;

/**
 * API for account registration management for a billing system.
 * 
 * <p>
 * A {@link BillingSystem} might implement this API to support account
 * registration functions within SolarNetwork.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface BillingSystemRegistrar {

	/**
	 * Get a unique system key for the accounting functions of this system.
	 * 
	 * @return a unique key
	 */
	String getAccountingSystemKey();

	/**
	 * Test if an accounting key is supported by this registrar.
	 * 
	 * @param key
	 *        the key to test
	 * @return {@literal true} if the key is supported
	 */
	boolean supportsAccountingSystemKey(String key);

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
	 *         {@code userId} does not exist
	 * @throws ValidationException
	 *         if the input is not valid
	 */
	SnAccount<?, ?, ?> createAccount(Long userId, SnAccountCreationInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException;

	/**
	 * Get an account for a user.
	 *
	 * @param user
	 *        the user to get the account for
	 * @return the account entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if an
	 *         account for {@code user} does not exist
	 */
	SnAccount<?, ?, ?> getAccountForUser(User user)
			throws IllegalArgumentException, AuthorizationException;

}
