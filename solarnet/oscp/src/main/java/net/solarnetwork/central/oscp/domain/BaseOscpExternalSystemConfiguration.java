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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.util.ObjectUtils;
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

	private @Nullable String token;
	private @Nullable String baseUrl;
	private @Nullable String oscpVersion;
	private Long flexibilityProviderId;
	private RegistrationStatus registrationStatus;
	private @Nullable SystemSettings settings;
	private @Nullable Instant heartbeatDate;
	private @Nullable Instant offlineDate;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the configuration name
	 * @param flexibilityProviderId
	 *        the flexibility provider ID
	 * @param registrationStatus
	 *        the registration status
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BaseOscpExternalSystemConfiguration(UserLongCompositePK id, Instant created, String name,
			Long flexibilityProviderId, RegistrationStatus registrationStatus) {
		super(id, created, name);
		this.flexibilityProviderId = requireNonNullArgument(flexibilityProviderId,
				"flexibilityProviderId");
		this.registrationStatus = requireNonNullArgument(registrationStatus, "registrationStatus");
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
	public boolean isSameAs(@Nullable C other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		final var o = ObjectUtils.nonnull(other, "other");
		// @formatter:off
		return (Objects.equals(token, o.getToken())
				&& Objects.equals(baseUrl, o.getBaseUrl())
				&& Objects.equals(oscpVersion, o.getOscpVersion())
				&& Objects.equals(flexibilityProviderId, o.getFlexibilityProviderId())
				&& Objects.equals(registrationStatus, o.getRegistrationStatus())
				&& Objects.equals(settings, o.getSettings())
				&& Objects.equals(heartbeatDate, o.getHeartbeatDate())
				&& Objects.equals(offlineDate, o.getOfflineDate()));
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
	public @Nullable String combinedGroupAssetId() {
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
	public final @Nullable String getToken() {
		return token;
	}

	/**
	 * Set the authentication token.
	 *
	 * @param token
	 *        the token to set
	 */
	public final void setToken(@Nullable String token) {
		this.token = token;
	}

	/**
	 * Get the base URL to the OSCP API.
	 *
	 * @return the baseUrl the base URL
	 */
	public final @Nullable String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Set the base URL to the OSCP API.
	 *
	 * @param baseUrl
	 *        the URL to set
	 */
	public final void setBaseUrl(@Nullable String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Get the OSCP version.
	 *
	 * @return the version
	 */
	public final @Nullable String getOscpVersion() {
		return oscpVersion;
	}

	/**
	 * Set the OSCP version.
	 *
	 * @param oscpVersion
	 *        the version to set
	 */
	public final void setOscpVersion(@Nullable String oscpVersion) {
		this.oscpVersion = oscpVersion;
	}

	/**
	 * Get the registration status.
	 *
	 * @return the registrationStatus
	 */
	public final RegistrationStatus getRegistrationStatus() {
		return registrationStatus;
	}

	/**
	 * Set the registration status.
	 *
	 * @param registrationStatus
	 *        the registrationStatus to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setRegistrationStatus(RegistrationStatus registrationStatus) {
		this.registrationStatus = requireNonNullArgument(registrationStatus, "registrationStatus");
	}

	/**
	 * Get the ID of the Flexibility Provider token associated with this
	 * configuration.
	 *
	 * @return the flexibility provider ID
	 */
	@Override
	public final Long getFlexibilityProviderId() {
		return flexibilityProviderId;
	}

	/**
	 * Set the ID of the Flexibility Provider token associated with this
	 * configuration.
	 *
	 * @param flexibilityProviderId
	 *        the flexibility provider ID to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setFlexibilityProviderId(Long flexibilityProviderId) {
		this.flexibilityProviderId = requireNonNullArgument(flexibilityProviderId,
				"flexibilityProviderId");
	}

	/**
	 * Get the system settings.
	 *
	 * @return the settings
	 */
	public final @Nullable SystemSettings getSettings() {
		return settings;
	}

	/**
	 * Set the system settings.
	 *
	 * @param settings
	 *        the settings to set
	 */
	public final void setSettings(@Nullable SystemSettings settings) {
		this.settings = settings;
	}

	/**
	 * Get the last heartbeat time.
	 *
	 * @return the heartbeat date
	 */
	public final @Nullable Instant getHeartbeatDate() {
		return heartbeatDate;
	}

	/**
	 * Set the last heartbeat time.
	 *
	 * @param heartbeatDate
	 *        the heartbeat date to set
	 */
	public final void setHeartbeatDate(@Nullable Instant heartbeatDate) {
		this.heartbeatDate = heartbeatDate;
	}

	/**
	 * Get the date after which the system should be considered "offline".
	 *
	 * @return the offline date
	 */
	public final @Nullable Instant getOfflineDate() {
		return offlineDate;
	}

	/**
	 * Set the date after which the system should be considered "offline".
	 *
	 * @param offlineDate
	 *        the offline date to set
	 */
	public final void setOfflineDate(@Nullable Instant offlineDate) {
		this.offlineDate = offlineDate;
	}

}
