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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * DTO for user secret entity.
 * 
 * @author matt
 * @version 1.0
 */
public class UserSecretInput {

	@NotBlank
	@Size(max = 64)
	private String key;

	@NotBlank
	@Size(max = 64)
	private String topic;

	@NotEmpty
	@Size(max = 4096)
	private byte[] secret;

	/**
	 * Constructor.
	 */
	public UserSecretInput() {
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
	 * Get the topic ID.
	 * 
	 * @return the topic ID
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * Set the topic ID.
	 * 
	 * @param topic
	 *        the topic ID to set
	 */
	public void setTopic(String topicId) {
		this.topic = topicId;
	}

	/**
	 * Get the secret.
	 * 
	 * @return the secret
	 */
	public byte[] getSecret() {
		return secret;
	}

	/**
	 * Set the secret.
	 * 
	 * @param secret
	 *        the secret to set
	 */
	public void setSecret(byte[] secret) {
		this.secret = secret;
	}

	/**
	 * Set the secret as a string value.
	 * 
	 * <p>
	 * The secret will be encoded as UTF-8 bytes.
	 * </p>
	 * 
	 * @param value
	 *        the secret value
	 */
	public void setSecretValue(String value) {
		secret = (value != null ? value.getBytes(StandardCharsets.UTF_8) : null);
	}

	/**
	 * Set the secret as a Base64-encoded value.
	 * 
	 * @param value
	 *        the secret value
	 */
	public void setSecretBase64(String value) {
		secret = (value != null ? Base64.getDecoder().decode(value) : null);
	}

	/**
	 * Set the secret as a hex-encoded value.
	 * 
	 * @param value
	 *        the secret value
	 */
	public void setSecretHex(String value) {
		secret = (value != null ? HexFormat.of().parseHex(value) : null);
	}

}
