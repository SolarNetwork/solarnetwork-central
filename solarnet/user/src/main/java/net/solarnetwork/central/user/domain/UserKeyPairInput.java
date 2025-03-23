/* ==================================================================
 * UserSecretInput.java - 22/03/2025 8:41:25 am
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

package net.solarnetwork.central.user.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user key pair entity.
 * 
 * @author matt
 * @version 1.0
 */
public class UserKeyPairInput {

	@NotBlank
	@Size(max = 64)
	private String key;

	@NotBlank
	@Size(max = 16384)
	private String keyPem;

	@NotBlank
	@Size(max = 64)
	private String password;

	/**
	 * Constructor.
	 */
	public UserKeyPairInput() {
		super();
	}

	/**
	 * Get the key name.
	 * 
	 * @return the key name
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the key name.
	 * 
	 * @param key
	 *        the key name to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Get the key data.
	 * 
	 * @return the key data as a PEM encoded string
	 */
	public String getKeyPem() {
		return keyPem;
	}

	/**
	 * Set the key data as a PEM encoded string.
	 * 
	 * @param keyPem
	 *        the key data to set
	 */
	public void setKeyPem(String keyPem) {
		this.keyPem = keyPem;
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

}
