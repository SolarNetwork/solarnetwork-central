/* ==================================================================
 * CloudAccessSettings.java - 27/08/2022 3:47:14 pm
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

package net.solarnetwork.central.cloud.domain;

/**
 * General cloud provider access settings.
 * 
 * @author matt
 * @version 1.0
 */
public class CloudAccessSettings {

	private String region;
	private String accessToken;
	private String accessSecret;

	/**
	 * Get the cloud provider region name.
	 * 
	 * @return the region name
	 */
	public String getRegion() {
		return region;
	}

	/**
	 * Set the cloud provider region name.
	 * 
	 * @param region
	 *        the region name to set
	 */
	public void setRegion(String region) {
		this.region = region;
	}

	/**
	 * Get the client access token.
	 * 
	 * @return the token
	 */
	public String getAccessToken() {
		return accessToken;
	}

	/**
	 * Set the client access token.
	 * 
	 * @param accessToken
	 *        the token to set
	 */
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	/**
	 * Get the client access token secret.
	 * 
	 * @return
	 */
	public String getAccessSecret() {
		return accessSecret;
	}

	/**
	 * Set the client access token secret.
	 * 
	 * @param accessSecret
	 *        the secret to set
	 */
	public void setAccessSecret(String accessSecret) {
		this.accessSecret = accessSecret;
	}

}
