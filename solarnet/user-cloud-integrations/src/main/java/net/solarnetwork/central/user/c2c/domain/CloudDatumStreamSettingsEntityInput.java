/* ==================================================================
 * CloudDatumStreamSettingsEntityInput.java - 28/10/2024 4:26:49 pm
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

package net.solarnetwork.central.user.c2c.domain;

import java.time.Instant;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRelated;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettingsEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * DTO for datum stream settings entity.
 *
 * @author matt
 * @version 1.1
 */
public class CloudDatumStreamSettingsEntityInput implements
		CloudIntegrationsConfigurationInput<CloudDatumStreamSettingsEntity, UserLongCompositePK>,
		CloudDatumStreamRelated {

	private boolean publishToSolarIn = true;

	private boolean publishToSolarFlux = false;

	@Override
	public CloudDatumStreamSettingsEntity toEntity(UserLongCompositePK id, Instant date) {
		CloudDatumStreamSettingsEntity conf = new CloudDatumStreamSettingsEntity(id, date);
		conf.setPublishToSolarIn(publishToSolarIn);
		conf.setPublishToSolarFlux(publishToSolarFlux);
		return conf;
	}

	/**
	 * Get the "publish to SolarIn" toggle.
	 *
	 * @return {@literal true} if data should be published to SolarIn
	 */
	public boolean isPublishToSolarIn() {
		return publishToSolarIn;
	}

	/**
	 * Set the "publish to SolarIn" toggle.
	 *
	 * @param publishToSolarIn
	 *        {@literal true} if data should be published to SolarIn
	 */
	public void setPublishToSolarIn(boolean publishToSolarIn) {
		this.publishToSolarIn = publishToSolarIn;
	}

	/**
	 * Get the "publish to SolarFlux" toggle.
	 *
	 * @return {@literal true} if data should be published to SolarFlux
	 */
	public boolean isPublishToSolarFlux() {
		return publishToSolarFlux;
	}

	/**
	 * Set the "publish to SolarFlux" toggle.
	 *
	 * @param publishToSolarFlux
	 *        {@literal true} if data should be published to SolarFlux
	 */
	public void setPublishToSolarFlux(boolean publishToSolarFlux) {
		this.publishToSolarFlux = publishToSolarFlux;
	}

}
