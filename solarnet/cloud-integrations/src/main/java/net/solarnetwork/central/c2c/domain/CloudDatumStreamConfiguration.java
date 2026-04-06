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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseIdentifiableUserModifiableEntity;
import net.solarnetwork.central.dao.UserRelatedStdIdentifiableConfigurationEntity;
import net.solarnetwork.central.domain.ObjectDatumIdRelated;
import net.solarnetwork.central.domain.UserIdentifiableSystem;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * A cloud datum stream configuration entity.
 *
 * <p>
 * The purpose of this entity is to provide external cloud datum stream schedule
 * and information mapping the cloud data into a datum stream. An associated
 * {@link CloudDatumStreamMappingConfiguration} defines the actual mapping of
 * cloud data references to datum stream properties.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
@JsonIgnoreProperties({ "id", "fullyConfigured", "datumStreamId" })
@JsonPropertyOrder({ "userId", "configId", "created", "modified", "enabled", "name",
		"datumStreamMappingId", "schedule", "kind", "objectId", "sourceId", "serviceIdentifier",
		"serviceProperties" })
public final class CloudDatumStreamConfiguration
		extends BaseIdentifiableUserModifiableEntity<CloudDatumStreamConfiguration, UserLongCompositePK>
		implements
		CloudIntegrationsConfigurationEntity<CloudDatumStreamConfiguration, UserLongCompositePK>,
		UserRelatedStdIdentifiableConfigurationEntity<CloudDatumStreamConfiguration, UserLongCompositePK>,
		UserIdentifiableSystem, ObjectDatumIdRelated, CloudDatumStreamIdRelated,
		CloudDatumStreamMappingIdRelated {

	/**
	 * A system identifier component included in {@link #systemIdentifier()}.
	 */
	public static final String CLOUD_INTEGRATION_SYSTEM_IDENTIFIER = "c2c-ds";

	@Serial
	private static final long serialVersionUID = 1899493393926823115L;

	private @Nullable Long datumStreamMappingId;
	private @Nullable String schedule;
	private ObjectDatumKind kind;
	private @Nullable Long objectId;
	private @Nullable String sourceId;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the name
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param kind
	 *        the datum kind
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CloudDatumStreamConfiguration(UserLongCompositePK id, Instant created, String name,
			String serviceIdentifier, ObjectDatumKind kind) {
		super(id, created, name, serviceIdentifier);
		this.kind = requireNonNullArgument(kind, "kind");
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
	 * @param name
	 *        the name
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param kind
	 *        the datum kind
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CloudDatumStreamConfiguration(Long userId, Long configId, Instant created, String name,
			String serviceIdentifier, ObjectDatumKind kind) {
		this(new UserLongCompositePK(userId, configId), created, name, serviceIdentifier, kind);
	}

	@Override
	public CloudDatumStreamConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new CloudDatumStreamConfiguration(id, created(), getName(), getServiceIdentifier(),
				kind);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(CloudDatumStreamConfiguration entity) {
		super.copyTo(entity);
		entity.setDatumStreamMappingId(datumStreamMappingId);
		entity.setSchedule(schedule);
		entity.setKind(kind);
		entity.setObjectId(objectId);
		entity.setSourceId(sourceId);
	}

	@Override
	public boolean isSameAs(@Nullable CloudDatumStreamConfiguration other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final var o = nonnull(other, "other");
		// @formatter:off
		return Objects.equals(this.datumStreamMappingId, o.datumStreamMappingId)
				&& Objects.equals(this.schedule, o.schedule)
				&& Objects.equals(this.kind, o.kind)
				&& Objects.equals(this.objectId, o.objectId)
				&& Objects.equals(this.sourceId, o.sourceId)
				;
		// @formatter:on
	}

	@Override
	public boolean isFullyConfigured() {
		return datumStreamMappingId != null && kind != null && objectId != null && sourceId != null
				&& !sourceId.isEmpty();
	}

	/**
	 * Create a datum ID with a given timestamp.
	 *
	 * <p>
	 * The kind, object ID, and source ID values of this configuration will be
	 * used.
	 * </p>
	 *
	 * @param ts
	 *        the desired timestamp of the ID
	 * @return the ID
	 */
	public DatumId datumId(Instant ts) {
		return DatumId.datumId(kind, objectId, sourceId, ts);
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
		if ( getName() != null ) {
			builder.append("name=");
			builder.append(getName());
			builder.append(", ");
		}
		if ( datumStreamMappingId != null ) {
			builder.append("datumStreamMappingId=");
			builder.append(datumStreamMappingId);
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
	 * Get a unique identifier based on this configuration.
	 *
	 * <p>
	 * The identifier follows this syntax:
	 * </p>
	 *
	 * <pre>{@code
	 * USER_ID:c2c-ds:CONFIG_ID
	 * }</pre>
	 *
	 * <p>
	 * Where {@code USER_ID} is {@link #getUserId()} and {@code CONFIG_ID} is
	 * {@link #getConfigId()}.
	 * </p>
	 *
	 * @return the system identifier
	 * @see #CLOUD_INTEGRATION_SYSTEM_IDENTIFIER
	 */
	@Override
	public final String systemIdentifier() {
		return systemIdentifierForComponents(CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, getConfigId());
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the configuration ID
	 */
	public final Long getConfigId() {
		return id().getEntityId();
	}

	@Override
	public final Long getDatumStreamId() {
		return getConfigId();
	}

	/**
	 * Get the associated {@link CloudDatumStreamMappingConfiguration}
	 * {@code configId}.
	 *
	 * @return the datum stream mapping ID
	 */
	@Override
	public final @Nullable Long getDatumStreamMappingId() {
		return datumStreamMappingId;
	}

	/**
	 * Set the associated {@link CloudDatumStreamMappingConfiguration}
	 * {@code configId}.
	 *
	 * @param datumStreamMappingId
	 *        the integration ID to set
	 */
	public final void setDatumStreamMappingId(@Nullable Long datumStreamMappingId) {
		this.datumStreamMappingId = datumStreamMappingId;
	}

	/**
	 * Get the schedule at which to poll for data.
	 *
	 * @return the schedule, as either a cron schedule or a number of seconds,
	 *         or {@code null} if polling is not used
	 */
	public final @Nullable String getSchedule() {
		return schedule;
	}

	/**
	 * Set the schedule at which to pull data.
	 *
	 * @param schedule
	 *        the schedule to set, as either a cron schedule or a number of
	 *        seconds, or {@code null} if polling is not used
	 */
	public final void setSchedule(@Nullable String schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the datum stream kind.
	 *
	 * @return the kind
	 */
	@Override
	public final ObjectDatumKind getKind() {
		return kind;
	}

	/**
	 * Set the datum stream kind.
	 *
	 * @param kind
	 *        the kind to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setKind(ObjectDatumKind kind) {
		this.kind = requireNonNullArgument(kind, "kind");
	}

	/**
	 * Get the datum stream object ID.
	 *
	 * @return the object ID
	 */
	@Override
	public final @Nullable Long getObjectId() {
		return objectId;
	}

	/**
	 * Set the datum stream object ID.
	 *
	 * @param objectId
	 *        the object ID to set
	 */
	public final void setObjectId(@Nullable Long objectId) {
		this.objectId = objectId;
	}

	/**
	 * Get the datum stream source ID.
	 *
	 * @return the source ID
	 */
	public final @Nullable String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the datum stream source ID.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public final void setSourceId(@Nullable String sourceId) {
		this.sourceId = sourceId;
	}

}
