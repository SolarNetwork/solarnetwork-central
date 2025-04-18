/* ==================================================================
 * TransformConfiguration.java - 28/03/2024 11:21:47 am
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

package net.solarnetwork.central.inin.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.service.IdentifiableConfiguration;

/**
 * Transform configuration.
 *
 * @param <C>
 *        the transform configuration type
 * @author matt
 * @version 1.0
 */
public abstract sealed class TransformConfiguration<C extends TransformConfiguration<C>>
		extends BaseUserModifiableEntity<C, UserLongCompositePK>
		implements InstructionInputConfigurationEntity<C, UserLongCompositePK>, IdentifiableConfiguration
		permits TransformConfiguration.RequestTransformConfiguration,
		TransformConfiguration.ResponseTransformConfiguration {

	@Serial
	private static final long serialVersionUID = -2206409216936875018L;

	private final TransformPhase phase;
	private String name;
	private String serviceIdentifier;
	private String servicePropsJson;

	private Map<String, Object> serviceProps;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param phase
	 *        the phase
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	protected TransformConfiguration(UserLongCompositePK id, Instant created, TransformPhase phase) {
		super(id, created);
		this.phase = requireNonNullArgument(phase, "phase");
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
	 * @param phase
	 *        the phase
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	protected TransformConfiguration(Long userId, Long transformId, Instant created,
			TransformPhase phase) {
		this(new UserLongCompositePK(userId, transformId), created, phase);
	}

	/**
	 * A request transform configuration.
	 */
	@JsonIgnoreProperties({ "id", "serviceProps" })
	@JsonPropertyOrder({ "userId", "transformId", "created", "modified", "enabled", "name", "phase",
			"serviceIdentifier", "serviceProperties" })
	public static final class RequestTransformConfiguration
			extends TransformConfiguration<RequestTransformConfiguration> {

		@Serial
		private static final long serialVersionUID = -2376857425680066351L;

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
		public RequestTransformConfiguration(UserLongCompositePK id, Instant created) {
			super(id, created, TransformPhase.Request);
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
		 * @throws IllegalArgumentException
		 *         if any argument is {@literal null}
		 */
		public RequestTransformConfiguration(Long userId, Long transformId, Instant created) {
			this(new UserLongCompositePK(userId, transformId), created);
		}

		@Override
		public RequestTransformConfiguration copyWithId(UserLongCompositePK id) {
			var copy = new RequestTransformConfiguration(id, getCreated());
			copyTo(copy);
			return copy;
		}

	}

	/**
	 * A request transform configuration.
	 */
	@JsonIgnoreProperties({ "id", "serviceProps" })
	@JsonPropertyOrder({ "userId", "transformId", "created", "modified", "enabled", "name", "phase",
			"serviceIdentifier", "serviceProperties" })
	public static final class ResponseTransformConfiguration
			extends TransformConfiguration<ResponseTransformConfiguration> {

		@Serial
		private static final long serialVersionUID = -7774474475322183244L;

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
		public ResponseTransformConfiguration(UserLongCompositePK id, Instant created) {
			super(id, created, TransformPhase.Response);
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
		 * @throws IllegalArgumentException
		 *         if any argument is {@literal null}
		 */
		public ResponseTransformConfiguration(Long userId, Long transformId, Instant created) {
			this(new UserLongCompositePK(userId, transformId), created);
		}

		@Override
		public ResponseTransformConfiguration copyWithId(UserLongCompositePK id) {
			var copy = new ResponseTransformConfiguration(id, getCreated());
			copyTo(copy);
			return copy;
		}

	}

	@Override
	public void copyTo(C entity) {
		super.copyTo(entity);
		entity.setName(name);
		entity.setServiceIdentifier(serviceIdentifier);
		entity.setServicePropsJson(servicePropsJson);
	}

	@Override
	public boolean isSameAs(C other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.name, other.getName())
				&& Objects.equals(this.phase, other.getPhase())
				&& Objects.equals(this.serviceIdentifier, other.getServiceIdentifier())
				&& Objects.equals(this.servicePropsJson, other.getServicePropsJson())
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
		if ( phase != null ) {
			builder.append("phase=");
			builder.append(phase);
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
	public Long getTransformId() {
		UserLongCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the phase.
	 *
	 * @return the phase
	 */
	public TransformPhase getPhase() {
		return phase;
	}

	@Override
	public String getServiceIdentifier() {
		return serviceIdentifier;
	}

	/**
	 * Set the identifier of the transform service to use.
	 *
	 * @param serviceIdentifier
	 *        the identifier to use
	 */
	public void setServiceIdentifier(String serviceIdentifier) {
		this.serviceIdentifier = serviceIdentifier;
	}

	/**
	 * Get the service properties object as a JSON string.
	 *
	 * @return a JSON encoded string, or {@literal null} if no service
	 *         properties available
	 */
	@JsonIgnore
	public String getServicePropsJson() {
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
	public void setServicePropsJson(String json) {
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
	public Map<String, Object> getServiceProps() {
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
	public void setServiceProps(Map<String, Object> serviceProps) {
		this.serviceProps = serviceProps;
		servicePropsJson = JsonUtils.getJSONString(serviceProps, null);
	}

	@Override
	public Map<String, ?> getServiceProperties() {
		return getServiceProps();
	}

}
