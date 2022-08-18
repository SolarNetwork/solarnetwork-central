/* ==================================================================
 * CapacityOptimizerConfiguration.java - 14/08/2022 7:21:50 am
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

package net.solarnetwork.central.oscp.domain;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Base OSCP configuration entity.
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "userId", "configId", "created", "modified", "enabled", "name", "token", "baseUrl",
		"oscpVersion", "flexibilityProviderId", "registrationStatus", "serviceProps" })
public abstract class BaseOscpExternalSystemConfiguration<C extends BaseOscpExternalSystemConfiguration<C>>
		extends BaseOscpConfigurationEntity<C> {

	private static final long serialVersionUID = 2657431353983332547L;

	private String token;
	private String baseUrl;
	private String oscpVersion;
	private Long flexibilityProviderId;
	private RegistrationStatus registrationStatus;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseOscpExternalSystemConfiguration(UserLongCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param user
	 *        ID the user ID
	 * @param entityId
	 *        the entity ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseOscpExternalSystemConfiguration(Long userId, Long entityId, Instant created) {
		super(userId, entityId, created);
	}

	@Override
	public BaseOscpExternalSystemConfiguration<C> clone() {
		return (BaseOscpExternalSystemConfiguration<C>) super.clone();
	}

	@Override
	public void copyTo(C entity) {
		super.copyTo(entity);
		entity.setBaseUrl(baseUrl);
		entity.setOscpVersion(oscpVersion);
		entity.setRegistrationStatus(registrationStatus);
		entity.setToken(token);
	}

	/**
	 * Get the authentication token.
	 * 
	 * <p>
	 * Note this is normally only included when creating a configuration entity;
	 * afterwards the token is omitted.
	 * </p>
	 * 
	 * @return the token the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Set the authentication token.
	 * 
	 * @param token
	 *        the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * Get the base URL to the OSCP API.
	 * 
	 * @return the baseUrl the base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Set the base URL to the OSCP API.
	 * 
	 * @param baseUrl
	 *        the URL to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Get the OSCP version.
	 * 
	 * @return the version
	 */
	public String getOscpVersion() {
		return oscpVersion;
	}

	/**
	 * Set the OSCP version.
	 * 
	 * @param oscpVersion
	 *        the version to set
	 */
	public void setOscpVersion(String oscpVersion) {
		this.oscpVersion = oscpVersion;
	}

	/**
	 * Get the registration status.
	 * 
	 * @return the registrationStatus
	 */
	public RegistrationStatus getRegistrationStatus() {
		return registrationStatus;
	}

	/**
	 * Set the registration status.
	 * 
	 * @param registrationStatus
	 *        the registrationStatus to set
	 */
	public void setRegistrationStatus(RegistrationStatus registrationStatus) {
		this.registrationStatus = registrationStatus;
	}

	/**
	 * Get the ID of the Flexibility Provider token associated with this
	 * configuration.
	 * 
	 * @return the flexibility provider ID
	 */
	public Long getFlexibilityProviderId() {
		return flexibilityProviderId;
	}

	/**
	 * Set the ID of the Flexibility Provider token associated with this
	 * configuration.
	 * 
	 * @param flexibilityProviderId
	 *        the flexibility provider ID to set
	 */
	public void setFlexibilityProviderId(Long flexibilityProviderId) {
		this.flexibilityProviderId = flexibilityProviderId;
	}

}
