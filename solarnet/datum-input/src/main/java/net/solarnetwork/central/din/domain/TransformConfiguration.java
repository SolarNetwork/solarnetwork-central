/* ==================================================================
 * TransformConfiguration.java - 20/02/2024 5:26:54 pm
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

package net.solarnetwork.central.din.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.service.IdentifiableConfiguration;

/**
 * Transform configuration.
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id", "serviceProps" })
@JsonPropertyOrder({ "userId", "transformId", "created", "modified", "enabled", "name",
		"serviceIdentifier", "serviceProperties" })
public class TransformConfiguration
		extends BaseUserModifiableEntity<TransformConfiguration, UserLongCompositePK>
		implements DatumInputConfigurationEntity<TransformConfiguration, UserLongCompositePK>,
		IdentifiableConfiguration {

	@Serial
	private static final long serialVersionUID = -4398071329872156479L;

	private String name;
	private String serviceIdentifier;
	private @Nullable String servicePropsJson;

	private @Nullable Map<String, Object> serviceProps;

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
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public TransformConfiguration(UserLongCompositePK id, Instant created, String name,
			String serviceIdentifier) {
		super(id, created);
		this.name = requireNonNullArgument(name, "name");
		this.serviceIdentifier = requireNonNullArgument(serviceIdentifier, "serviceIdentifier");
		setEnabled(true);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param transformId
	 *        the transform ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the name
	 * @param serviceIdentifier
	 *        the service identifier
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public TransformConfiguration(Long userId, Long transformId, Instant created, String name,
			String serviceIdentifier) {
		this(new UserLongCompositePK(userId, transformId), created, name, serviceIdentifier);
	}

	@Override
	public TransformConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new TransformConfiguration(id, nonnull(getCreated(), "created"), name,
				serviceIdentifier);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(TransformConfiguration entity) {
		super.copyTo(entity);
		entity.setName(name);
		entity.setServiceIdentifier(serviceIdentifier);
		entity.setServicePropsJson(servicePropsJson);
	}

	@Override
	public boolean isSameAs(@Nullable TransformConfiguration other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final var o = nonnull(other, "other");
		// @formatter:off
		return Objects.equals(this.name, o.name)
				&& Objects.equals(this.serviceIdentifier, o.serviceIdentifier)
				&& Objects.equals(this.servicePropsJson, o.servicePropsJson)
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Transform{");
		if ( getUserId() != null ) {
			builder.append("userId=");
			builder.append(getUserId());
			builder.append(", ");
		}
		if ( getTransformId() != null ) {
			builder.append("transformId=");
			builder.append(getTransformId());
			builder.append(", ");
		}
		if ( name != null ) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if ( serviceIdentifier != null ) {
			builder.append("serviceIdentifier=");
			builder.append(serviceIdentifier);
			builder.append(", ");
		}
		builder.append("enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the transform ID.
	 *
	 * @return the endpoint ID
	 */
	public final Long getTransformId() {
		return pk().getEntityId();
	}

	@Override
	public final String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setName(String name) {
		this.name = requireNonNullArgument(name, "name");
	}

	@Override
	public final String getServiceIdentifier() {
		return serviceIdentifier;
	}

	/**
	 * Set the identifier of the
	 * {@link net.solarnetwork.central.din.biz.TransformService} to use.
	 *
	 * @param serviceIdentifier
	 *        the identifier to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setServiceIdentifier(String serviceIdentifier) {
		this.serviceIdentifier = requireNonNullArgument(serviceIdentifier, "serviceIdentifier");
	}

	/**
	 * Get the service properties object as a JSON string.
	 *
	 * @return a JSON encoded string, or {@code null} if no service properties
	 *         available
	 */
	@JsonIgnore
	public final @Nullable String getServicePropsJson() {
		return servicePropsJson;
	}

	/**
	 * Set the service properties object via a JSON string.
	 *
	 * <p>
	 * This method will remove any previously created service properties and
	 * replace it with the values parsed from the JSON. All floating point
	 * values will be converted to {@link BigDecimal} instances.
	 * </p>
	 *
	 * @param json
	 *        the JSON to parse as service properties
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public final void setServicePropsJson(@Nullable String json) {
		servicePropsJson = json;
		serviceProps = null;
	}

	/**
	 * Get the service properties.
	 *
	 * <p>
	 * This will decode the {@link #getServicePropsJson()} value into a map
	 * instance.
	 * </p>
	 *
	 * @return the service properties
	 */
	@JsonIgnore
	public final @Nullable Map<String, Object> getServiceProps() {
		if ( serviceProps == null && servicePropsJson != null ) {
			serviceProps = JsonUtils.getStringMap(servicePropsJson);
		}
		return serviceProps;
	}

	/**
	 * Set the service properties to use.
	 *
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setServicePropsJson(String)} as well.
	 * </p>
	 *
	 * @param serviceProps
	 *        the service properties to set
	 */
	@JsonSetter("serviceProperties")
	public final void setServiceProps(@Nullable Map<String, Object> serviceProps) {
		this.serviceProps = serviceProps;
		servicePropsJson = JsonUtils.getJSONString(serviceProps, null);
	}

	@Override
	public final @Nullable Map<String, ?> getServiceProperties() {
		return getServiceProps();
	}
}
