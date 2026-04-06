/* ==================================================================
 * BaseOscpExternalSystemConfigurationInput.java - 15/08/2022 1:09:08 pm
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

package net.solarnetwork.central.user.oscp.domain;

import java.net.URI;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;

/**
 * Base DTO for external system configuration input.
 *
 * @param <T>
 *        the configuration type
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public abstract class BaseOscpExternalSystemConfigurationInput<T extends BaseOscpExternalSystemConfiguration<T>>
		extends BaseOscpConfigurationInput<T> {

	private @Nullable URI baseUrl;

	private @Nullable String oscpVersion;

	private @Nullable RegistrationStatus registrationStatus;

	@SuppressWarnings("NullAway")
	@Override
	protected void populateConfiguration(T conf) {
		super.populateConfiguration(conf);
		if ( baseUrl != null ) {
			conf.setBaseUrl(baseUrl.toString());
		}
		conf.setOscpVersion(oscpVersion);
		conf.setRegistrationStatus(registrationStatus);
	}

	/**
	 * Get the base URL to the OSCP API.
	 *
	 * @return the baseUrl the base URL
	 */
	public final @Nullable URI getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Set the base URL to the OSCP API.
	 *
	 * @param baseUrl
	 *        the URL to set
	 */
	public final void setBaseUrl(@Nullable URI baseUrl) {
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
	public final @Nullable RegistrationStatus getRegistrationStatus() {
		return registrationStatus;
	}

	/**
	 * Set the registration status.
	 *
	 * @param registrationStatus
	 *        the registrationStatus to set
	 */
	public final void setRegistrationStatus(@Nullable RegistrationStatus registrationStatus) {
		this.registrationStatus = registrationStatus;
	}

}
