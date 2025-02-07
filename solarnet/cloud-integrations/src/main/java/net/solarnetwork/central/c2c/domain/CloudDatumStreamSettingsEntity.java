/* ==================================================================
 * CloudDatumStreamSettingsEntity.java - 28/10/2024 7:15:32 am
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

package net.solarnetwork.central.c2c.domain;

import java.io.Serial;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Cloud datum stream settings, to override {@link UserSettingsEntity}.
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id", "enabled" })
@JsonPropertyOrder({ "userId", "datumStreamId", "created", "modified", "publishToSolarIn",
		"publishToSolarFlux" })
public final class CloudDatumStreamSettingsEntity
		extends BaseUserModifiableEntity<CloudDatumStreamSettingsEntity, UserLongCompositePK> implements
		CloudIntegrationsConfigurationEntity<CloudDatumStreamSettingsEntity, UserLongCompositePK>,
		CloudDatumStreamSettings {

	@Serial
	private static final long serialVersionUID = -5768166630955664067L;

	private boolean publishToSolarIn = true;
	private boolean publishToSolarFlux = false;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CloudDatumStreamSettingsEntity(UserLongCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param dataSourceId
	 *        the data source ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CloudDatumStreamSettingsEntity(Long userId, Long dataSourceId, Instant created) {
		this(new UserLongCompositePK(userId, dataSourceId), created);
	}

	@Override
	public boolean isFullyConfigured() {
		return true;
	}

	@Override
	public CloudDatumStreamSettingsEntity copyWithId(UserLongCompositePK id) {
		var copy = new CloudDatumStreamSettingsEntity(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(CloudDatumStreamSettingsEntity entity) {
		super.copyTo(entity);
		entity.setPublishToSolarIn(publishToSolarIn);
		entity.setPublishToSolarFlux(publishToSolarFlux);
	}

	@Override
	public boolean isSameAs(CloudDatumStreamSettingsEntity other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return publishToSolarIn == other.publishToSolarIn
				&& publishToSolarFlux == other.publishToSolarFlux
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(64);
		builder.append("CloudDatumStreamSettingsEntity{userId=");
		builder.append(getUserId());
		builder.append(", datumStreamId=");
		builder.append(getDatumStreamId());
		builder.append(", publishToSolarIn=");
		builder.append(publishToSolarIn);
		builder.append(", publishToSolarFlux=");
		builder.append(publishToSolarFlux);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the cloud datum stream ID.
	 *
	 * @return the cloud datum stream ID
	 */
	public Long getDatumStreamId() {
		UserLongCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
	}

	@Override
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

	@Override
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
