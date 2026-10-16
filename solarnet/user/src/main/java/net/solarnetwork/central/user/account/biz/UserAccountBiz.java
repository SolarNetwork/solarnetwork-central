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

import java.util.Collection;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.account.domain.SnAccountLinkInput;
import net.solarnetwork.central.user.account.domain.SnLinkedAccountInfo;

/**
 * Service API for SolarUser account management.
 *
 * @author matt
 * @version 1.1
 */
public interface UserAccountBiz {

	/**
	 * Create a new account.
	 *
	 * @param userId
	 *        the user ID to create the account for
	 * @param input
	 *        the account input
	 * @return the newly created account information
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
	SnAccountInfo createAccount(Long userId, SnAccountCreationInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException;

	/**
	 * Get an account.
	 *
	 * @param userId
	 *        the user ID to get the account for
	 * @return the account information
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws AuthorizationException
	 *         with , or
	 *         {@link AuthorizationException.Reason#REGISTRATION_NOT_CONFIRMED}
	 *         if the user has not registered for an account or
	 *         {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if an
	 *         account does not exist for the given user
	 */
	SnAccountInfo getAccountForUser(Long userId) throws IllegalArgumentException, AuthorizationException;

	/**
	 * Update an account.
	 *
	 * @param userId
	 *        the user ID to update the account for
	 * @param input
	 *        the account input
	 * @return the updated account information
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if
	 *         {@code userId} does not exist or no account exists for the given
	 *         user
	 * @throws ValidationException
	 *         if the input is not valid
	 */
	SnAccountInfo updateAccount(Long userId, SnAccountCreationInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException;

	/**
	 * Verify the credentials of a parent account to link an account to.
	 *
	 * <p>
	 * Nothing is linked. On success the input is marked as verified, which
	 * clears its password, so a later
	 * {@link #createAccount(Long, SnAccountCreationInput)} or
	 * {@link #updateAccount(Long, SnAccountCreationInput)} given the same input
	 * as its parent account links to the verified account without needing the
	 * password again.
	 * </p>
	 *
	 * @param userId
	 *        the ID of the user whose account is to be linked as a child
	 * @param input
	 *        the parent account credentials
	 * @return the parent account information
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if
	 *         {@code userId} does not exist
	 * @throws ValidationException
	 *         if the credentials are not valid, or the accounts can not be
	 *         linked
	 * @since 1.1
	 */
	SnLinkedAccountInfo verifyParentAccount(Long userId, SnAccountLinkInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException;

	/**
	 * Link a child account to a parent account.
	 *
	 * @param userId
	 *        the ID of the parent account user
	 * @param input
	 *        the child account credentials
	 * @return the child account information
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if
	 *         {@code userId} does not exist
	 * @throws ValidationException
	 *         if the credentials are not valid, or the accounts can not be
	 *         linked
	 * @since 1.1
	 */
	SnLinkedAccountInfo linkAccount(Long userId, SnAccountLinkInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException;

	/**
	 * Unlink a child account from its parent.
	 *
	 * <p>
	 * No error will be thrown if the requested child account is not linked when
	 * this method is called.
	 * </p>
	 *
	 * @param userId
	 *        the ID of the parent account user
	 * @param childUserId
	 *        the ID of the child account user to unlink
	 * @return the child accounts still linked after the removal, never
	 *         {@code null}
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if
	 *         {@code userId} does not exist
	 * @since 1.1
	 */
	Collection<SnLinkedAccountInfo> unlinkAccount(Long userId, Long childUserId)
			throws IllegalArgumentException, AuthorizationException;

}
