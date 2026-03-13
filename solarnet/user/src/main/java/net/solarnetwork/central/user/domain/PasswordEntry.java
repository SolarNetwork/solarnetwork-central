/* ==================================================================
 * PasswordEntry.java - Mar 19, 2013 6:46:30 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import org.jspecify.annotations.Nullable;

/**
 * Password reset bean.
 * 
 * @author matt
 * @version 1.0
 */
public class PasswordEntry {

	private @Nullable String username;
	private @Nullable String confirmationCode;
	private @Nullable String password;
	private @Nullable String passwordConfirm;

	/**
	 * Default constructor.
	 */
	public PasswordEntry() {
		super();
	}

	/**
	 * Construct with a password.
	 * 
	 * @param password
	 *        the password to set
	 */
	public PasswordEntry(@Nullable String password) {
		super();
		this.password = password;
		this.passwordConfirm = password;
	}

	public final @Nullable String getPassword() {
		return password;
	}

	public final void setPassword(@Nullable String password) {
		this.password = password;
	}

	public final @Nullable String getPasswordConfirm() {
		return passwordConfirm;
	}

	public final void setPasswordConfirm(@Nullable String passwordConfirm) {
		this.passwordConfirm = passwordConfirm;
	}

	public final @Nullable String getUsername() {
		return username;
	}

	public final void setUsername(@Nullable String username) {
		this.username = username;
	}

	public final @Nullable String getConfirmationCode() {
		return confirmationCode;
	}

	public final void setConfirmationCode(@Nullable String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

}
