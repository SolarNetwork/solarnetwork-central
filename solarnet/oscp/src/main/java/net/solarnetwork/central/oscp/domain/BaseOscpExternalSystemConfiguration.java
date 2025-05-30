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

import java.io.Serial;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.util.StringUtils;

/**
 * Base OSCP configuration entity.
 *
 * @author matt
 * @version 1.1
 */
@JsonPropertyOrder({ "userId", "configId", "created", "modified", "enabled", "name", "token", "baseUrl",
		"oscpVersion", "flexibilityProviderId", "registrationStatus", "settings", "heartbeatDate",
		"offlineDate", "serviceProps" })
public abstract class BaseOscpExternalSystemConfiguration<C extends BaseOscpExternalSystemConfiguration<C>>
		extends BaseOscpConfigurationEntity<C> implements ExternalSystemConfiguration {

	@Serial
	private static final long serialVersionUID = 2484734886666757050L;

	private String token;
	private String baseUrl;
	private String oscpVersion;
	private Long flexibilityProviderId;
	private RegistrationStatus registrationStatus;
	private SystemSettings settings;
	private Instant heartbeatDate;
	private Instant offlineDate;

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
	 * @param userId
	 *        the user ID
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
		entity.setFlexibilityProviderId(flexibilityProviderId);
		entity.setRegistrationStatus(registrationStatus);
		entity.setToken(token);
		entity.setSettings(settings);
		entity.setHeartbeatDate(heartbeatDate);
	}

	@Override
	public boolean isSameAs(C other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return (Objects.equals(this.token, other.getToken())
				&& Objects.equals(this.baseUrl, other.getBaseUrl())
				&& Objects.equals(this.oscpVersion, other.getOscpVersion())
				&& Objects.equals(this.flexibilityProviderId, other.getFlexibilityProviderId())
				&& Objects.equals(this.registrationStatus, other.getRegistrationStatus())
				&& Objects.equals(this.settings, other.getSettings())
				&& Objects.equals(this.heartbeatDate, other.getHeartbeatDate())
				&& Objects.equals(this.offlineDate, other.getOfflineDate()));
		// @formatter:on
	}

	@Override
	public boolean useGroupAssetMeasurement() {
		Map<String, Object> props = getServiceProps();
		Object v = (props != null ? props.get(ExternalSystemServiceProperties.ASSET_MEAESUREMENT)
				: null);
		if ( v instanceof Boolean b ) {
			return b;
		} else if ( v instanceof Number n ) {
			return n.intValue() != 0;
		} else if ( v != null ) {
			return StringUtils.parseBoolean(v.toString());
		}
		return false;
	}

	@Override
	public String combinedGroupAssetId() {
		Map<String, Object> props = getServiceProps();
		Object v = (props != null ? props.get(ExternalSystemServiceProperties.COMBINED_ASSET_ID) : null);
		return (v != null ? v.toString() : null);
	}

	/**
	 * Get the authentication token.
	 *
	 * <p>
	 * Note this is normally only included when creating a configuration entity;
	 * afterwards the token is omitted.
	 * </p>
	 *
	 * @return the token
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
	@Override
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

	/**
	 * Get the system settings.
	 *
	 * @return the settings
	 */
	public SystemSettings getSettings() {
		return settings;
	}

	/**
	 * Set the system settings.
	 *
	 * @param settings
	 *        the settings to set
	 */
	public void setSettings(SystemSettings settings) {
		this.settings = settings;
	}

	/**
	 * Get the last heartbeat time.
	 *
	 * @return the heartbeat date
	 */
	public Instant getHeartbeatDate() {
		return heartbeatDate;
	}

	/**
	 * Set the last heartbeat time.
	 *
	 * @param heartbeatDate
	 *        the heartbeat date to set
	 */
	public void setHeartbeatDate(Instant heartbeatDate) {
		this.heartbeatDate = heartbeatDate;
	}

	/**
	 * Get the date after which the system should be considered "offline".
	 *
	 * @return the offline date
	 */
	public Instant getOfflineDate() {
		return offlineDate;
	}

	/**
	 * Set the date after which the system should be considered "offline".
	 *
	 * @param offlineDate
	 *        the offline date to set
	 */
	public void setOfflineDate(Instant offlineDate) {
		this.offlineDate = offlineDate;
	}

}
