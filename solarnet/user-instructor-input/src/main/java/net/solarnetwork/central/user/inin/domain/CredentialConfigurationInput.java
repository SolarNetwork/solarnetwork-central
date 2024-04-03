/* ==================================================================
 * CredentialConfigurationInput.java - 25/02/2024 7:34:30 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.inin.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.ZoneOffset;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.inin.domain.CredentialConfiguration;
import net.solarnetwork.util.DateUtils;

/**
 * DTO for datum input credential configuration.
 *
 * @author matt
 * @version 1.0
 */
public class CredentialConfigurationInput
		extends BaseInstructionInputConfigurationInput<CredentialConfiguration, UserLongCompositePK> {

	@NotNull
	@NotBlank
	@Size(max = 256)
	private String username;

	@Size(max = 64)
	private String password;

	private String expires;

	private boolean oauth;

	/**
	 * Constructor.
	 */
	public CredentialConfigurationInput() {
		super();
	}

	@Override
	public CredentialConfiguration toEntity(UserLongCompositePK id, Instant date) {
		CredentialConfiguration conf = new CredentialConfiguration(requireNonNullArgument(id, "id"),
				date);
		populateConfiguration(conf);
		return conf;
	}

	@Override
	protected void populateConfiguration(CredentialConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setUsername(username);
		conf.setPassword(password);
		if ( expires != null && !expires.isBlank() ) {
			var ts = DateUtils.parseIsoTimestamp(expires, ZoneOffset.UTC);
			if ( ts == null ) {
				throw new IllegalArgumentException("Invalid expiration date format.");
			}
			conf.setExpires(ts.toInstant());
		}
		conf.setOauth(oauth);
	}

	/**
	 * Get the username.
	 *
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username.
	 *
	 * @param username
	 *        the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Get the password.
	 *
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the password.
	 *
	 * @param password
	 *        the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Get the expiration date.
	 *
	 * @return the expiration date as an ISO-8601 string
	 */
	public String getExpires() {
		return expires;
	}

	/**
	 * Set the expiration date.
	 *
	 * @param expires
	 *        the expiration date to set, as an ISO-8601 string
	 */
	public void setExpires(String expires) {
		this.expires = expires;
	}

	/**
	 * Get the OAuth mode.
	 *
	 * @return {@literal true} if the {@code username} represents an OAuth
	 *         client credentials issuer URL
	 */
	public boolean isOauth() {
		return oauth;
	}

	/**
	 * Set the OAuth mode.
	 *
	 * @param oauth
	 *        {@literal true} if the {@code username} represents an OAuth client
	 *        credentials issuer URL
	 */
	public void setOauth(boolean oauth) {
		this.oauth = oauth;
	}

}
