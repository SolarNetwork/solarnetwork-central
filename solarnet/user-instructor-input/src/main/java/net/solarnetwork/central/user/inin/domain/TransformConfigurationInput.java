/* ==================================================================
 * TransformConfigurationInput.java - 25/02/2024 7:42:20 am
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

package net.solarnetwork.central.user.inin.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.inin.domain.TransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;

/**
 * Base DTO for datum input transform configuration.
 *
 * @param <C>
 *        the configuration type
 * @author matt
 * @version 1.0
 */
public abstract sealed class TransformConfigurationInput<C extends TransformConfiguration<C>>
		extends BaseInstructionInputConfigurationInput<C, UserLongCompositePK>
		permits TransformConfigurationInput.RequestTransformConfigurationInput,
		TransformConfigurationInput.ResponseTransformConfigurationInput {

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String name;

	@NotNull
	@NotBlank
	@Size(max = 128)
	private String serviceIdentifier;

	private Map<String, Object> serviceProperties;

	/**
	 * Constructor.
	 */
	protected TransformConfigurationInput() {
		super();
	}

	@Override
	protected void populateConfiguration(C conf) {
		super.populateConfiguration(conf);
		conf.setName(name);
		conf.setServiceIdentifier(serviceIdentifier);
		conf.setServiceProps(serviceProperties);
	}

	/**
	 * DTO for datum input request transform configuration.
	 */
	public static final class RequestTransformConfigurationInput
			extends TransformConfigurationInput<RequestTransformConfiguration> {

		/**
		 * Constructor.
		 */
		public RequestTransformConfigurationInput() {
			super();
		}

		@Override
		public RequestTransformConfiguration toEntity(UserLongCompositePK id, Instant date) {
			RequestTransformConfiguration conf = new RequestTransformConfiguration(
					requireNonNullArgument(id, "id"), date);
			populateConfiguration(conf);
			return conf;
		}

	}

	/**
	 * DTO for datum input response transform configuration.
	 */
	public static final class ResponseTransformConfigurationInput
			extends TransformConfigurationInput<ResponseTransformConfiguration> {

		/**
		 * Constructor.
		 */
		public ResponseTransformConfigurationInput() {
			super();
		}

		@Override
		public ResponseTransformConfiguration toEntity(UserLongCompositePK id, Instant date) {
			ResponseTransformConfiguration conf = new ResponseTransformConfiguration(
					requireNonNullArgument(id, "id"), date);
			populateConfiguration(conf);
			return conf;
		}

	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
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
	 * Get the identifier of the transform service to use.
	 *
	 * @return the identifier
	 */
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
	 * Get the service properties.
	 *
	 * @return the service properties
	 */
	public Map<String, Object> getServiceProperties() {
		return serviceProperties;
	}

	/**
	 * Set the service properties to use.
	 *
	 * @param serviceProperties
	 *        the service properties to set
	 */
	public void setServiceProperties(Map<String, Object> serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

}
