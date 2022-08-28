/* ==================================================================
 * SecretsBiz.java - 27/08/2022 2:09:10 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.biz;

import java.util.Map;
import net.solarnetwork.codec.JsonUtils;

/**
 * API for secret management.
 * 
 * <p>
 * The intention of this API is that implementations store the secret values in
 * a secure, encrypted manner.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface SecretsBiz {

	/** A qualifier for secrets components. */
	public static final String SECRETS = "secrets";

	/**
	 * Get a secret value.
	 * 
	 * @param secretName
	 *        the unique name of the secret to get
	 * @return the secret value, or {@literal null} if one does not exist
	 */
	String getSecret(String secretName);

	/**
	 * Put a secret value.
	 * 
	 * <p>
	 * This will replace any existing value of a secret with the same name.
	 * </p>
	 * 
	 * @param secretName
	 *        the unique name of the secret to put
	 * @param secretValue
	 *        the value to store
	 */
	void putSecret(String secretName, String secretValue);

	/**
	 * Delete a secret.
	 * 
	 * @param secretName
	 *        the name of the secret to delete
	 */
	void deleteSecret(String secretName);

	/**
	 * Get a secret value that is a JSON object string.
	 * 
	 * <p>
	 * This method will interpret the associated secret value as a JSON object
	 * string, and return that as a Map. If the secret value exists but cannot
	 * be decoded as a JSON object, {@literal null} will be returned.
	 * </p>
	 * 
	 * @param secretName
	 *        the unique name of the secret to put
	 * @return the secret value, decoded from a JSON object
	 */
	default Map<String, Object> getSecretMap(String secretName) {
		String s = getSecret(secretName);
		if ( s == null ) {
			return null;
		}
		return JsonUtils.getStringMap(s);
	}

	/**
	 * Put a Map as a JSON object string secret value.
	 * 
	 * @param secretName
	 *        the unique name of the secret to put
	 * @param secretValue
	 *        the Map to encode as a JSON object value
	 */
	default void putSecret(String secretName, Map<String, ?> secretValue) {
		String s = JsonUtils.getJSONString(secretValue, null);
		putSecret(secretName, s);
	}

}
