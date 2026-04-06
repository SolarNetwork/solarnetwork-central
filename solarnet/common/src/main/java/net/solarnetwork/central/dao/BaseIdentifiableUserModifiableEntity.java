/* ==================================================================
 * BaseIdentifiableUserModifiableEntity.java - 26/09/2024 1:13:54 pm
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

package net.solarnetwork.central.dao;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.util.StringUtils;

/**
 * A base user-related entity that is also an identifiable configuration.
 *
 * @author matt
 * @version 1.1
 */
public abstract class BaseIdentifiableUserModifiableEntity<C extends BaseIdentifiableUserModifiableEntity<C, K>, K extends UserRelatedCompositeKey<K>>
		extends BaseUserModifiableEntity<C, K>
		implements UserRelatedStdIdentifiableConfigurationEntity<C, K> {

	@Serial
	private static final long serialVersionUID = -7821821709345090306L;

	/** The name. */
	private String name;

	/** The service identifier. */
	private String serviceIdentifier;

	/** The service properties as JSON. */
	private @Nullable String servicePropsJson;

	/** The service properties. */
	private transient @Nullable Map<String, Object> serviceProps;

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
	public BaseIdentifiableUserModifiableEntity(K id, Instant created, String name,
			String serviceIdentifier) {
		super(id, created);
		this.name = requireNonNullArgument(name, "name");
		this.serviceIdentifier = requireNonNullArgument(serviceIdentifier, "serviceIdentifier");
	}

	@Override
	public void copyTo(C entity) {
		super.copyTo(entity);
		entity.setName(name);
		entity.setServiceIdentifier(serviceIdentifier);
		entity.setServicePropsJson(servicePropsJson);
	}

	@Override
	public boolean isSameAs(@Nullable C other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final C o = nonnull(other, "other");
		// @formatter:off
		return Objects.equals(this.name, o.getName())
				&& Objects.equals(this.serviceIdentifier, o.getServiceIdentifier())
				// compare decoded JSON, as JSON key order not assumed
				&& Objects.equals(getServiceProperties(), o.getServiceProperties())
				;
		// @formatter:on
	}

	@Override
	public void maskSensitiveInformation(
			@Nullable Function<String, @Nullable Set<String>> sensitiveKeyProvider,
			TextEncryptor encryptor) {
		Set<String> secureKeys = (sensitiveKeyProvider != null && serviceIdentifier != null
				? sensitiveKeyProvider.apply(serviceIdentifier)
				: null);
		if ( secureKeys != null && !secureKeys.isEmpty() ) {
			setServiceProps(SecurityUtils.encryptedMap(getServiceProps(), secureKeys, encryptor));
		}
	}

	@Override
	public void unmaskSensitiveInformation(
			@Nullable Function<String, @Nullable Set<String>> sensitiveKeyProvider,
			TextEncryptor encryptor) {
		Set<String> secureKeys = (sensitiveKeyProvider != null && serviceIdentifier != null
				? sensitiveKeyProvider.apply(serviceIdentifier)
				: null);
		if ( secureKeys != null && !secureKeys.isEmpty() ) {
			setServiceProps(SecurityUtils.decryptedMap(getServiceProps(), secureKeys, encryptor));
		}
	}

	@Override
	public BaseIdentifiableUserModifiableEntity<C, K> digestSensitiveInformation(
			Function<String, @Nullable Set<String>> sensitiveKeyProvider) {
		Set<String> secureKeys = (sensitiveKeyProvider != null && serviceIdentifier != null
				? sensitiveKeyProvider.apply(serviceIdentifier)
				: null);
		if ( secureKeys != null && !secureKeys.isEmpty() ) {
			setServiceProps(StringUtils.sha256MaskedMap(getServiceProps(), secureKeys));
		}
		return this;
	}

	@Override
	public final String getName() {
		return name;
	}

	/**
	 * Set the configuration name.
	 *
	 * @param name
	 *        the name to use
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
	 * Set the unique identifier for the service this configuration is
	 * associated with.
	 *
	 * @param serviceIdentifier
	 *        the identifier of the service to use
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
