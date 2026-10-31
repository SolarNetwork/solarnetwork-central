/* ==================================================================
 * SnAccountLinkInput.java - 15 Oct 2026 11:51:19 am
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

package net.solarnetwork.central.user.account.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Account linking configuration input.
 *
 * <p>
 * The credentials of the account to link to can be verified ahead of linking,
 * for example when a form is submitted for review before it is saved. The
 * verified account is then kept in place of the password, which is never
 * serialized. Only {@link #verified(SnLinkedAccountInfo)} sets the verified
 * account, so it can not be bound from request data, and changing the username
 * clears it.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class SnAccountLinkInput implements Serializable {

	@Serial
	private static final long serialVersionUID = -4417077429800131886L;

	private @Nullable String username;
	private transient @Nullable String password;
	private @Nullable SnLinkedAccountInfo verifiedAccount;

	/**
	 * Constructor.
	 */
	public SnAccountLinkInput() {
		super();
	}

	/**
	 * Test if the username and password are provided and non-empty.
	 *
	 * @return {@code true} if both the username and password are non-empty
	 */
	public boolean hasUsernameAndPassword() {
		return (username != null && !username.isBlank() && password != null && !password.isBlank());
	}

	/**
	 * Get the target account username.
	 *
	 * @return the username
	 */
	public final @Nullable String getUsername() {
		return username;
	}

	/**
	 * Get the target account username, assumed non-null.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasUsernameAndPassword()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return the username (presumed non-null)
	 */
	@SuppressWarnings("NullAway")
	public final String username() {
		return getUsername();
	}

	/**
	 * Set the target account username.
	 *
	 * @param username
	 *        the username to set
	 */
	public final void setUsername(@Nullable String username) {
		final SnLinkedAccountInfo verified = this.verifiedAccount;
		if ( verified != null
				&& (username == null || !verified.email().equalsIgnoreCase(username.trim())) ) {
			this.verifiedAccount = null;
		}
		this.username = username;
	}

	/**
	 * Get the target account password.
	 *
	 * @return the password
	 */
	public final @Nullable String getPassword() {
		return password;
	}

	/**
	 * Get the target account password, assumed non-null.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasUsernameAndPassword()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return the password (presumed non-null)
	 */
	@SuppressWarnings("NullAway")
	public final String password() {
		return getPassword();
	}

	/**
	 * Set the target account password.
	 *
	 * @param password
	 *        the password to set
	 */
	public final void setPassword(@Nullable String password) {
		this.password = password;
	}

	/**
	 * Mark the credentials as verified.
	 *
	 * <p>
	 * The password is cleared, as it is no longer needed.
	 * </p>
	 *
	 * @param account
	 *        the account the credentials were verified for
	 * @throws IllegalArgumentException
	 *         if {@code account} is {@code null}
	 */
	public final void verified(SnLinkedAccountInfo account) {
		this.verifiedAccount = requireNonNullArgument(account, "account");
		this.password = null;
	}

	/**
	 * Get the account the credentials were verified for.
	 *
	 * @return the verified account, or {@code null} if the credentials have not
	 *         been verified
	 */
	@JsonIgnore
	public final @Nullable SnLinkedAccountInfo getVerifiedAccount() {
		return verifiedAccount;
	}

}
