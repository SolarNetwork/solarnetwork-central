/* ==================================================================
 * CloudDatumStreamConfiguration.java - 29/09/2024 2:35:00 pm
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

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseIdentifiableUserModifiableEntity;
import net.solarnetwork.central.dao.UserRelatedStdIdentifiableConfigurationEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * A cloud datum stream configuration entity.
 *
 * <p>
 * The purpose of this entity is to provide external cloud datum stream schedule
 * and information mapping the cloud data into a datum stream.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "configId", "created", "modified", "enabled", "name", "objectId",
		"sourceId", "propertyMappings", "serviceIdentifier", "serviceProperties" })
public class CloudDatumStreamConfiguration
		extends BaseIdentifiableUserModifiableEntity<CloudDatumStreamConfiguration, UserLongCompositePK>
		implements
		UserRelatedStdIdentifiableConfigurationEntity<CloudDatumStreamConfiguration, UserLongCompositePK> {

	private static final long serialVersionUID = 6232772467622425418L;

	private Long integrationId;
	private ObjectDatumKind kind;
	private Long objectId;
	private String sourceId;
	private List<CloudDatumStreamPropertyMapping> propertyMappings;

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
	public CloudDatumStreamConfiguration(UserLongCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param configId
	 *        the configuration ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CloudDatumStreamConfiguration(Long userId, Long configId, Instant created) {
		this(new UserLongCompositePK(userId, configId), created);
	}

	@Override
	public CloudDatumStreamConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new CloudDatumStreamConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(CloudDatumStreamConfiguration entity) {
		super.copyTo(entity);
		entity.setIntegrationId(integrationId);
		entity.setKind(kind);
		entity.setObjectId(objectId);
		entity.setSourceId(sourceId);
	}

	@Override
	public boolean isSameAs(CloudDatumStreamConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.integrationId, other.getIntegrationId())
				&& Objects.equals(this.kind, other.getKind())
				&& Objects.equals(this.objectId, other.getObjectId())
				&& Objects.equals(this.sourceId, other.getSourceId())
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CloudDatumStream{");
		if ( getUserId() != null ) {
			builder.append("userId=");
			builder.append(getUserId());
			builder.append(", ");
		}
		if ( getConfigId() != null ) {
			builder.append("configId=");
			builder.append(getConfigId());
			builder.append(", ");
		}
		if ( integrationId != null ) {
			builder.append("integrationId=");
			builder.append(integrationId);
			builder.append(", ");
		}
		if ( kind != null ) {
			builder.append("kind=");
			builder.append(kind);
			builder.append(", ");
		}
		if ( objectId != null ) {
			builder.append("objectId=");
			builder.append(objectId);
			builder.append(", ");
		}
		if ( sourceId != null ) {
			builder.append("sourceId=");
			builder.append(sourceId);
			builder.append(", ");
		}
		builder.append("enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the configuration ID
	 */
	public Long getConfigId() {
		UserLongCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
	}

	/**
	 * Get the associated {@link CloudIntegrationConfiguration}
	 * {@code configId}.
	 *
	 * @return the integration ID
	 */
	public final Long getIntegrationId() {
		return integrationId;
	}

	/**
	 * Set the associated {@link CloudIntegrationConfiguration}
	 * {@code configId}.
	 *
	 * @param integrationId
	 *        the integration ID to set
	 */
	public final void setIntegrationId(Long integrationId) {
		this.integrationId = integrationId;
	}

	/**
	 * Get the datum stream kind.
	 *
	 * @return the kind
	 */
	public final ObjectDatumKind getKind() {
		return kind;
	}

	/**
	 * Set the datum stream kind.
	 *
	 * @param kind
	 *        the kind to set
	 */
	public final void setKind(ObjectDatumKind kind) {
		this.kind = kind;
	}

	/**
	 * Get the datum stream object ID.
	 *
	 * @return the object ID
	 */
	public final Long getObjectId() {
		return objectId;
	}

	/**
	 * Set the datum stream object ID.
	 *
	 * @param objectId
	 *        the object ID to set
	 */
	public final void setObjectId(Long nodeId) {
		this.objectId = nodeId;
	}

	/**
	 * Get the datum stream source ID.
	 *
	 * @return the source ID
	 */
	public final String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the datum stream source ID.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public final void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the property mappings.
	 *
	 * @return the property mappings
	 */
	public final List<CloudDatumStreamPropertyMapping> getPropertyMappings() {
		return propertyMappings;
	}

	/**
	 * Set the property mappings.
	 *
	 * @param propertyMappings
	 *        the property mappings to set
	 */
	public final void setPropertyMappings(List<CloudDatumStreamPropertyMapping> propertyMappings) {
		this.propertyMappings = propertyMappings;
	}

}
